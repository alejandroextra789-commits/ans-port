package com.ans.client.render;

import com.ans.Ans;
import com.ans.client.bedrock.EntityBundle;
import com.ans.client.bedrock.EntityBundles;
import com.ans.client.bedrock.EntityRuntime;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.mob.AnimatedMob;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.lang.reflect.Field;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.Entity;

public class ReplacedEntityRenderer<E extends Entity> extends GeoReplacedEntityRenderer<MobAnimatable, E, EntityRenderState> {
   private static final DataTicket<EntityRuntime> RUNTIME = DataTicket.create("ans_runtime_entity", EntityRuntime.class);
   private static final DataTicket<Float> YAW = DataTicket.create("ans_yaw", Float.class);
   protected final AnimatedMob<E> mob;
   private boolean failed;

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
         EntityRenderer<?, ?> renderer = vanilla.create(context);
         Field f = EntityRenderer.class.getDeclaredField("shadowRadius");
         f.setAccessible(true);
         return f.getFloat(renderer);
      } catch (ReflectiveOperationException | RuntimeException e) {
         return 0.0F;
      }
   }

   public void addRenderData(MobAnimatable animatable, E entity, EntityRenderState renderState, float partialTick) {
      if (entity != null) {
         renderState.addGeckolibData(YAW, this.mob.yaw(entity, partialTick));
         EntityBundle bundle = EntityBundles.get(this.mob.id(), this.mob.bundleFor(entity));
         AnimatableManager<?> manager = (AnimatableManager<?>)renderState.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
         if (bundle != null && manager != null) {
            EntityRuntime runtime = (EntityRuntime)manager.getAnimatableData(RUNTIME);
            if (runtime == null || runtime.bundle() != bundle) {
               runtime = new EntityRuntime(this.mob.id(), bundle);
               manager.setAnimatableData(RUNTIME, runtime);
            }

            try {
               renderState.addGeckolibData(FrameResult.TICKET, runtime.tick(entity, partialTick, renderState));
               if (ReplacedMobRenderer.debug) {
                  ReplacedMobRenderer.DEBUG_RUNTIMES.put(entity.getId(), runtime);
               }
            } catch (RuntimeException e) {
               if (!this.failed) {
                  Ans.LOGGER.error("[ans] {} failed to animate {}; drawing its rest pose (logged once)", new Object[]{this.mob, entity, e});
               }

               this.failed = true;
            }
         }
      }
   }

   public void applyAnimationControllers(RenderPassInfo<EntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
      LayerSubmitter.applyPose((FrameResult)renderPassInfo.getGeckolibData(FrameResult.TICKET), snapshots);
   }

   public void submitRenderTasks(RenderPassInfo<EntityRenderState> renderPassInfo, OrderedSubmitNodeCollector renderTasks, RenderType renderType) {
      if (renderType != null) {
         FrameResult frame = (FrameResult)renderPassInfo.getGeckolibData(FrameResult.TICKET);
         if (frame == null) {
            super.submitRenderTasks(renderPassInfo, renderTasks, renderType);
         } else {
            LayerSubmitter.submit(this.getGeoModel(), renderPassInfo, renderTasks, frame, ((EntityRenderState)renderPassInfo.renderState()).isInvisible);
         }
      }
   }

   protected void applyRotations(RenderPassInfo<EntityRenderState> renderPassInfo, PoseStack poseStack, float nativeScale) {
      float yaw = (Float)((EntityRenderState)renderPassInfo.renderState()).getOrDefaultGeckolibData(YAW, 0.0F);
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
   }

   public void scaleModelForRender(RenderPassInfo<EntityRenderState> renderPassInfo, float widthScale, float heightScale) {
      super.scaleModelForRender(renderPassInfo, widthScale, heightScale);
      FrameResult frame = (FrameResult)renderPassInfo.getGeckolibData(FrameResult.TICKET);
      if (frame != null && (frame.scaleX() != 1.0F || frame.scaleY() != 1.0F || frame.scaleZ() != 1.0F)) {
         renderPassInfo.poseStack().scale(frame.scaleX(), frame.scaleY(), frame.scaleZ());
      }
   }

   protected float getShadowRadius(EntityRenderState renderState) {
      FrameResult frame = (FrameResult)renderState.getGeckolibData(FrameResult.TICKET);
      float scale = frame == null ? 1.0F : Math.max(frame.scaleX(), frame.scaleZ());
      return this.shadowRadius * scale;
   }
}
