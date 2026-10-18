package com.ans.client.mob.groups;

import com.ans.Ans;
import com.ans.client.equipment.MobEquipment;
import com.ans.client.mob.AnimatedMob;
import com.ans.client.molang.EngineVariables;
import com.ans.client.render.EffectsOnlyRuntimes;
import java.util.List;
import com.ans.AnsPlatform;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.CrossbowItem;

public final class VillageMobs {
   private static final List<String> PROFESSIONS = List.of(
      "none",
      "farmer",
      "fisherman",
      "shepherd",
      "fletcher",
      "librarian",
      "cartographer",
      "cleric",
      "armorer",
      "weaponsmith",
      "toolsmith",
      "butcher",
      "leatherworker",
      "mason",
      "nitwit"
   );
   private static final List<String> TYPES = List.of("plains", "desert", "jungle", "savanna", "snow", "swamp", "taiga");

   private VillageMobs() {
   }

   public static void register() {
      villagers();
      illagers();
      AnimatedMob.builder(EntityType.VEX).query("is_charging", v -> v.isCharging() ? 1.0 : 0.0).register();
      AnimatedMob.builder(EntityType.WITCH).register();
      AnimatedMob.builder(EntityType.RAVAGER)
         .query("is_delayed_attacking", r -> r.getAttackTick() > 0 ? 1.0 : 0.0)
         .query("is_stunned", r -> r.getStunnedTick() > 0 ? 1.0 : 0.0)
         .query("is_roaring", r -> r.getRoarTick() > 0 ? 1.0 : 0.0)
         .register();
      AnimatedMob.builder(EntityType.ALLAY).query("is_dancing", a -> a.isDancing() ? 1.0 : 0.0).register();
      AnimatedMob.builder(EntityType.SNOW_GOLEM).query("is_sheared", g -> g.hasPumpkin() ? 0.0 : 1.0).shadow(50.0F).register();
   }

   private static void villagers() {
      boolean villagerNews = AnsPlatform.isModLoaded("villagernews");
      AnimatedMob.Builder<Villager> villager = AnimatedMob.builder(EntityType.VILLAGER, "villager_v2")
         .query("variant", e -> index(PROFESSIONS, e.getVillagerData().profession()))
         .query("mark_variant", e -> index(TYPES, e.getVillagerData().type()))
         .query("trade_tier", e -> Mth.clamp(e.getVillagerData().level() - 1, 0, 4))
         .query("skin_id", e -> Math.floorMod(e.getUUID().hashCode(), 6));
      if (villagerNews) {
         Ans.LOGGER.info("[ans] Villager News is installed: leaving the villager and wandering trader renderers to it (villager: pack sounds/particles only)");
         if (EffectsOnlyRuntimes.ENABLED) {
            villager.effectsOnly("body", "head", "root_leftleg", "root_rightleg").register();
         }
      } else {
         MobEquipment.builder().headItems(true).register(EntityType.VILLAGER);
         MobEquipment.builder().headItems(true).register(EntityType.WANDERING_TRADER);
         villager.register();
         AnimatedMob.builder(EntityType.WANDERING_TRADER).register();
      }
   }

   private static void illagers() {
      for (EntityType<? extends Mob> type : List.of(EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER)) {
         MobEquipment.builder().headItems(true).register(type);
      }

      EngineVariables.registerFor(EntityType.VINDICATOR, "has_target", m -> m.isAggressive() ? 1.0 : 0.0);
      EngineVariables.registerFor(EntityType.PILLAGER, "has_target", VillageMobs::pillagerHasTarget);
      EngineVariables.registerFor(EntityType.EVOKER, "has_target", e -> !e.isAggressive() && !e.isCastingSpell() ? 0.0 : 1.0);
      AnimatedMob.builder(EntityType.PILLAGER).query("has_target", VillageMobs::pillagerHasTarget).register();
      AnimatedMob.builder(EntityType.VINDICATOR).register();
      AnimatedMob.builder(EntityType.EVOKER, "evocation_illager")
         .query("is_casting", e -> e.isCastingSpell() ? 1.0 : 0.0)
         .query("has_target", e -> !e.isAggressive() && !e.isCastingSpell() ? 0.0 : 1.0)
         .register();
   }

   private static double pillagerHasTarget(Pillager p) {
      return !p.isAggressive() && !p.isChargingCrossbow() && !CrossbowItem.isCharged(p.getMainHandItem()) ? 0.0 : 1.0;
   }


   private static int index(List<String> order, Holder<?> holder) {
      int i = holder.unwrapKey().map(k -> order.indexOf(k.location().getPath())).orElse(0);
      return Math.max(0, i);
   }
}
