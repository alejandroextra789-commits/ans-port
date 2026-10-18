package com.ans.client.particle;

import com.ans.client.molang.Molang;
import com.ans.client.molang.MolangExpr;
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

public final class BundleParticles {
   public static final BundleParticles EMPTY = new BundleParticles(Map.of(), Map.of());
   private final Map<String, List<BundleParticles.Key>> animations;
   private final Map<String, Map<String, List<BundleParticles.Effect>>> states;

   private BundleParticles(Map<String, List<BundleParticles.Key>> animations, Map<String, Map<String, List<BundleParticles.Effect>>> states) {
      this.animations = animations;
      this.states = states;
   }

   public boolean isEmpty() {
      return this.animations.isEmpty() && this.states.isEmpty();
   }

   public List<BundleParticles.Key> animation(String key) {
      return this.animations.getOrDefault(key, List.of());
   }

   public List<BundleParticles.Effect> state(String controllerKey, String state) {
      Map<String, List<BundleParticles.Effect>> m = this.states.get(controllerKey);
      return m == null ? List.of() : m.getOrDefault(state, List.of());
   }

   public static BundleParticles parse(JsonObject root) {
      if (root != null && root.has("animations") && root.get("animations").isJsonObject()) {
         Map<String, List<BundleParticles.Key>> anims = new HashMap<>();
         Map<String, Map<String, List<BundleParticles.Effect>>> states = new HashMap<>();

         for (Entry<String, JsonElement> e : root.getAsJsonObject("animations").entrySet()) {
            if (e.getValue().isJsonObject()) {
               JsonObject o = e.getValue().getAsJsonObject();
               String key = e.getKey().toLowerCase(Locale.ROOT);
               if (o.has("particle_effects") && o.get("particle_effects").isJsonObject()) {
                  List<BundleParticles.Key> keys = new ArrayList<>();

                  for (Entry<String, JsonElement> k : o.getAsJsonObject("particle_effects").entrySet()) {
                     double t;
                     try {
                        t = Double.parseDouble(k.getKey());
                     } catch (NumberFormatException ex) {
                        continue;
                     }

                     List<BundleParticles.Effect> ev = effects(k.getValue());
                     if (!ev.isEmpty()) {
                        keys.add(new BundleParticles.Key(t, ev));
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
                           List<BundleParticles.Effect> ev = effects(s.getValue().getAsJsonObject().get("particle_effects"));
                           if (!ev.isEmpty()) {
                              states.computeIfAbsent(key, var0x -> new HashMap<>()).put(s.getKey(), ev);
                           }
                        }
                     }
                  }
               }
            }
         }

         return anims.isEmpty() && states.isEmpty() ? EMPTY : new BundleParticles(Collections.unmodifiableMap(anims), Collections.unmodifiableMap(states));
      } else {
         return EMPTY;
      }
   }

   private static List<BundleParticles.Effect> effects(JsonElement e) {
      if (e != null && !e.isJsonNull()) {
         List<BundleParticles.Effect> out = new ArrayList<>();
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

   private static void add(JsonElement x, List<BundleParticles.Effect> out) {
      if (x.isJsonObject()) {
         JsonObject o = x.getAsJsonObject();
         JsonElement r = o.get("resolved");
         String id = r != null && r.isJsonPrimitive() ? r.getAsString() : null;
         if (id == null) {
            JsonElement eff = o.get("effect");
            if (eff != null && eff.isJsonPrimitive() && eff.getAsString().contains(":")) {
               id = eff.getAsString();
            }
         }

         if (id != null && !id.isEmpty()) {
            JsonElement loc = o.get("locator");
            JsonElement bind = o.get("bind_to_actor");
            boolean bound = bind == null
               || !bind.isJsonPrimitive()
               || (bind.getAsJsonPrimitive().isBoolean() ? bind.getAsBoolean() : !"false".equalsIgnoreCase(bind.getAsString()));
            out.add(
               new BundleParticles.Effect(
                  id.toLowerCase(Locale.ROOT),
                  loc != null && loc.isJsonPrimitive() && !loc.getAsString().isEmpty() ? loc.getAsString() : null,
                  Molang.compile(o.get("pre_effect_script"), null),
                  bound
               )
            );
         }
      }
   }

   public record Effect(String effect, String locator, MolangExpr preEffect, boolean bound) {
   }

   public record Key(double time, List<BundleParticles.Effect> effects) {
   }
}
