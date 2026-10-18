package com.ans.client.molang;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** Interpolated entity rotation helpers matching Minecraft 1.20.1's API. */
public final class EntityRotation {
   private EntityRotation() {}

   public static float yaw(Entity entity, float partialTick) {
      return Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
   }

   public static float pitch(Entity entity, float partialTick) {
      return Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
   }
}
