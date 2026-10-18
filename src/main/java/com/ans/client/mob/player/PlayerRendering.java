package com.ans.client.mob.player;

import com.ans.client.bedrock.ResourceOverrides;
import com.ans.client.mob.AnimatedMob;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/** Player texture/model lookup using Minecraft 1.20.1's legacy skin API. */
public final class PlayerRendering {
   public static boolean enabled = !"false".equalsIgnoreCase(System.getProperty("ans.player", "true"));
   static final ResourceLocation WIDE_GEOMETRY = AnimatedMob.geoId("player");
   static final ResourceLocation SLIM_GEOMETRY = AnimatedMob.geoId("player_slim");
   static final ResourceLocation CAPE_GEOMETRY = AnimatedMob.geoId("player_cape");
   static final Map<String, ResourceLocation> TEST_SKINS = new ConcurrentHashMap<>();

   private PlayerRendering() {}

   public static boolean replaces(AbstractClientPlayer player) {
      return enabled;
   }

   public static ResourceLocation skinBody(AbstractClientPlayer player) {
      ResourceLocation test = TEST_SKINS.get(player.getGameProfile().getName());
      return test != null ? test : player.getSkinTextureLocation();
   }

   public static void install() {
      ResourceOverrides.texture(EntityType.PLAYER,
         (entity, key) -> key.equals("default") && entity instanceof AbstractClientPlayer player ? skinBody(player) : null);
      ResourceOverrides.geometry(EntityType.PLAYER,
         (entity, key) -> key.equals("default") && entity instanceof AbstractClientPlayer player
            && "slim".equals(player.getModelName()) ? SLIM_GEOMETRY : null);
   }

   static ResourceLocation geometry(AbstractClientPlayer player) {
      return "slim".equals(player.getModelName()) ? SLIM_GEOMETRY : WIDE_GEOMETRY;
   }
}
