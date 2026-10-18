package com.ans.client.equipment;

import com.ans.client.bedrock.BoneLookup;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.bedrock.MobPose;
import com.ans.client.bedrock.RenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * Equipment drawn on top of a GeckoLib-driven mob, ported to 1.20.1.
 *
 * - Armour: vanilla humanoid armour pieces (PLAYER_INNER/OUTER_ARMOR) with vanilla textures.
 * - Held items and helmet: Minecraft's ItemRenderer.renderStatic.
 *
 * Not yet ported: GeckoLib armour models (GeoArmorRenderer), dyed leather tint,
 * and baby-size fitting. Those are marked TODO below.
 */
public final class EquipmentLayer<E extends LivingEntity> {
   private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private final MobEquipment config;
   private final HumanoidModel<LivingEntity> inner;
   private final HumanoidModel<LivingEntity> outer;

   public EquipmentLayer(Context context, MobEquipment config) {
      this.config = config;
      this.inner = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
      this.outer = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
   }

   /** Draws held items, the helmet item and armour. Call while the mob's body transform is on the stack. */
   public void render(FrameResult frame, E entity, GeoModel<?> geoModel, PoseStack ps, MultiBufferSource buffers, int light, float partialTick) {
      if (frame == null || entity == null) {
         return;
      }
      boolean leftHanded = !this.config.mainHandRight && entity.getMainArm() == HumanoidArm.LEFT;
      ItemStack main = entity.getMainHandItem();
      ItemStack off = entity.getOffhandItem();
      ItemStack right = leftHanded ? off : main;
      ItemStack left = leftHanded ? main : off;
      float eq = entity.isBaby() && frame.scaleY() > 0.0F ? 1.0F / frame.scaleY() : 1.0F;

      if (!right.isEmpty()) {
         this.heldItem(frame, geoModel, ps, buffers, light, entity, right, this.config.rightItemBone, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, eq, false);
      }
      if (!left.isEmpty()) {
         this.heldItem(frame, geoModel, ps, buffers, light, entity, left, this.config.leftItemBone, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, eq, true);
      }
      if (this.config.armor) {
         this.armour(frame, geoModel, ps, buffers, light, entity);
      }
      if (this.config.headItems) {
         ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
         if (!head.isEmpty() && !(head.getItem() instanceof ArmorItem)) {
            GeoBone bone = this.bone(frame, geoModel, this.config.headBone);
            if (bone != null) {
               ps.pushPose();
               toBone(ps, bone, frame.pose());
               ps.scale(-eq, -eq, eq);
               ps.translate(0.0F, -0.25F, 0.0F);
               ps.mulPose(Axis.YP.rotationDegrees(180.0F));
               ps.scale(0.625F, -0.625F, -0.625F);
               Minecraft.getInstance().getItemRenderer().renderStatic(head, ItemDisplayContext.HEAD, light,
                  OverlayTexture.NO_OVERLAY, ps, buffers, entity.level(), entity.getId());
               ps.popPose();
            }
         }
      }
   }

   private void heldItem(FrameResult frame, GeoModel<?> geoModel, PoseStack ps, MultiBufferSource buffers, int light,
                         LivingEntity entity, ItemStack stack, String boneName, ItemDisplayContext ctx, float eq, boolean leftHand) {
      GeoBone bone = this.bone(frame, geoModel, boneName);
      if (bone == null) {
         return;
      }
      ps.pushPose();
      toBone(ps, bone, frame.pose());
      if (eq != 1.0F) {
         ps.scale(eq, eq, eq);
      }
      ps.mulPose(Axis.XP.rotationDegrees(-90.0F));
      ps.translate(0.0F, 0.1875F, -0.1875F);
      if (this.config.itemScale != 1.0F) {
         ps.scale(this.config.itemScale, this.config.itemScale, this.config.itemScale);
      }
      Minecraft.getInstance().getItemRenderer().renderStatic(entity, stack, ctx, leftHand, ps, buffers,
         entity.level(), light, OverlayTexture.NO_OVERLAY, entity.getId());
      ps.popPose();
   }

   private void armour(FrameResult frame, GeoModel<?> geoModel, PoseStack ps, MultiBufferSource buffers, int light, LivingEntity entity) {
      for (EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack stack = entity.getItemBySlot(slot);
         if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armor)) {
            continue;
         }
         boolean legs = slot == EquipmentSlot.LEGS;
         ResourceLocation tex = new ResourceLocation("minecraft",
            "textures/models/armor/" + armor.getMaterial().getName() + "_layer_" + (legs ? 2 : 1) + ".png");
         HumanoidModel<LivingEntity> model = legs || slot == EquipmentSlot.FEET ? this.inner : this.outer;
         for (Segment seg : this.segments(slot, model)) {
            GeoBone bone = this.bone(frame, geoModel, seg.bone());
            if (bone == null) {
               continue;
            }
            ps.pushPose();
            toBone(ps, bone, frame.pose());
            ps.scale(-1.0F, -1.0F, 1.0F);
            VertexConsumer vc = buffers.getBuffer(RenderType.armorCutoutNoCull(tex));
            setOnly(model, seg.part());
            model.renderToBuffer(ps, vc, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            setOnly(model, null);
            ps.popPose();
         }
      }
   }

   /** Maps an armour slot to the GeckoLib bones it covers, with the vanilla part drawn on each. */
   private List<Segment> segments(EquipmentSlot slot, HumanoidModel<LivingEntity> m) {
      MobEquipment c = this.config;
      return switch (slot) {
         case HEAD -> List.of(new Segment(c.headBone, m.head));
         case CHEST -> List.of(new Segment(c.bodyBone, m.body), new Segment(c.rightArmBone, m.rightArm), new Segment(c.leftArmBone, m.leftArm));
         case LEGS -> List.of(new Segment(c.bodyBone, m.body), new Segment(c.rightLegBone, m.rightLeg), new Segment(c.leftLegBone, m.leftLeg));
         case FEET -> List.of(new Segment(c.rightLegBone, m.rightLeg), new Segment(c.leftLegBone, m.leftLeg));
         default -> List.of();
      };
   }

   /** Shows only {@code part} (or nothing when null) on the six main armour parts. */
   private static void setOnly(HumanoidModel<?> m, ModelPart part) {
      ModelPart[] all = {m.head, m.body, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg};
      for (ModelPart p : all) {
         p.visible = p == part;
      }
   }

   private GeoBone bone(FrameResult frame, GeoModel<?> geoModel, String name) {
      for (RenderLayer layer : frame.layers()) {
         BakedGeoModel model = geoModel.getBakedModel(layer.model());
         if (!BoneLookup.isMissing(model)) {
            Optional<GeoBone> bone = model.getBone(name);
            if (bone.isPresent()) {
               return bone.get();
            }
         }
      }
      return null;
   }

   /**
    * Applies the pose of a bone and its ancestors to the stack (pivot in pixels, rotation in degrees).
    * Approximation of GeckoLib 5's RenderUtil.transformToBone; verify in game.
    */
   public static void toBone(PoseStack ps, GeoBone bone, MobPose pose) {
      List<GeoBone> chain = new java.util.ArrayList<>();
      for (GeoBone b = bone; b != null; b = b.getParent()) {
         chain.add(0, b);
      }
      for (GeoBone b : chain) {
         float[] v = pose.get(b.getName());
         float px = b.getPivotX() / 16.0F;
         float py = b.getPivotY() / 16.0F;
         float pz = b.getPivotZ() / 16.0F;
         ps.translate(px, py, pz);
         if (v != null) {
            ps.mulPose(Axis.ZP.rotationDegrees(finite(v[2])));
            ps.mulPose(Axis.YP.rotationDegrees(finite(v[1])));
            ps.mulPose(Axis.XP.rotationDegrees(finite(v[0])));
            ps.translate(finite(v[3]) / 16.0F, finite(v[4]) / 16.0F, finite(v[5]) / 16.0F);
         }
         ps.translate(-px, -py, -pz);
      }
   }

   private static float finite(float f) {
      return Float.isFinite(f) ? f : 0.0F;
   }

   private record Segment(String bone, ModelPart part) {
   }
}
