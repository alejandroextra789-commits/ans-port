package com.ans.client.sound;

import com.ans.Ans;
import com.ans.client.mob.player.PlayerRendering;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.ans.AnsClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class LocalPlayerSounds {
   private static final long STALE_MS = 100L;
   private static final Map<Integer, Long> LAST_TICK = new ConcurrentHashMap<>();
   private static boolean failed;

   private LocalPlayerSounds() {
   }

   public static void init() {
      AnsClientEvents.registerEndTick(LocalPlayerSounds::tick);
   }

   public static void ran(int entityId) {
      LAST_TICK.put(entityId, System.nanoTime());
   }

   private static void tick(Minecraft mc) {
      LocalPlayer player = mc.player;
      if (player != null && mc.level != null && !failed && PlayerRendering.replaces(player)) {
         if (LAST_TICK.size() > 4096) {
            LAST_TICK.clear();
         }

         Long last = LAST_TICK.get(player.getId());
         if (last == null || System.nanoTime() - last >= 100000000L) {
            try {
               mc.getEntityRenderDispatcher().extractEntity(player, mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
            } catch (RuntimeException e) {
               failed = true;
               Ans.LOGGER.error("[ans] could not run the local player's animations for its sounds (disabled)", e);
            }
         }
      } else {
         LAST_TICK.clear();
      }
   }
}
