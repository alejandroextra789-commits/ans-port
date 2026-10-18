package com.ans.client.mob.monster;

import com.ans.client.bedrock.FrameResult;
import com.ans.client.equipment.EquipmentLayer;
import com.ans.client.render.MobLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/** Enderman carried block, drawn in the "fakearms" hand; the transform matches vanilla EndermanRenderer. */
public final class EndermanCarriedBlockLayer {
   private static final String BONE = "fakearms";

   private EndermanCarriedBlockLayer() {
   }

   public static void render(FrameResult frame, LivingEntity entity, GeoModel<?> model, PoseStack ps, MultiBufferSource buffers, int light) {
      if (!(entity instanceof EnderMan enderman)) {
         return;
      }
      BlockState carried = enderman.getCarriedBlock();
      if (carried == null) {
         return;
      }
      GeoBone bone = MobLayers.bone(frame, model, BONE, true);
      if (bone == null) {
         return;
      }
      ps.pushPose();
      EquipmentLayer.toBone(ps, bone, frame.pose());
      ps.scale(-1.0F, -1.0F, 1.0F);
      float s = Math.abs(bone.getPivotY()) / 13.5F;
      if (s > 0.01F && Math.abs(s - 1.0F) > 0.05F) {
         ps.scale(s, s, s);
      }

      ps.translate(0.0F, 0.03125F, -0.125F);
      ps.mulPose(Axis.XP.rotationDegrees(20.0F));
      ps.mulPose(Axis.YP.rotationDegrees(45.0F));
      ps.translate(0.25F, 0.1875F, 0.25F);
      ps.scale(-0.5F, -0.5F, 0.5F);
      ps.mulPose(Axis.YP.rotationDegrees(90.0F));
      Minecraft.getInstance().getBlockRenderer().renderSingleBlock(carried, ps, buffers, light, OverlayTexture.NO_OVERLAY);
      ps.popPose();
   }
}
