package com.ans.client.particle;

import com.ans.client.molang.Molang;
import com.ans.client.molang.MolangExpr;
import com.ans.client.molang.MolangVars;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;

public final class ParticleDef {
   static final Set<String> SUPPORTED = Set.of(
      "minecraft:emitter_initialization",
      "minecraft:emitter_local_space",
      "minecraft:emitter_rate_instant",
      "minecraft:emitter_rate_steady",
      "minecraft:emitter_rate_manual",
      "minecraft:emitter_lifetime_once",
      "minecraft:emitter_lifetime_looping",
      "minecraft:emitter_lifetime_expression",
      "minecraft:emitter_lifetime_events",
      "minecraft:emitter_shape_point",
      "minecraft:emitter_shape_sphere",
      "minecraft:emitter_shape_box",
      "minecraft:emitter_shape_disc",
      "minecraft:emitter_shape_custom",
      "minecraft:emitter_shape_entity_aabb",
      "minecraft:particle_initial_speed",
      "minecraft:particle_initial_spin",
      "minecraft:particle_initialization",
      "minecraft:particle_lifetime_expression",
      "minecraft:particle_lifetime_events",
      "minecraft:particle_expire_if_in_blocks",
      "minecraft:particle_expire_if_not_in_blocks",
      "minecraft:particle_kill_plane",
      "minecraft:particle_motion_dynamic",
      "minecraft:particle_motion_parametric",
      "minecraft:particle_motion_collision",
      "minecraft:particle_appearance_billboard",
      "minecraft:particle_appearance_tinting",
      "minecraft:particle_appearance_lighting"
   );
   public final String id;
   public final ResourceLocation texture;
   public final ParticleDef.Render render;
   public final List<String> unsupported;
   final List<ParticleDef.Curve> curves;
   final MolangExpr creation;
   final MolangExpr emitterPerUpdate;
   final boolean localPosition;
   final boolean localRotation;
   final boolean localVelocity;
   final ParticleDef.Rate rate;
   final MolangExpr numParticles;
   final MolangExpr spawnRate;
   final MolangExpr maxParticles;
   final ParticleDef.Life life;
   final MolangExpr activeTime;
   final MolangExpr sleepTime;
   final MolangExpr activation;
   final MolangExpr expiration;
   final List<String> emitterCreationEvents;
   final List<String> emitterExpirationEvents;
   final List<ParticleDef.TimedEvents> emitterTimeline;
   final ParticleDef.Shape shape;
   final MolangExpr[] offset;
   final MolangExpr radius;
   final MolangExpr[] halfDims;
   final boolean surfaceOnly;
   final MolangExpr [] planeNormal;
   final int directionMode;
   final MolangExpr [] direction;
   final MolangExpr speedScalar;
   final MolangExpr [] speedVector;
   final MolangExpr spinRotation;
   final MolangExpr spinRate;
   final MolangExpr particlePerUpdate;
   final MolangExpr particlePerRender;
   final MolangExpr maxLifetime;
   final MolangExpr particleExpiration;
   final List<String> particleCreationEvents;
   final List<String> particleExpirationEvents;
   final List<ParticleDef.TimedEvents> particleTimeline;
   final Set<String> expireInBlocks;
   final Set<String> expireNotInBlocks;
   final double [] killPlane;
   final boolean dynamic;
   final MolangExpr[] linearAcceleration;
   final MolangExpr linearDrag;
   final MolangExpr rotationAcceleration;
   final MolangExpr rotationDrag;
   final MolangExpr [] parametricPosition;
   final MolangExpr [] parametricDirection;
   final MolangExpr parametricRotation;
   final boolean collision;
   final MolangExpr collisionEnabled;
   final double collisionDrag;
   final double restitution;
   final double collisionRadius;
   final boolean expireOnContact;
   final List<String> collisionEvents;
   final double[] collisionEventMinSpeed;
   final boolean visible;
   final MolangExpr[] size;
   final ParticleDef.Facing facing;
   final boolean customDirection;
   final double minSpeedThreshold;
   final MolangExpr [] customDirectionExpr;
   final double texWidth;
   final double texHeight;
   final MolangExpr [] uv;
   final MolangExpr [] uvSize;
   final ParticleDef.Flipbook flipbook;
   final MolangExpr [] color;
   final ParticleDef.Gradient gradient;
   final boolean lit;
   final Map<String, ParticleDef.Event> events;
   private static final Pattern TEMP = Pattern.compile("(?i)\\b(?:t|temp)\\.([a-z_][a-z0-9_.]*)");

   ParticleDef(String id, JsonObject root) {
      this.id = id;
      String tex = str(root, "texture");
      this.texture = tex == null ? null : ResourceLocation.tryParse(tex);

      this.render = switch (String.valueOf(str(root, "render"))) {
         case "blend" -> ParticleDef.Render.BLEND;
         case "opaque" -> ParticleDef.Render.OPAQUE;
         case "add" -> ParticleDef.Render.ADD;
         default -> ParticleDef.Render.ALPHA;
      };
      JsonObject c = root.has("components") && root.get("components").isJsonObject() ? root.getAsJsonObject("components") : new JsonObject();
      List<String> unknown = new ArrayList<>();

      for (String k : c.keySet()) {
         if (!SUPPORTED.contains(k)) {
            unknown.add(k);
         }
      }

      this.unsupported = List.copyOf(unknown);
      List<ParticleDef.Curve> curves = new ArrayList<>();
      if (root.has("curves") && root.get("curves").isJsonObject()) {
         for (Entry<String, JsonElement> e : root.getAsJsonObject("curves").entrySet()) {
            if (e.getValue().isJsonObject()) {
               JsonObject o = e.getValue().getAsJsonObject();
               String name = e.getKey().toLowerCase(Locale.ROOT);
               name = name.startsWith("variable.") ? name.substring(9) : (name.startsWith("v.") ? name.substring(2) : name);
               JsonElement nodes = o.get("nodes");
               MolangExpr[] n = nodes != null && nodes.isJsonArray() ? exprs(nodes.getAsJsonArray()) : new MolangExpr[0];
               curves.add(
                  new ParticleDef.Curve(
                     MolangVars.id(name),
                     String.valueOf(str(o, "type")).toLowerCase(Locale.ROOT),
                     expr(o.get("input"), Molang.ZERO),
                     expr(o.get("horizontal_range"), Molang.ONE),
                     n
                  )
               );
            }
         }
      }

      this.curves = List.copyOf(curves);
      JsonObject init = obj(c, "minecraft:emitter_initialization");
      this.creation = init == null ? null : expr(init.get("creation_expression"), null);
      this.emitterPerUpdate = init == null ? null : expr(init.get("per_update_expression"), null);
      JsonObject local = obj(c, "minecraft:emitter_local_space");
      this.localPosition = local != null && bool(local.get("position"));
      this.localRotation = local != null && bool(local.get("rotation"));
      this.localVelocity = local != null && bool(local.get("velocity"));
      JsonObject inst = obj(c, "minecraft:emitter_rate_instant");
      JsonObject steady = obj(c, "minecraft:emitter_rate_steady");
      JsonObject manual = obj(c, "minecraft:emitter_rate_manual");
      if (steady != null) {
         this.rate = ParticleDef.Rate.STEADY;
         this.spawnRate = expr(steady.get("spawn_rate"), Molang.ONE);
         this.maxParticles = expr(steady.get("max_particles"), MolangExpr.constant(50.0));
         this.numParticles = Molang.ZERO;
      } else if (inst != null) {
         this.rate = ParticleDef.Rate.INSTANT;
         this.numParticles = expr(inst.get("num_particles"), MolangExpr.constant(10.0));
         this.spawnRate = Molang.ZERO;
         this.maxParticles = MolangExpr.constant(1.0E9);
      } else {
         this.rate = ParticleDef.Rate.MANUAL;
         this.maxParticles = manual == null ? MolangExpr.constant(50.0) : expr(manual.get("max_particles"), MolangExpr.constant(50.0));
         this.numParticles = Molang.ZERO;
         this.spawnRate = Molang.ZERO;
      }

      JsonObject once = obj(c, "minecraft:emitter_lifetime_once");
      JsonObject loop = obj(c, "minecraft:emitter_lifetime_looping");
      JsonObject lex = obj(c, "minecraft:emitter_lifetime_expression");
      if (loop != null) {
         this.life = ParticleDef.Life.LOOPING;
         this.activeTime = expr(loop.get("active_time"), Molang.ONE);
         this.sleepTime = expr(loop.get("sleep_time"), Molang.ZERO);
         this.activation = Molang.ONE;
         this.expiration = Molang.ZERO;
      } else if (lex != null) {
         this.life = ParticleDef.Life.EXPRESSION;
         this.activation = expr(lex.get("activation_expression"), Molang.ONE);
         this.expiration = expr(lex.get("expiration_expression"), Molang.ZERO);
         this.activeTime = Molang.ZERO;
         this.sleepTime = Molang.ZERO;
      } else {
         this.life = ParticleDef.Life.ONCE;
         this.activeTime = once == null ? MolangExpr.constant(10.0) : expr(once.get("active_time"), MolangExpr.constant(10.0));
         this.sleepTime = Molang.ZERO;
         this.activation = Molang.ONE;
         this.expiration = Molang.ZERO;
      }

      JsonObject lev = obj(c, "minecraft:emitter_lifetime_events");
      this.emitterCreationEvents = names(lev == null ? null : lev.get("creation_event"));
      this.emitterExpirationEvents = names(lev == null ? null : lev.get("expiration_event"));
      this.emitterTimeline = timeline(lev == null ? null : lev.get("timeline"));
      ParticleDef.Shape shape = ParticleDef.Shape.POINT;
      JsonObject s = obj(c, "minecraft:emitter_shape_point");
      if (s == null && (s = obj(c, "minecraft:emitter_shape_sphere")) != null) {
         shape = ParticleDef.Shape.SPHERE;
      } else if (s == null && (s = obj(c, "minecraft:emitter_shape_box")) != null) {
         shape = ParticleDef.Shape.BOX;
      } else if (s == null && (s = obj(c, "minecraft:emitter_shape_disc")) != null) {
         shape = ParticleDef.Shape.DISC;
      } else if (s == null && (s = obj(c, "minecraft:emitter_shape_custom")) != null) {
         shape = ParticleDef.Shape.CUSTOM;
      } else if (s == null && (s = obj(c, "minecraft:emitter_shape_entity_aabb")) != null) {
         shape = ParticleDef.Shape.ENTITY_AABB;
      }

      if (s == null) {
         s = new JsonObject();
      }

      this.shape = shape;
      this.offset = vec(s.get("offset"), Molang.ZERO);
      this.radius = expr(s.get("radius"), Molang.ONE);
      this.halfDims = vec(s.get("half_dimensions"), Molang.ZERO);
      this.surfaceOnly = bool(s.get("surface_only"));
      JsonElement pn = s.get("plane_normal");
      if (pn != null && pn.isJsonPrimitive() && pn.getAsJsonPrimitive().isString()) {
         String axis = pn.getAsString().toLowerCase(Locale.ROOT);
         this.planeNormal = new MolangExpr[]{
            axis.equals("x") ? Molang.ONE : Molang.ZERO, axis.equals("y") ? Molang.ONE : Molang.ZERO, axis.equals("z") ? Molang.ONE : Molang.ZERO
         };
      } else {
         this.planeNormal = pn != null && pn.isJsonArray() ? vec(pn, Molang.ZERO) : null;
      }

      JsonElement dir = s.get("direction");
      if (dir != null && dir.isJsonArray()) {
         this.directionMode = 2;
         this.direction = vec(dir, Molang.ZERO);
      } else {
         this.directionMode = dir != null && dir.isJsonPrimitive() && dir.getAsString().equalsIgnoreCase("inwards")
            ? 1
            : (shape != ParticleDef.Shape.POINT && shape != ParticleDef.Shape.CUSTOM ? 0 : 2);
         this.direction = this.directionMode == 2 ? new MolangExpr[]{Molang.ZERO, Molang.ZERO, Molang.ZERO} : null;
      }

      JsonElement sp = c.get("minecraft:particle_initial_speed");
      if (sp != null && sp.isJsonArray()) {
         this.speedVector = vec(sp, Molang.ZERO);
         this.speedScalar = null;
      } else {
         this.speedVector = null;
         this.speedScalar = expr(sp, Molang.ZERO);
      }

      JsonObject spin = obj(c, "minecraft:particle_initial_spin");
      this.spinRotation = spin == null ? Molang.ZERO : expr(spin.get("rotation"), Molang.ZERO);
      this.spinRate = spin == null ? Molang.ZERO : expr(spin.get("rotation_rate"), Molang.ZERO);
      JsonObject pinit = obj(c, "minecraft:particle_initialization");
      this.particlePerUpdate = pinit == null ? null : expr(pinit.get("per_update_expression"), null);
      this.particlePerRender = pinit == null ? null : expr(pinit.get("per_render_expression"), null);
      JsonObject plife = obj(c, "minecraft:particle_lifetime_expression");
      this.maxLifetime = plife == null
         ? MolangExpr.constant(1.0)
         : expr(plife.get("max_lifetime"), plife.has("expiration_expression") ? null : MolangExpr.constant(1.0));
      this.particleExpiration = plife == null ? null : expr(plife.get("expiration_expression"), null);
      JsonObject pev = obj(c, "minecraft:particle_lifetime_events");
      this.particleCreationEvents = names(pev == null ? null : pev.get("creation_event"));
      this.particleExpirationEvents = names(pev == null ? null : pev.get("expiration_event"));
      this.particleTimeline = timeline(pev == null ? null : pev.get("timeline"));
      this.expireInBlocks = blocks(c.get("minecraft:particle_expire_if_in_blocks"));
      this.expireNotInBlocks = blocks(c.get("minecraft:particle_expire_if_not_in_blocks"));
      JsonElement kp = c.get("minecraft:particle_kill_plane");
      if (kp != null && kp.isJsonArray() && kp.getAsJsonArray().size() == 4) {
         double[] plane = new double[4];

         for (int i = 0; i < 4; i++) {
            plane[i] = kp.getAsJsonArray().get(i).getAsDouble();
         }

         this.killPlane = plane;
      } else {
         this.killPlane = null;
      }

      JsonObject dyn = obj(c, "minecraft:particle_motion_dynamic");
      this.dynamic = dyn != null;
      this.linearAcceleration = vec(dyn == null ? null : dyn.get("linear_acceleration"), Molang.ZERO);
      this.linearDrag = dyn == null ? Molang.ZERO : expr(dyn.get("linear_drag_coefficient"), Molang.ZERO);
      this.rotationAcceleration = dyn == null ? Molang.ZERO : expr(dyn.get("rotation_acceleration"), Molang.ZERO);
      this.rotationDrag = dyn == null ? Molang.ZERO : expr(dyn.get("rotation_drag_coefficient"), Molang.ZERO);
      JsonObject par = obj(c, "minecraft:particle_motion_parametric");
      this.parametricPosition = par != null && par.has("relative_position") ? vec(par.get("relative_position"), Molang.ZERO) : null;
      this.parametricDirection = par != null && par.has("direction") ? vec(par.get("direction"), Molang.ZERO) : null;
      this.parametricRotation = par == null ? null : expr(par.get("rotation"), null);
      JsonObject col = obj(c, "minecraft:particle_motion_collision");
      this.collision = col != null;
      this.collisionEnabled = col == null ? Molang.ZERO : expr(col.get("enabled"), Molang.ONE);
      this.collisionDrag = col == null ? 0.0 : num(col.get("collision_drag"), 0.0);
      this.restitution = col == null ? 0.0 : num(col.get("coefficient_of_restitution"), 0.0);
      this.collisionRadius = col == null ? 0.0 : num(col.get("collision_radius"), 0.0);
      this.expireOnContact = col != null && bool(col.get("expire_on_contact"));
      List<String> cev = new ArrayList<>();
      List<Double> cmin = new ArrayList<>();
      if (col != null && col.get("events") != null) {
         JsonElement ev = col.get("events");
         JsonArray arr = ev.isJsonArray() ? ev.getAsJsonArray() : new JsonArray();
         if (ev.isJsonObject()) {
            arr.add(ev);
         }

         for (JsonElement x : arr) {
            if (x.isJsonObject()) {
               String n = str(x.getAsJsonObject(), "event");
               if (n != null) {
                  cev.add(n);
                  cmin.add(num(x.getAsJsonObject().get("min_speed"), 2.0));
               }
            }
         }
      }

      this.collisionEvents = List.copyOf(cev);
      this.collisionEventMinSpeed = cmin.stream().mapToDouble(Double::doubleValue).toArray();
      JsonObject bb = obj(c, "minecraft:particle_appearance_billboard");
      this.visible = bb != null;
      if (bb == null) {
         bb = new JsonObject();
      }

      JsonElement sz = bb.get("size");
      this.size = sz != null && sz.isJsonArray() ? vec2(sz, Molang.ZERO) : new MolangExpr[]{MolangExpr.constant(0.1), MolangExpr.constant(0.1)};
      this.facing = facing(str(bb, "facing_camera_mode"));
      JsonObject dircomp = obj(bb, "direction");
      this.customDirection = dircomp != null && "custom".equalsIgnoreCase(str(dircomp, "mode"));
      this.minSpeedThreshold = dircomp == null ? 0.01 : num(dircomp.get("min_speed_threshold"), 0.01);
      this.customDirectionExpr = dircomp != null && dircomp.has("custom_direction") ? vec(dircomp.get("custom_direction"), Molang.ZERO) : null;
      JsonObject uvo = obj(bb, "uv");
      this.texWidth = uvo == null ? 1.0 : num(uvo.get("texture_width"), 1.0);
      this.texHeight = uvo == null ? 1.0 : num(uvo.get("texture_height"), 1.0);
      JsonObject fb = uvo == null ? null : obj(uvo, "flipbook");
      if (fb != null) {
         this.flipbook = new ParticleDef.Flipbook(
            vec2(fb.get("base_UV"), Molang.ZERO),
            vec2(fb.get("size_UV"), Molang.ZERO),
            vec2(fb.get("step_UV"), Molang.ZERO),
            expr(fb.get("frames_per_second"), Molang.ONE),
            expr(fb.get("max_frame"), Molang.ONE),
            bool(fb.get("stretch_to_lifetime")),
            bool(fb.get("loop"))
         );
         this.uv = null;
         this.uvSize = null;
      } else {
         this.flipbook = null;
         this.uv = uvo != null && uvo.has("uv") ? vec2(uvo.get("uv"), Molang.ZERO) : null;
         this.uvSize = uvo != null && uvo.has("uv_size") ? vec2(uvo.get("uv_size"), Molang.ZERO) : null;
      }

      JsonObject tint = obj(c, "minecraft:particle_appearance_tinting");
      JsonElement colour = tint == null ? null : tint.get("color");
      MolangExpr[] color = null;
      ParticleDef.Gradient gradient = null;
      if (colour != null && colour.isJsonArray()) {
         JsonArray a = colour.getAsJsonArray();
         color = new MolangExpr[]{expr(at(a, 0), Molang.ONE), expr(at(a, 1), Molang.ONE), expr(at(a, 2), Molang.ONE), expr(at(a, 3), Molang.ONE)};
      } else if (colour != null && colour.isJsonPrimitive() && colour.getAsString().startsWith("#")) {
         float[] f = hex(colour.getAsString());
         color = new MolangExpr[]{MolangExpr.constant(f[0]), MolangExpr.constant(f[1]), MolangExpr.constant(f[2]), MolangExpr.constant(f[3])};
      } else if (colour != null && colour.isJsonObject()) {
         JsonObject g = colour.getAsJsonObject();
         JsonElement gr = g.get("gradient");
         List<double[]> keys = new ArrayList<>();
         List<float[]> cols = new ArrayList<>();
         if (gr != null && gr.isJsonObject()) {
            for (Entry<String, JsonElement> e : gr.getAsJsonObject().entrySet()) {
               try {
                  keys.add(new double[]{Double.parseDouble(e.getKey()), keys.size()});
                  cols.add(colorValue(e.getValue()));
               } catch (NumberFormatException var47) {
               }
            }
         } else if (gr != null && gr.isJsonArray()) {
            JsonArray a = gr.getAsJsonArray();

            for (int i = 0; i < a.size(); i++) {
               keys.add(new double[]{a.size() == 1 ? 0.0 : (double)i / (a.size() - 1), i});
               cols.add(colorValue(a.get(i)));
            }
         }

         keys.sort((x, y) -> Double.compare(x[0], y[0]));
         double[] k = new double[keys.size()];
         float[][] cc = new float[keys.size()][];

         for (int i = 0; i < k.length; i++) {
            k[i] = keys.get(i)[0];
            cc[i] = cols.get((int)keys.get(i)[1]);
         }

         if (k.length > 0) {
            gradient = new ParticleDef.Gradient(expr(g.get("interpolant"), Molang.ZERO), k, cc);
         }
      }

      this.color = color;
      this.gradient = gradient;
      this.lit = c.has("minecraft:particle_appearance_lighting");
      Map<String, ParticleDef.Event> events = new LinkedHashMap<>();
      if (root.has("events") && root.get("events").isJsonObject()) {
         for (Entry<String, JsonElement> e : root.getAsJsonObject("events").entrySet()) {
            events.put(e.getKey(), event(e.getValue()));
         }
      }

      this.events = events;
   }

   public List<String> unsupported() {
      return this.unsupported;
   }

   private static String str(JsonObject o, String k) {
      JsonElement e = o.get(k);
      return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
   }

   private static JsonObject obj(JsonObject o, String k) {
      JsonElement e = o.get(k);
      return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
   }

   private static boolean bool(JsonElement e) {
      if (e == null || !e.isJsonPrimitive()) {
         return false;
      } else if (e.getAsJsonPrimitive().isBoolean()) {
         return e.getAsBoolean();
      } else {
         return e.getAsJsonPrimitive().isNumber() ? e.getAsDouble() != 0.0 : "true".equalsIgnoreCase(e.getAsString());
      }
   }

   private static double num(JsonElement e, double fallback) {
      if (e != null && e.isJsonPrimitive()) {
         try {
            return e.getAsDouble();
         } catch (RuntimeException ex) {
            MolangExpr x = Molang.compile(e, null);
            return x != null && x.isConstant() ? x.constantValue() : fallback;
         }
      } else {
         return fallback;
      }
   }

   private static JsonElement at(JsonArray a, int i) {
      return i < a.size() ? a.get(i) : null;
   }

   static MolangExpr expr(JsonElement e, MolangExpr fallback) {
      MolangExpr x = Molang.compile(e, fallback);
      return x == null ? fallback : x;
   }

   private static MolangExpr[] exprs(JsonArray a) {
      MolangExpr[] out = new MolangExpr[a.size()];

      for (int i = 0; i < out.length; i++) {
         out[i] = expr(a.get(i), Molang.ZERO);
      }

      return out;
   }

   private static MolangExpr[] vec(JsonElement e, MolangExpr fill) {
      MolangExpr[] out = new MolangExpr[]{fill, fill, fill};
      if (e != null && e.isJsonArray()) {
         JsonArray a = e.getAsJsonArray();

         for (int i = 0; i < 3 && i < a.size(); i++) {
            out[i] = expr(a.get(i), fill);
         }
      } else if (e != null && e.isJsonPrimitive()) {
         Arrays.fill(out, expr(e, fill));
      }

      return out;
   }

   private static MolangExpr[] vec2(JsonElement e, MolangExpr fill) {
      MolangExpr[] out = new MolangExpr[]{fill, fill};
      if (e != null && e.isJsonArray()) {
         JsonArray a = e.getAsJsonArray();

         for (int i = 0; i < 2 && i < a.size(); i++) {
            out[i] = expr(a.get(i), fill);
         }
      }

      return out;
   }

   private static List<String> names(JsonElement e) {
      if (e != null && !e.isJsonNull()) {
         List<String> out = new ArrayList<>();
         if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) {
               if (x.isJsonPrimitive()) {
                  out.add(x.getAsString());
               }
            }
         } else if (e.isJsonPrimitive()) {
            out.add(e.getAsString());
         }

         return List.copyOf(out);
      } else {
         return List.of();
      }
   }

   private static List<ParticleDef.TimedEvents> timeline(JsonElement e) {
      if (e != null && e.isJsonObject()) {
         List<ParticleDef.TimedEvents> out = new ArrayList<>();

         for (Entry<String, JsonElement> kv : e.getAsJsonObject().entrySet()) {
            try {
               out.add(new ParticleDef.TimedEvents(Double.parseDouble(kv.getKey()), names(kv.getValue())));
            } catch (NumberFormatException var5) {
            }
         }

         out.sort((a, b) -> Double.compare(a.time(), b.time()));
         return List.copyOf(out);
      } else {
         return List.of();
      }
   }

   private static Set<String> blocks(JsonElement e) {
      if (e != null && e.isJsonArray()) {
         Set<String> out = new TreeSet<>();

         for (JsonElement x : e.getAsJsonArray()) {
            String id = x.isJsonPrimitive() ? x.getAsString() : (x.isJsonObject() ? str(x.getAsJsonObject(), "name") : null);
            if (id != null) {
               id = id.toLowerCase(Locale.ROOT);
               if (!id.contains(":")) {
                  id = "minecraft:" + id;
               }

               if (id.equals("minecraft:flowing_water")) {
                  id = "minecraft:water";
               }

               if (id.equals("minecraft:flowing_lava")) {
                  id = "minecraft:lava";
               }

               out.add(id);
            }
         }

         return out;
      } else {
         return null;
      }
   }

   private static ParticleDef.Facing facing(String s) {
      if (s == null) {
         return ParticleDef.Facing.ROTATE_XYZ;
      }

      try {
         return ParticleDef.Facing.valueOf(s.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
         return ParticleDef.Facing.ROTATE_XYZ;
      }
   }

   private static float[] colorValue(JsonElement e) {
      if (e.isJsonPrimitive()) {
         return hex(e.getAsString());
      }

      if (!e.isJsonArray()) {
         return new float[]{1.0F, 1.0F, 1.0F, 1.0F};
      }

      JsonArray a = e.getAsJsonArray();
      float[] f = new float[]{1.0F, 1.0F, 1.0F, 1.0F};

      for (int i = 0; i < 4 && i < a.size(); i++) {
         f[i] = (float)num(a.get(i), 1.0);
      }

      return f;
   }

   static float[] hex(String s) {
      String h = s.startsWith("#") ? s.substring(1) : s;

      try {
         long v = Long.parseLong(h, 16);
         return h.length() <= 6
            ? new float[]{(float)(v >> 16 & 255L) / 255.0F, (float)(v >> 8 & 255L) / 255.0F, (float)(v & 255L) / 255.0F, 1.0F}
            : new float[]{(float)(v >> 16 & 255L) / 255.0F, (float)(v >> 8 & 255L) / 255.0F, (float)(v & 255L) / 255.0F, (float)(v >> 24 & 255L) / 255.0F};
      } catch (NumberFormatException ex) {
         return new float[]{1.0F, 1.0F, 1.0F, 1.0F};
      }
   }

   private static ParticleDef.Event event(JsonElement e) {
      if (!e.isJsonObject()) {
         return new ParticleDef.Event(List.of(), List.of(), new double[0], null, null, null, new int[0], new int[0], null, null);
      }

      JsonObject o = e.getAsJsonObject();
      List<ParticleDef.Event> seq = new ArrayList<>();
      if (o.has("sequence") && o.get("sequence").isJsonArray()) {
         for (JsonElement x : o.getAsJsonArray("sequence")) {
            seq.add(event(x));
         }
      }

      List<ParticleDef.Event> rnd = new ArrayList<>();
      List<Double> w = new ArrayList<>();
      if (o.has("randomize") && o.get("randomize").isJsonArray()) {
         for (JsonElement x : o.getAsJsonArray("randomize")) {
            rnd.add(event(x));
            w.add(x.isJsonObject() ? num(x.getAsJsonObject().get("weight"), 1.0) : 1.0);
         }
      }

      String effect = null;
      String type = null;
      String sound = null;
      MolangExpr pre = null;
      int[] slots = new int[0];
      int[] targets = new int[0];
      JsonObject pe = obj(o, "particle_effect");
      if (pe != null) {
         effect = str(pe, "effect");
         type = String.valueOf(str(pe, "type")).toLowerCase(Locale.ROOT);
         String src = str(pe, "pre_effect_expression");
         if (src != null && !src.isBlank()) {
            Matcher m = TEMP.matcher(src);
            Set<String> temps = new TreeSet<>();
            StringBuilder sb = new StringBuilder();

            while (m.find()) {
               String n = m.group(1).toLowerCase(Locale.ROOT);
               temps.add(n);
               m.appendReplacement(sb, Matcher.quoteReplacement("v.__evt_" + n));
            }

            m.appendTail(sb);
            pre = Molang.compile(sb.toString());
            slots = new int[temps.size()];
            targets = new int[temps.size()];
            int i = 0;

            for (String n : temps) {
               slots[i] = MolangVars.id(n);
               targets[i] = MolangVars.id("__evt_" + n);
               i++;
            }
         }
      }

      JsonObject se = obj(o, "sound_effect");
      if (se != null) {
         sound = str(se, "event_name");
      }

      MolangExpr ex = expr(o.get("expression"), null);
      return new ParticleDef.Event(
         List.copyOf(seq), List.copyOf(rnd), w.stream().mapToDouble(Double::doubleValue).toArray(), effect, type, pre, slots, targets, sound, ex
      );
   }

   record Curve(int slot, String type, MolangExpr input, MolangExpr range, MolangExpr[] nodes) {
   }

   record Event(
      List<ParticleDef.Event> sequence,
      List<ParticleDef.Event> randomize,
      double[] weights,
      String effect,
      String effectType,
      MolangExpr preEffect,
      int[] tempSlots,
      int[] tempTargets,
      String sound,
      MolangExpr expression
   ) {
   }

   public enum Facing {
      ROTATE_XYZ,
      ROTATE_Y,
      LOOKAT_XYZ,
      LOOKAT_Y,
      LOOKAT_DIRECTION,
      DIRECTION_X,
      DIRECTION_Y,
      DIRECTION_Z,
      EMITTER_TRANSFORM_XY,
      EMITTER_TRANSFORM_XZ,
      EMITTER_TRANSFORM_YZ;
   }

   record Flipbook(MolangExpr[] base, MolangExpr[] size, MolangExpr[] step, MolangExpr fps, MolangExpr maxFrame, boolean stretch, boolean loop) {
   }

   record Gradient(MolangExpr interpolant, double[] keys, float[][] colors) {
   }

   public enum Life {
      ONCE,
      LOOPING,
      EXPRESSION;
   }

   public enum Rate {
      INSTANT,
      STEADY,
      MANUAL;
   }

   public enum Render {
      ALPHA,
      BLEND,
      OPAQUE,
      ADD;
   }

   public enum Shape {
      POINT,
      SPHERE,
      BOX,
      DISC,
      CUSTOM,
      ENTITY_AABB;
   }

   record TimedEvents(double time, List<String> events) {
   }
}
