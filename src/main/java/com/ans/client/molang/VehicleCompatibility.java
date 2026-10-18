package com.ans.client.molang;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;

/** Vehicle query compatibility for Minecraft 1.20.1, before VehicleEntity existed. */
public final class VehicleCompatibility {
   private VehicleCompatibility() {}

   public static int hurtTime(Entity entity) {
      if (entity instanceof Boat boat) return boat.getHurtTime();
      if (entity instanceof AbstractMinecart cart) return cart.getHurtTime();
      return 0;
   }

   public static int hurtDirection(Entity entity) {
      if (entity instanceof Boat boat) return boat.getHurtDir();
      if (entity instanceof AbstractMinecart cart) return cart.getHurtDir();
      return 0;
   }

   public static double structuralIntegrity(Entity entity) {
      if (entity instanceof Boat boat) return Math.max(0.0, 40.0 - boat.getDamage());
      if (entity instanceof AbstractMinecart cart) return Math.max(0.0, 40.0 - cart.getDamage());
      return 0.0;
   }
}
