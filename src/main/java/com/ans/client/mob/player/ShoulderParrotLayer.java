package com.ans.client.mob.player;

import com.ans.client.bedrock.FrameResult;
import com.ans.client.equipment.EquipmentLayer;
import com.ans.client.render.MobAnimatable;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.ParrotModel.Pose;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.animal.parrot.Parrot.Variant;
import net.minecraft.world.entity.player.Player;

final class ShoulderParrotLayer extends GeoRenderLayer<MobAnimatable, Player, LivingEntityRenderState> {
   private final ParrotModel model;

   ShoulderParrotLayer(PlayerRenderer renderer, Context context) {
      super(renderer);
      this.model = new ParrotModel(context.bakeLayer(ModelLayers.PARROT));
   }

   public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
      if (info.renderState() instanceof PlayerRenderState s && !s.isInvisible && !s.isSpectator) {
         if (s.parrotOnLeftShoulder != null || s.parrotOnRightShoulder != null) {
            FrameResult frame = (FrameResult)info.getGeckolibData(FrameResult.TICKET);
            GeoBone body = frame == null ? null : PlayerRenderer.bone(this.getGeoModel(), frame, "body");
            if (body != null) {
               if (s.parrotOnLeftShoulder != null) {
                  this.submit(info, collector, frame, body, s, s.parrotOnLeftShoulder, true);
               }

               if (s.parrotOnRightShoulder != null) {
                  this.submit(info, collector, frame, body, s, s.parrotOnRightShoulder, false);
               }
            }
         }
      }
   }

   private void submit(
      RenderPassInfo<LivingEntityRenderState> info,
      SubmitNodeCollector collector,
      FrameResult frame,
      GeoBone body,
      PlayerRenderState s,
      Variant variant,
      boolean left
   ) {
      PoseStack ps = info.poseStack();
      ps.pushPose();
      ps.last().set(info.getModelRenderMatrixPose());
      EquipmentLayer.toBone(ps, body, frame.pose());
      ps.scale(-1.0F, -1.0F, 1.0F);
      ps.translate(left ? 0.4F : -0.4F, -1.5F, 0.0F);
      ParrotRenderState parrot = new ParrotRenderState();
      parrot.pose = Pose.ON_SHOULDER;
      parrot.ageInTicks = s.ageInTicks;
      parrot.walkAnimationPos = s.walkAnimationPos;
      parrot.walkAnimationSpeed = s.walkAnimationSpeed;
      parrot.yRot = s.yRot;
      parrot.xRot = s.xRot;
      collector.submitModel(
         this.model, parrot, ps, ParrotRenderer.getVariantTexture(variant), info.packedLight(), OverlayTexture.NO_OVERLAY, s.outlineColor, null
      );
      ps.popPose();
   }
}
