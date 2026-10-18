package com.ans.client.mob.groups;

import com.ans.client.mob.AnimatedMob;
import com.ans.client.mob.pets.FoxHeldItemLayer;
import com.ans.client.mob.pets.WolfArmorLayer;
import com.ans.client.molang.EngineVariables;
import com.ans.client.molang.MolangQueries;
import com.ans.client.render.BedrockTextures;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

public final class PetMobs {
   private static final List<String> WOLF_VARIANTS = List.of("pale", "ashen", "black", "chestnut", "rusty", "snowy", "spotted", "striped", "woods");
   private static final List<String> CAT_VARIANTS = List.of(
      "white", "black", "red", "siamese", "british_shorthair", "calico", "persian", "ragdoll", "tabby", "all_black", "jellie"
   );

   private PetMobs() {
   }

   public static void register() {
      wolf();
      cat();
      AnimatedMob.builder(EntityType.OCELOT).register();
      parrot();
      fox();
      horses();
      camels();
      traderLlama();
      panda();
      polarBear();
      AnimatedMob.builder(EntityType.MOOSHROOM).query("variant", m -> m.getVariant().ordinal()).register();
      bee();
   }

   private static void wolf() {
      AnimatedMob.builder(EntityType.WOLF)
         .query("variant", w -> 0.0) // Wolf coat variants do not exist in Minecraft 1.20.1
         .query("is_interested", w -> w.isInterested() ? 1.0 : 0.0)
         .query("is_shaking_wetness", w -> w.getShakeAnim(1.0F) > 0.0F ? 1.0 : 0.0)
         .query("tail_angle", Wolf::getTailAngle)
         .changeColor((w, pt) -> w.isTame() ? collar(w.getCollarColor()) : -1)
         .register();
      WolfArmorLayer.install();
   }

   private static void cat() {
      AnimatedMob.builder(EntityType.CAT)
         .query("variant", c -> index(CAT_VARIANTS, c.getVariant()))
         .query("is_sleeping", c -> c.isLying() ? 1.0 : 0.0)
         .changeColor((c, pt) -> c.isTame() ? collar(c.getCollarColor()) : -1)
         .actorScale(c -> c.isBaby() ? 0.4 : 0.8)
         .register();
      EngineVariables.registerForPartial(EntityType.CAT, "liedownamount", (c, pt) -> c.getLieDownAmount(pt));
      EngineVariables.registerFor(EntityType.CAT, "lieonplayer", c -> c.isLyingOnTopOfSleepingPlayer() ? 1.0 : 0.0);

      for (String v : CAT_VARIANTS) {
         BedrockTextures.composite(
            vanilla("cat/cat_" + v + "_tame_baby"),
            32,
            32,
            BedrockTextures.part(vanilla("cat/cat_" + v + "_baby"), 0, 0, false),
            BedrockTextures.part(vanilla("cat/cat_collar_baby"), 0, 0, true)
         );
      }
   }

   private static void parrot() {
      AnimatedMob.builder(EntityType.PARROT)
         .query("variant", p -> p.getVariant().ordinal())
         .query("is_dancing", p -> p.isPartyParrot() ? 1.0 : 0.0)
         .register();
   }

   private static void fox() {
      AnimatedMob.builder(EntityType.FOX)
         .query("variant", f -> f.getVariant().ordinal())
         .query("is_sitting", f -> f.isSitting() ? 1.0 : 0.0)
         .query("is_stalking", f -> f.isCrouching() ? 1.0 : 0.0)
         .query("has_target", f -> !f.isAggressive() && !f.isPouncing() ? 0.0 : 1.0)
         .register();
      FoxHeldItemLayer.install();
   }

   private static void horses() {
      AnimatedMob.builder(EntityType.HORSE).query("variant", h -> h.getVariant().ordinal()).query("mark_variant", h -> h.getMarkings().ordinal()).register();
      AnimatedMob.builder(EntityType.DONKEY).register();
      AnimatedMob.builder(EntityType.MULE).register();
      AnimatedMob.builder(EntityType.SKELETON_HORSE).register();
      AnimatedMob.builder(EntityType.ZOMBIE_HORSE).register();

      for (EntityType<? extends AbstractHorse> type : List.of(
         EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.SKELETON_HORSE, EntityType.ZOMBIE_HORSE
      )) {
         EngineVariables.registerFor(type, "armor_texture_slot", PetMobs::horseArmorSlot);
         EngineVariables.registerForPartial(type, "stand_anim", (h, pt) -> h.getStandAnim(pt));
         EngineVariables.registerFor(type, "shake_tail", h -> h.tailCounter > 0 ? 1.0 : 0.0);
      }

      for (String coat : List.of("horse_white", "horse_creamy", "horse_chestnut", "horse_brown", "horse_black", "horse_gray", "horse_darkbrown")) {
         saddled("horse/" + coat, 64, "horse_saddle");
      }

      saddled("horse/donkey", 64, "donkey_saddle");
      saddled("horse/mule", 64, "mule_saddle");
      saddled("horse/horse_skeleton", 64, "skeleton_horse_saddle");
      saddled("horse/horse_zombie", 64, "zombie_horse_saddle");
      saddled("camel/camel", 128, "camel_saddle");
   }

   private static void camels() {
      AnimatedMob.builder(EntityType.CAMEL).actorScale(c -> c.isBaby() ? 0.45 : 1.0).register();
   }

   private static void traderLlama() {
      EngineVariables.registerFor(EntityType.TRADER_LLAMA, "DecorTextureIndex", PetMobs::carpetIndex);
      AnimatedMob.builder(EntityType.TRADER_LLAMA).query("variant", l -> l.getVariant().ordinal()).query("mark_variant", var0 -> 1.0).register();
   }

   private static void panda() {
      AnimatedMob.builder(EntityType.PANDA)
         .query("variant", p -> p.getVariant().ordinal())
         .query("is_eating", p -> p.isEating() ? 1.0 : 0.0)
         .query("roll_counter", p -> p.rollCounter)
         .query("sneeze_counter", p -> p.getSneezeCounter())
         .register();
      MolangQueries.registerFor(EntityType.PANDA, "sit_amount", partial((p, pt) -> (double)p.getSitAmount(pt)));
      MolangQueries.registerFor(EntityType.PANDA, "lie_amount", partial((p, pt) -> (double)p.getLieOnBackAmount(pt)));
   }

   private static void polarBear() {
      AnimatedMob.builder(EntityType.POLAR_BEAR).register();
      MolangQueries.registerFor(EntityType.POLAR_BEAR, "standing_scale", partial((b, pt) -> (double)b.getStandingAnimationScale(pt)));
   }


   private static void bee() {
      AnimatedMob.builder(EntityType.BEE).property("minecraft:has_nectar", b -> b.hasNectar()).query("mark_variant", b -> b.hasStung() ? 1.0 : 0.0).register();
   }

   private static int collar(DyeColor color) {
      return 0xFF000000 | color.getTextureDiffuseColor() & 16777215;
   }

   private static int index(List<String> order, Holder<?> holder) {
      if (holder == null) {
         return 0;
      }

      int i = holder.unwrapKey().map(k -> order.indexOf(k.location().getPath())).orElse(0);
      return Math.max(0, i);
   }

   private static int horseArmorSlot(LivingEntity entity) {
      // Horse armor is still present in 1.20.1, but it is exposed through
      // AbstractHorse rather than the later generic BODY equipment slot.
      ItemStack body = entity instanceof AbstractHorse horse ? horse.getArmor() : ItemStack.EMPTY;
      if (body.isEmpty()) {
         return 0;
      }

      return switch (BuiltInRegistries.ITEM.getKey(body.getItem()).getPath()) {
         case "leather_horse_armor" -> 1;
         case "iron_horse_armor" -> 2;
         case "golden_horse_armor" -> 3;
         case "diamond_horse_armor" -> 4;
         case "copper_horse_armor" -> 5;
         case "netherite_horse_armor" -> 6;
         default -> 0;
      };
   }

   private static int carpetIndex(LivingEntity entity) {
      // In 1.20.1 llamas expose their carpet color directly; the later BODY
      // equipment slot does not exist yet.
      if (entity instanceof Llama llama) {
         int swag = llama.getSwag();
         return swag < 0 ? 0 : swag + 1;
      }
      return 0;
   }

   private static <E extends LivingEntity> MolangQueries.Factory partial(BiFunction<E, Float, Double> fn) {
      return var1 -> c -> c.entity == null ? 0.0 : fn.apply((E)c.entity, c.partialTick);
   }

   private static void saddled(String coat, int size, String saddleAsset) {
      BedrockTextures.composite(
         vanilla(coat),
         size,
         size,
         BedrockTextures.part(vanilla(coat), 0, 0, false),
         BedrockTextures.part(vanilla("equipment/" + saddleAsset + "/saddle"), 0, 0, false)
      );
   }

   private static ResourceLocation vanilla(String entityTexture) {
      return new ResourceLocation("minecraft", "textures/entity/" + entityTexture + ".png");
   }
}
