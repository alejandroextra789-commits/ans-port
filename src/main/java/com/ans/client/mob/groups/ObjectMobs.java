package com.ans.client.mob.groups;

import com.ans.Ans;
import com.ans.client.equipment.MobEquipment;
import com.ans.client.mob.AnimatedMob;
import com.ans.client.molang.EngineVariables;
import com.ans.client.render.BedrockTextures;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.alchemy.PotionUtils;

public final class ObjectMobs {
   private static final Map<String, Integer> POTION_IDS = new HashMap<>();

   private ObjectMobs() {
   }

   public static void register() {
      projectiles();
      potions();
      minecarts();
      AnimatedMob.builder(EntityType.EXPERIENCE_ORB, "xp_orb").shadow(0.5F).register();
      AnimatedMob.builder(EntityType.EVOKER_FANGS, "evocation_fang").register();
      AnimatedMob.builder(EntityType.FIREWORK_ROCKET, "fireworks_rocket").register();
      AnimatedMob.builder(EntityType.END_CRYSTAL, "ender_crystal").register();
      armorStand();
   }

   private static void projectiles() {
      AnimatedMob.builder(EntityType.ARROW).register();
      AnimatedMob.builder(EntityType.SNOWBALL).register();
      AnimatedMob.builder(EntityType.ENDER_PEARL).register();
      AnimatedMob.builder(EntityType.EYE_OF_ENDER, "eye_of_ender_signal").register();
      AnimatedMob.builder(EntityType.EXPERIENCE_BOTTLE, "xp_bottle").register();
      AnimatedMob.builder(EntityType.FIREBALL).register();
      AnimatedMob.builder(EntityType.SMALL_FIREBALL).register();
      AnimatedMob.builder(EntityType.DRAGON_FIREBALL).register();
      AnimatedMob.builder(EntityType.SHULKER_BULLET).register();
      ResourceLocation dangerous = Ans.id("entity/wither_skull_dangerous.json");
      AnimatedMob.builder(EntityType.WITHER_SKULL).bundleFor(s -> s.isDangerous() ? dangerous : null).register();
      // Egg climate variants were added after Minecraft 1.20.1; this target has only the temperate variant.
      AnimatedMob.builder(EntityType.EGG).property("minecraft:climate_variant", e -> "temperate").register();
   }

   private static double potionId(ThrowableItemProjectile e) {
      Potion potion = PotionUtils.getPotion(e.getItem());
      String key = net.minecraft.core.registries.BuiltInRegistries.POTION.getKey(potion).getPath();
      return key.isEmpty() ? 0.0 : POTION_IDS.getOrDefault(key, 0).intValue();
   }

   private static void potions() {
      AnimatedMob.builder(EntityType.SPLASH_POTION).query("variant", ObjectMobs::potionId).register();
      AnimatedMob.builder(EntityType.LINGERING_POTION).query("variant", ObjectMobs::potionId).register();
   }

   private static void minecarts() {
      BedrockTextures.composite(
         new ResourceLocation("minecraft", "textures/block/repeating_command_block_front_frame0.png"),
         16,
         16,
         BedrockTextures.crop(new ResourceLocation("minecraft", "textures/block/repeating_command_block_front.png"), 0, 0, 16, 16, 0, 0, false)
      );
      BedrockTextures.composite(
         new ResourceLocation("minecraft", "textures/block/repeating_command_block_back_frame0.png"),
         16,
         16,
         BedrockTextures.crop(new ResourceLocation("minecraft", "textures/block/repeating_command_block_back.png"), 0, 0, 16, 16, 0, 0, false)
      );
      minecart(EntityType.MINECART, "minecart");
      minecart(EntityType.CHEST_MINECART, "chest_minecart");
      minecart(EntityType.COMMAND_BLOCK_MINECART, "command_block_minecart");
      minecart(EntityType.HOPPER_MINECART, "hopper_minecart");
      minecart(EntityType.TNT_MINECART, "tnt_minecart");
   }

   private static <E extends AbstractMinecart> void minecart(EntityType<E> type, String id) {
      EngineVariables.registerForPartial(type, "rail_rotation.z", (e, pt) -> Mth.lerp(pt, e.xRotO, e.getXRot()));
      AnimatedMob.builder(type, id).yaw((e, pt) -> Mth.wrapDegrees(Mth.rotLerp(pt, e.yRotO, e.getYRot()))).register();
   }

   private static void armorStand() {
      MobEquipment.humanoid(EntityType.ARMOR_STAND, true, true);
      AnimatedMob.builder(EntityType.ARMOR_STAND).query("hurt_direction", e -> e.level().getGameTime() - e.lastHit < 5L ? -1.0 : 0.0).register();
   }

   static {
      String[] ids = new String[]{
         "water",
         "mundane",
         "long_mundane",
         "thick",
         "awkward",
         "night_vision",
         "long_night_vision",
         "invisibility",
         "long_invisibility",
         "leaping",
         "long_leaping",
         "strong_leaping",
         "fire_resistance",
         "long_fire_resistance",
         "swiftness",
         "long_swiftness",
         "strong_swiftness",
         "slowness",
         "long_slowness",
         "water_breathing",
         "long_water_breathing",
         "healing",
         "strong_healing",
         "harming",
         "strong_harming",
         "poison",
         "long_poison",
         "strong_poison",
         "regeneration",
         "long_regeneration",
         "strong_regeneration",
         "strength",
         "long_strength",
         "strong_strength",
         "weakness",
         "long_weakness",
         "wither",
         "turtle_master",
         "long_turtle_master",
         "strong_turtle_master",
         "slow_falling",
         "long_slow_falling",
         "strong_slowness",
         "wind_charged",
         "weaving",
         "oozing",
         "infested"
      };

      for (int i = 0; i < ids.length; i++) {
         POTION_IDS.put(ids[i], i);
      }
   }
}
