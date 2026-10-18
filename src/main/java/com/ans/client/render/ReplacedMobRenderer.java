package com.ans.client.render;

import com.ans.Ans;
import com.ans.client.bedrock.EntityBundle;
import com.ans.client.bedrock.EntityBundles;
import com.ans.client.bedrock.EntityRuntime;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.equipment.EquipmentLayer;
import com.ans.client.equipment.MobEquipment;
import com.ans.client.mob.AnimatedMob;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

public class ReplacedMobRenderer<E extends LivingEntity> extends GeoReplacedEntityRenderer<MobAnimatable, E, LivingEntityRenderState> {
   private static final DataTicket<EntityRuntime> RUNTIME = DataTicket.create("ans_runtime", EntityRuntime.class);
   private static final int FULL_BRIGHT = 15728880;
   protected final AnimatedMob<E> mob;
   private boolean failed;
   public static boolean debug;
   public static final Map<Integer, EntityRuntime> DEBUG_RUNTIMES = new ConcurrentHashMap<>();
   static final boolean BEDROCK_BED_POSITION = !"false".equalsIgnoreCase(System.getProperty("ans.bedrockBedPosition", "true"));

   public ReplacedMobRenderer(Context context, AnimatedMob<E> mob, EntityRendererProvider<E> vanilla) {
      super(context, new MobGeoModel(mob), new MobAnimatable(mob));
      this.mob = mob;
      this.shadowRadius = Float.isNaN(mob.shadowRadius()) ? vanillaShadow(context, vanilla) : mob.shadowRadius();
      this.withRenderLayer(new EquipmentLayer(this, context, MobEquipment.get(mob.type())));
   }

   private static float vanillaShadow(Context context, EntityRendererProvider<?> vanilla) {
      if (vanilla == null) {
         return 0.5F;
      }

      try {
         EntityRenderer<?, ?> renderer = vanilla.create(context);
         Field f = EntityRenderer.class.getDeclaredField("shadowRadius");
         f.setAccessible(true);
         return f.getFloat(renderer);
      } catch (ReflectiveOperationException | RuntimeException e) {
         return 0.5F;
      }
   }

   public void addRenderData(MobAnimatable animatable, E entity, LivingEntityRenderState renderState, float partialTick) {
      if (entity != null) {
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
               if (debug) {
                  DEBUG_RUNTIMES.put(entity.getId(), runtime);
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

   public void applyAnimationControllers(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
      LayerSubmitter.applyPose((FrameResult)renderPassInfo.getGeckolibData(FrameResult.TICKET), snapshots);
   }

   public void submitRenderTasks(
      RenderPassInfo<LivingEntityRenderState> renderPassInfo, OrderedSubmitNodeCollector renderTasks, RenderType renderType
   ) {
      if (renderType != null) {
         FrameResult frame = (FrameResult)renderPassInfo.getGeckolibData(FrameResult.TICKET);
         if (frame == null) {
            super.submitRenderTasks(renderPassInfo, renderTasks, renderType);
         } else {
            LayerSubmitter.submit(this.getGeoModel(), renderPassInfo, renderTasks, frame, ((LivingEntityRenderState)renderPassInfo.renderState()).isInvisible);
         }
      }
   }

   public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, float widthScale, float heightScale) {
      float age = ((LivingEntityRenderState)renderPassInfo.renderState()).ageScale;
      super.scaleModelForRender(renderPassInfo, widthScale * age, heightScale * age);
      FrameResult frame = (FrameResult)renderPassInfo.getGeckolibData(FrameResult.TICKET);
      if (frame != null && (frame.scaleX() != 1.0F || frame.scaleY() != 1.0F || frame.scaleZ() != 1.0F)) {
         renderPassInfo.poseStack().scale(frame.scaleX(), frame.scaleY(), frame.scaleZ());
      }
   }

   protected float getShadowRadius(LivingEntityRenderState renderState) {
      FrameResult frame = (FrameResult)renderState.getGeckolibData(FrameResult.TICKET);
      float scale = frame == null ? 1.0F : Math.max(frame.scaleX(), frame.scaleZ());
      return this.shadowRadius * renderState.ageScale * scale;
   }

   protected void applyRotations(RenderPassInfo<LivingEntityRenderState> renderPassInfo, PoseStack poseStack, float nativeScale) {
      LivingEntityRenderState state = (LivingEntityRenderState)renderPassInfo.renderState();
      if (state.bedOrientation != null && state.getGeckolibData(DataTickets.ENTITY_POSE) == Pose.SLEEPING && state.deathTime <= 0.0F) {
         poseStack.mulPose(Axis.YP.rotationDegrees(-state.bedOrientation.toYRot()));
      } else {
         super.applyRotations(renderPassInfo, poseStack, nativeScale);
      }
   }

   public void adjustRenderPose(RenderPassInfo<LivingEntityRenderState> renderPassInfo) {
      LivingEntityRenderState state = (LivingEntityRenderState)renderPassInfo.renderState();
      if (BEDROCK_BED_POSITION && state.bedOrientation != null && !(state.deathTime > 0.0F) && state.getGeckolibData(DataTickets.ENTITY_POSE) == Pose.SLEEPING) {
         FrameResult frame = (FrameResult)renderPassInfo.getGeckolibData(FrameResult.TICKET);
         float sx = Math.max(1.0E-4F, state.ageScale * (frame == null ? 1.0F : frame.scaleX()));
         float sz = Math.max(1.0E-4F, state.ageScale * (frame == null ? 1.0F : frame.scaleZ()));
         PoseStack poseStack = renderPassInfo.poseStack();
         poseStack.translate(-state.bedOrientation.getStepX() * 0.5F / sx, 0.0F, -state.bedOrientation.getStepZ() * 0.5F / sz);
         this.applyRotations(renderPassInfo, poseStack, state.scale);
         poseStack.translate(0.0F, 0.01F, 0.0F);
      } else {
         super.adjustRenderPose(renderPassInfo);
      }
   }

   protected float getDeathMaxRotation(GeoRenderState renderState) {
      if (!this.mob.vanillaDeathRotation()) {
         return 0.0F;
      } else {
         return Float.isNaN(this.mob.deathRotation()) ? super.getDeathMaxRotation(renderState) : this.mob.deathRotation();
      }
   }
}
