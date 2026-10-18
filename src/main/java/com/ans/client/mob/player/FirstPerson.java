package com.ans.client.mob.player;

import com.ans.Ans;
import com.ans.client.attachable.AttachableRenderer;
import com.ans.client.attachable.Attachables;
import com.ans.client.bedrock.AnimationLookup;
import com.ans.client.bedrock.EntityBundle;
import com.ans.client.bedrock.EntityBundles;
import com.ans.client.bedrock.EntityRuntime;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.mixin.ItemInHandRendererAccessor;
import com.ans.client.mob.AnimatedMob;
import com.ans.client.mob.AnimatedMobs;
import com.ans.client.molang.MolangContext;
import com.ans.client.molang.MolangVars;
import com.ans.client.render.LayerSubmitter;
import software.bernie.geckolib.model.GeoModel;
import com.geckolib.renderer.base.RenderPassInfo;
import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

public final class FirstPerson {
   public static boolean enabled = !"false".equalsIgnoreCase(System.getProperty("ans.firstPerson", "true"));
   private static final float EYE = 1.62F;
   private static final EquipmentSlot[] HANDS = new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
   private static final int V_IS_FIRST_PERSON = MolangVars.id("is_first_person");
   private static final int V_BOB_ANIMATION = MolangVars.id("bob_animation");
   private static final int V_IS_PAPERDOLL = MolangVars.id("is_paperdoll");
   private static final int V_MAP_FACE_ICON = MolangVars.id("map_face_icon");
   private static EntityRuntime runtime;
   private static LocalPlayer runtimeOwner;
   private static final LivingEntityRenderState STATE = new PlayerRenderState();
   private static final CameraRenderState CAMERA = new CameraRenderState();
   private static boolean failed;
   private static float offHandArmHeight = 1.0F;
   private static float mainHandArmHeight = 1.0F;

   private FirstPerson() {
   }

   public static EntityRuntime runtime() {
      return runtime;
   }

   public static boolean submit(ItemInHandRenderer hands, float partialTick, PoseStack poseStack, SubmitNodeCollector collector, LocalPlayer player, int light) {
      if (enabled && !failed && PlayerRendering.replaces(player)) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
            AnimatedMob<?> mob = AnimatedMobs.get(EntityType.PLAYER);
            EntityBundle bundle = mob == null ? null : EntityBundles.get(mob.id(), mob.bundleFor(player));
            if (bundle == null) {
               return false;
            }

            try {
               draw(renderer, bundle, (ItemInHandRendererAccessor)hands, partialTick, poseStack, collector, player, light);
               return true;
            } catch (RuntimeException e) {
               failed = true;
               Ans.LOGGER.error("[ans] first-person view failed; Java's hands are drawn from now on (logged once)", e);
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static EntityRuntime runtime(EntityBundle bundle, LocalPlayer player) {
      EntityRuntime rt = runtime;
      if (rt == null || rt.bundle() != bundle || runtimeOwner != player || rt.generation() != AnimationLookup.generation()) {
         rt = new EntityRuntime("player", bundle).firstPerson(true, FirstPerson::engineVariables);
         runtime = rt;
         runtimeOwner = player;
      }

      return rt;
   }

   private static void engineVariables(MolangContext c) {
      c.vars.set(V_IS_FIRST_PERSON, 1.0);
      c.vars.set(V_IS_PAPERDOLL, 0.0);
      c.vars.set(V_MAP_FACE_ICON, 0.0);
      c.vars.set(V_BOB_ANIMATION, Minecraft.getInstance().options.bobView().get() ? 1.0 : 0.0);
      c.contextVars.put("player_offhand_arm_height", offHandArmHeight);
      c.contextVars.put("player_arm_height", mainHandArmHeight);
   }

   private static void draw(
      PlayerRenderer renderer,
      EntityBundle bundle,
      ItemInHandRendererAccessor hands,
      float pt,
      PoseStack ps,
      SubmitNodeCollector collector,
      LocalPlayer player,
      int light
   ) {
      mainHandArmHeight = Mth.lerp(pt, hands.ans$oMainHandHeight(), hands.ans$mainHandHeight());
      offHandArmHeight = Mth.lerp(pt, hands.ans$oOffHandHeight(), hands.ans$offHandHeight());
      PlayerRenderState state = (PlayerRenderState)STATE;
      state.getDataMap().clear();
      state.lightCoords = light;
      state.isInvisible = player.isInvisible();
      state.ageInTicks = player.tickCount + pt;
      EntityRuntime rt = runtime(bundle, player);
      FrameResult frame = rt.tick(player, pt, state);
      state.addGeckolibData(FrameResult.TICKET, frame);
      PlayerRenderer.hideModelParts(player, state);
      frame = (FrameResult)state.getGeckolibData(FrameResult.TICKET);
      List<Attachables.Frame> attached = new ArrayList<>(2);
      boolean[] covered = new boolean[2];
      if (Attachables.ENABLED) {
         for (int i = 0; i < HANDS.length; i++) {
            ItemStack stack = player.getItemBySlot(HANDS[i]);
            if (!stack.isEmpty()) {
               Attachables.Frame f = Attachables.tick(player, HANDS[i], stack, pt, state, true);
               if (f != null) {
                  attached.add(f);
                  covered[i] = true;
               }
            }
         }

         for (Attachables.Frame f : attached) {
            AttachableRenderer.resolveRelative(f, frame);
         }
      }

      if (!player.isScoping()) {
         GeoModel<?> geoModel = renderer.getGeoModel();
         ps.pushPose();
         ps.translate(0.0F, -1.62F, 0.0F);
         ps.mulPose(Axis.YP.rotationDegrees(180.0F));
         if (player.isPassenger()) {
            ps.mulPose(
               Axis.YP
                  .rotationDegrees(Mth.wrapDegrees(Mth.rotLerp(pt, player.yHeadRotO, player.yHeadRot) - Mth.rotLerp(pt, player.yBodyRotO, player.yBodyRot)))
            );
         }

         ps.scale(frame.scaleX(), frame.scaleY(), frame.scaleZ());
         RenderPassInfo<LivingEntityRenderState> info = RenderPassInfo.create(renderer, STATE, ps, CAMERA, true);
         info.captureModelRenderPose();
         if (!state.isInvisible) {
            LayerSubmitter.submit(geoModel, info, collector, frame, false);
         }

         if (!attached.isEmpty()) {
            AttachableRenderer.submit(geoModel, info, collector, frame, attached);
         }

         ps.popPose();
         javaHands(hands, pt, ps, collector, player, light, covered);
      }
   }

   private static void javaHands(
      ItemInHandRendererAccessor hands, float pt, PoseStack ps, SubmitNodeCollector collector, LocalPlayer player, int light, boolean[] covered
   ) {
      float attack = player.getAttackAnim(pt);
      InteractionHand swinging = (InteractionHand)MoreObjects.firstNonNull(player.swingingArm, InteractionHand.MAIN_HAND);
      float xRot = Mth.lerp(pt, player.xRotO, player.getXRot());
      ItemStack main = player.getMainHandItem();
      ItemStack off = player.getOffhandItem();

      for (int i = 0; i < 2; i++) {
         InteractionHand hand = i == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
         ItemStack stack = i == 0 ? main : off;
         if (!stack.isEmpty() && !player.isScoping()) {
            float handAttack = swinging == hand ? attack : 0.0F;
            float height = i == 0
               ? Mth.lerp(pt, hands.ans$oMainHandHeight(), hands.ans$mainHandHeight())
               : Mth.lerp(pt, hands.ans$oOffHandHeight(), hands.ans$offHandHeight());
            float inverseArmHeight = hands.ans$itemModelResolver().swapAnimationScale(stack) * (1.0F - height);
            if (net.minecraft.world.item.MapItem.getMapId(stack) != null) {
               ps.pushPose();
               if (i == 0 && off.isEmpty()) {
                  twoHandedMap(hands, ps, collector, light, xRot, inverseArmHeight, handAttack, stack);
               } else {
                  oneHandedMap(hands, ps, collector, light, inverseArmHeight, i == 0 ? HumanoidArm.RIGHT : HumanoidArm.LEFT, handAttack, stack);
               }

               ps.popPose();
            } else if (!covered[i]) {
               hands.ans$submitArmWithItem(player, pt, xRot, hand, handAttack, stack, inverseArmHeight, ps, collector, light);
            }
         }
      }
   }

   private static void twoHandedMap(
      ItemInHandRendererAccessor hands, PoseStack ps, SubmitNodeCollector collector, int light, float xRot, float inverseArmHeight, float attack, ItemStack map
   ) {
      float sqrtAttack = Mth.sqrt(attack);
      ps.translate(0.0F, 0.2F * Mth.sin(attack * (float) Math.PI) / 2.0F, -0.4F * Mth.sin(sqrtAttack * (float) Math.PI));
      float tilt = hands.ans$calculateMapTilt(xRot);
      ps.translate(0.0F, 0.04F + inverseArmHeight * -1.2F + tilt * -0.5F, -0.72F);
      ps.mulPose(Axis.XP.rotationDegrees(tilt * -85.0F));
      ps.mulPose(Axis.XP.rotationDegrees(Mth.sin(sqrtAttack * (float) Math.PI) * 20.0F));
      ps.scale(2.0F, 2.0F, 2.0F);
      hands.ans$renderMap(ps, collector, light, map);
   }

   private static void oneHandedMap(
      ItemInHandRendererAccessor hands,
      PoseStack ps,
      SubmitNodeCollector collector,
      int light,
      float inverseArmHeight,
      HumanoidArm arm,
      float attack,
      ItemStack map
   ) {
      float invert = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      ps.translate(invert * 0.125F, -0.125F, 0.0F);
      ps.translate(invert * 0.51F, -0.08F + inverseArmHeight * -1.2F, -0.75F);
      float sqrtAttack = Mth.sqrt(attack);
      float xSwing = Mth.sin(sqrtAttack * (float) Math.PI);
      ps.translate(invert * -0.5F * xSwing, 0.4F * Mth.sin(sqrtAttack * (float) (Math.PI * 2)) - 0.3F * xSwing, -0.3F * Mth.sin(attack * (float) Math.PI));
      ps.mulPose(Axis.XP.rotationDegrees(xSwing * -45.0F));
      ps.mulPose(Axis.YP.rotationDegrees(invert * xSwing * -30.0F));
      hands.ans$renderMap(ps, collector, light, map);
   }

   public static String describe(boolean variables) {
      EntityRuntime rt = runtime;
      return rt == null ? "(no first-person runtime yet)" : rt.describe(variables);
   }
}
