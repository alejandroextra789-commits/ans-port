package com.ans.client.sound;

import com.ans.Ans;
import com.ans.client.mob.AnimatedMob;
import com.ans.client.mob.AnimatedMobs;
import java.util.Locale;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SoundMuting {
   private static final double RADIUS = 1.5;
   private static boolean replaying;

   private SoundMuting() {
   }

   /** Uses Forge's legacy sound event so cancellation works on Minecraft 1.20.1. */
   public static void onPlaySound(net.minecraftforge.client.event.sound.PlaySoundEvent event) {
      SoundInstance sound = event.getSound();
      if (sound == null) {
         return;
      }
      try {
         if (shouldMute(sound)) {
            event.setSound(null);
         }
      } catch (RuntimeException e) {
         Ans.LOGGER.debug("[ans] sound filter failed for {}", sound.getLocation(), e);
      }
   }

   public static boolean shouldMute(SoundInstance sound) {
      if (replaying) {
         return false;
      }

      ResourceLocation id = sound.getLocation();
      if (id != null && id.getNamespace().equals("minecraft")) {
         String path = id.getPath();
         if (PackSounds.DEBUG_ALL) {
            Ans.LOGGER
               .info(
                  String.format(Locale.ROOT, "[ans-sound] vanilla %s %s pos=%.2f,%.2f,%.2f", id, sound.getSource(), sound.getX(), sound.getY(), sound.getZ())
               );
         }

         String step = stepEvent(path);
         String voice = step == null ? voiceEvent(path) : null;
         String item = step == null && voice == null ? itemEvent(path) : null;
         boolean silenced = PackSounds.silenced(id);
         if (step == null && voice == null && item == null && !silenced) {
            return false;
         }

         Minecraft mc = Minecraft.getInstance();
         if (mc.level == null) {
            return false;
         }

         if (silenced) {
            if (PackSounds.DEBUG) {
               Ans.LOGGER.info("[ans-sound] mute {} (silenced by the pack)", id);
            }

            return true;
         } else {
            if (sound.isRelative()) {
               return false;
            }

            Vec3 at = new Vec3(sound.getX(), sound.getY(), sound.getZ());
            if (item != null) {
               return replaceItem(mc, sound, id, at, item);
            }

            Entity source = nearest(mc, at, e -> true);
            if (source == null) {
               return false;
            }

            AnimatedMob<?> mob = AnimatedMobs.get(source.getType());
            if (step == null) {
               String typePath = BuiltInRegistries.ENTITY_TYPE.getKey(source.getType()).getPath();
               if (!sameMob(path, typePath)) {
                  return false;
               }

               String bedrockId = mob != null ? mob.id() : typePath;
               PackSounds.Override o = PackSounds.entityOverride(bedrockId, source, voice);
               if (o == null) {
                  return false;
               }

               if (o.mute()) {
                  if (PackSounds.DEBUG) {
                     Ans.LOGGER.info("[ans-sound] mute {} from {} #{} (pack event '{}' muted)", new Object[]{id, bedrockId, source.getId(), voice});
                  }

                  return true;
               } else {
                  return o.id() != null && !o.id().equals(id)
                     ? replace(o, sound, id + " from " + bedrockId + " #" + source.getId() + " ('" + voice + "')")
                     : false;
               }
            } else if (mob != null && PackSounds.muted(mob.id()).contains(step)) {
               if (PackSounds.DEBUG) {
                  Ans.LOGGER
                     .info(
                        String.format(
                           Locale.ROOT,
                           "[ans-sound] mute %s from %s #%d (pack mutes '%s') t=%d pos=%.2f,%.2f,%.2f",
                           id,
                           mob.id(),
                           source.getId(),
                           step,
                           source.tickCount,
                           at.x,
                           at.y,
                           at.z
                        )
                     );
               }

               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private static boolean replace(PackSounds.Override o, SoundInstance sound, String log) {
      replaying = true;

      try {
         return PackSounds.playOverride(o, sound.getX(), sound.getY(), sound.getZ(), sound.getSource(), log);
      } finally {
         replaying = false;
      }
   }

   private static boolean replaceItem(Minecraft mc, SoundInstance sound, ResourceLocation id, Vec3 at, String item) {
      String event;
      if (item.equals("ignite")) {
         event = "ignite";
      } else {
         Entity holder = nearest(mc, at, e -> e instanceof LivingEntity l && spearTier(l.getMainHandItem()) != null);
         if (holder == null) {
            return false;
         }

         event = "item." + spearTier(((LivingEntity)holder).getMainHandItem()) + "_spear." + item;
      }

      PackSounds.Override o = PackSounds.individual(event);
      if (o == null) {
         return false;
      } else if (o.mute()) {
         return true;
      } else {
         return o.id() != null && !o.id().equals(id) ? replace(o, sound, id + " ('" + event + "')") : false;
      }
   }

   private static String spearTier(ItemStack stack) {
      if (stack.isEmpty()) {
         return null;
      }

      String p = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
      return p.endsWith("_spear") ? p.substring(0, p.length() - "_spear".length()) : null;
   }

   static String stepEvent(String path) {
      int dot = path.lastIndexOf(46);
      String last = dot < 0 ? path : path.substring(dot + 1);

      return switch (last) {
         case "step", "step_wood" -> "step";
         case "step_sand", "step_lava", "gallop", "flap" -> last;
         default -> null;
      };
   }

   static String voiceEvent(String path) {
      if (!path.startsWith("entity.")) {
         return null;
      }

      if (path.startsWith("entity.player.attack.")) {
         return "attack." + path.substring("entity.player.attack.".length());
      }

      int dot = path.lastIndexOf(46);
      String last = path.substring(dot + 1);

      return switch (last) {
         case "ambient" -> "ambient";
         case "hurt", "hurt_small" -> "hurt";
         case "death", "death_small" -> "death";
         case "primed" -> "fuse";
         case "squish" -> "squish.big";
         case "squish_small" -> "squish.small";
         case "thunder" -> "thunder";
         case "impact" -> "explode";
         case "spawn" -> "spawn";
         case "small_fall", "big_fall" -> "fall";
         case "equip" -> "armor.equip_generic";
         case "unequip" -> "armor.unequip_generic";
         case "harness_goggles_down" -> "attach";
         case "harness_goggles_up" -> "detach";
         default -> null;
      };
   }

   static String itemEvent(String path) {
      return switch (path) {
         case "item.flintandsteel.use" -> "ignite";
         default -> null;
      };
   }

   static boolean sameMob(String path, String typePath) {
      int a = path.indexOf(46);
      int b = path.indexOf(46, a + 1);
      if (a >= 0 && b >= 0) {
         String mob = path.substring(a + 1, b);
         return mob.equals(typePath)
            || mob.startsWith(typePath + "_")
            || mob.endsWith("_" + typePath)
            || typePath.startsWith(mob + "_")
            || typePath.endsWith("_" + mob);
      } else {
         return false;
      }
   }

   private static Entity nearest(Minecraft mc, Vec3 at, Predicate<Entity> filter) {
      Entity best = null;
      double bestD = 2.25;

      for (Entity e : mc.level.getEntities((Entity)null, new AABB(at, at).inflate(2.5), filter)) {
         double d = e.position().distanceToSqr(at);
         if (d < bestD) {
            bestD = d;
            best = e;
         }
      }

      return best;
   }
}
