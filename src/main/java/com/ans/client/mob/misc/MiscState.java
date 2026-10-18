package com.ans.client.mob.misc;

import com.ans.Ans;
import com.ans.client.molang.MolangContext;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Shulker;

public final class MiscState {
   private static final int SWING_TICKS = 6;
   private static final Map<Entity, Integer> COMBAT = new WeakHashMap<>();
   private static EntityDataAccessor<Byte> shulkerPeek;
   private static boolean shulkerPeekResolved;

   private MiscState() {
   }

   public static float elapsedTicks(AnimationState state, Entity entity) {
      if (!state.isStarted()) {
         return -1.0F;
      }

      float age = entity.tickCount + MolangContext.current().partialTick;
      return (float)state.getTimeInMillis(age) / 50.0F;
   }

   public static float remainingTicks(AnimationState state, Entity entity, int duration) {
      float t = elapsedTicks(state, entity);
      return !(t < 0.0F) && !(t >= duration) ? duration - t : 0.0F;
   }

   public static double swing(AnimationState state, Entity entity) {
      float t = elapsedTicks(state, entity);
      return !(t < 0.0F) && !(t >= 6.0F) ? Math.max(0.01, t / 6.0F) : 0.0;
   }

   public static boolean recentlyInCombat(Entity entity, boolean inCombat, int memoryTicks) {
      synchronized (COMBAT) {
         if (inCombat) {
            COMBAT.put(entity, entity.tickCount);
            return true;
         } else {
            Integer last = COMBAT.get(entity);
            return last != null && entity.tickCount - last <= memoryTicks && entity.tickCount >= last;
         }
      }
   }

   public static int shulkerRawPeek(Shulker shulker) {
      if (!shulkerPeekResolved) {
         shulkerPeekResolved = true;

         try {
            Field f = Shulker.class.getDeclaredField("DATA_PEEK_ID");
            f.setAccessible(true);
            shulkerPeek = (EntityDataAccessor<Byte>)f.get(null);
         } catch (ReflectiveOperationException | RuntimeException e) {
            Ans.LOGGER.warn("[ans] shulker peek data unavailable ({}); q.has_target falls back to the peek amount", e.toString());
         }
      }

      if (shulkerPeek != null) {
         return (Byte)shulker.getEntityData().get(shulkerPeek);
      } else {
         return shulker.getClientPeekAmount(1.0F) > 0.5F ? 100 : 0;
      }
   }
}
