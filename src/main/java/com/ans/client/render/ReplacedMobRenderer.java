package com.ans.client.render;

import com.ans.Ans;
import com.ans.client.bedrock.EntityBundle;
import com.ans.client.bedrock.EntityBundles;
import com.ans.client.bedrock.EntityRuntime;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.equipment.EquipmentLayer;
import com.ans.client.equipment.MobEquipment;
import com.ans.client.mob.AnimatedMob;
import com.ans.client.util.ArgbCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

/**
 * Replaced living-mob renderer, ported to GeckoLib 4.
 *
 * Same flow as ReplacedEntityRenderer, plus: baby scale, bed orientation while sleeping,
 * and the equipment layer. Death rotation is not applied on the Bedrock path yet.
 */
public class ReplacedMobRenderer<E extends LivingEntity> extends GeoReplacedEntityRenderer<E, MobAnimatable> {
   private static final int FULL_BRIGHT = 15728880;
   protected final AnimatedMob<E> mob;
   private final EquipmentLayer<E> equipment;
   private final Map<Entity, EntityRuntime> runtimes = new WeakHashMap<>();
   private boolean failed;
   public static boolean debug;
   public static final Map<Integer, EntityRuntime> DEBUG_RUNTIMES = new ConcurrentHashMap<>();
   static final boolean BEDROCK_BED_POSITION = !"false".equalsIgnoreCase(System.getProperty("ans.bedrockBedPosition", "true"));
   private float lastScale = 1.0F;

   public ReplacedMobRenderer(Context context, AnimatedMob<E> mob, EntityRendererProvider<E> vanilla) {
      super(context, new MobGeoModel(mob), new MobAnimatable(mob));
      this.mob = mob;
      this.shadowRadius = Float.isNaN(mob.shadowRadius()) ? vanillaShadow(context, vanilla) : mob.shadowRadius();
      this.equipment = new EquipmentLayer<>(context, MobEquipment.get(mob.type()));
   }

   private static float vanillaShadow(Context context, EntityRendererProvider<?> vanilla) {
      if (vanilla == null) {
         return 0.5F;
      }
      try {
         EntityRenderer<?> renderer = vanilla.create(context);
         Field f = EntityRenderer.class.getDeclaredField("shadowRadius");
         f.setAccessible(true);
         return f.getFloat(renderer);
      } catch (ReflectiveOperationException | RuntimeException e) {
         return 0.5F;
      }
   }

   @Override
   public void render(E entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
      FrameResult frame = entity == null ? null : computeFrame(entity, partialTick);
      if (frame != null) {
         frame = this.adjustFrame(entity, frame, partialTick);
      }
      if (frame == null) {
         super.render(entity, entityYaw, partialTick, poseStack, buffer, light);
         return;
      }

      float age = entity instanceof AgeableMob a && a.isBaby() ? 0.5F : 1.0F;
      poseStack.pushPose();
      try {
         applyBodyTransform(entity, partialTick, poseStack, age, frame);
         final float pt = partialTick;
         LayerSubmitter.submit(
            poseStack,
            buffer,
            this.getGeoModel(),
            frame,
            ArgbCompat.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F),
            light,
            OverlayTexture.NO_OVERLAY,
            false,
            null,
            null,
            (model, type, vc, l, o, c) -> this.actuallyRender(
               poseStack, this.getAnimatable(), model, type, buffer, vc, false, pt, l, o,
               ArgbCompat.red(c) / 255.0F, ArgbCompat.green(c) / 255.0F, ArgbCompat.blue(c) / 255.0F, ArgbCompat.alpha(c) / 255.0F
            )
         );
         this.equipment.render(frame, entity, this.getGeoModel(), poseStack, buffer, light, partialTick);
      } finally {
         poseStack.popPose();
      }
   }

   /** Yaw (or bed orientation while sleeping), baby scale, and Bedrock frame scale. */
   private void applyBodyTransform(E entity, float partialTick, PoseStack poseStack, float age, FrameResult frame) {
      Direction bed = entity.getPose() == Pose.SLEEPING && entity.deathTime <= 0 ? entity.getBedOrientation() : null;
      if (BEDROCK_BED_POSITION && bed != null) {
         float sx = Math.max(1.0E-4F, age * frame.scaleX());
         float sz = Math.max(1.0E-4F, age * frame.scaleZ());
         poseStack.translate(-bed.getStepX() * 0.5F / sx, 0.0F, -bed.getStepZ() * 0.5F / sz);
         poseStack.mulPose(Axis.YP.rotationDegrees(-bed.toYRot()));
         poseStack.translate(0.0F, 0.01F, 0.0F);
      } else {
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - this.mob.yaw(entity, partialTick)));
      }
      this.adjustBody(entity, partialTick, poseStack);
      poseStack.scale(age, age, age);
      if (frame.scaleX() != 1.0F || frame.scaleY() != 1.0F || frame.scaleZ() != 1.0F) {
         poseStack.scale(frame.scaleX(), frame.scaleY(), frame.scaleZ());
      }
   }

   private FrameResult computeFrame(E entity, float partialTick) {
      EntityBundle bundle = EntityBundles.get(this.mob.id(), this.mob.bundleFor(entity));
      if (bundle == null) {
         return null;
      }
      EntityRuntime runtime = this.runtimes.get(entity);
      if (runtime == null || runtime.bundle() != bundle) {
         runtime = new EntityRuntime(this.mob.id(), bundle);
         this.runtimes.put(entity, runtime);
      }
      try {
         FrameResult frame = runtime.tick(entity, partialTick);
         if (debug) {
            DEBUG_RUNTIMES.put(entity.getId(), runtime);
         }
         this.lastScale = frame == null ? 1.0F : Math.max(frame.scaleX(), frame.scaleZ());
         return frame;
      } catch (RuntimeException e) {
         if (!this.failed) {
            Ans.LOGGER.error("[ans] {} failed to animate {}; drawing its rest pose (logged once)", this.mob, entity, e);
         }
         this.failed = true;
         return null;
      }
   }


   /** Hook for subclasses: may replace or extend the frame (e.g. hidden parts, extra layers). */
   protected FrameResult adjustFrame(E entity, FrameResult frame, float partialTick) {
      return frame;
   }

   /** Hook for subclasses: extra body rotation applied after the yaw, before scaling. */
   protected void adjustBody(E entity, float partialTick, PoseStack poseStack) {
   }

   @Override
   protected float getDeathMaxRotation(MobAnimatable animatable) {
      if (!this.mob.vanillaDeathRotation()) {
         return 0.0F;
      }
      return Float.isNaN(this.mob.deathRotation()) ? super.getDeathMaxRotation(animatable) : this.mob.deathRotation();
   }
}
