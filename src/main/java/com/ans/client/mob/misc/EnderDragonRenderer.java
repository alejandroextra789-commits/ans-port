package com.ans.client.mob.misc;

import com.ans.client.bedrock.BoneLookup;

import com.ans.client.bedrock.FrameResult;
import com.ans.client.bedrock.MobPose;
import com.ans.client.bedrock.RenderKind;
import com.ans.client.bedrock.RenderLayer;
import com.ans.client.mob.AnimatedMob;
import com.ans.client.render.BedrockTextures;
import com.ans.client.render.MobAnimatable;
import com.ans.client.render.ReplacedMobRenderer;
import software.bernie.geckolib.core.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.object.DataTicket;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import com.ans.client.util.ArgbCompat;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class EnderDragonRenderer extends ReplacedMobRenderer<EnderDragon> {
   private static final ResourceLocation EXPLODING = new ResourceLocation("minecraft", "textures/entity/enderdragon/dragon_exploding.png");
   private static final DataTicket<Vec3> BEAM = DataTicket.create("ans_dragon_beam", Vec3.class);
   private static final DataTicket<Float> DEATH = DataTicket.create("ans_dragon_death", Float.class);
   private static final DataTicket<Float> YAW = DataTicket.create("ans_dragon_yaw", Float.class);
   private static final DataTicket<Float> PITCH = DataTicket.create("ans_dragon_pitch", Float.class);
   private static final DataTicket<Float> AGE = DataTicket.create("ans_dragon_age", Float.class);
   private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0) / 2.0);

   public EnderDragonRenderer(Context context, AnimatedMob<EnderDragon> mob, EntityRendererProvider<EnderDragon> vanilla) {
      super(context, mob, vanilla);
   }

   protected boolean affectedByCulling(EnderDragon entity) {
      return false;
   }

   public void addRenderData(MobAnimatable animatable, EnderDragon entity, LivingEntityRenderState renderState, float partialTick) {
      super.addRenderData(animatable, entity, renderState, partialTick);
      if (entity != null) {
         renderState.bodyRot = entity.flightHistory.get(0, partialTick).yRot();
         EndCrystal crystal = entity.nearestCrystal;
         Vec3 beam = crystal == null
            ? Vec3.ZERO
            : crystal.getPosition(partialTick).add(0.0, EndCrystalRenderer.getY(crystal.time + partialTick), 0.0).subtract(entity.getPosition(partialTick));
         renderState.addGeckolibData(BEAM, beam);
         renderState.addGeckolibData(DEATH, entity.dragonDeathTime > 0 ? entity.dragonDeathTime + partialTick : 0.0F);
         renderState.addGeckolibData(YAW, entity.flightHistory.get(7, partialTick).yRot());
         renderState.addGeckolibData(PITCH, (float)(entity.flightHistory.get(5, partialTick).y() - entity.flightHistory.get(10, partialTick).y()) * 10.0F);
         renderState.addGeckolibData(AGE, entity.tickCount + partialTick);
      }
   }

   @Override
   public void submitRenderTasks(RenderPassInfo<LivingEntityRenderState> info, OrderedSubmitNodeCollector renderTasks, RenderType renderType) {
      Float death = (Float)info.getGeckolibData(DEATH);
      FrameResult frame = (FrameResult)info.getGeckolibData(FrameResult.TICKET);
      if (renderType != null && death != null && !(death <= 0.0F) && frame != null && !((LivingEntityRenderState)info.renderState()).isInvisible) {
         RenderLayer body = null;

         for (RenderLayer layer : frame.layers()) {
            if (!EXPLODING.equals(layer.texture())) {
               body = layer;
               break;
            }
         }

         if (body != null) {
            BakedGeoModel model = this.getGeoModel().getBakedModel(body.model());
            if (!BoneLookup.isMissing(model)) {
               boolean emissive = body.kindOf("body") == RenderKind.EMISSIVE_ALPHA;
               ResourceLocation texture = emissive ? BedrockTextures.emissiveSplit(body.texture()).plain() : BedrockTextures.resolve(body.texture());
               int alpha = ArgbCompat.white(1.0F - death / 200.0F);
               int light = info.packedLight();
               RenderLayer layer = body;
               renderTasks.submitCustomGeometry(info.poseStack(), RenderType.entityCutoutNoCull(texture), (pose, buffer) -> {
                  PoseStack ps = info.poseStack();
                  ps.pushPose();
                  ps.last().set(pose);
                  posed(model, frame.pose(), layer, () -> model.render(info, buffer, light, OverlayTexture.NO_OVERLAY, alpha));
                  ps.popPose();
               });
               if (emissive) {
                  ResourceLocation glow = BedrockTextures.emissiveSplit(body.texture()).tinted();
                  renderTasks.submitCustomGeometry(info.poseStack(), RenderType.entityTranslucentEmissive(glow), (pose, buffer) -> {
                     PoseStack ps = info.poseStack();
                     ps.pushPose();
                     ps.last().set(pose);
                     posed(model, frame.pose(), layer, () -> model.render(info, buffer, light, OverlayTexture.NO_OVERLAY, -1));
                     ps.popPose();
                  });
               }
            }
         }
      } else {
         super.submitRenderTasks(info, renderTasks, renderType);
      }
   }

   private static void posed(BakedGeoModel model, MobPose pose, RenderLayer layer, Runnable render) {
      List<BoneSnapshot> applied = new ArrayList<>();

      try {
         for (GeoBone bone : ((Map)BoneLookup.of(model)).values()) {
            float[] v = pose.get(bone.getName());
            boolean hidden = !layer.visible(bone.getName());
            if (v != null || hidden) {
               BoneSnapshot s = BoneSnapshot.create(bone);
               if (v != null) {
                  s.setRotation(v[0], v[1], v[2]);
                  s.setTranslation(v[3], v[4], v[5]);
                  s.setScale(v[6], v[7], v[8]);
               }

               if (hidden) {
                  s.skipRender(true);
               }

               s.apply();
               applied.add(s);
            }
         }

         render.run();
      } finally {
         for (BoneSnapshot s : applied) {
            s.cleanup();
         }
      }
   }

   public void submit(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      super.submit(state, poseStack, collector, camera);
      Float death = (Float)state.getGeckolibData(DEATH);
      Float age = (Float)state.getGeckolibData(AGE);
      if (death != null && death > 0.0F) {
         Float yaw = (Float)state.getGeckolibData(YAW);
         Float pitch = (Float)state.getGeckolibData(PITCH);
         poseStack.pushPose();
         poseStack.mulPose(Axis.YP.rotationDegrees(-(yaw == null ? 0.0F : yaw)));
         poseStack.mulPose(Axis.XP.rotationDegrees(pitch == null ? 0.0F : pitch));
         poseStack.translate(0.0F, 0.0F, 1.0F);
         poseStack.scale(-1.0F, -1.0F, 1.0F);
         poseStack.translate(0.0F, -1.501F, 0.0F);
         poseStack.translate(0.0F, -1.0F, -2.0F);
         submitRays(poseStack, death / 200.0F, collector);
         poseStack.popPose();
      }

      Vec3 beam = (Vec3)state.getGeckolibData(BEAM);
      if (beam != null && beam != Vec3.ZERO) {
         net.minecraft.client.renderer.entity.EnderDragonRenderer.submitCrystalBeams(
            (float)beam.x, (float)beam.y, (float)beam.z, age == null ? 0.0F : age, poseStack, collector, state.lightCoords
         );
      }
   }

   private static void submitRays(PoseStack poseStack, float deathTime, SubmitNodeCollector collector) {
      collector.submitCustomGeometry(
         poseStack,
         RenderType.entityTranslucentEmissive(new ResourceLocation("minecraft", "textures/entity/end_gateway_beam.png")),
         (pose, buffer) -> {
            float overDrive = Math.min(deathTime > 0.8F ? (deathTime - 0.8F) / 0.2F : 0.0F, 1.0F);
            int innerColor = ArgbCompat.colorFromFloat(1.0F - overDrive, 1.0F, 1.0F, 1.0F);
            RandomSource random = RandomSource.createThreadLocalInstance(432L);
            Vector3f origin = new Vector3f();
            Vector3f outerLeft = new Vector3f();
            Vector3f outerRight = new Vector3f();
            Vector3f outerBottom = new Vector3f();
            Quaternionf rot = new Quaternionf();
            int rayCount = Mth.floor((deathTime + deathTime * deathTime) / 2.0F * 60.0F);

            for (int i = 0; i < rayCount; i++) {
               rot.rotationXYZ(
                     random.nextFloat() * (float) (Math.PI * 2), random.nextFloat() * (float) (Math.PI * 2), random.nextFloat() * (float) (Math.PI * 2)
                  )
                  .rotateXYZ(
                     random.nextFloat() * (float) (Math.PI * 2),
                     random.nextFloat() * (float) (Math.PI * 2),
                     random.nextFloat() * (float) (Math.PI * 2) + deathTime * (float) (Math.PI / 2)
                  );
               pose.rotate(rot);
               float length = random.nextFloat() * 20.0F + 5.0F + overDrive * 10.0F;
               float width = random.nextFloat() * 2.0F + 1.0F + overDrive * 2.0F;
               outerLeft.set(-HALF_SQRT_3 * width, length, -0.5F * width);
               outerRight.set(HALF_SQRT_3 * width, length, -0.5F * width);
               outerBottom.set(0.0F, length, width);
               buffer.addVertex(pose, origin).setColor(innerColor);
               buffer.addVertex(pose, outerLeft).setColor(16711935);
               buffer.addVertex(pose, outerRight).setColor(16711935);
               buffer.addVertex(pose, origin).setColor(innerColor);
               buffer.addVertex(pose, outerRight).setColor(16711935);
               buffer.addVertex(pose, outerBottom).setColor(16711935);
               buffer.addVertex(pose, origin).setColor(innerColor);
               buffer.addVertex(pose, outerBottom).setColor(16711935);
               buffer.addVertex(pose, outerLeft).setColor(16711935);
            }
         }
      );
   }
}
