package com.ans.client.sound;

import com.ans.client.mob.AnimatedMob;
import com.ans.client.mob.AnimatedMobs;
import com.ans.AnsClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class MobAttackSounds {
   private MobAttackSounds() {
   }

   static void init() {
      AnsClientEvents.registerEndTick(MobAttackSounds::tick);
   }

   private static void tick(Minecraft mc) {
      if (mc.level != null && !mc.isPaused()) {
         for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof LivingEntity living && !(e instanceof Player) && living.swinging && living.swingTime == 0) {
               AnimatedMob<?> mob = AnimatedMobs.get(e.getType());
               if (mob != null && PackSounds.hasOverride(mob.id(), "attack")) {
                  PackSounds.Override o = PackSounds.entityOverride(mob.id(), e, "attack");
                  if (o != null && !o.mute() && o.id() != null) {
                     PackSounds.playOverride(o, e.getX(), e.getY(), e.getZ(), e.getSoundSource(), "attack swing of " + mob.id() + " #" + e.getId());
                  }
               }
            }
         }
      }
   }
}
