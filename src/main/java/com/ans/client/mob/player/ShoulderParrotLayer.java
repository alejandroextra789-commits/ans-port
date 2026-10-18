package com.ans.client.mob.player;

import com.ans.client.bedrock.FrameResult;
import com.ans.client.equipment.EquipmentLayer;
import com.ans.client.render.MobLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/** Parrots perched on the player's shoulders, drawn from the "body" bone (1.20.1 ParrotModel). */
public final class ShoulderParrotLayer {
   private static ParrotModel model;

   private ShoulderParrotLayer() {
   }

   public static void render(FrameResult frame, Player player, GeoModel<?> geoModel, PoseStack ps, MultiBufferSource buffers, int light) {
      if (player.isInvisible() || player.isSpectator()) {
         return;
      }
      CompoundTag left = player.getShoulderEntityLeft();
      CompoundTag right = player.getShoulderEntityRight();
      boolean hasLeft = left != null && !left.isEmpty();
      boolean hasRight = right != null && !right.isEmpty();
      if (!hasLeft && !hasRight) {
         return;
      }
      GeoBone body = MobLayers.bone(frame, geoModel, "body", false);
      if (body == null) {
         return;
      }
      if (hasLeft) {
         submit(frame, player, body, ps, buffers, light, left, true);
      }
      if (hasRight) {
         submit(frame, player, body, ps, buffers, light, right, false);
      }
   }

   private static void submit(FrameResult frame, Player player, GeoBone body, PoseStack ps, MultiBufferSource buffers, int light, CompoundTag tag, boolean left) {
      ps.pushPose();
      EquipmentLayer.toBone(ps, body, frame.pose());
      ps.scale(-1.0F, -1.0F, 1.0F);
      ps.translate(left ? 0.4F : -0.4F, -1.5F, 0.0F);
      Parrot.Variant variant = Parrot.Variant.byId(tag.getInt("Variant"));
      VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(ParrotRenderer.getVariantTexture(variant)));
      float partialTick = 0.0F;
      model().renderOnShoulder(ps, vc, light, OverlayTexture.NO_OVERLAY,
         player.walkAnimation.position(), player.walkAnimation.speed(), player.yHeadRot - player.yBodyRot, player.getXRot(), player.tickCount);
      ps.popPose();
   }

   private static ParrotModel model() {
      if (model == null) {
         model = new ParrotModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PARROT));
      }
      return model;
   }
}
