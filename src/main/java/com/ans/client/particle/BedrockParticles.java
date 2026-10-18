package com.ans.client.particle;

import net.minecraft.util.Mth;
import com.ans.Ans;
import com.ans.client.bedrock.EntityRuntime;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.bedrock.MobPose;
import com.ans.client.molang.Molang;
import com.ans.client.molang.MolangContext;
import com.ans.client.molang.MolangScope;
import com.ans.client.sound.PackSounds;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.Map.Entry;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.ans.AnsPlatform;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector3f;

public final class BedrockParticles {
   private static final int MAX_EMITTERS = 3000;
   private static final int MAX_PARTICLES = 20000;
   private static final int FULL_BRIGHT = 15728880;
   private static final SplittableRandom RANDOM = new SplittableRandom();
   public static final boolean ENABLED = !"false".equalsIgnoreCase(System.getProperty("ans.particles"));
   public static boolean DEBUG = Boolean.getBoolean("ans.debugParticles");
   private static long lastStats;
   private static long updateNanos;
   private static long updateFrames;
   private static final List<Emitter> EMITTERS = new ArrayList<>();
   private static final List<Emitter> PENDING = new ArrayList<>();
   private static final ArrayDeque<MolangContext> POOL = new ArrayDeque<>();
   private static double lastTime = Double.NaN;
   private static ClientLevel lastLevel;
   private static long frameId;
   private static int liveParticles;
   public static int lastEmitters;
   public static int lastParticles;
   public static int lastQuads;
   public static long spawnedEmitters;
   private static final Map<String, double[]> PROBE = new LinkedHashMap<>();
   private static final Map<String, String> PROBE_INFO = new LinkedHashMap<>();

   private BedrockParticles() {
   }

   public static void init() {
      if (ENABLED) {
         Path dir = AnsPlatform.gameDir();
         if (Files.isRegularFile(dir.resolve("ans_particles_debug.txt"))) {
            DEBUG = true;
         }

         if (Files.isRegularFile(dir.resolve("ans_particles_thirdperson.txt"))) {
            AnsClientEvents.registerEndTick(mc -> {
               if (mc.player != null && mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
                  mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
               }
            });
         }

         SurfaceQueries.install();
         MinecraftForge.EVENT_BUS.addListener(BedrockParticles::onRenderLevelStage);
      }
   }

   public static void capture(MobPose pose, Matrix4fc modelMatrix, Vec3 camera) {
      if (ENABLED && pose.owner instanceof EntityRuntime rt) {
         rt.particles().capture(modelMatrix, camera);
      }
   }

   public static void captureAttachable(
      EntityRuntime attachable,
      Matrix4fc holderModelMatrix,
      Vec3 camera,
      FrameResult own,
      Map<BakedGeoModel, ? extends Map<String, ? extends Matrix4fc>> anchors
   ) {
      if (ENABLED) {
         attachable.particles().captureAttachable(holderModelMatrix, camera, own, anchors);
      }
   }

   private static MolangContext context() {
      MolangContext c = POOL.poll();
      if (c == null) {
         c = new MolangContext(new MolangScope());
         c.keepTemps = true;
         return c;
      } else {
         c.vars.clear();
         c.temps.clear();
         c.contextVars.clear();
         c.frameCache.clear();
         return c;
      }
   }

   private static void release(MolangContext c) {
      c.actor = null;
      c.entity = null;
      c.owner = null;
      if (POOL.size() < 256) {
         POOL.push(c);
      }
   }

   static Emitter spawn(RuntimeEffects owner, Entity actor, BundleParticles.Effect e) {
      if (ENABLED && EMITTERS.size() + PENDING.size() < 3000) {
         ParticleDef def = ParticleDefs.get(e.effect());
         if (def == null) {
            return null;
         }

         Emitter em = new Emitter(def, context(), actor, owner, e.locator(), e.bound());
         em.setup(Minecraft.getInstance().getFrameTime(), 0.0, frameId);
         MolangContext previous = em.ctx.enter();

         try {
            if (def.creation != null) {
               def.creation.exec(em.ctx);
            }

            if (e.preEffect() != null) {
               e.preEffect().exec(em.ctx);
            }
         } finally {
            MolangContext.restore(previous);
         }

         PENDING.add(em);
         spawnedEmitters++;
         if (DEBUG) {
            Ans.LOGGER
               .info(
                  "[ans-particle] {} #{} {} at {}{}",
                  new Object[]{
                     owner.runtime.mobId(), actor.getId(), e.effect(), e.locator() == null ? "origin" : "locator " + e.locator(), e.bound() ? "" : " (unbound)"
                  }
               );
         }

         return em;
      } else {
         return null;
      }
   }

   static void event(Emitter src, String name, Emitter.P p) {
      ParticleDef.Event ev = src.def.events.get(name);
      if (ev != null) {
         try {
            run(src, ev, p, 0);
         } catch (RuntimeException ex) {
            Molang.warn("particle event " + src.def.id + "/" + name + ": " + ex);
         }
      }
   }

   private static void run(Emitter src, ParticleDef.Event ev, Emitter.P p, int depth) {
      if (depth <= 8) {
         if (p != null) {
            src.bindParticle(p);
         }

         if (ev.expression() != null) {
            ev.expression().exec(src.ctx);
         }

         for (ParticleDef.Event s : ev.sequence()) {
            run(src, s, p, depth + 1);
         }

         if (!ev.randomize().isEmpty()) {
            double total = 0.0;

            for (double w : ev.weights()) {
               total += Math.max(0.0, w);
            }

            double r = RANDOM.nextDouble() * total;

            for (int i = 0; i < ev.randomize().size(); i++) {
               r -= Math.max(0.0, ev.weights()[i]);
               if (r <= 0.0) {
                  run(src, ev.randomize().get(i), p, depth + 1);
                  break;
               }
            }
         }

         if (ev.effect() != null) {
            child(src, ev, p);
         }

         if (ev.sound() != null && src.actor != null) {
            EntityRuntime rt = src.runtime();
            PackSounds.play(src.actor, src.ctx.partialTick, List.of(ev.sound()), rt == null ? "particle" : rt.mobId(), "particle " + src.def.id);
         }
      }
   }

   private static void child(Emitter src, ParticleDef.Event ev, Emitter.P p) {
      if (EMITTERS.size() + PENDING.size() < 3000 && ev.effect() != null) {
         ParticleDef def = ParticleDefs.get(ev.effect());
         if (def != null) {
            String type = ev.effectType() == null ? "emitter" : ev.effectType();
            boolean bound = type.equals("emitter_bound");
            Emitter em = new Emitter(def, context(), src.actor, src.owner, src.locator, bound && src.bound);
            em.pos.set(src.pos);
            em.rot.set(src.rot);
            if (p != null && !bound) {
               src.worldPos(p, em.pos);
            }

            em.placed = true;
            if (type.startsWith("particle")) {
               em.burst = 1;
            }

            if (type.equals("particle_with_velocity") && p != null) {
               Vector3d v = new Vector3d(p.vel);
               if (src.def.localPosition && src.def.localRotation) {
                  Vector3f t = src.rot.transform(new Vector3f((float)v.x, (float)v.y, (float)v.z));
                  v.set(t.x, t.y, t.z);
               }

               em.inheritVelocity = v;
            }

            em.setup(src.ctx.partialTick, 0.0, frameId);
            MolangContext previous = em.ctx.enter();

            try {
               if (def.creation != null) {
                  def.creation.exec(em.ctx);
               }

               if (ev.preEffect() != null) {
                  for (int i = 0; i < ev.tempSlots().length; i++) {
                     Object v = src.ctx.temps.getOrNull(ev.tempSlots()[i]);
                     if (v != null) {
                        em.vars.set(ev.tempTargets()[i], v);
                     }
                  }

                  ev.preEffect().exec(em.ctx);
               }
            } finally {
               MolangContext.restore(previous);
            }

            PENDING.add(em);
         }
      }
   }

   private static void probe(Emitter em, Vector3d pos, Vec3 cam, float sw, float sh, float tu0, float tv0, float tu1, float tv1, int color) {
      double dist = Math.sqrt((pos.x - cam.x) * (pos.x - cam.x) + (pos.y - cam.y) * (pos.y - cam.y) + (pos.z - cam.z) * (pos.z - cam.z));
      double size = Math.max(Math.abs(sw), Math.abs(sh));
      double[] v = PROBE.computeIfAbsent(em.def.id, var0x -> new double[]{0.0, 0.0, Double.MAX_VALUE});
      v[0]++;
      boolean biggest = size > v[1];
      v[1] = Math.max(v[1], size);
      v[2] = Math.min(v[2], dist);
      if (biggest || !PROBE_INFO.containsKey(em.def.id)) {
         String actor = em.actor == null ? "-" : BuiltInRegistries.ENTITY_TYPE.getKey(em.actor.getType()).getPath();
         String rel = "";
         if (em.actor != null) {
            double dx = pos.x - em.actor.getX();
            double dy = pos.y - em.actor.getY();
            double dz = pos.z - em.actor.getZ();
            double yr = Math.toRadians(em.actor.getYRot());
            rel = String.format(
               Locale.ROOT,
               " rel(left,up,fwd)=(%.2f,%.2f,%.2f) pitch=%.0f",
               dx * Math.cos(yr) + dz * Math.sin(yr),
               dy,
               -dx * Math.sin(yr) + dz * Math.cos(yr),
               em.actor.getXRot()
            );
         }

         PROBE_INFO.put(
            em.def.id,
            String.format(
               Locale.ROOT,
               "%s tex=%s %s half=%.3fx%.3f uv=[%.3f,%.3f..%.3f,%.3f] argb=%08x actor=%s locator=%s%s",
               em.def.id,
               em.def.texture,
               em.def.render,
               sw,
               sh,
               tu0,
               tv0,
               tu1,
               tv1,
               color,
               actor,
               em.locator,
               rel
            )
         );
      }
   }

   private static void dumpProbe() {
      if (!PROBE.isEmpty()) {
         List<String> ids = new ArrayList<>(PROBE.keySet());
         ids.sort((a, b) -> Double.compare(PROBE.get(a)[2], PROBE.get(b)[2]));

         for (String id : ids) {
            double[] v = PROBE.get(id);
            Ans.LOGGER
               .info(
                  "[ans-particle-probe] quads={} maxHalf={} minCamDist={} {}",
                  new Object[]{(int)v[0], String.format(Locale.ROOT, "%.3f", v[1]), String.format(Locale.ROOT, "%.2f", v[2]), PROBE_INFO.get(id)}
               );
         }

         PROBE.clear();
         PROBE_INFO.clear();
      }
   }

   private static float[] gradient(ParticleDef.Gradient g, double t) {
      double[] k = g.keys();
      float[][] c = g.colors();
      if (!(t <= k[0]) && k.length != 1) {
         for (int i = 1; i < k.length; i++) {
            if (t <= k[i]) {
               float f = (float)((t - k[i - 1]) / (k[i] - k[i - 1]));
               float[] a = c[i - 1];
               float[] b = c[i];
               return new float[]{a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f, a[3] + (b[3] - a[3]) * f};
            }
         }

         return c[k.length - 1];
      } else {
         return c[0];
      }
   }

   private static int argb(float a, float r, float g, float b) {
      return (int)(clamp(a) * 255.0F + 0.5F) << 24
         | (int)(clamp(r) * 255.0F + 0.5F) << 16
         | (int)(clamp(g) * 255.0F + 0.5F) << 8
         | (int)(clamp(b) * 255.0F + 0.5F);
   }

   private static float clamp(float v) {
      return !(v > 0.0F) ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private static void onRenderLevelStage(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
      frame(event);
   }

   private static void frame(RenderLevelStageEvent event) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level != lastLevel) {
         clear();
         lastLevel = level;
      }

      if (level != null && (!EMITTERS.isEmpty() || !PENDING.isEmpty())) {
         long t0 = System.nanoTime();
         float pt = mc.getFrameTime();
         double now = ((float)level.getGameTime() + pt) / 20.0;
         double dt = Double.isNaN(lastTime) ? 0.0 : Mth.clamp(now - lastTime, 0.0, 0.25);
         lastTime = now;
         frameId++;
         BedrockParticles.Frame f = new BedrockParticles.Frame(pt, now, level, event.getCamera());
         EMITTERS.addAll(PENDING);
         PENDING.clear();
         int particles = 0;

         for (int k = 0; k < EMITTERS.size(); k++) {
            Emitter em = EMITTERS.get(k);
            em.setup(pt, dt, frameId);
            MolangContext previous = em.ctx.enter();

            try {
               em.update(dt, f);
            } catch (RuntimeException ex) {
               Molang.warn("particle " + em.def.id + ": " + ex);
               em.expire();
               em.particles.clear();
            } finally {
               MolangContext.restore(previous);
            }

            particles += em.particles.size();
         }

         for (int k = EMITTERS.size() - 1; k >= 0; k--) {
            Emitter em = EMITTERS.get(k);
            if (em.finished()) {
               EMITTERS.remove(k);
               release(em.ctx);
            }
         }

         liveParticles = particles;
         lastEmitters = EMITTERS.size();
         lastParticles = particles;
         lastQuads = f.quads;
         updateNanos = updateNanos + (System.nanoTime() - t0);
         updateFrames++;
         if (DEBUG && System.currentTimeMillis() - lastStats > 1000L) {
            lastStats = System.currentTimeMillis();
            Ans.LOGGER
               .info(
                  "[ans-particle] {} emitters, {} particles, {} quads, {} spawned total, update+build {} ms/frame: {}",
                  new Object[]{
                     lastEmitters,
                     lastParticles,
                     lastQuads,
                     spawnedEmitters,
                     String.format(Locale.ROOT, "%.3f", updateNanos / 1000000.0 / Math.max(1L, updateFrames)),
                     describe()
                  }
               );
            updateNanos = 0L;
            updateFrames = 0L;
            dumpProbe();
         }

         MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
         PoseStack.Pose pose = event.getPoseStack().last();
         for (Entry<RenderType, BedrockParticles.Bucket> e : f.buckets.entrySet()) {
            BedrockParticles.Bucket b = e.getValue();
            if (b.n != 0) {
               VertexConsumer vc = buffers.getBuffer(e.getKey());
               for (int v = 0; v < b.n; v++) {
                  int o = v * 5;
                  vc.vertex(pose.pose(), b.f[o], b.f[o + 1], b.f[o + 2])
                     .uv(b.f[o + 3], b.f[o + 4])
                     .color(b.i[v * 2])
                     .uv2(b.i[v * 2 + 1])
                     .endVertex();
               }
               buffers.endBatch(e.getKey());
            }
         }
      } else {
         lastQuads = 0;
         lastParticles = 0;
         lastEmitters = 0;
      }
   }

   public static void clear() {
      for (Emitter e : EMITTERS) {
         release(e.ctx);
      }

      for (Emitter e : PENDING) {
         release(e.ctx);
      }

      EMITTERS.clear();
      PENDING.clear();
      lastTime = Double.NaN;
      liveParticles = 0;
   }

   public static int emitterCount() {
      return EMITTERS.size() + PENDING.size();
   }

   public static String describe() {
      Map<String, int[]> m = new LinkedHashMap<>();

      for (Emitter e : EMITTERS) {
         int[] v = m.computeIfAbsent(e.def.id, var0x -> new int[2]);
         v[0]++;
         v[1] += e.particles.size();
      }

      StringBuilder sb = new StringBuilder();

      for (Entry<String, int[]> e : m.entrySet()) {
         sb.append(e.getKey()).append('x').append(e.getValue()[0]).append('(').append(e.getValue()[1]).append(") ");
      }

      return sb.toString();
   }

   static final class Bucket {
      float[] f = new float[1280];
      int[] i = new int[512];
      int n;

      void vertex(float x, float y, float z, float u, float v, int color, int light) {
         if ((this.n + 1) * 5 > this.f.length) {
            this.f = Arrays.copyOf(this.f, this.f.length * 2);
            this.i = Arrays.copyOf(this.i, this.i.length * 2);
         }

         int o = this.n * 5;
         this.f[o] = x;
         this.f[o + 1] = y;
         this.f[o + 2] = z;
         this.f[o + 3] = u;
         this.f[o + 4] = v;
         this.i[this.n * 2] = color;
         this.i[this.n * 2 + 1] = light;
         this.n++;
      }
   }

   static final class Frame {
      final float partialTick;
      final double time;
      final ClientLevel level;
      final Vec3 cam;
      final Vector3f camRight;
      final Vector3f camUp;
      final Map<RenderType, BedrockParticles.Bucket> buckets = new LinkedHashMap<>();
      int spawned;
      int quads;
      private final Vector3d w = new Vector3d();
      private final Vector3f r = new Vector3f();
      private final Vector3f u = new Vector3f();
      private final Vector3f n = new Vector3f();
      private final Vector3f d = new Vector3f();
      private final Vector3f tmp = new Vector3f();

      Frame(float partialTick, double time, ClientLevel level, net.minecraft.client.Camera camera) {
         this.partialTick = partialTick;
         this.time = time;
         this.level = level;
         this.cam = camera.getPosition();
         org.joml.Vector3f left = camera.getLeftVector();
         org.joml.Vector3f up = camera.getUpVector();
         this.camRight = new Vector3f(-left.x, -left.y, -left.z);
         this.camUp = new Vector3f(up.x, up.y, up.z);
      }

      boolean budget() {
         return BedrockParticles.liveParticles + this.spawned < 20000;
      }

      void draw(Emitter em, Emitter.P p) {
         ParticleDef def = em.def;
         if (def.texture != null && def.visible) {
            MolangContext c = em.ctx;
            c.temps.clear();
            if (def.particlePerRender != null) {
               def.particlePerRender.exec(c);
            }

            float sw = (float)def.size[0].eval(c);
            float sh = (float)def.size[1].eval(c);
            if (Float.isFinite(sw) && Float.isFinite(sh) && (!(Math.abs(sw) < 1.0E-5F) || !(Math.abs(sh) < 1.0E-5F))) {
               RenderType type = ParticleRenderTypes.get(def.texture, def.render);
               if (type != null) {
                  double u0;
                  double v0;
                  double us;
                  double vs;
                  if (def.flipbook != null) {
                     ParticleDef.Flipbook fb = def.flipbook;
                     double maxFrame = Math.max(1.0, Math.floor(fb.maxFrame().eval(c)));
                     double fps = fb.stretch() ? (p.lifetime > 0.0 && Double.isFinite(p.lifetime) ? maxFrame / p.lifetime : 0.0) : fb.fps().eval(c);
                     double frame = Math.floor(p.age * fps);
                     if (fb.loop()) {
                        frame %= maxFrame;
                     } else {
                        frame = Math.min(frame, maxFrame - 1.0);
                     }

                     if (!(frame >= 0.0)) {
                        frame = 0.0;
                     }

                     u0 = fb.base()[0].eval(c) + fb.step()[0].eval(c) * frame;
                     v0 = fb.base()[1].eval(c) + fb.step()[1].eval(c) * frame;
                     us = fb.size()[0].eval(c);
                     vs = fb.size()[1].eval(c);
                  } else if (def.uv != null) {
                     u0 = def.uv[0].eval(c);
                     v0 = def.uv[1].eval(c);
                     us = def.uvSize != null ? def.uvSize[0].eval(c) : def.texWidth;
                     vs = def.uvSize != null ? def.uvSize[1].eval(c) : def.texHeight;
                  } else {
                     u0 = 0.0;
                     v0 = 0.0;
                     us = def.texWidth;
                     vs = def.texHeight;
                  }

                  float tu0 = (float)(u0 / def.texWidth);
                  float tv0 = (float)(v0 / def.texHeight);
                  float tu1 = (float)((u0 + us) / def.texWidth);
                  float tv1 = (float)((v0 + vs) / def.texHeight);
                  float cr = 1.0F;
                  float cg = 1.0F;
                  float cb = 1.0F;
                  float ca = 1.0F;
                  if (def.gradient != null) {
                     float[] col = BedrockParticles.gradient(def.gradient, def.gradient.interpolant().eval(c));
                     cr = col[0];
                     cg = col[1];
                     cb = col[2];
                     ca = col[3];
                  } else if (def.color != null) {
                     cr = (float)def.color[0].eval(c);
                     cg = (float)def.color[1].eval(c);
                     cb = (float)def.color[2].eval(c);
                     ca = (float)def.color[3].eval(c);
                  }

                  int color = BedrockParticles.argb(ca, cr, cg, cb);
                  if (color >>> 24 != 0 || def.render == ParticleDef.Render.ADD) {
                     Vector3d pos = em.worldPos(p, this.w);
                     int light = 15728880;
                     if (def.lit && this.level != null) {
                        BlockPos bp = BlockPos.containing(pos.x, pos.y, pos.z);
                        light = this.level.hasChunkAt(bp) ? LevelRenderer.getLightColor(this.level, bp) : 0;
                     }

                     if (this.facing(em, p, pos)) {
                        if (BedrockParticles.DEBUG) {
                           BedrockParticles.probe(em, pos, this.cam, sw, sh, tu0, tv0, tu1, tv1, color);
                        }

                        double rad = Math.toRadians(p.rotation);
                        if (rad != 0.0) {
                           float cs = (float)Math.cos(rad);
                           float sn = (float)Math.sin(rad);
                           this.tmp.set(this.r).mul(cs).fma(sn, this.u);
                           this.u.mul(cs).fma(-sn, this.r);
                           this.r.set(this.tmp);
                        }

                        float x = (float)(pos.x - this.cam.x);
                        float y = (float)(pos.y - this.cam.y);
                        float z = (float)(pos.z - this.cam.z);
                        float rx = this.r.x * sw;
                        float ry = this.r.y * sw;
                        float rz = this.r.z * sw;
                        float ux = this.u.x * sh;
                        float uy = this.u.y * sh;
                        float uz = this.u.z * sh;
                        BedrockParticles.Bucket b = this.buckets.computeIfAbsent(type, var0 -> new BedrockParticles.Bucket());
                        b.vertex(x + rx - ux, y + ry - uy, z + rz - uz, tu1, tv1, color, light);
                        b.vertex(x + rx + ux, y + ry + uy, z + rz + uz, tu1, tv0, color, light);
                        b.vertex(x - rx + ux, y - ry + uy, z - rz + uz, tu0, tv0, color, light);
                        b.vertex(x - rx - ux, y - ry - uy, z - rz - uz, tu0, tv1, color, light);
                        this.quads++;
                     }
                  }
               }
            }
         }
      }

      private boolean facing(Emitter em, Emitter.P p, Vector3d pos) {
         ParticleDef def = em.def;
         Vector3f toCam = this.n.set((float)(this.cam.x - pos.x), (float)(this.cam.y - pos.y), (float)(this.cam.z - pos.z));
         switch (def.facing) {
            case ROTATE_XYZ:
               this.r.set(this.camRight);
               this.u.set(this.camUp);
               break;
            case ROTATE_Y:
               this.u.set(0.0F, 1.0F, 0.0F);
               this.r.set(this.camRight.x, 0.0F, this.camRight.z);
               if (this.r.lengthSquared() < 1.0E-8F) {
                  this.r.set(1.0F, 0.0F, 0.0F);
               }

               this.r.normalize();
               break;
            case LOOKAT_XYZ:
               if (toCam.lengthSquared() < 1.0E-8F) {
                  return false;
               }

               toCam.normalize();
               this.r.set(0.0F, 1.0F, 0.0F).cross(toCam);
               if (this.r.lengthSquared() < 1.0E-6F) {
                  this.r.set(this.camRight);
               }

               this.r.normalize();
               this.u.set(toCam).cross(this.r).normalize();
               break;
            case LOOKAT_Y:
               toCam.y = 0.0F;
               if (toCam.lengthSquared() < 1.0E-8F) {
                  return false;
               }

               toCam.normalize();
               this.u.set(0.0F, 1.0F, 0.0F);
               this.r.set(this.u).cross(toCam).normalize();
               break;
            case EMITTER_TRANSFORM_XY:
               em.rot.transform(this.r.set(1.0F, 0.0F, 0.0F));
               em.rot.transform(this.u.set(0.0F, 1.0F, 0.0F));
               break;
            case EMITTER_TRANSFORM_XZ:
               em.rot.transform(this.r.set(1.0F, 0.0F, 0.0F));
               em.rot.transform(this.u.set(0.0F, 0.0F, -1.0F));
               break;
            case EMITTER_TRANSFORM_YZ:
               em.rot.transform(this.r.set(0.0F, 0.0F, -1.0F));
               em.rot.transform(this.u.set(0.0F, 1.0F, 0.0F));
               break;
            default:
               MolangContext c = em.ctx;
               if (def.customDirection && def.customDirectionExpr != null) {
                  this.d.set((float)def.customDirectionExpr[0].eval(c), (float)def.customDirectionExpr[1].eval(c), (float)def.customDirectionExpr[2].eval(c));
               } else {
                  this.d.set((float)p.dir.x, (float)p.dir.y, (float)p.dir.z);
               }

               if (def.localPosition && def.localRotation) {
                  em.rot.transform(this.d);
               }

               if (this.d.lengthSquared() < 1.0E-10F) {
                  this.d.set(0.0F, 1.0F, 0.0F);
               }

               this.d.normalize();
               Quaternionf q = new Quaternionf();
               switch (def.facing) {
                  case DIRECTION_X:
                     q.rotationTo(1.0F, 0.0F, 0.0F, this.d.x, this.d.y, this.d.z);
                     this.r.set(this.d);
                     q.transform(this.u.set(0.0F, 1.0F, 0.0F));
                     break;
                  case DIRECTION_Y:
                     q.rotationTo(0.0F, 1.0F, 0.0F, this.d.x, this.d.y, this.d.z);
                     this.u.set(this.d);
                     q.transform(this.r.set(1.0F, 0.0F, 0.0F));
                     break;
                  case DIRECTION_Z:
                     q.rotationTo(0.0F, 0.0F, 1.0F, this.d.x, this.d.y, this.d.z);
                     q.transform(this.r.set(1.0F, 0.0F, 0.0F));
                     q.transform(this.u.set(0.0F, 1.0F, 0.0F));
                     break;
                  default:
                     this.r.set(this.d);
                     this.u.set(toCam).cross(this.r);
                     if (this.u.lengthSquared() < 1.0E-8F) {
                        return false;
                     }

                     this.u.normalize();
               }
         }

         return true;
      }
   }
}
