package com.ans.client.sound;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public final class BundleSounds {
   public static final BundleSounds EMPTY = new BundleSounds(Map.of(), Map.of());
   private final Map<String, List<BundleSounds.Key>> animations;
   private final Map<String, Map<String, List<String>>> states;

   private BundleSounds(Map<String, List<BundleSounds.Key>> animations, Map<String, Map<String, List<String>>> states) {
      this.animations = animations;
      this.states = states;
   }

   public boolean isEmpty() {
      return this.animations.isEmpty() && this.states.isEmpty();
   }

   public List<BundleSounds.Key> animation(String key) {
      return this.animations.getOrDefault(key, List.of());
   }

   public List<String> state(String controllerKey, String state) {
      Map<String, List<String>> m = this.states.get(controllerKey);
      return m == null ? List.of() : m.getOrDefault(state, List.of());
   }

   public static BundleSounds parse(JsonObject root) {
      if (root != null && root.has("animations") && root.get("animations").isJsonObject()) {
         Map<String, List<BundleSounds.Key>> anims = new HashMap<>();
         Map<String, Map<String, List<String>>> states = new HashMap<>();

         for (Entry<String, JsonElement> e : root.getAsJsonObject("animations").entrySet()) {
            if (e.getValue().isJsonObject()) {
               JsonObject o = e.getValue().getAsJsonObject();
               String key = e.getKey().toLowerCase(Locale.ROOT);
               if (o.has("sound_effects") && o.get("sound_effects").isJsonObject()) {
                  List<BundleSounds.Key> keys = new ArrayList<>();

                  for (Entry<String, JsonElement> k : o.getAsJsonObject("sound_effects").entrySet()) {
                     double t;
                     try {
                        t = Double.parseDouble(k.getKey());
                     } catch (NumberFormatException ex) {
                        continue;
                     }

                     List<String> ev = events(k.getValue());
                     if (!ev.isEmpty()) {
                        keys.add(new BundleSounds.Key(t, ev));
                     }
                  }

                  keys.sort((a, b) -> Double.compare(a.time(), b.time()));
                  if (!keys.isEmpty()) {
                     anims.put(key, List.copyOf(keys));
                  }
               }

               if (o.has("controller") && o.get("controller").isJsonObject()) {
                  JsonObject c = o.getAsJsonObject("controller");
                  if (c.has("states") && c.get("states").isJsonObject()) {
                     for (Entry<String, JsonElement> s : c.getAsJsonObject("states").entrySet()) {
                        if (s.getValue().isJsonObject()) {
                           List<String> ev = events(s.getValue().getAsJsonObject().get("sound_effects"));
                           if (!ev.isEmpty()) {
                              states.computeIfAbsent(key, var0x -> new HashMap<>()).put(s.getKey(), ev);
                           }
                        }
                     }
                  }
               }
            }
         }

         return anims.isEmpty() && states.isEmpty() ? EMPTY : new BundleSounds(Collections.unmodifiableMap(anims), Collections.unmodifiableMap(states));
      } else {
         return EMPTY;
      }
   }

   private static List<String> events(JsonElement e) {
      if (e != null && !e.isJsonNull()) {
         List<String> out = new ArrayList<>();
         if (e.isJsonArray()) {
            for (JsonElement x : (JsonArray)e) {
               add(x, out);
            }
         } else {
            add(e, out);
         }

         return List.copyOf(out);
      } else {
         return List.of();
      }
   }

   private static void add(JsonElement x, List<String> out) {
      if (x.isJsonObject()) {
         JsonObject o = x.getAsJsonObject();
         JsonElement r = o.has("resolved") ? o.get("resolved") : o.get("effect");
         if (r != null && r.isJsonPrimitive() && !r.getAsString().isEmpty()) {
            out.add(r.getAsString());
         }
      }
   }

   public record Key(double time, List<String> events) {
   }
}
