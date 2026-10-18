package com.ans.client.mob.groups;

import com.ans.client.equipment.MobEquipment;
import com.ans.client.mob.AnimatedMob;
import java.util.List;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.core.Holder;
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
      MobEquipment.humanoid(EntityType.ZOMBIE, ModelLayers.ZOMBIE_ARMOR, ModelLayers.ZOMBIE_BABY_ARMOR);
      MobEquipment.humanoid(EntityType.HUSK, ModelLayers.HUSK_ARMOR, ModelLayers.HUSK_BABY_ARMOR);
      MobEquipment.humanoid(EntityType.DROWNED, ModelLayers.DROWNED_ARMOR, ModelLayers.DROWNED_BABY_ARMOR);
      MobEquipment.humanoid(EntityType.ZOMBIE_VILLAGER, ModelLayers.ZOMBIE_VILLAGER_ARMOR, ModelLayers.ZOMBIE_VILLAGER_BABY_ARMOR);
      AnimatedMob.builder(EntityType.ZOMBIE).register();
      AnimatedMob.builder(EntityType.HUSK).register();
      AnimatedMob.builder(EntityType.DROWNED).register();
      AnimatedMob.builder(EntityType.ZOMBIE_VILLAGER, "zombie_villager_v2")
         .query("variant", e -> index(PROFESSIONS, e.getVillagerData().profession()))
         .query("mark_variant", e -> index(TYPES, e.getVillagerData().type()))
         .register();
      MobEquipment.humanoid(EntityType.SKELETON, ModelLayers.SKELETON_ARMOR, null);
      MobEquipment.humanoid(EntityType.STRAY, ModelLayers.STRAY_ARMOR, null);
      AnimatedMob.builder(EntityType.SKELETON).register();
      AnimatedMob.builder(EntityType.STRAY).register();
      MobEquipment.humanoid(EntityType.WITHER_SKELETON, ModelLayers.WITHER_SKELETON_ARMOR, null);
      AnimatedMob.builder(EntityType.WITHER_SKELETON).actorScale(e -> 1.2).register();
   }

   private static int index(List<String> order, Holder<?> holder) {
      int i = holder.unwrapKey().map(k -> order.indexOf(k.location().getPath())).orElse(0);
      return Math.max(0, i);
   }
}
