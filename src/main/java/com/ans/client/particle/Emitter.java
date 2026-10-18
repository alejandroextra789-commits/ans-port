package com.ans.client.particle;

import com.ans.client.bedrock.EntityRuntime;
import com.ans.client.molang.MolangContext;
import com.ans.client.molang.MolangExpr;
import com.ans.client.molang.MolangScope;
import com.ans.client.molang.MolangVars;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

public final class Emitter {
   static final int V_EMITTER_AGE = MolangVars.id("emitter_age");
   static final int V_EMITTER_LIFETIME = MolangVars.id("emitter_lifetime");
   static final int[] V_EMITTER_RANDOM = new int[]{
      MolangVars.id("emitter_random_1"), MolangVars.id("emitter_random_2"), MolangVars.id("emitter_random_3"), MolangVars.id("emitter_random_4")
   };
   static final int V_PARTICLE_AGE = MolangVars.id("particle_age");
   static final int V_PARTICLE_LIFETIME = MolangVars.id("particle_lifetime");
   static final int[] V_PARTICLE_RANDOM = new int[]{
      MolangVars.id("particle_random_1"), MolangVars.id("particle_random_2"), MolangVars.id("particle_random_3"), MolangVars.id("particle_random_4")
   };
   private static final SplittableRandom RANDOM = new SplittableRandom();
   final ParticleDef def;
   final MolangContext ctx;
   final MolangScope vars;
   final Entity actor;
   final RuntimeEffects owner;
   final String locator;
   boolean bound;
   final Vector3d pos = new Vector3d();
   final Matrix3f rot = new Matrix3f();
   boolean placed;
   double age;
   boolean started;
   boolean expired;
   boolean active;
   boolean suspended;
   boolean begun;
   private boolean expirationFired;
   double activeTime;
   double sleepTime;
   double loopStart;
   double spawnAccum;
   int burst;
   Vector3d inheritVelocity;
   private int timelineIndex;
   private double suspendedSince = -1.0;
   final float[] rand = new float[4];
   final List<Emitter.P> particles = new ArrayList<>();

   Emitter(ParticleDef def, MolangContext ctx, Entity actor, RuntimeEffects owner, String locator, boolean bound) {
      this.def = def;
      this.ctx = ctx;
      this.vars = ctx.vars;
      this.actor = actor;
      this.owner = owner;
      this.locator = locator;
      this.bound = bound;

      for (int i = 0; i < 4; i++) {
         this.rand[i] = (float)RANDOM.nextDouble();
         this.vars.set(V_EMITTER_RANDOM[i], this.rand[i]);
      }

      this.vars.set(V_EMITTER_AGE, 0.0);
   }

   public void expire() {
      this.expired = true;
   }

   boolean finished() {
      return this.expired && this.particles.isEmpty();
   }

   void setup(float partialTick, double dt, long frame) {
      MolangContext c = this.ctx;
      c.actor = this.actor;
      c.entity = this.actor instanceof LivingEntity l ? l : null;
      c.partialTick = partialTick;
      c.deltaTime = dt;
      c.frame = frame;
      c.owner = this.owner != null ? this.owner.runtime : null;
      c.frameCache.clear();
   }

   boolean place(float partialTick) {
      if (this.owner != null && this.actor != null && (this.bound || !this.placed)) {
         int r = this.owner.resolve(this.actor, this.locator, partialTick, this.pos, this.rot, !this.placed);
         if (r == 2) {
            this.expire();
            this.bound = false;
         } else if (r == 0) {
            this.placed = true;
            this.suspended = false;
         } else {
            this.suspended = true;
         }

         return this.placed;
      } else {
         return this.placed;
      }
   }

   void update(double dt, BedrockParticles.Frame f) {
      if (!this.place(f.partialTick)) {
         if (this.suspendedSince < 0.0) {
            this.suspendedSince = f.time;
         }

         if (f.time - this.suspendedSince > 1.0) {
            this.expire();
         }

         if (this.expired) {
            this.particles.clear();
         }
      } else {
         MolangScope v = this.vars;
         MolangContext c = this.ctx;
         c.temps.clear();
         if (!this.started) {
            this.started = true;

            for (String e : this.def.emitterCreationEvents) {
               BedrockParticles.event(this, e, null);
            }
         } else if (!this.suspended) {
            this.age += dt;
         }

         v.set(V_EMITTER_AGE, this.age);
         v.set(V_EMITTER_LIFETIME, this.activeTime);

         for (int i = 0; i < 4; i++) {
            v.set(V_EMITTER_RANDOM[i], this.rand[i]);
         }

         this.curves();
         if (this.def.emitterPerUpdate != null) {
            this.def.emitterPerUpdate.exec(c);
         }

         if (!this.expired && !this.suspended) {
            this.lifetime(dt, f);
         }

         if (this.expired && !this.expirationFired) {
            this.expirationFired = true;

            for (String e : this.def.emitterExpirationEvents) {
               BedrockParticles.event(this, e, null);
            }
         }

         for (List<ParticleDef.TimedEvents> tl = this.def.emitterTimeline;
            this.timelineIndex < tl.size() && tl.get(this.timelineIndex).time() <= this.age;
            this.timelineIndex++
         ) {
            for (String e : tl.get(this.timelineIndex).events()) {
               BedrockParticles.event(this, e, null);
            }
         }

         for (int i = 0; i < this.particles.size(); i++) {
            Emitter.P p = this.particles.get(i);
            if (!p.dead) {
               this.updateParticle(p, dt, f);
            }

            if (!p.dead) {
               f.draw(this, p);
            }
         }

         this.particles.removeIf(px -> px.dead);
      }
   }

   private void lifetime(double dt, BedrockParticles.Frame f) {
      ParticleDef d = this.def;
      MolangContext c = this.ctx;
      if (this.burst > 0) {
         for (int i = 0; i < this.burst; i++) {
            this.spawn(f);
         }

         this.burst = 0;
         this.expire();
      } else {
         boolean begin = false;
         switch (d.life) {
            case ONCE:
               if (!this.begun) {
                  this.begun = true;
                  this.activeTime = d.activeTime.eval(c);
                  this.vars.set(V_EMITTER_LIFETIME, this.activeTime);
                  this.active = true;
                  begin = true;
               } else if (this.age > this.activeTime) {
                  this.active = false;
                  this.expire();
                  return;
               }
               break;
            case LOOPING:
               if (!this.begun || this.age - this.loopStart >= Math.max(this.activeTime + this.sleepTime, 0.05)) {
                  if (this.begun) {
                     this.loopStart = this.age;
                  }

                  this.begun = true;
                  this.activeTime = d.activeTime.eval(c);
                  this.sleepTime = d.sleepTime.eval(c);
                  this.vars.set(V_EMITTER_LIFETIME, this.activeTime);
                  begin = true;
               }

               this.active = this.age - this.loopStart < this.activeTime || begin;
               break;
            case EXPRESSION:
               boolean act = d.activation.test(c);
               begin = act && !this.active;
               this.active = act;
         }

         if (this.active) {
            if (begin && d.rate == ParticleDef.Rate.INSTANT) {
               int n = (int)Math.round(d.numParticles.eval(c));

               for (int i = 0; i < n && f.budget(); i++) {
                  this.spawn(f);
               }
            } else if (d.rate == ParticleDef.Rate.STEADY) {
               this.spawnAccum = this.spawnAccum + d.spawnRate.eval(c) * dt;
               double max = d.maxParticles.eval(c);

               while (this.spawnAccum >= 1.0 && f.budget()) {
                  this.spawnAccum--;
                  if (this.particles.size() < max) {
                     this.spawn(f);
                  }
               }

               if (this.spawnAccum > 1.0) {
                  this.spawnAccum = 1.0;
               }
            }
         }

         if (d.life == ParticleDef.Life.EXPRESSION && d.expiration.test(c)) {
            this.expire();
         }
      }
   }

   void bindParticle(Emitter.P p) {
      MolangScope v = this.vars;
      v.set(V_PARTICLE_AGE, p.age);
      v.set(V_PARTICLE_LIFETIME, p.lifetime);

      for (int i = 0; i < 4; i++) {
         v.set(V_PARTICLE_RANDOM[i], p.rand[i]);
      }

      this.curves();
   }

   private void curves() {
      List<ParticleDef.Curve> cs = this.def.curves;

      for (int i = 0; i < cs.size(); i++) {
         ParticleDef.Curve cv = cs.get(i);
         this.vars.set(cv.slot(), this.curve(cv));
      }
   }

   private double curve(ParticleDef.Curve cv) {
      MolangContext c = this.ctx;
      double range = cv.range().eval(c);
      double t = range == 0.0 ? 0.0 : cv.input().eval(c) / range;
      MolangExpr[] n = cv.nodes();
      if (n.length == 0) {
         return 0.0;
      }

      if (n.length == 1) {
         return n[0].eval(c);
      }

      switch (cv.type()) {
         case "bezier":
            if (n.length >= 4) {
               t = clamp01(t);
               double a = n[0].eval(c);
               double b = n[1].eval(c);
               double cc = n[2].eval(c);
               double d = n[3].eval(c);
               double u = 1.0 - t;
               return u * u * u * a + 3.0 * u * u * t * b + 3.0 * u * t * t * cc + t * t * t * d;
            }
            break;
         case "catmull_rom":
            if (n.length >= 4) {
               t = clamp01(t);
               double s = t * (n.length - 3);
               int i = Math.min((int)Math.floor(s), n.length - 4);
               double f = s - i;
               double p0 = n[i].eval(c);
               double p1 = n[i + 1].eval(c);
               double p2 = n[i + 2].eval(c);
               double p3 = n[i + 3].eval(c);
               return 0.5 * (2.0 * p1 + (-p0 + p2) * f + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * f * f + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * f * f * f);
            }
      }

      t = clamp01(t);
      double s = t * (n.length - 1);
      int i = Math.min((int)Math.floor(s), n.length - 2);
      double f = s - i;
      double a = n[i].eval(c);
      double b = n[i + 1].eval(c);
      return a + (b - a) * f;
   }

   private static double clamp01(double t) {
      return !Double.isFinite(t) ? 0.0 : (t < 0.0 ? 0.0 : (t > 1.0 ? 1.0 : t));
   }

   Emitter.P spawn(BedrockParticles.Frame f) {
      ParticleDef d = this.def;
      MolangContext c = this.ctx;
      Emitter.P p = new Emitter.P();
      c.temps.clear();

      for (int i = 0; i < 4; i++) {
         p.rand[i] = (float)RANDOM.nextDouble();
      }

      p.lifetime = 0.0;
      this.bindParticle(p);
      p.lifetime = d.maxLifetime == null ? Double.POSITIVE_INFINITY : d.maxLifetime.eval(c);
      if (!Double.isFinite(p.lifetime) && d.maxLifetime != null) {
         p.lifetime = 0.0;
      }

      this.vars.set(V_PARTICLE_LIFETIME, Double.isFinite(p.lifetime) ? p.lifetime : 0.0);
      Vector3d off = new Vector3d(d.offset[0].eval(c), d.offset[1].eval(c), d.offset[2].eval(c));
      Vector3d local = new Vector3d();
      Vector3d dir = new Vector3d();
      boolean world = false;
      switch (d.shape) {
         case SPHERE:
            double r = d.radius.eval(c);
            randomUnit(local);
            dir.set(local);
            local.mul(d.surfaceOnly ? r : r * Math.cbrt(RANDOM.nextDouble()));
            break;
         case BOX:
            double hx = d.halfDims[0].eval(c);
            double hy = d.halfDims[1].eval(c);
            double hz = d.halfDims[2].eval(c);
            local.set(rnd(-hx, hx), rnd(-hy, hy), rnd(-hz, hz));
            if (d.surfaceOnly) {
               int axis = RANDOM.nextInt(3);
               double sign = RANDOM.nextBoolean() ? 1.0 : -1.0;
               if (axis == 0) {
                  local.x = hx * sign;
               } else if (axis == 1) {
                  local.y = hy * sign;
               } else {
                  local.z = hz * sign;
               }
            }

            dir.set(local);
            break;
         case DISC:
            double rad = d.radius.eval(c);
            double ang = RANDOM.nextDouble() * Math.PI * 2.0;
            double dist = d.surfaceOnly ? rad : rad * Math.sqrt(RANDOM.nextDouble());
            local.set(dist * Math.cos(ang), 0.0, dist * Math.sin(ang));
            if (d.planeNormal != null) {
               Vector3f nrm = new Vector3f((float)d.planeNormal[0].eval(c), (float)d.planeNormal[1].eval(c), (float)d.planeNormal[2].eval(c));
               if (nrm.lengthSquared() > 1.0E-8F) {
                  nrm.normalize();
                  Quaternionf q = new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, nrm.x, nrm.y, nrm.z);
                  Vector3f l = new Vector3f((float)local.x, (float)local.y, (float)local.z).rotate(q);
                  local.set(l.x, l.y, l.z);
               }
            }

            dir.set(local);
            break;
         case ENTITY_AABB:
            if (this.actor != null) {
               AABB bb = this.actor.getBoundingBox();
               double x = rnd(bb.minX, bb.maxX);
               double y = rnd(bb.minY, bb.maxY);
               double z = rnd(bb.minZ, bb.maxZ);
               if (d.surfaceOnly) {
                  int axis = RANDOM.nextInt(3);
                  boolean hi = RANDOM.nextBoolean();
                  if (axis == 0) {
                     x = hi ? bb.maxX : bb.minX;
                  } else if (axis == 1) {
                     y = hi ? bb.maxY : bb.minY;
                  } else {
                     z = hi ? bb.maxZ : bb.minZ;
                  }
               }

               Vec3 center = bb.getCenter();
               local.set(x, y, z);
               dir.set(x - center.x, y - center.y, z - center.z);
               world = true;
            }
      }

      if (d.directionMode == 2 && d.direction != null) {
         dir.set(d.direction[0].eval(c), d.direction[1].eval(c), d.direction[2].eval(c));
      } else if (d.shape == ParticleDef.Shape.POINT || d.shape == ParticleDef.Shape.CUSTOM) {
         randomUnit(dir);
      }

      if (d.directionMode == 1) {
         dir.negate();
      }

      if (dir.lengthSquared() > 1.0E-12) {
         dir.normalize();
      }

      Vector3d speed = new Vector3d(dir);
      if (d.speedVector != null) {
         speed.mul(d.speedVector[0].eval(c), d.speedVector[1].eval(c), d.speedVector[2].eval(c));
      } else if (d.speedScalar != null) {
         speed.mul(d.speedScalar.eval(c));
      }

      if (world) {
         if (d.localPosition) {
            Vector3f l = this.rot.transformTranspose(toF(local.sub(this.pos)));
            p.pos.set(l.x, l.y, l.z);
         } else {
            p.pos.set(local);
         }

         p.vel.set(speed);
      } else {
         local.add(off);
         if (d.localPosition) {
            p.pos.set(local);
            p.vel.set(speed);
            if (!d.localRotation) {
               this.transform(p.pos);
               this.transform(p.vel);
            }
         } else {
            p.pos.set(local);
            this.transform(p.pos);
            p.pos.add(this.pos);
            p.vel.set(speed);
            this.transform(p.vel);
         }
      }

      if (this.inheritVelocity != null && !d.localPosition) {
         p.vel.add(this.inheritVelocity);
      }

      p.dir.set(p.vel);
      p.rotation = d.spinRotation.eval(c);
      p.rotationRate = d.spinRate.eval(c);
      this.particles.add(p);
      f.spawned++;

      for (String e : d.particleCreationEvents) {
         BedrockParticles.event(this, e, p);
      }

      return p;
   }

   private void transform(Vector3d v) {
      Vector3f t = toF(v);
      this.rot.transform(t);
      v.set(t.x, t.y, t.z);
   }

   private static Vector3f toF(Vector3d v) {
      return new Vector3f((float)v.x, (float)v.y, (float)v.z);
   }

   private static double rnd(double a, double b) {
      return a == b ? a : a + RANDOM.nextDouble() * (b - a);
   }

   private static void randomUnit(Vector3d out) {
      double z = RANDOM.nextDouble() * 2.0 - 1.0;
      double a = RANDOM.nextDouble() * Math.PI * 2.0;
      double r = Math.sqrt(1.0 - z * z);
      out.set(r * Math.cos(a), z, r * Math.sin(a));
   }

   Vector3d worldPos(Emitter.P p, Vector3d out) {
      out.set(p.pos);
      if (this.def.localPosition) {
         if (this.def.localRotation) {
            this.transform(out);
         }

         out.add(this.pos);
      }

      return out;
   }

   private void updateParticle(Emitter.P p, double dt, BedrockParticles.Frame f) {
      ParticleDef d = this.def;
      MolangContext c = this.ctx;
      if (p.fresh) {
         p.fresh = false;
         dt = 0.0;
      }

      p.age += dt;
      c.temps.clear();
      this.bindParticle(p);
      if (d.particlePerUpdate != null) {
         d.particlePerUpdate.exec(c);
      }

      if ((d.particleExpiration == null || !d.particleExpiration.test(c)) && !(p.age >= p.lifetime)) {
         if (d.parametricPosition != null) {
            p.pos.set(d.parametricPosition[0].eval(c), d.parametricPosition[1].eval(c), d.parametricPosition[2].eval(c));
            if (!d.localPosition && this.actor == null) {
               this.transform(p.pos);
               p.pos.add(this.pos);
            }
         } else if (d.dynamic || d.speedScalar != null || d.speedVector != null) {
            if (d.dynamic) {
               double ax = d.linearAcceleration[0].eval(c);
               double ay = d.linearAcceleration[1].eval(c);
               double az = d.linearAcceleration[2].eval(c);
               double drag = d.linearDrag.eval(c);
               p.vel.x = p.vel.x + (ax - drag * p.vel.x) * dt;
               p.vel.y = p.vel.y + (ay - drag * p.vel.y) * dt;
               p.vel.z = p.vel.z + (az - drag * p.vel.z) * dt;
               double racc = d.rotationAcceleration.eval(c);
               double rdrag = d.rotationDrag.eval(c);
               p.rotationRate = p.rotationRate + (racc - rdrag * p.rotationRate) * dt;
            }

            this.move(p, dt, f);
            if (p.dead) {
               return;
            }
         }

         if (d.parametricRotation != null) {
            p.rotation = d.parametricRotation.eval(c);
         } else {
            p.rotation = p.rotation + p.rotationRate * dt;
         }

         if (d.parametricDirection != null) {
            p.dir.set(d.parametricDirection[0].eval(c), d.parametricDirection[1].eval(c), d.parametricDirection[2].eval(c));
         } else if (p.vel.lengthSquared() > d.minSpeedThreshold * d.minSpeedThreshold) {
            p.dir.set(p.vel);
         }

         if (d.killPlane != null) {
            Vector3d w = this.worldPos(p, new Vector3d()).sub(this.pos);
            double[] k = d.killPlane;
            double side = k[0] * w.x + k[1] * w.y + k[2] * w.z + k[3];
            if (!Double.isNaN(p.killSide) && Math.signum(side) != Math.signum(p.killSide) && side != 0.0) {
               this.kill(p);
               return;
            }

            p.killSide = side;
         }

         if ((d.expireInBlocks != null || d.expireNotInBlocks != null) && f.level != null) {
            Vector3d w = this.worldPos(p, new Vector3d());
            BlockState bs = f.level.getBlockState(BlockPos.containing(w.x, w.y, w.z));
            String id = bs.isAir() ? "minecraft:air" : BuiltInRegistries.BLOCK.getKey(bs.getBlock()).toString();
            if (d.expireInBlocks != null && d.expireInBlocks.contains(id)) {
               this.kill(p);
               return;
            }

            if (d.expireNotInBlocks != null && !d.expireNotInBlocks.contains(id)) {
               this.kill(p);
               return;
            }
         }

         for (List<ParticleDef.TimedEvents> tl = d.particleTimeline; p.timelineIndex < tl.size() && tl.get(p.timelineIndex).time() <= p.age; p.timelineIndex++) {
            for (String e : tl.get(p.timelineIndex).events()) {
               BedrockParticles.event(this, e, p);
            }
         }
      } else {
         this.kill(p);
      }
   }

   private void move(Emitter.P p, double dt, BedrockParticles.Frame f) {
      ParticleDef d = this.def;
      double dx = p.vel.x * dt;
      double dy = p.vel.y * dt;
      double dz = p.vel.z * dt;
      if (d.collision
         && !d.localPosition
         && f.level != null
         && (dx != 0.0 || dy != 0.0 || dz != 0.0)
         && d.collisionEnabled.test(this.ctx)
         && dx * dx + dy * dy + dz * dz < 100.0) {
         double r = Math.max(d.collisionRadius, 0.005);
         AABB bb = new AABB(p.pos.x - r, p.pos.y - r, p.pos.z - r, p.pos.x + r, p.pos.y + r, p.pos.z + r);
         Vec3 m = Entity.collideBoundingBox(null, new Vec3(dx, dy, dz), bb, f.level, List.of());
         boolean cx = Math.abs(m.x - dx) > 1.0E-7;
         boolean cy = Math.abs(m.y - dy) > 1.0E-7;
         boolean cz = Math.abs(m.z - dz) > 1.0E-7;
         p.pos.add(m.x, m.y, m.z);
         if (cx || cy || cz) {
            double speed = p.vel.length();
            if (d.expireOnContact) {
               this.kill(p);
               return;
            }

            for (int i = 0; i < d.collisionEvents.size(); i++) {
               if (speed >= d.collisionEventMinSpeed[i]) {
                  BedrockParticles.event(this, d.collisionEvents.get(i), p);
               }
            }

            if (cx) {
               p.vel.x = -p.vel.x * d.restitution;
            }

            if (cy) {
               p.vel.y = -p.vel.y * d.restitution;
            }

            if (cz) {
               p.vel.z = -p.vel.z * d.restitution;
            }

            if (d.collisionDrag > 0.0) {
               double s = p.vel.length();
               if (s > 1.0E-9) {
                  p.vel.mul(Math.max(0.0, s - d.collisionDrag * dt) / s);
               }
            }
         }
      } else {
         p.pos.add(dx, dy, dz);
      }
   }

   private void kill(Emitter.P p) {
      if (!p.dead) {
         p.dead = true;

         for (String e : this.def.particleExpirationEvents) {
            BedrockParticles.event(this, e, p);
         }
      }
   }

   EntityRuntime runtime() {
      return this.owner == null ? null : this.owner.runtime;
   }

   Level level() {
      return this.actor == null ? null : this.actor.level();
   }

   static final class P {
      final Vector3d pos = new Vector3d();
      final Vector3d vel = new Vector3d();
      final Vector3d dir = new Vector3d();
      double age;
      double lifetime;
      double rotation;
      double rotationRate;
      final float[] rand = new float[4];
      boolean dead;
      boolean fresh = true;
      int timelineIndex;
      double killSide = Double.NaN;
   }
}
