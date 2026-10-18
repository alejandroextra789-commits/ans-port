package com.ans.client.mob.groups;

import com.ans.client.equipment.MobEquipment;
import com.ans.client.mob.AnimatedMob;
import java.util.List;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

public final class UndeadMobs {
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

   private UndeadMobs() {
   }

   public static void register() {
      MobEquipment.humanoid(EntityType.ZOMBIE, true, true);
      MobEquipment.humanoid(EntityType.HUSK, true, true);
      MobEquipment.humanoid(EntityType.DROWNED, true, true);
      MobEquipment.humanoid(EntityType.ZOMBIE_VILLAGER, true, true);
      AnimatedMob.builder(EntityType.ZOMBIE).register();
      AnimatedMob.builder(EntityType.HUSK).register();
      AnimatedMob.builder(EntityType.DROWNED).register();
      AnimatedMob.builder(EntityType.ZOMBIE_VILLAGER, "zombie_villager_v2")
         .query("variant", e -> RegistryIndex.of(PROFESSIONS, BuiltInRegistries.VILLAGER_PROFESSION, e.getVillagerData().getProfession()))
         .query("mark_variant", e -> RegistryIndex.of(TYPES, BuiltInRegistries.VILLAGER_TYPE, e.getVillagerData().getType()))
         .register();
      MobEquipment.humanoid(EntityType.SKELETON, true, false);
      MobEquipment.humanoid(EntityType.STRAY, true, false);
      AnimatedMob.builder(EntityType.SKELETON).register();
      AnimatedMob.builder(EntityType.STRAY).register();
      MobEquipment.humanoid(EntityType.WITHER_SKELETON, true, false);
      AnimatedMob.builder(EntityType.WITHER_SKELETON).actorScale(e -> 1.2).register();
   }

}
