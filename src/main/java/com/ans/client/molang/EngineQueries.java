package com.ans.client.molang;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class EngineQueries {
   private EngineQueries() {
   }

   public static void install() {
      bones();
      items();
      vehicles();
      objects();
   }

   private static void bones() {
      MolangQueries.registerString("bone_orientation_trs", a -> new MolangQueries.Bound() {
         @Override
         public double eval(MolangContext c) {
            return 0.0;
         }

         @Override
         public Object value(MolangContext c) {
            double[] v = a.length > 0 && c.owner instanceof MolangContext.BoneSource src ? src.boneOrientationTrs(MolangQueries.string(a[0], c)) : null;
            return MolangStruct.trs(v != null ? v : new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0});
         }
      });
      MolangQueries.registerString("bone_origin", a -> new MolangQueries.Bound() {
         @Override
         public double eval(MolangContext c) {
            return 0.0;
         }

         @Override
         public Object value(MolangContext c) {
            double[] v = a.length > 0 && c.owner instanceof MolangContext.BoneSource src ? src.boneOrigin(MolangQueries.string(a[0], c)) : null;
            if (v == null) {
               v = new double[3];
            }

            return new MolangStruct().put("x", v[0]).put("y", v[1]).put("z", v[2]);
         }
      });
   }

   private static ItemStack mainHand(MolangContext c) {
      return c.entity == null ? ItemStack.EMPTY : c.entity.getMainHandItem();
   }

   private static void items() {
      MolangQueries.simple("item_is_charged", c -> {
         ItemStack s = mainHand(c);
         return s.is(Items.CROSSBOW) && CrossbowItem.isCharged(s) ? 1.0 : 0.0;
      });
      MolangQueries.simple("get_animation_frame", c -> {
         LivingEntity e = c.entity;
         if (e == null) {
            return 0.0;
         }

         ItemStack s = c.attachable != null ? e.getItemBySlot(attachableSlot(c.attachable.itemSlot())) : e.getMainHandItem();
         if (s.is(Items.CLOCK) || s.is(Items.COMPASS) || s.is(Items.RECOVERY_COMPASS)) {
            return EngineQueries.ItemFrames.frame(s, e);
         }

         if (s.is(Items.FISHING_ROD)) {
            return fishingRodCast(s, e) ? 1.0 : 0.0;
         }

         if (!s.is(Items.CROSSBOW)) {
            return 0.0;
         }

         if (!CrossbowItem.isCharged(s)) {
            if (e.isUsingItem() && e.getUseItem() == s) {
               float pull = e.getTicksUsingItem() / Math.max(1, CrossbowItem.getChargeDuration(s));
               return pull < 0.58F ? 1.0 : (pull < 1.0F ? 2.0 : 3.0);
            } else {
               return 0.0;
            }
         } else {
            return hasFireworkCharge(s) ? 5.0 : 4.0;
         }
      });
      MolangQueries.register(
         "armor_texture_slot", a -> c -> c.entity != null && a.length != 0 ? armorTextureIndex(c.entity.getItemBySlot(armorSlot((int)a[0].eval(c)))) : 0.0
      );
      MolangQueries.register("armor_color_slot", a -> c -> {
         if (c.entity != null && a.length >= 2) {
            ItemStack s = c.entity.getItemBySlot(armorSlot((int)a[0].eval(c)));
            int argb = s.isEmpty() ? -1 : (s.getItem() instanceof DyeableLeatherItem leather ? leather.getColor(s) : -1);
            int ch = (int)a[1].eval(c);
            int v = ch == 0 ? argb >> 16 & 0xFF : (ch == 1 ? argb >> 8 & 0xFF : (ch == 2 ? argb & 0xFF : 255));
            return v / 255.0;
         } else {
            return 0.0;
         }
      });
   }

   private static EquipmentSlot attachableSlot(String slot) {
      return switch (slot) {
         case "off_hand" -> EquipmentSlot.OFFHAND;
         case "head" -> EquipmentSlot.HEAD;
         case "chest", "torso" -> EquipmentSlot.CHEST;
         case "legs" -> EquipmentSlot.LEGS;
         case "feet" -> EquipmentSlot.FEET;
         default -> EquipmentSlot.MAINHAND;
      };
   }

   private static boolean fishingRodCast(ItemStack s, LivingEntity e) {
      return e instanceof Player p && p.fishing != null && e.getItemHeldByArm(holdingArm(p)) == s;
   }

   /** 1.20.1 keeps charged crossbow projectiles in the item's NBT (the accessor is private). */
   private static boolean hasFireworkCharge(ItemStack s) {
      net.minecraft.nbt.ListTag list = s.getOrCreateTag().getList("ChargedProjectiles", net.minecraft.nbt.Tag.TAG_COMPOUND);
      for (net.minecraft.nbt.Tag t : list) {
         if (ItemStack.of((net.minecraft.nbt.CompoundTag)t).is(Items.FIREWORK_ROCKET)) {
            return true;
         }
      }
      return false;
   }

   /** Same rule as vanilla FishingHookRenderer.getHoldingArm, which is not public in 1.20.1. */
   private static net.minecraft.world.entity.HumanoidArm holdingArm(Player p) {
      return p.getMainHandItem().getItem() instanceof net.minecraft.world.item.FishingRodItem ? p.getMainArm() : p.getMainArm().getOpposite();
   }

   public static EquipmentSlot armorSlot(int n) {
      return switch (n) {
         case 0 -> EquipmentSlot.HEAD;
         case 1 -> EquipmentSlot.CHEST;
         case 2 -> EquipmentSlot.LEGS;
         case 3 -> EquipmentSlot.FEET;
         default -> EquipmentSlot.CHEST;
      };
   }

   public static int armorTextureIndex(ItemStack s) {
      if (s.isEmpty()) {
         return 0;
      }

      String path = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
      if (path.startsWith("leather")) {
         return 1;
      } else if (path.startsWith("iron") || path.startsWith("chainmail")) {
         return 2;
      } else if (path.startsWith("golden")) {
         return 3;
      } else if (path.startsWith("diamond")) {
         return 4;
      } else if (path.startsWith("copper")) {
         return 5;
      } else {
         return path.startsWith("netherite") ? 6 : 0;
      }
   }

   private static void vehicles() {
      MolangQueries.actor("structural_integrity", e -> VehicleCompatibility.structuralIntegrity(e));
      MolangQueries.flag(
         "has_dash_cooldown",
         e -> e instanceof PlayerRideableJumping j && j.getJumpCooldown() > 0
            || e instanceof Camel camel && camel.isDashing()
      );
      MolangQueries.flag("is_shield_powered", e -> e instanceof WitherBoss w && w.isPowered());
   }

   private static void objects() {
      MolangQueries.actor("texture_frame_index", e -> e instanceof ExperienceOrb orb ? orb.getIcon() : 0.0);
      MolangQueries.simple("life_span", c -> {
         if (c.actor instanceof EvokerFangs fangs) {
            float p = fangs.getAnimationProgress(c.partialTick);
            return p <= 0.0F ? 0.0 : 20.0F * (1.0F - p) + 2.0F + (c.actor.tickCount + c.partialTick);
         } else {
            return 0.0;
         }
      });
   }

   private static final class ItemFrames {
      static int frame(ItemStack stack, LivingEntity entity) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) return 0;
         if (stack.is(Items.CLOCK)) {
            // The modern item-property API is unavailable in 1.20.1; retain a smooth 64-frame day cycle.
            long time = level.getDayTime();
            return Math.floorMod((int)Math.floor((time % 24000L) * 64.0 / 24000.0), 64);
         }
         if (stack.is(Items.RECOVERY_COMPASS)) {
            // Recovery-compass tracking state is not exposed by the 1.20.1 API used here.
            return 0;
         }
         double targetX = level.getSharedSpawnPos().getX() + 0.5;
         double targetZ = level.getSharedSpawnPos().getZ() + 0.5;
         net.minecraft.nbt.CompoundTag lodestone = stack.getTagElement("LodestonePos");
         if (lodestone != null && stack.hasTag() && stack.getTag().contains("LodestoneDimension")
               && level.dimension().location().toString().equals(stack.getTag().getString("LodestoneDimension"))) {
            targetX = lodestone.getInt("X") + 0.5;
            targetZ = lodestone.getInt("Z") + 0.5;
         }
         double dx = targetX - entity.getX();
         double dz = targetZ - entity.getZ();
         double angle = Math.atan2(dz, dx) / (Math.PI * 2.0) - 0.25 - entity.getYRot() / 360.0;
         return Math.floorMod((int)Math.round(angle * 32.0), 32);
      }
   }
}
