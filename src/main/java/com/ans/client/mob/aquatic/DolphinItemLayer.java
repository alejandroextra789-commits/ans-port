package com.ans.client.mob.aquatic;

import com.ans.client.bedrock.FrameResult;
import com.ans.client.equipment.EquipmentLayer;
import com.ans.client.render.MobLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/** Dolphin main-hand item, carried at the "nose" bone. */
public final class DolphinItemLayer {
   private static final String BONE = "nose";

   private DolphinItemLayer() {
   }

   public static void render(FrameResult frame, LivingEntity entity, GeoModel<?> model, PoseStack ps, MultiBufferSource buffers, int light) {
      ItemStack stack = entity.getMainHandItem();
      if (stack.isEmpty()) {
         return;
      }
      GeoBone bone = MobLayers.bone(frame, model, BONE, false);
      if (bone == null) {
         return;
      }
      ps.pushPose();
      EquipmentLayer.toBone(ps, bone, frame.pose());
      ps.translate(0.0F, 0.0625F, -0.25F);
      Minecraft.getInstance().getItemRenderer().renderStatic(entity, stack, ItemDisplayContext.GROUND, false, ps, buffers,
         entity.level(), light, OverlayTexture.NO_OVERLAY, entity.getId());
      ps.popPose();
   }
}
