package com.ans.client.mob.nether;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

public final class ShotTracker {
   private static final Map<LivingEntity, ShotTracker.State> STATES = new WeakHashMap<>();
   private static final int FRESH = 3;

   private ShotTracker() {
   }

   public static synchronized ShotTracker.State update(LivingEntity e, boolean charged, Class<? extends Projectile> projectile, double range) {
      ShotTracker.State s = STATES.computeIfAbsent(e, var0x -> new ShotTracker.State());
      int now = e.tickCount;
      boolean flipped = charged != s.charged;
      if (flipped) {
         if (charged) {
            s.chargeStart = now;
            s.shotThisCharge = false;
         } else {
            s.chargeEnd = now;
         }

         s.charged = charged;
      }

      boolean recent = charged || now - s.chargeEnd <= 20;
      if (recent && (flipped || s.scannedTick != now)) {
         s.scannedTick = now;
         boolean fired = !e.level().getEntitiesOfClass(projectile, e.getBoundingBox().inflate(range), p -> p.tickCount < 3 && p.getOwner() == e).isEmpty();
         if (fired) {
            s.lastShot = now;
            if (charged) {
               s.shotThisCharge = true;
            }
         }
      }

      return s;
   }

   public static final class State {
      public boolean charged;
      public boolean shotThisCharge;
      public int chargeStart = -1073741824;
      public int chargeEnd = -1073741824;
      public int lastShot = -1073741824;
      private int scannedTick = Integer.MIN_VALUE;

      public int sinceChargeEnd(LivingEntity e) {
         return e.tickCount - this.chargeEnd;
      }

      public int sinceShot(LivingEntity e) {
         return e.tickCount - this.lastShot;
      }
   }
}
