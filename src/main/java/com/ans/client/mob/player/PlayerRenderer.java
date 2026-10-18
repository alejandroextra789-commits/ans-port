package com.ans.client.mob.player;

import com.ans.client.bedrock.BonePattern;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.bedrock.RenderKind;
import com.ans.client.bedrock.RenderLayer;
import com.ans.client.mob.AnimatedMob;
import com.ans.client.render.LayerSubmitter;
import com.ans.client.render.ReplacedMobRenderer;
import com.ans.client.render.UiRender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Player renderer for the 1.20.1 port. Reads the live player instead of a GeckoLib 5 render state:
 * hidden skin parts, the cape layer, distance-based camera fade and the fall-flying pose.
 *
 * Not yet ported: wings, shoulder parrots and paper-doll preview (they were separate render layers
 * in GeckoLib 5 and need their own rewrite).
 */
public final class PlayerRenderer extends ReplacedMobRenderer<Player> {
   private static final List<RenderLayer.MaterialRule> CAPE_MATERIALS = List.of(new RenderLayer.MaterialRule(new BonePattern("*"), RenderKind.CUTOUT));
   public static boolean cameraFadeEnabled = !"false".equalsIgnoreCase(System.getProperty("ans.cameraFade", "true"));
   static final double FADE_DISTANCE = 0.8;
   static final float FADE_MIN = 0.15F;
   private static final PlayerModelPart[] SKIN_PARTS = new PlayerModelPart[]{
      PlayerModelPart.HAT,
      PlayerModelPart.JACKET,
      PlayerModelPart.LEFT_SLEEVE,
      PlayerModelPart.RIGHT_SLEEVE,
      PlayerModelPart.LEFT_PANTS_LEG,
      PlayerModelPart.RIGHT_PANTS_LEG
   };
   private static final String[] SKIN_BONES = new String[]{"hat", "jacket", "leftsleeve", "rightsleeve", "leftpants", "rightpants"};
   static final Map<String, ResourceLocation> TEST_CAPES = new ConcurrentHashMap<>();

   public PlayerRenderer(Context context, AnimatedMob<Player> mob) {
      super(context, mob, null);
      this.shadowRadius = 0.5F;
   }

   @Override
   public void render(Player entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
      float fade = entity instanceof AbstractClientPlayer p && !UiRender.active() ? cameraFade(p, partialTick) : 1.0F;
      float previous = LayerSubmitter.fade(fade);
      try {
         super.render(entity, entityYaw, partialTick, poseStack, buffer, light);
      } finally {
         LayerSubmitter.fade(previous);
      }
   }

   static float cameraFade(AbstractClientPlayer player, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (cameraFadeEnabled && player == mc.getCameraEntity() && !player.isSpectator()) {
         Camera camera = mc.gameRenderer.getMainCamera();
         if (!camera.isDetached()) {
            return 1.0F;
         }

         Vec3 at = player.getPosition(partialTick);
         AABB box = player.getBoundingBox().move(at.subtract(player.position()));
         Vec3 c = camera.getPosition();
         double dx = Math.max(Math.max(box.minX - c.x, 0.0), c.x - box.maxX);
         double dy = Math.max(Math.max(box.minY - c.y, 0.0), c.y - box.maxY);
         double dz = Math.max(Math.max(box.minZ - c.z, 0.0), c.z - box.maxZ);
         double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
         return d >= FADE_DISTANCE ? 1.0F : (float)Mth.clamp(FADE_MIN + (1.0F - FADE_MIN) * d / FADE_DISTANCE, FADE_MIN, 1.0F);
      }
      return 1.0F;
   }

   @Override
   protected FrameResult adjustFrame(Player entity, FrameResult frame, float partialTick) {
      if (!(entity instanceof AbstractClientPlayer player)) {
         return frame;
      }

      List<RenderLayer> layers = hideModelParts(player, frame.layers());
      layers = addCape(player, frame, layers);
      return new FrameResult(frame.pose(), List.copyOf(layers), frame.scaleX(), frame.scaleY(), frame.scaleZ());
   }

   @Override
   protected void adjustBody(Player entity, float partialTick, PoseStack poseStack) {
      if (entity.getPose() == Pose.SLEEPING && entity.deathTime <= 0) {
         return;
      }

      if (entity.isFallFlying() && !entity.isAutoSpinAttack() && entity.deathTime <= 0) {
         float t = entity.getFallFlyingTicks() + partialTick;
         float scale = Mth.clamp(t * t / 100.0F, 0.0F, 1.0F);
         poseStack.mulPose(Axis.XP.rotationDegrees(scale * (-90.0F - entity.getXRot())));
      }
   }

   /** Removes the body layers for skin parts the player has turned off. */
   static List<RenderLayer> hideModelParts(AbstractClientPlayer p, List<RenderLayer> layers) {
      List<RenderLayer.VisibilityRule> hidden = null;
      for (int i = 0; i < SKIN_PARTS.length; i++) {
         if (!p.isModelPartShown(SKIN_PARTS[i])) {
            if (hidden == null) {
               hidden = new ArrayList<>();
            }
            hidden.add(new RenderLayer.VisibilityRule(new BonePattern(SKIN_BONES[i]), false));
         }
      }

      if (hidden == null) {
         return layers;
      }

      List<RenderLayer> out = new ArrayList<>(layers.size());
      for (RenderLayer l : layers) {
         if (!l.model().equals(PlayerRendering.WIDE_GEOMETRY) && !l.model().equals(PlayerRendering.SLIM_GEOMETRY)) {
            out.add(l);
            continue;
         }

         List<RenderLayer.VisibilityRule> vis = new ArrayList<>(l.visibility());
         vis.addAll(hidden);
         out.add(new RenderLayer(
            l.model(),
            l.texture(),
            l.materials(),
            List.copyOf(vis),
            l.tint(),
            l.hurtOverlay(),
            l.ignoreLighting(),
            l.uvOffsetU(),
            l.uvOffsetV(),
            l.changeColor(),
            l.overlay(),
            l.uvScaleU(),
            l.uvScaleV()
         ));
      }
      return out;
   }

   /** Adds the cape layer when the body is visible and the player shows a cape. */
   private static List<RenderLayer> addCape(AbstractClientPlayer player, FrameResult frame, List<RenderLayer> layers) {
      ResourceLocation cape = player.getCloakTextureLocation();
      ResourceLocation testCape = TEST_CAPES.isEmpty() ? null : TEST_CAPES.get(player.getGameProfile().getName());
      if (testCape != null) {
         cape = testCape;
      }

      if (cape == null || !player.isModelPartShown(PlayerModelPart.CAPE) || player.isInvisible() || player.isSpectator() || layers.isEmpty()) {
         return layers;
      }

      var chest = player.getItemBySlot(EquipmentSlot.CHEST);
      if (chest.getItem() instanceof ElytraItem) {
         return layers;
      }

      boolean body = false;
      for (RenderLayer l : layers) {
         body |= l.model().equals(PlayerRendering.WIDE_GEOMETRY) || l.model().equals(PlayerRendering.SLIM_GEOMETRY);
      }
      if (!body) {
         return layers;
      }

      List<RenderLayer> out = new ArrayList<>(layers);
      out.add(new RenderLayer(PlayerRendering.CAPE_GEOMETRY, cape, CAPE_MATERIALS, List.of(), -1, true, false));
      if (chest.getItem() instanceof ArmorItem) {
         float[] bone = frame.pose().bones().computeIfAbsent("cape", var0 -> new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F});
         bone[4] += 0.85F;
         bone[5] += 1.1F;
      }
      return out;
   }
}
