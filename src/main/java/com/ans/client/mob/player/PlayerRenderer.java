package com.ans.client.mob.player;

import com.ans.client.bedrock.BoneLookup;

import net.minecraft.util.Mth;
import com.ans.client.bedrock.BonePattern;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.bedrock.RenderKind;
import com.ans.client.bedrock.RenderLayer;
import com.ans.client.mob.AnimatedMob;
import com.ans.client.render.LayerSubmitter;
import com.ans.client.render.MobAnimatable;
import com.ans.client.render.ReplacedMobRenderer;
import com.ans.client.render.UiRender;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
      this.withRenderLayer(new PlayerWingsLayer(this, context));
      this.withRenderLayer(new ShoulderParrotLayer(this, context));
   }

   public LivingEntityRenderState createRenderState(MobAnimatable animatable, Player entity) {
      return new PlayerRenderState();
   }

   public void addRenderData(MobAnimatable animatable, Player entity, LivingEntityRenderState state, float partialTick) {
      PlayerRenderState ps = state instanceof PlayerRenderState s ? s : null;
      AbstractClientPlayer player = entity instanceof AbstractClientPlayer p ? p : null;
      if (ps != null && player != null) {
         extract(player, ps, partialTick);
         ps.cameraFade = 1.0F;
      }

      if (ps != null && player != null && UiRender.active()) {
         PaperDoll.tick(this.mob, player, ps, Minecraft.getInstance().getFrameTime());
      } else {
         super.addRenderData(animatable, entity, state, partialTick);
      }

      if (ps != null && player != null) {
         hideModelParts(player, ps);
         this.addCape(player, ps);
         if (!UiRender.active()) {
            ps.cameraFade = cameraFade(player, partialTick);
         }
      }
   }

   static float cameraFade(AbstractClientPlayer player, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (cameraFadeEnabled && player == mc.getCameraEntity() && !player.isSpectator()) {
         Camera camera = mc.gameRenderer.mainCamera();
         if (!camera.isDetached()) {
            return 1.0F;
         }

         Vec3 at = player.getPosition(partialTick);
         AABB box = player.getBoundingBox().move(at.subtract(player.position()));
         Vec3 c = camera.position();
         double dx = Math.max(Math.max(box.minX - c.x, 0.0), c.x - box.maxX);
         double dy = Math.max(Math.max(box.minY - c.y, 0.0), c.y - box.maxY);
         double dz = Math.max(Math.max(box.minZ - c.z, 0.0), c.z - box.maxZ);
         double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
         return d >= 0.8 ? 1.0F : (float)Mth.clamp(0.15F + 0.85F * d / 0.8, 0.15F, 1.0);
      } else {
         return 1.0F;
      }
   }

   public void submit(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState camera) {
      float fade = state instanceof PlayerRenderState ps ? ps.cameraFade : 1.0F;
      if (fade >= 1.0F) {
         super.submit(state, poseStack, renderTasks, camera);
      } else {
         float previous = LayerSubmitter.fade(fade);

         try {
            super.submit(state, poseStack, renderTasks, camera);
         } finally {
            LayerSubmitter.fade(previous);
         }
      }
   }

   static void hideModelParts(AbstractClientPlayer p, PlayerRenderState s) {
      FrameResult frame = (FrameResult)s.getGeckolibData(FrameResult.TICKET);
      if (frame != null) {
         List<RenderLayer.VisibilityRule> hidden = null;

         for (int i = 0; i < SKIN_PARTS.length; i++) {
            if (!p.isModelPartShown(SKIN_PARTS[i])) {
               if (hidden == null) {
                  hidden = new ArrayList<>();
               }

               hidden.add(new RenderLayer.VisibilityRule(new BonePattern(SKIN_BONES[i]), false));
            }
         }

         if (hidden != null) {
            List<RenderLayer> layers = new ArrayList<>(frame.layers().size());

            for (RenderLayer l : frame.layers()) {
               if (!l.model().equals(PlayerRendering.WIDE_GEOMETRY) && !l.model().equals(PlayerRendering.SLIM_GEOMETRY)) {
                  layers.add(l);
               } else {
                  List<RenderLayer.VisibilityRule> vis = new ArrayList<>(l.visibility());
                  vis.addAll(hidden);
                  layers.add(
                     new RenderLayer(
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
                     )
                  );
               }
            }

            s.addGeckolibData(FrameResult.TICKET, new FrameResult(frame.pose(), List.copyOf(layers), frame.scaleX(), frame.scaleY(), frame.scaleZ()));
         }
      }
   }

   private static void extract(AbstractClientPlayer p, PlayerRenderState s, float pt) {
      s.skin = p.getSkinTextureLocation();
      s.cape = p.getCloakTextureLocation();
      s.elytra = p.getElytraTextureLocation();
      s.modelName = p.getModelName();
      ResourceLocation testCape = TEST_CAPES.isEmpty() ? null : TEST_CAPES.get(p.getGameProfile().getName());
      if (testCape != null) {
         s.cape = testCape;
      }

      s.showCape = p.isModelPartShown(PlayerModelPart.CAPE);
      s.isSpectator = p.isSpectator();
      s.parrotOnLeftShoulder = p.getParrotVariantOnShoulder(true);
      s.parrotOnRightShoulder = p.getParrotVariantOnShoulder(false);
      s.mainArm = p.getMainArm();
      s.isCrouching = p.isCrouching();
      s.isFallFlying = p.isFallFlying();
      s.fallFlyingTimeInTicks = p.getFallFlyingTicks() + pt;
      s.isVisuallySwimming = p.isVisuallySwimming();
      s.isPassenger = p.isPassenger();
      s.speedValue = PlayerQueries.glidingSpeed(p);
      s.swimAmount = p.getSwimAmount(pt);
      s.useItemHand = p.getUsedItemHand();
      s.maxCrossbowChargeDuration = CrossbowItem.getChargeDuration(p.getUseItem(), p);
      s.ticksUsingItem = p.getTicksUsingItem(pt);
      s.isUsingItem = p.isUsingItem();
      s.elytraRotX = p.elytraAnimationState.getRotX(pt);
      s.elytraRotY = p.elytraAnimationState.getRotY(pt);
      s.elytraRotZ = p.elytraAnimationState.getRotZ(pt);
      s.headEquipment = p.getItemBySlot(EquipmentSlot.HEAD).copy();
      s.chestEquipment = p.getItemBySlot(EquipmentSlot.CHEST).copy();
      s.legsEquipment = p.getItemBySlot(EquipmentSlot.LEGS).copy();
      s.feetEquipment = p.getItemBySlot(EquipmentSlot.FEET).copy();
   }

   private void addCape(AbstractClientPlayer player, PlayerRenderState s) {
      FrameResult frame = (FrameResult)s.getGeckolibData(FrameResult.TICKET);
      if (frame != null && s.cape != null && s.showCape && !s.isInvisible && !s.isSpectator && !frame.layers().isEmpty()) {
         if (!(s.chestEquipment.getItem() instanceof ElytraItem)) {
            boolean body = false;

            for (RenderLayer l : frame.layers()) {
               body |= l.model().equals(PlayerRendering.WIDE_GEOMETRY) || l.model().equals(PlayerRendering.SLIM_GEOMETRY);
            }

            if (body) {
               List<RenderLayer> layers = new ArrayList<>(frame.layers());
               layers.add(new RenderLayer(PlayerRendering.CAPE_GEOMETRY, s.cape, CAPE_MATERIALS, List.of(), -1, true, false));
               if (s.chestEquipment.getItem() instanceof ArmorItem) {
                  float[] cape = frame.pose().bones().computeIfAbsent("cape", var0 -> new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F});
                  cape[4] += 0.85F;
                  cape[5] += 1.1F;
               }

               s.addGeckolibData(FrameResult.TICKET, new FrameResult(frame.pose(), List.copyOf(layers), frame.scaleX(), frame.scaleY(), frame.scaleZ()));
            }
         }
      }
   }


   @Override
   protected void applyRotations(RenderPassInfo<LivingEntityRenderState> info, PoseStack poseStack, float nativeScale) {
      LivingEntityRenderState state = (LivingEntityRenderState)info.renderState();
      if (state.getGeckolibData(DataTickets.ENTITY_POSE) == Pose.SLEEPING && state.deathTime <= 0.0F) {
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - (Float)state.getOrDefaultGeckolibData(DataTickets.ENTITY_BODY_YAW, 0.0F)));
      } else {
         super.applyRotations(info, poseStack, nativeScale);
         if (state instanceof PlayerRenderState ps && ps.isFallFlying && !ps.isAutoSpinAttack && state.deathTime <= 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(ps.fallFlyingScale() * (-90.0F - ps.xRot)));
         }
      }
   }

   @Override
   public void adjustRenderPose(RenderPassInfo<LivingEntityRenderState> info) {
      this.applyRotations(info, info.poseStack(), ((LivingEntityRenderState)info.renderState()).scale);
      info.poseStack().translate(0.0F, 0.01F, 0.0F);
   }

   public void applyRenderLayers(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector renderTasks) {
      if (!(info.renderState() instanceof PlayerRenderState s && s.isSpectator)) {
         super.applyRenderLayers(info, renderTasks);
      }
   }

   static GeoBone bone(GeoModel<?> geoModel, FrameResult frame, String name) {
      for (RenderLayer layer : frame.layers()) {
         BakedGeoModel model = geoModel.getBakedModel(layer.model());
         if (!BoneLookup.isMissing(model)) {
            Optional<GeoBone> bone = model.getBone(name);
            if (bone.isPresent()) {
               return bone.get();
            }
         }
      }

      return null;
   }
}
