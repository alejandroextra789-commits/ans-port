package com.ans.client.mob.pets;

import com.ans.client.bedrock.FrameResult;
import com.ans.client.equipment.EquipmentLayer;
import com.ans.client.render.MobLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/** Fox main-hand item, drawn at the "held_item" bone. */
public final class FoxHeldItemLayer {
   private static final String BONE = "held_item";

   private FoxHeldItemLayer() {
   }

   public static void render(FrameResult frame, LivingEntity entity, GeoModel<?> model, PoseStack ps, MultiBufferSource buffers, int light) {
      ItemStack stack = entity.getItemBySlot(EquipmentSlot.MAINHAND);
      if (stack.isEmpty()) {
         return;
      }
      GeoBone bone = MobLayers.bone(frame, model, BONE, false);
      if (bone == null) {
         return;
      }
      ps.pushPose();
      EquipmentLayer.toBone(ps, bone, frame.pose());
      ps.scale(-1.0F, -1.0F, 1.0F);
      ps.mulPose(Axis.XP.rotationDegrees(90.0F));
      Minecraft.getInstance().getItemRenderer().renderStatic(entity, stack, ItemDisplayContext.GROUND, false, ps, buffers,
         entity.level(), light, OverlayTexture.NO_OVERLAY, entity.getId());
      ps.popPose();
   }
}
