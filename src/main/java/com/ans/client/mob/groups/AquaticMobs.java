package com.ans.client.mob.groups;

import com.ans.client.bedrock.ChangeColors;
import com.ans.client.mob.AnimatedMob;
import com.ans.client.mob.aquatic.DolphinItemLayer;
import com.ans.client.mob.aquatic.GuardianBeamLayer;
import com.ans.client.molang.EngineVariables;
import com.ans.client.molang.MolangQueries;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.Holder;
import com.ans.client.util.ArgbCompat;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.axolotl.Axolotl.Variant;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish.Base;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.monster.Guardian;

public final class AquaticMobs {
   private static final List<Variant> AXOLOTLS = List.of(Variant.LUCY, Variant.CYAN, Variant.GOLD, Variant.WILD, Variant.BLUE);
   private static final List<String> FROGS = List.of("temperate", "cold", "warm");

   private AquaticMobs() {
   }

   public static void register() {
      fish();
      squids();
      axolotl();
      dolphin();
      turtle();
      guardians();
      frog();
   }

   private static void fish() {
      AnimatedMob.builder(EntityType.COD).register();
      AnimatedMob.builder(EntityType.SALMON).actorScale(Salmon::getSalmonScale).register();
      AnimatedMob.builder(EntityType.PUFFERFISH).query("variant", Pufferfish::getPuffState).actorScale(var0 -> 1.2).register();
      AnimatedMob.builder(EntityType.TROPICAL_FISH, "tropicalfish").actorScale(var0 -> 1.3).register();
      EngineVariables.registerFor(EntityType.TROPICAL_FISH, "TropicalFish.Base", f -> f.getPattern().base() == Base.SMALL ? 0.0 : 1.0);
      EngineVariables.registerFor(EntityType.TROPICAL_FISH, "TropicalFish.Pattern", f -> f.getPattern().ordinal());
      ChangeColors.registerTextureTints(
         EntityType.TROPICAL_FISH, (f, var1x, i) -> ArgbCompat.opaque((i == 0 ? f.getBaseColor() : f.getPatternColor()).getTextureDiffuseColor())
      );

      for (EntityType<? extends LivingEntity> type : List.of(EntityType.COD, EntityType.SALMON, EntityType.TROPICAL_FISH)) {
         EngineVariables.registerFor(type, "AnimationAmount", e -> e.tickCount);
         EngineVariables.registerFor(type, "AnimationAmountPrev", e -> e.tickCount - 1);
      }

      for (EntityType<? extends LivingEntity> type : List.of(EntityType.COD, EntityType.SALMON, EntityType.TROPICAL_FISH, EntityType.PUFFERFISH)) {
         pitchQuery(type);
      }
   }

   private static <E extends LivingEntity> void pitchQuery(EntityType<E> type) {
      partialTickQuery(type, "body_x_rotation", (e, pt) -> (double)Mth.lerp(pt, e.xRotO, e.getXRot()));
   }

   private static void squids() {
      AnimatedMob.builder(EntityType.SQUID).register();
      AnimatedMob.builder(EntityType.GLOW_SQUID).register();

      for (EntityType<? extends Squid> type : List.of(EntityType.SQUID, EntityType.GLOW_SQUID)) {
         squid(type);
      }

      EngineVariables.registerFor(EntityType.GLOW_SQUID, "glow_alpha", s -> Mth.clamp(s.getDarkTicksRemaining() / 10.0, 0.0, 1.0));
   }

   private static <E extends Squid> void squid(EntityType<E> type) {
      partialTickQuery(type, "body_x_rotation", (s, pt) -> (double)Mth.lerp(pt, s.xBodyRotO, s.xBodyRot));
      EngineVariables.registerForPartial(type, "squid.swim_rotation", (s, pt) -> Mth.lerp(pt, s.zBodyRotO, s.zBodyRot));
   }

   private static void axolotl() {
      AnimatedMob.builder(EntityType.AXOLOTL)
         .query("variant", a -> Math.max(0, AXOLOTLS.indexOf(a.getVariant())))
         .query("is_playing_dead", a -> a.isPlayingDead() ? 1.0 : 0.0)
         .register();
      pitchQuery(EntityType.AXOLOTL);
   }

   private static void dolphin() {
      AnimatedMob.builder(EntityType.DOLPHIN).register();
      pitchQuery(EntityType.DOLPHIN);
      DolphinItemLayer.install();
   }

   private static void turtle() {
      AnimatedMob.builder(EntityType.TURTLE).actorScale(t -> t.isBaby() ? 0.16 : 1.0).register();
   }

   private static void guardians() {
      for (EntityType<? extends Guardian> type : List.of(EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN)) {
         AnimatedMob.builder(type).register();
         perType(type, "has_target", g -> !g.hasActiveAttackTarget() && !g.isAggressive() ? 0.0 : 1.0);
      }

      GuardianBeamLayer.install();
   }

   private static void frog() {
      AnimatedMob.builder(EntityType.FROG)
         .query("variant", f -> index(FROGS, f.getVariant()))
         .query("is_croaking", f -> f.getPose() == Pose.CROAKING ? 1.0 : 0.0)
         .query("is_eating_mob", f -> f.getPose() == Pose.USING_TONGUE ? 1.0 : 0.0)
         .register();
   }



   private static int index(List<String> order, Holder<?> holder) {
      int i = holder.unwrapKey().map(k -> order.indexOf(k.location().getPath())).orElse(0);
      return Math.max(0, i);
   }

   private static <E extends LivingEntity> void perType(EntityType<E> type, String name, ToDoubleFunction<E> fn) {
      MolangQueries.registerFor(type, name, fn);
   }

   private static <E extends LivingEntity> void partialTickQuery(EntityType<E> type, String name, BiFunction<E, Float, Double> fn) {
      MolangQueries.registerFor(type, name, var1x -> c -> c.entity == null ? 0.0 : fn.apply((E)c.entity, c.partialTick));
   }
}
