package com.ans.client.bedrock;

import com.ans.Ans;
import com.ans.client.molang.Molang;
import com.ans.client.molang.MolangExpr;
import com.ans.client.particle.BundleParticles;
import com.ans.client.sound.BundleSounds;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class EntityBundle {
   public final String id;
   public final ResourceLocation animationFile;
   public final Map<String, ResourceLocation> geometry;
   public final Map<String, ResourceLocation> textures;
   public final Map<String, EntityBundle.Material> materials;
   public final Map<String, EntityBundle.AnimationDef> animations;
   public final Map<String, EntityBundle.ControllerDef> controllers;
   public final List<MolangExpr> initialize;
   public final List<MolangExpr> preAnimation;
   public final List<EntityBundle.Entry> animate;
   public final MolangExpr scale;
   public final MolangExpr scaleX;
   public final MolangExpr scaleY;
   public final MolangExpr scaleZ;
   public final List<EntityBundle.RenderControllerDef> renderControllers;
   public final Map<String, List<String>> relativeToEntity;
   public final BundleSounds sounds;
   public final BundleParticles particles;
   private final String source;

   private EntityBundle(String id, JsonObject root, String source) {
      this.id = id;
      this.source = source;
      this.animationFile = root.has("animation_file") && !root.get("animation_file").isJsonNull()
         ? new ResourceLocation(root.get("animation_file").getAsString())
         : null;
      this.geometry = this.identifiers(root.getAsJsonObject("geometry"));
      this.textures = this.identifiers(root.getAsJsonObject("textures"));
      Map<String, EntityBundle.Material> mats = new LinkedHashMap<>();
      JsonObject m = root.getAsJsonObject("materials");
      if (m != null) {
         for (Map.Entry<String, JsonElement> e : m.entrySet()) {
            JsonElement v = e.getValue();
            if (v.isJsonObject()) {
               JsonObject o = v.getAsJsonObject();
               mats.put(
                  key(e.getKey()),
                  new EntityBundle.Material(
                     str(o, "bedrock", ""),
                     RenderKind.parse(str(o, "render_type", "cutout")),
                     o.has("color_mask") && o.get("color_mask").getAsBoolean(),
                     o.has("multi_texture") && o.get("multi_texture").getAsBoolean(),
                     o.has("uv_anim") && o.get("uv_anim").getAsBoolean(),
                     o.has("glint") && o.get("glint").getAsBoolean()
                  )
               );
            } else if (v.isJsonPrimitive()) {
               mats.put(key(e.getKey()), new EntityBundle.Material(v.getAsString(), RenderKind.CUTOUT));
            }
         }
      }

      this.materials = Collections.unmodifiableMap(mats);
      Map<String, EntityBundle.AnimationDef> anims = new LinkedHashMap<>();
      Map<String, EntityBundle.ControllerDef> ctrls = new LinkedHashMap<>();
      Map<String, List<String>> relative = new LinkedHashMap<>();
      JsonObject a = root.getAsJsonObject("animations");
      if (a != null) {
         for (Map.Entry<String, JsonElement> e : a.entrySet()) {
            if (e.getValue().isJsonObject()) {
               JsonObject o = e.getValue().getAsJsonObject();
               String k = key(e.getKey());
               if (o.has("relative_to_entity") && o.get("relative_to_entity").isJsonArray()) {
                  List<String> bones = new ArrayList<>();

                  for (JsonElement x : o.getAsJsonArray("relative_to_entity")) {
                     bones.add(x.getAsString().toLowerCase(Locale.ROOT));
                  }

                  relative.put(k, List.copyOf(bones));
               }

               if (o.has("controller")) {
                  ctrls.put(k, this.controller(k, o.getAsJsonObject("controller")));
               } else if (!o.has("missing") || !o.get("missing").getAsBoolean()) {
                  anims.put(k, this.animation(k, o));
               }
            }
         }
      }

      this.animations = Collections.unmodifiableMap(anims);
      this.controllers = Collections.unmodifiableMap(ctrls);
      this.relativeToEntity = Collections.unmodifiableMap(relative);
      this.sounds = BundleSounds.parse(root);
      this.particles = BundleParticles.parse(root);
      JsonObject scripts = root.has("scripts") ? root.getAsJsonObject("scripts") : new JsonObject();
      this.initialize = scriptList(scripts.get("initialize"));
      this.preAnimation = scriptList(scripts.get("pre_animation"));
      this.animate = entries(scripts.get("animate"));
      this.scale = Molang.compile(scripts.get("scale"), null);
      this.scaleX = Molang.compile(scripts.get("scaleX"), null);
      this.scaleY = Molang.compile(scripts.get("scaleY"), null);
      this.scaleZ = Molang.compile(scripts.get("scaleZ"), null);
      List<EntityBundle.RenderControllerDef> rcs = new ArrayList<>();
      JsonArray rca = root.getAsJsonArray("render_controllers");
      if (rca != null) {
         for (JsonElement e : rca) {
            if (e.isJsonObject()) {
               JsonObject o = e.getAsJsonObject();
               if (!o.has("missing") || !o.get("missing").getAsBoolean()) {
                  rcs.add(this.renderController(o));
               }
            }
         }
      }

      this.renderControllers = Collections.unmodifiableList(rcs);
   }

   public static EntityBundle parse(String id, JsonObject root, String source) {
      return new EntityBundle(id, root, source);
   }

   @Override
   public String toString() {
      return "EntityBundle[" + this.source + "]";
   }

   private static String key(String k) {
      return k.toLowerCase(Locale.ROOT);
   }

   private static String str(JsonObject o, String k, String fallback) {
      JsonElement e = o.get(k);
      return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
   }

   private Map<String, ResourceLocation> identifiers(JsonObject o) {
      Map<String, ResourceLocation> out = new LinkedHashMap<>();
      if (o == null) {
         return out;
      }

      for (Map.Entry<String, JsonElement> e : o.entrySet()) {
         if (e.getValue().isJsonPrimitive()) {
            ResourceLocation id = ResourceLocation.tryParse(e.getValue().getAsString());
            if (id != null) {
               out.put(key(e.getKey()), id);
            } else {
               this.warn("bad resource id " + e.getValue());
            }
         }
      }

      return Collections.unmodifiableMap(out);
   }

   private static List<MolangExpr> scriptList(JsonElement e) {
      List<MolangExpr> out = new ArrayList<>();
      if (e != null && !e.isJsonNull()) {
         if (e.isJsonArray()) {
            for (JsonElement s : e.getAsJsonArray()) {
               MolangExpr x = Molang.compile(s, null);
               if (x != null) {
                  out.add(x);
               }
            }
         } else {
            MolangExpr x = Molang.compile(e, null);
            if (x != null) {
               out.add(x);
            }
         }

         return List.copyOf(out);
      } else {
         return out;
      }
   }

   private static List<EntityBundle.Entry> entries(JsonElement e) {
      List<EntityBundle.Entry> out = new ArrayList<>();
      if (e != null && e.isJsonArray()) {
         for (JsonElement x : e.getAsJsonArray()) {
            if (x.isJsonPrimitive()) {
               out.add(new EntityBundle.Entry(key(x.getAsString()), null));
            } else if (x.isJsonObject()) {
               for (Map.Entry<String, JsonElement> kv : x.getAsJsonObject().entrySet()) {
                  out.add(new EntityBundle.Entry(key(kv.getKey()), Molang.compile(kv.getValue(), Molang.ONE)));
               }
            }
         }

         return List.copyOf(out);
      } else {
         return out;
      }
   }

   private EntityBundle.AnimationDef animation(String key, JsonObject o) {
      String anim = o.has("animation") && !o.get("animation").isJsonNull() ? o.get("animation").getAsString() : null;
      EntityBundle.Loop loop = EntityBundle.Loop.ONCE;
      JsonElement l = o.get("loop");
      if (l != null && l.isJsonPrimitive()) {
         if (l.getAsJsonPrimitive().isBoolean()) {
            loop = l.getAsBoolean() ? EntityBundle.Loop.LOOP : EntityBundle.Loop.ONCE;
         } else {
            String s = l.getAsString().toLowerCase(Locale.ROOT);
            loop = s.equals("hold_on_last_frame")
               ? EntityBundle.Loop.HOLD
               : (!s.equals("true") && !s.equals("loop") ? EntityBundle.Loop.ONCE : EntityBundle.Loop.LOOP);
         }
      }

      double length = Double.NaN;
      JsonElement len = o.get("animation_length");
      if (len != null && len.isJsonPrimitive() && len.getAsJsonPrimitive().isNumber()) {
         length = len.getAsDouble();
      }

      List<EntityBundle.TimelineEvent> timeline = new ArrayList<>();
      JsonObject tl = o.getAsJsonObject("timeline");
      if (tl != null) {
         for (Map.Entry<String, JsonElement> e : tl.entrySet()) {
            try {
               timeline.add(new EntityBundle.TimelineEvent(Double.parseDouble(e.getKey()), scriptList(e.getValue())));
            } catch (NumberFormatException ex) {
               this.warn("animation " + key + ": bad timeline time '" + e.getKey() + "'");
            }
         }

         timeline.sort((x, y) -> Double.compare(x.time(), y.time()));
      }

      boolean override = o.has("override_previous_animation")
         && o.get("override_previous_animation").isJsonPrimitive()
         && o.get("override_previous_animation").getAsBoolean();
      return new EntityBundle.AnimationDef(
         key,
         anim,
         loop,
         length,
         Molang.compile(o.get("anim_time_update"), null),
         Molang.compile(o.get("blend_weight"), null),
         Molang.compile(o.get("start_delay"), null),
         Molang.compile(o.get("loop_delay"), null),
         override,
         List.copyOf(timeline)
      );
   }

   private EntityBundle.ControllerDef controller(String key, JsonObject o) {
      Map<String, EntityBundle.State> states = new LinkedHashMap<>();
      JsonObject so = o.getAsJsonObject("states");
      if (so != null) {
         for (Map.Entry<String, JsonElement> e : so.entrySet()) {
            if (e.getValue().isJsonObject()) {
               JsonObject s = e.getValue().getAsJsonObject();
               List<EntityBundle.Transition> transitions = new ArrayList<>();
               JsonArray ta = s.getAsJsonArray("transitions");
               if (ta != null) {
                  for (JsonElement t : ta) {
                     if (t.isJsonObject()) {
                        for (Map.Entry<String, JsonElement> kv : t.getAsJsonObject().entrySet()) {
                           transitions.add(new EntityBundle.Transition(kv.getKey(), Molang.compile(kv.getValue(), Molang.ZERO)));
                        }
                     }
                  }
               }

               states.put(
                  e.getKey(),
                  new EntityBundle.State(
                     e.getKey(),
                     entries(s.get("animations")),
                     List.copyOf(transitions),
                     blend(s.get("blend_transition")),
                     scriptList(s.get("on_entry")),
                     scriptList(s.get("on_exit"))
                  )
               );
            }
         }
      }

      if (states.isEmpty()) {
         this.warn("controller " + key + " has no states");
         states.put("default", new EntityBundle.State("default", List.of(), List.of(), EntityBundle.Blend.NONE, List.of(), List.of()));
      }

      String initial = str(o, "initial_state", states.containsKey("default") ? "default" : states.keySet().iterator().next());
      if (!states.containsKey(initial)) {
         initial = states.keySet().iterator().next();
      }

      return new EntityBundle.ControllerDef(key, initial, Collections.unmodifiableMap(states));
   }

   private static EntityBundle.Blend blend(JsonElement e) {
      if (e == null || e.isJsonNull()) {
         return EntityBundle.Blend.NONE;
      }

      if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
         return new EntityBundle.Blend(Math.max(0.0, e.getAsDouble()), null, null);
      }

      if (!e.isJsonObject()) {
         return EntityBundle.Blend.NONE;
      }

      List<double[]> points = new ArrayList<>();

      for (Map.Entry<String, JsonElement> kv : e.getAsJsonObject().entrySet()) {
         try {
            points.add(new double[]{Double.parseDouble(kv.getKey()), kv.getValue().getAsDouble()});
         } catch (RuntimeException var5) {
         }
      }

      if (points.isEmpty()) {
         return EntityBundle.Blend.NONE;
      }

      points.sort((x, y) -> Double.compare(x[0], y[0]));
      double[] t = new double[points.size()];
      double[] v = new double[points.size()];

      for (int i = 0; i < t.length; i++) {
         t[i] = points.get(i)[0];
         v[i] = points.get(i)[1];
      }

      return new EntityBundle.Blend(t[t.length - 1], t, v);
   }

   private EntityBundle.RenderControllerDef renderController(JsonObject o) {
      Map<String, List<String>> arrays = new LinkedHashMap<>();
      JsonObject ao = o.getAsJsonObject("arrays");
      if (ao != null) {
         for (Map.Entry<String, JsonElement> e : ao.entrySet()) {
            if (e.getValue().isJsonArray()) {
               List<String> list = new ArrayList<>();

               for (JsonElement x : e.getValue().getAsJsonArray()) {
                  list.add(x.getAsString().toLowerCase(Locale.ROOT));
               }

               arrays.put(e.getKey().toLowerCase(Locale.ROOT), List.copyOf(list));
            }
         }
      }

      List<MolangExpr> textures = new ArrayList<>();
      JsonElement te = o.get("textures");
      if (te != null && te.isJsonArray()) {
         for (JsonElement t : te.getAsJsonArray()) {
            textures.add(Molang.compile(t, Molang.ZERO));
         }
      } else if (te != null) {
         textures.add(Molang.compile(te, Molang.ZERO));
      }

      boolean ignoreLighting = o.has("ignore_lighting") && o.get("ignore_lighting").isJsonPrimitive() && o.get("ignore_lighting").getAsBoolean();
      return new EntityBundle.RenderControllerDef(
         str(o, "id", "?"),
         Molang.compile(o.get("condition"), null),
         Collections.unmodifiableMap(arrays),
         Molang.compile(o.get("geometry"), null),
         List.copyOf(textures),
         rules(o.get("materials")),
         rules(o.get("part_visibility")),
         color(o.get("color")),
         color(o.get("overlay_color")),
         color(o.get("on_fire_color")),
         color(o.get("is_hurt_color")),
         ignoreLighting,
         uvAnim(o.get("uv_anim"))
      );
   }

   private static EntityBundle.UvAnim uvAnim(JsonElement e) {
      if (e != null && e.isJsonObject() && e.getAsJsonObject().has("offset") && e.getAsJsonObject().get("offset").isJsonArray()) {
         JsonArray off = e.getAsJsonObject().getAsJsonArray("offset");
         JsonElement sc = e.getAsJsonObject().get("scale");
         JsonArray scale = sc != null && sc.isJsonArray() ? sc.getAsJsonArray() : new JsonArray();
         return new EntityBundle.UvAnim(
            off.size() > 0 ? Molang.compile(off.get(0), Molang.ZERO) : Molang.ZERO,
            off.size() > 1 ? Molang.compile(off.get(1), Molang.ZERO) : Molang.ZERO,
            scale.size() > 0 ? Molang.compile(scale.get(0), Molang.ONE) : Molang.ONE,
            scale.size() > 1 ? Molang.compile(scale.get(1), Molang.ONE) : Molang.ONE
         );
      } else {
         return null;
      }
   }

   private static List<EntityBundle.Rule> rules(JsonElement e) {
      List<EntityBundle.Rule> out = new ArrayList<>();
      if (e != null && e.isJsonArray()) {
         for (JsonElement x : e.getAsJsonArray()) {
            if (x.isJsonArray() && x.getAsJsonArray().size() == 2) {
               JsonArray p = x.getAsJsonArray();
               out.add(new EntityBundle.Rule(new BonePattern(p.get(0).getAsString()), Molang.compile(p.get(1), Molang.ZERO)));
            } else if (x.isJsonObject()) {
               for (Map.Entry<String, JsonElement> kv : x.getAsJsonObject().entrySet()) {
                  out.add(new EntityBundle.Rule(new BonePattern(kv.getKey()), Molang.compile(kv.getValue(), Molang.ZERO)));
               }
            }
         }

         return List.copyOf(out);
      } else {
         return out;
      }
   }

   private static EntityBundle.Color color(JsonElement e) {
      if (e != null && e.isJsonObject()) {
         JsonObject o = e.getAsJsonObject();
         return new EntityBundle.Color(
            Molang.compile(o.get("r"), Molang.ZERO),
            Molang.compile(o.get("g"), Molang.ZERO),
            Molang.compile(o.get("b"), Molang.ZERO),
            Molang.compile(o.get("a"), Molang.ONE)
         );
      } else {
         return null;
      }
   }

   private void warn(String message) {
      Ans.LOGGER.warn("[ans] {}: {}", this.source, message);
   }

   public record AnimationDef(
      String key,
      String animation,
      EntityBundle.Loop loop,
      double length,
      MolangExpr animTimeUpdate,
      MolangExpr blendWeight,
      MolangExpr startDelay,
      MolangExpr loopDelay,
      boolean overridePrevious,
      List<EntityBundle.TimelineEvent> timeline
   ) {
   }

   public record Blend(double seconds, double [] curveTimes, double [] curveValues) {
      static final EntityBundle.Blend NONE = new EntityBundle.Blend(0.0, null, null);

      public double outgoing(double t) {
         if (this.curveTimes != null && this.curveValues != null) {
            if (t <= this.curveTimes[0]) {
               return this.curveValues[0];
            }

            for (int i = 1; i < this.curveTimes.length; i++) {
               if (t <= this.curveTimes[i]) {
                  double t0 = this.curveTimes[i - 1];
                  double t1 = this.curveTimes[i];
                  double f = t1 == t0 ? 1.0 : (t - t0) / (t1 - t0);
                  return this.curveValues[i - 1] + (this.curveValues[i] - this.curveValues[i - 1]) * f;
               }
            }

            return this.curveValues[this.curveValues.length - 1];
         } else {
            return this.seconds <= 0.0 ? 0.0 : Math.max(0.0, 1.0 - t / this.seconds);
         }
      }
   }

   public record Color(MolangExpr r, MolangExpr g, MolangExpr b, MolangExpr a) {
   }

   public record ControllerDef(String key, String initialState, Map<String, EntityBundle.State> states) {
   }

   public record Entry(String ref, MolangExpr weight) {
   }

   public enum Loop {
      ONCE,
      LOOP,
      HOLD;
   }

   public record Material(String bedrock, RenderKind kind, boolean colorMask, boolean multiTexture, boolean uvAnim, boolean glint) {
      public Material(String bedrock, RenderKind kind, boolean colorMask, boolean multiTexture, boolean uvAnim) {
         this(bedrock, kind, colorMask, multiTexture, uvAnim, false);
      }

      public Material(String bedrock, RenderKind kind) {
         this(bedrock, kind, false, false);
      }

      public Material(String bedrock, RenderKind kind, boolean colorMask, boolean multiTexture) {
         this(bedrock, kind, colorMask, multiTexture, false);
      }
   }

   public record RenderControllerDef(
      String id,
      MolangExpr condition,
      Map<String, List<String>> arrays,
      MolangExpr geometry,
      List<MolangExpr> textures,
      List<EntityBundle.Rule> materials,
      List<EntityBundle.Rule> partVisibility,
      EntityBundle.Color color,
      EntityBundle.Color overlayColor,
      EntityBundle.Color onFireColor,
      EntityBundle.Color isHurtColor,
      boolean ignoreLighting,
      EntityBundle.UvAnim uvAnim
   ) {
   }

   public record Rule(BonePattern pattern, MolangExpr expr) {
   }

   public record State(
      String name,
      List<EntityBundle.Entry> animations,
      List<EntityBundle.Transition> transitions,
      EntityBundle.Blend blend,
      List<MolangExpr> onEntry,
      List<MolangExpr> onExit
   ) {
   }

   public record TimelineEvent(double time, List<MolangExpr> scripts) {
   }

   public record Transition(String target, MolangExpr condition) {
   }

   public record UvAnim(MolangExpr offsetU, MolangExpr offsetV, MolangExpr scaleU, MolangExpr scaleV) {
      public UvAnim(MolangExpr offsetU, MolangExpr offsetV) {
         this(offsetU, offsetV, Molang.ONE, Molang.ONE);
      }
   }
}
