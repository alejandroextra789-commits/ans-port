package com.ans.client.equipment;

import com.ans.client.molang.MolangContext;
import com.ans.client.molang.MolangQueries;
import com.ans.client.molang.Node;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;

public final class EquipmentQueries {
   private static final Map<String, Predicate<ItemStack>> TAGS = new HashMap<>();

   private EquipmentQueries() {
   }

   public static void install() {
      MolangQueries.registerString("get_equipped_item_name", a -> new MolangQueries.Bound() {
         @Override
         public double eval(MolangContext c) {
            return 0.0;
         }

         @Override
         public Object value(MolangContext c) {
            ItemStack s = EquipmentQueries.stack(c, a, 0);
            return s.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
         }
      });
      MolangQueries.register("is_item_name_any", a -> c -> {
         ItemStack s = stack(c, a, 0);
         int from = a.length > 2 && !(a[1].value(c) instanceof String) ? 2 : 1;
         String id = s.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(s.getItem()).toString();

         for (int i = from; i < a.length; i++) {
            String n = MolangQueries.string(a[i], c);
            if (!n.isEmpty() && !n.equals("minecraft:air") && !n.equals("air")) {
               if (!s.isEmpty() && (n.contains(":") ? n : "minecraft:" + n).equals(id)) {
                  return 1.0;
               }
            } else if (s.isEmpty()) {
               return 1.0;
            }
         }

         return 0.0;
      });
      MolangQueries.register("is_item_equipped", a -> c -> stack(c, a, 0).isEmpty() ? 0.0 : 1.0);
      MolangQueries.register("equipped_item_any_tag", a -> c -> {
         ItemStack s = stack(c, a, 0);
         if (s.isEmpty()) {
            return 0.0;
         }

         for (int i = 1; i < a.length; i++) {
            if (hasTag(s, MolangQueries.string(a[i], c))) {
               return 1.0;
            }
         }

         return 0.0;
      });
      MolangQueries.register("equipped_item_all_tags", a -> c -> {
         ItemStack s = stack(c, a, 0);
         if (!s.isEmpty() && a.length >= 2) {
            for (int i = 1; i < a.length; i++) {
               if (!hasTag(s, MolangQueries.string(a[i], c))) {
                  return 0.0;
               }
            }

            return 1.0;
         } else {
            return 0.0;
         }
      });
      MolangQueries.simple(
         "main_hand_item_use_duration", c -> c.entity != null && c.entity.isUsingItem() ? c.entity.getTicksUsingItem() / 20.0 : 0.0
      );
      MolangQueries.simple("item_in_use_duration", c -> c.entity != null && c.entity.isUsingItem() ? c.entity.getTicksUsingItem() / 20.0 : 0.0);
      MolangQueries.simple("main_hand_item_max_duration", c -> c.entity == null ? 0.0 : c.entity.getMainHandItem().getUseDuration() / 20.0);
      MolangQueries.register("has_armor_slot", a -> c -> {
         if (c.entity != null && a.length != 0) {
            EquipmentSlot s = switch ((int)a[0].eval(c)) {
               case 0 -> EquipmentSlot.HEAD;
               case 1 -> EquipmentSlot.CHEST;
               case 2 -> EquipmentSlot.LEGS;
               case 3 -> EquipmentSlot.FEET;
               default -> EquipmentSlot.CHEST;
            };
            return c.entity.getItemBySlot(s).isEmpty() ? 0.0 : 1.0;
         } else {
            return 0.0;
         }
      });
      MolangQueries.flag("facing_target_to_range_attack", e -> e instanceof Mob m && m.isAggressive() && isRangedWeapon(m.getMainHandItem()));
      MolangQueries.simple("is_avoiding_mobs", var0 -> 0.0);
      MolangQueries.simple("ticks_since_last_kinetic_weapon_hit", var0 -> -1.0);
   }

   public static EquipmentSlot slot(String bedrock) {
      return switch (bedrock.toLowerCase(Locale.ROOT)) {
         case "slot.weapon.mainhand", "main_hand", "mainhand" -> EquipmentSlot.MAINHAND;
         case "slot.weapon.offhand", "off_hand", "offhand" -> EquipmentSlot.OFFHAND;
         case "slot.armor.head" -> EquipmentSlot.HEAD;
         case "slot.armor.chest" -> EquipmentSlot.CHEST;
         case "slot.armor.legs" -> EquipmentSlot.LEGS;
         case "slot.armor.feet" -> EquipmentSlot.FEET;
         case "slot.armor.body", "slot.armor", "slot.saddle" -> null;
         default -> null;
      };
   }

   private static ItemStack stack(MolangContext c, Node[] a, int slotArg) {
      LivingEntity e = c.entity;
      if (e == null) {
         return ItemStack.EMPTY;
      }

      EquipmentSlot s = EquipmentSlot.MAINHAND;
      if (a.length > slotArg) {
         Object v = a[slotArg].value(c);
         if (v instanceof String str) {
            s = slot(str);
         } else if (v instanceof Double d && d == 1.0) {
            s = EquipmentSlot.OFFHAND;
         }
      }

      return s == null ? ItemStack.EMPTY : e.getItemBySlot(s);
   }

   private static void tag(String bedrock, TagKey<Item> java) {
      TAGS.put(bedrock, s -> s.is(java));
   }

   private static void tier(String bedrock, String prefix) {
      TAGS.put(bedrock, s -> isTool(s) && BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().startsWith(prefix));
   }

   private static boolean isTool(ItemStack s) {
      return s.is(ItemTags.SWORDS) || s.is(ItemTags.AXES) || s.is(ItemTags.PICKAXES) || s.is(ItemTags.SHOVELS) || s.is(ItemTags.HOES);
   }

   public static boolean hasTag(ItemStack stack, String tag) {
      Predicate<ItemStack> p = TAGS.get(tag.contains(":") ? tag : "minecraft:" + tag);
      return p != null && p.test(stack);
   }

   public static boolean isRangedWeapon(ItemStack stack) {
      return stack.getItem() instanceof ProjectileWeaponItem || stack.is(Items.TRIDENT);
   }

   static {
      tag("minecraft:is_sword", ItemTags.SWORDS);
      tag("minecraft:is_axe", ItemTags.AXES);
      tag("minecraft:is_pickaxe", ItemTags.PICKAXES);
      tag("minecraft:is_shovel", ItemTags.SHOVELS);
      tag("minecraft:is_hoe", ItemTags.HOES);
      tag("minecraft:is_fish", ItemTags.FISHES);
      tag("minecraft:is_meat", ItemTags.MEAT);
      tag("minecraft:boats", ItemTags.BOATS);
      tag("minecraft:boat", ItemTags.BOATS);
      tag("minecraft:arrow", ItemTags.ARROWS);
      TAGS.put("minecraft:is_trident", s -> s.is(Items.TRIDENT));
      TAGS.put("minecraft:is_tool", EquipmentQueries::isTool);
      TAGS.put("minecraft:is_food", s -> s.getItem().isEdible());
      TAGS.put("minecraft:is_armor", s -> s.is(ItemTags.HEAD_ARMOR) || s.is(ItemTags.CHEST_ARMOR) || s.is(ItemTags.LEG_ARMOR) || s.is(ItemTags.FOOT_ARMOR));
      tier("minecraft:wooden_tier", "wooden_");
      tier("minecraft:stone_tier", "stone_");
      tier("minecraft:copper_tier", "copper_");
      tier("minecraft:iron_tier", "iron_");
      tier("minecraft:golden_tier", "golden_");
      tier("minecraft:diamond_tier", "diamond_");
      tier("minecraft:netherite_tier", "netherite_");
   }
}
