package com.ans.client.render;

import com.ans.Ans;
import com.ans.client.bedrock.EntityBundle;
import com.ans.client.bedrock.EntityBundles;
import com.ans.client.bedrock.EntityRuntime;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.mob.AnimatedMob;
import com.ans.client.util.ArgbCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

/**
 * Replaced non-living entity renderer, ported to GeckoLib 4.
 *
 * The per-frame Bedrock result is computed in render() and drawn by LayerSubmitter.
 * It is not stored on a GeckoLib render state (GL4 has none). When the runtime is
 * unavailable or fails, the entity falls back to GeckoLib's own render path (rest pose).
 */
public class ReplacedEntityRenderer<E extends Entity> extends GeoReplacedEntityRenderer<E, MobAnimatable> {
   protected final AnimatedMob<E> mob;
   private final Map<Entity, EntityRuntime> runtimes = new WeakHashMap<>();
   private boolean failed;
   private float lastScale = 1.0F;

   public ReplacedEntityRenderer(Context context, AnimatedMob<E> mob, EntityRendererProvider<E> vanilla) {
      super(context, new MobGeoModel(mob), new MobAnimatable(mob));
      this.mob = mob;
      this.shadowRadius = Float.isNaN(mob.shadowRadius()) ? vanillaShadow(context, vanilla) : mob.shadowRadius();
   }

   private static float vanillaShadow(Context context, EntityRendererProvider<?> vanilla) {
      if (vanilla == null) {
         return 0.0F;
      }
      try {
         EntityRenderer<?> renderer = vanilla.create(context);
         Field f = EntityRenderer.class.getDeclaredField("shadowRadius");
         f.setAccessible(true);
         return f.getFloat(renderer);
      } catch (ReflectiveOperationException | RuntimeException e) {
         return 0.0F;
      }
   }

   @Override
   public void render(E entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
      FrameResult frame = entity == null ? null : computeFrame(entity, partialTick);
      if (frame == null) {
         super.render(entity, entityYaw, partialTick, poseStack, buffer, light);
         return;
      }

      float yaw = this.mob.yaw(entity, partialTick);
      poseStack.pushPose();
      try {
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
         if (frame.scaleX() != 1.0F || frame.scaleY() != 1.0F || frame.scaleZ() != 1.0F) {
            poseStack.scale(frame.scaleX(), frame.scaleY(), frame.scaleZ());
         }
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
      } finally {
         poseStack.popPose();
      }
   }

   /** Runs the Bedrock runtime for this entity. Null means "draw the rest pose". */
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

}
