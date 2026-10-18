package com.ans.client.sound;

import com.ans.Ans;
import com.ans.client.bedrock.AnimationLookup;
import com.ans.client.molang.Molang;
import com.ans.client.molang.MolangContext;
import com.ans.client.molang.MolangExpr;
import com.ans.client.molang.MolangScope;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class PackSounds {
   public static final ResourceLocation META = Ans.id("sound_meta.json");
   public static final boolean DEBUG_ALL = "all".equalsIgnoreCase(System.getProperty("ans.debugSounds"));
   public static final boolean DEBUG = DEBUG_ALL || Boolean.getBoolean("ans.debugSounds");
   private static volatile int seenGeneration = -1;
   private static Map<String, PackSounds.Event> events = Map.of();
   private static Map<String, Set<String>> mute = Map.of();
   private static Set<ResourceLocation> silencedVanilla = Set.of();
   private static Set<ResourceLocation> twoD = Set.of();
   private static Map<String, Map<String, PackSounds.Override>> entityEvents = Map.of();
   private static Map<String, PackSounds.Variants> variants = Map.of();
   private static Map<String, PackSounds.Override> individual = Map.of();
   private static final Set<String> WARNED = new HashSet<>();

   private PackSounds() {
   }

   public static void init() {
      LocalPlayerSounds.init();
      MobAttackSounds.init();
   }

   private static void ensureLoaded() {
      int generation = AnimationLookup.generation();
      if (generation != seenGeneration) {
         synchronized (PackSounds.class) {
            if (generation != seenGeneration) {
               Map<String, PackSounds.Event> ev = new HashMap<>();
               Map<String, Set<String>> mu = new HashMap<>();
               Set<ResourceLocation> sil = new HashSet<>();
               Set<ResourceLocation> flat = new HashSet<>();
               Map<String, Map<String, PackSounds.Override>> ee = new HashMap<>();
               Map<String, PackSounds.Variants> va = new HashMap<>();
               Map<String, PackSounds.Override> ind = new HashMap<>();

               try {
                  Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(META);
                  if (res.isEmpty()) {
                     Ans.LOGGER.warn("[ans] {} not found: run work/converter/sounds.py (pack sounds are not played)", META);
                  } else {
                     try (Reader reader = res.get().openAsReader()) {
                        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                        if (root.has("events")) {
                           for (Entry<String, JsonElement> e : root.getAsJsonObject("events").entrySet()) {
                              JsonObject o = e.getValue().getAsJsonObject();
                              ResourceLocation id = o.has("id") ? ResourceLocation.tryParse(o.get("id").getAsString()) : null;
                              SoundSource src = o.has("category") ? source(o.get("category").getAsString()) : null;
                              boolean is3d = !o.has("3d") || o.get("3d").getAsBoolean();
                              boolean silent = id == null || o.has("silent") && o.get("silent").getAsBoolean();
                              ev.put(e.getKey(), new PackSounds.Event(id, src, is3d, silent));
                              if (!is3d && id != null) {
                                 flat.add(id);
                              }
                           }
                        }

                        if (root.has("entity_events")) {
                           for (Entry<String, JsonElement> e : root.getAsJsonObject("entity_events").entrySet()) {
                              ee.put(e.getKey(), overrides(e.getValue().getAsJsonObject()));
                           }
                        }

                        if (root.has("variants")) {
                           for (Entry<String, JsonElement> e : root.getAsJsonObject("variants").entrySet()) {
                              JsonObject o = e.getValue().getAsJsonObject();
                              Map<String, Map<String, PackSounds.Override>> m = new HashMap<>();

                              for (Entry<String, JsonElement> k : o.getAsJsonObject("map").entrySet()) {
                                 m.put(k.getKey(), overrides(k.getValue().getAsJsonObject()));
                              }

                              va.put(e.getKey(), new PackSounds.Variants(Molang.compile(o.get("key").getAsString()), Map.copyOf(m)));
                           }
                        }

                        if (root.has("individual_events")) {
                           ind.putAll(overrides(root.getAsJsonObject("individual_events")));
                        }

                        if (root.has("mute")) {
                           for (Entry<String, JsonElement> e : root.getAsJsonObject("mute").entrySet()) {
                              Set<String> s = new HashSet<>();

                              for (JsonElement x : e.getValue().getAsJsonArray()) {
                                 s.add(x.getAsString());
                              }

                              mu.put(e.getKey(), Set.copyOf(s));
                           }
                        }

                        if (root.has("silenced_vanilla")) {
                           for (JsonElement x : root.getAsJsonArray("silenced_vanilla")) {
                              ResourceLocation id = ResourceLocation.tryParse(x.getAsString());
                              if (id != null) {
                                 sil.add(id);
                              }
                           }
                        }
                     }
                  }
               } catch (Exception e) {
                  Ans.LOGGER.error("[ans] Could not read {}", META, e);
               }

               events = Map.copyOf(ev);
               mute = Map.copyOf(mu);
               silencedVanilla = Set.copyOf(sil);
               twoD = Set.copyOf(flat);
               entityEvents = Map.copyOf(ee);
               variants = Map.copyOf(va);
               individual = Map.copyOf(ind);
               WARNED.clear();
               seenGeneration = generation;
               Ans.LOGGER
                  .info(
                     "[ans] Pack sounds: {} events, {} entities with muted engine sounds, {} silenced vanilla events, {} entities with event overrides, {} with variants, {} individual events{}",
                     new Object[]{
                        events.size(),
                        mute.size(),
                        silencedVanilla.size(),
                        entityEvents.size(),
                        variants.size(),
                        individual.size(),
                        DEBUG ? " (debug log on)" : ""
                     }
                  );
            }
         }
      }
   }

   private static Map<String, PackSounds.Override> overrides(JsonObject o) {
      Map<String, PackSounds.Override> out = new HashMap<>();

      for (Entry<String, JsonElement> e : o.entrySet()) {
         if (e.getValue().isJsonObject()) {
            JsonObject x = e.getValue().getAsJsonObject();
            ResourceLocation id = x.has("id") && x.get("id").isJsonPrimitive() ? ResourceLocation.tryParse(x.get("id").getAsString()) : null;
            float volume = x.has("volume") && x.get("volume").isJsonPrimitive() ? x.get("volume").getAsFloat() : 1.0F;
            float pMin = 1.0F;
            float pMax = 1.0F;
            JsonElement p = x.get("pitch");
            if (p != null && p.isJsonArray() && p.getAsJsonArray().size() >= 2) {
               pMin = p.getAsJsonArray().get(0).getAsFloat();
               pMax = p.getAsJsonArray().get(1).getAsFloat();
            } else if (p != null && p.isJsonPrimitive()) {
               pMin = pMax = p.getAsFloat();
            }

            boolean mute = x.has("mute") && x.get("mute").getAsBoolean();
            if (id != null || mute) {
               out.put(e.getKey(), new PackSounds.Override(id, volume, pMin, pMax, mute));
            }
         }
      }

      return Map.copyOf(out);
   }

   public static PackSounds.Override entityOverride(String bedrockId, Entity entity, String event) {
      ensureLoaded();
      PackSounds.Variants v = variants.get(bedrockId);
      if (v != null) {
         String key = variantKey(v.key(), entity);
         Map<String, PackSounds.Override> m = key == null ? null : v.map().get(key);
         PackSounds.Override o = m == null ? null : m.get(event);
         if (o != null) {
            return o;
         }
      }

      Map<String, PackSounds.Override> top = entityEvents.get(bedrockId);
      return top == null ? null : top.get(event);
   }

   public static boolean hasOverride(String bedrockId, String event) {
      ensureLoaded();
      Map<String, PackSounds.Override> top = entityEvents.get(bedrockId);
      if (top != null && top.containsKey(event)) {
         return true;
      }

      PackSounds.Variants v = variants.get(bedrockId);
      if (v != null) {
         for (Map<String, PackSounds.Override> m : v.map().values()) {
            if (m.containsKey(event)) {
               return true;
            }
         }
      }

      return false;
   }

   public static PackSounds.Override individual(String event) {
      ensureLoaded();
      return individual.get(event);
   }

   private static String variantKey(MolangExpr key, Entity entity) {
      MolangContext c = new MolangContext(new MolangScope());
      c.actor = entity;
      c.entity = entity instanceof LivingEntity living ? living : null;
      c.partialTick = 1.0F;
      MolangContext previous = c.enter();

      try {
         Object v = key.value(c);
         if (v instanceof String s) {
            return s;
         } else if (v instanceof Number n) {
            double d = n.doubleValue();
            return d == Math.rint(d) ? Long.toString((long)d) : Double.toString(d);
         } else {
            return null;
         }
      } catch (RuntimeException e) {
         return null;
      } finally {
         MolangContext.restore(previous);
      }
   }

   public static boolean playOverride(PackSounds.Override o, double x, double y, double z, SoundSource source, String log) {
      if (o.id() == null) {
         return false;
      }

      Minecraft mc = Minecraft.getInstance();
      RandomSource random = SoundInstance.createUnseededRandom();
      float pitch = o.pitchMin() == o.pitchMax() ? o.pitchMin() : o.pitchMin() + random.nextFloat() * (o.pitchMax() - o.pitchMin());
      boolean flat = twoD.contains(o.id());
      SoundInstance sound = new SimpleSoundInstance(
         o.id(),
         source,
         o.volume(),
         pitch,
         random,
         false,
         0,
         flat ? Attenuation.NONE : Attenuation.LINEAR,
         flat ? 0.0 : x,
         flat ? 0.0 : y,
         flat ? 0.0 : z,
         flat
      );
      mc.getSoundManager().play(sound);
      if (DEBUG) {
         Ans.LOGGER
            .info(String.format(Locale.ROOT, "[ans-sound] replace %s -> %s vol=%.2f pitch=%.2f pos=%.2f,%.2f,%.2f", log, o.id(), o.volume(), pitch, x, y, z));
      }

      return true;
   }

   static SoundSource source(String category) {
      return switch (category.toLowerCase(Locale.ROOT)) {
         case "player" -> SoundSource.PLAYERS;
         case "neutral" -> SoundSource.NEUTRAL;
         case "hostile" -> SoundSource.HOSTILE;
         case "block", "bottle" -> SoundSource.BLOCKS;
         case "weather" -> SoundSource.WEATHER;
         case "music" -> SoundSource.MUSIC;
         case "record" -> SoundSource.RECORDS;
         case "ambient" -> SoundSource.AMBIENT;
         case "ui" -> SoundSource.UI;
         case "master" -> SoundSource.MASTER;
         default -> null;
      };
   }

   public static Set<String> muted(String bundleId) {
      ensureLoaded();
      return mute.getOrDefault(bundleId, Set.of());
   }

   public static boolean silenced(ResourceLocation id) {
      ensureLoaded();
      return silencedVanilla.contains(id);
   }

   public static void play(Entity entity, float partialTick, List<String> names, String mobId, String origin) {
      if (!names.isEmpty()) {
         ensureLoaded();
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null && entity.level() == mc.level) {
            for (String name : names) {
               PackSounds.Event ev = events.get(name);
               if (ev != null && !ev.silent() && ev.id() != null) {
                  if (entity.isSilent()) {
                     if (DEBUG) {
                        Ans.LOGGER.info("[ans-sound] skip {} #{} {} ({}): entity is silent", new Object[]{mobId, entity.getId(), name, origin});
                     }
                  } else {
                     SoundSource src = ev.source() != null ? ev.source() : entity.getSoundSource();
                     Vec3 pos = entity.getPosition(partialTick);
                     SoundInstance sound;
                     if (ev.is3d()) {
                        sound = new SimpleSoundInstance(
                           ev.id(), src, 1.0F, 1.0F, SoundInstance.createUnseededRandom(), false, 0, Attenuation.LINEAR, pos.x, pos.y, pos.z, false
                        );
                     } else {
                        sound = new SimpleSoundInstance(
                           ev.id(), src, 1.0F, 1.0F, SoundInstance.createUnseededRandom(), false, 0, Attenuation.NONE, 0.0, 0.0, 0.0, true
                        );
                     }

                     mc.getSoundManager().play(sound);
                     if (DEBUG) {
                        Ans.LOGGER
                           .info(
                              String.format(
                                 Locale.ROOT,
                                 "[ans-sound] play %s #%d %s -> %s (%s)%s t=%d pos=%.2f,%.2f,%.2f",
                                 mobId,
                                 entity.getId(),
                                 name,
                                 ev.id(),
                                 origin,
                                 ev.is3d() ? "" : " 2D",
                                 entity.tickCount,
                                 pos.x,
                                 pos.y,
                                 pos.z
                              )
                           );
                     }
                  }
               } else if (DEBUG && WARNED.add(name)) {
                  Ans.LOGGER.info("[ans-sound] {} #{}: '{}' ({}) has no playable sound", new Object[]{mobId, entity.getId(), name, origin});
               }
            }
         }
      }
   }

   public record Event(ResourceLocation id, SoundSource source, boolean is3d, boolean silent) {
   }

   public record Override(ResourceLocation id, float volume, float pitchMin, float pitchMax, boolean mute) {
   }

   private record Variants(MolangExpr key, Map<String, Map<String, PackSounds.Override>> map) {
   }
}
