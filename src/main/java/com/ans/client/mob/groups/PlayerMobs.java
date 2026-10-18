package com.ans.client.mob.groups;

import com.ans.client.bedrock.EntityRuntime;
import com.ans.client.bedrock.RelativeBones;
import com.ans.client.equipment.MobEquipment;
import com.ans.client.mob.AnimatedMob;
import com.ans.client.mob.player.PlayerQueries;
import com.ans.client.mob.player.PlayerRenderer;
import com.ans.client.mob.player.PlayerRendering;
import com.ans.client.mob.player.PlayerScenario;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EntityType;

public final class PlayerMobs {
   private PlayerMobs() {
   }

   public static void register() {
      PlayerQueries.install();
      PlayerRendering.install();
      RelativeBones.enable("player");
      EntityRuntime.clampNegativeWeights("player");
      MobEquipment.builder().armor(ModelLayers.PLAYER_ARMOR, null).mainHandAlwaysRight().register(EntityType.PLAYER);
      AnimatedMob.builder(EntityType.PLAYER).renderer((context, mob, var2) -> new PlayerRenderer(context, mob)).register();
      PlayerScenario.init();
   }
}
