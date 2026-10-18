package com.ans.client.mob.groups;

import com.ans.client.mob.AnimatedMob;
import com.ans.client.molang.EngineVariables;
import com.ans.client.molang.MolangQueries;
import java.util.function.BiFunction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;

public final class MonsterMobs {
   private MonsterMobs() {
   }

   public static void register() {
      AnimatedMob.builder(EntityType.CREEPER).query("swelling_dir", Creeper::getSwellDir).query("is_powered", c -> c.isPowered() ? 1.0 : 0.0).register();
      partialTickQuery(EntityType.CREEPER, "swell_amount", (c, pt) -> (double)c.getSwelling(pt));
      AnimatedMob.builder(EntityType.SPIDER).deathRotation(180.0F).register();
      AnimatedMob.builder(EntityType.CAVE_SPIDER).deathRotation(180.0F).register();
      AnimatedMob.builder(EntityType.ENDERMAN)
         .query("is_carrying_block", e -> e.getCarriedBlock() != null ? 1.0 : 0.0)
         .query("is_angry", e -> e.isCreepy() ? 1.0 : 0.0)
         .register();
      AnimatedMob.builder(EntityType.IRON_GOLEM).register();
      EngineVariables.registerFor(EntityType.IRON_GOLEM, "attack_animation_tick", g -> g.getAttackAnimationTick());
      EngineVariables.registerFor(EntityType.IRON_GOLEM, "offer_flower_tick", g -> g.getOfferFlowerTick());
   }

   private static <E extends LivingEntity> void partialTickQuery(EntityType<E> type, String name, BiFunction<E, Float, Double> fn) {
      MolangQueries.registerFor(type, name, var1x -> c -> c.entity == null ? 0.0 : fn.apply((E)c.entity, c.partialTick));
   }
}
