package com.ans.client.particle;

import com.ans.client.bedrock.BoneTransforms;
import com.ans.client.bedrock.EntityRuntime;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.bedrock.MobPose;
import com.ans.client.bedrock.RenderLayer;
import com.ans.client.molang.BedrockQueries;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import com.ans.client.bedrock.BoneLookup;
import com.ans.client.bedrock.GeoLocatorData;
import com.ans.client.bedrock.LocatorReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector3f;

public final class RuntimeEffects {
   private static final long FRESH_NANOS = 250000000L;
   static final boolean PLAYER_EYE_EMITTERS = !"false".equalsIgnoreCase(System.getProperty("ans.playerEyeEmitters", "true"));
   static final boolean SPAWN_AT_TICK_POSITION = !"false".equalsIgnoreCase(System.getProperty("ans.effectSpawnTickPos", "true"));
   final EntityRuntime runtime;
   private final Matrix4f captured = new Matrix4f();
   private long capturedAt;
   private long tickedAt = System.nanoTime();
   private FrameResult ownFrame;
   private Map<BakedGeoModel, ? extends Map<String, ? extends Matrix4fc>> anchors;
   private boolean attachable;
   private Entity actor;
   static final int OK = 0;
   static final int SUSPENDED = 1;
   static final int GONE = 2;
   private static final String AL = "0123456789abcdefghijklmnopqrstuvwxyz";

   public RuntimeEffects(EntityRuntime runtime) {
      this.runtime = runtime;
   }

   public void ticked(Entity actor, boolean attachable) {
      this.tickedAt = System.nanoTime();
      this.actor = actor;
      this.attachable = attachable;
   }

   public void capture(Matrix4fc cameraRelative, Vec3 camera) {
      Matrix4f m = new Matrix4f(cameraRelative).translateLocal((float)camera.x, (float)camera.y, (float)camera.z);
      Entity a = this.actor;
      if (a != null) {
         Vector3f t = m.getTranslation(new Vector3f());
         if (a.position().distanceToSqr(t.x, t.y, t.z) > 64.0) {
            return;
         }
      }

      this.captured.set(m);
      this.capturedAt = System.nanoTime();
   }

   public void captureAttachable(
      Matrix4fc cameraRelative, Vec3 camera, FrameResult own, Map<BakedGeoModel, ? extends Map<String, ? extends Matrix4fc>> anchors
   ) {
      this.capture(cameraRelative, camera);
      this.ownFrame = own;
      this.anchors = anchors;
      this.attachable = true;
   }

   public void fire(Entity actor, List<BundleParticles.Effect> effects) {
      if (actor != null) {
         for (BundleParticles.Effect e : effects) {
            BedrockParticles.spawn(this, actor, e);
         }
      }
   }

   public List<Emitter> start(Entity actor, List<BundleParticles.Effect> effects) {
      if (actor != null && !effects.isEmpty()) {
         List<Emitter> out = new ArrayList<>(effects.size());

         for (BundleParticles.Effect e : effects) {
            Emitter em = BedrockParticles.spawn(this, actor, e);
            if (em != null) {
               out.add(em);
            }
         }

         return out;
      } else {
         return List.of();
      }
   }

   public static void stop(List<?> emitters) {
      if (emitters != null) {
         for (Object o : emitters) {
            if (o instanceof Emitter e) {
               e.expire();
            }
         }
      }
   }

   int resolve(Entity actor, String locator, float partialTick, Vector3d pos, Matrix3f rot) {
      return this.resolve(actor, locator, partialTick, pos, rot, false);
   }

   int resolve(Entity actor, String locator, float partialTick, Vector3d pos, Matrix3f rot, boolean spawn) {
      if (actor.isRemoved()) {
         return 2;
      }

      long now = System.nanoTime();
      if (now - this.tickedAt > 60000000000L) {
         return 2;
      }

      if (now - this.tickedAt > 250000000L) {
         return 1;
      }

      boolean fresh = now - this.capturedAt < 250000000L;
      if (this.attachable && !fresh) {
         return 1;
      }

      Matrix4f m = new Matrix4f();
      if (fresh) {
         m.set(this.captured);
      } else {
         fallback(actor, partialTick, m);
      }

      if (locator != null) {
         if (this.attachable) {
            this.attachableLocator(m, locator);
         } else {
            this.entityLocator(m, locator);
         }
      } else if (!this.attachable) {
         float up = PLAYER_EYE_EMITTERS ? (float)BedrockQueries.bedrockPositionOffset(actor) : 0.0F;
         if (spawn && SPAWN_AT_TICK_POSITION) {
            m.setTranslation((float)actor.getX(), (float)actor.getY() + up, (float)actor.getZ());
         } else {
            m.translateLocal(0.0F, up, 0.0F);
         }
      }

      Vector3f t = m.getTranslation(new Vector3f());
      pos.set(t.x, t.y, t.z);
      m.get3x3(rot);
      normalize(rot);
      return 0;
   }

   private static void fallback(Entity actor, float partialTick, Matrix4f m) {
      Vec3 p = actor.getPosition(partialTick);
      float yaw = actor instanceof LivingEntity l ? Mth.rotLerp(partialTick, l.yBodyRotO, l.yBodyRot) : Mth.rotLerp(partialTick, actor.yRotO, actor.getYRot());
      m.translation((float)p.x, (float)p.y, (float)p.z).rotateY((float)Math.toRadians(180.0F - yaw));
      if (actor instanceof LivingEntity l && l.isBaby()) {
         m.scale(0.5F);
      }
   }

   private static void normalize(Matrix3f r) {
      Vector3f c = new Vector3f();

      for (int i = 0; i < 3; i++) {
         r.getColumn(i, c);
         float len = c.length();
         if (len > 1.0E-6F) {
            r.setColumn(i, c.div(len));
         }
      }
   }

   private void entityLocator(Matrix4f m, String name) {
      List<ResourceLocation> models = new ArrayList<>(this.runtime.lastModels());
      ResourceLocation def = this.runtime.bundle().geometry.get("default");
      if (def != null && !models.contains(def)) {
         models.add(def);
      }

      MobPose pose = this.runtime.lastPose();

      for (ResourceLocation id : models) {
         BakedGeoModel model = BoneTransforms.model(id);
         GeoLocatorData loc = model == null ? null : locator(id, name);
         if (loc != null) {
            List<GeoBone> chain = new ArrayList<>();

            for (GeoBone b = parentOf(model, loc); b != null; b = b.getParent()) {
               chain.add(b);
            }

            for (int i = chain.size() - 1; i >= 0; i--) {
               prep(m, chain.get(i), pose);
            }

            applyLocator(m, loc);
            return;
         }
      }
   }

   private void attachableLocator(Matrix4f m, String name) {
      FrameResult own = this.ownFrame;
      Map<BakedGeoModel, ? extends Map<String, ? extends Matrix4fc>> anchors = this.anchors;
      if (own != null && anchors != null) {
         Matrix4f base = new Matrix4f(m);

         for (RenderLayer layer : own.layers()) {
            BakedGeoModel model = BoneTransforms.model(layer.model());
            GeoLocatorData loc = model == null ? null : locator(layer.model(), name);
            if (loc != null) {
               Map<String, ? extends Matrix4fc> a = (Map<String, ? extends Matrix4fc>)anchors.get(model);
               if (a == null) {
                  a = Map.of();
               }

               List<GeoBone> chain = new ArrayList<>();

               for (GeoBone x = parentOf(model, loc); x != null; x = x.getParent()) {
                  chain.add(x);
               }

               for (int i = chain.size() - 1; i >= 0; i--) {
                  GeoBone bone = chain.get(i);
                  if (a.containsKey(bone.getName())) {
                     Matrix4fc anchor = a.get(bone.getName());
                     if (anchor == null) {
                        return;
                     }

                     m.set(base).mul(anchor);
                  }

                  prep(m, bone, own.pose());
               }

               applyLocator(m, loc);
               return;
            }
         }
      }
   }

   private static void prep(Matrix4f m, GeoBone b, MobPose pose) {
      float[] v = pose.get(b.getName());
      if (v != null) {
         m.translate(-fin(v[3], 0.0F) / 16.0F, fin(v[4], 0.0F) / 16.0F, fin(v[5], 0.0F) / 16.0F);
      }

      m.translate(b.getPivotX() / 16.0F, b.getPivotY() / 16.0F, b.getPivotZ() / 16.0F);
      float rx = b.getInitialSnapshot().getRotX() + (v == null ? 0.0F : fin(v[0], 0.0F));
      float ry = b.getInitialSnapshot().getRotY() + (v == null ? 0.0F : fin(v[1], 0.0F));
      float rz = b.getInitialSnapshot().getRotZ() + (v == null ? 0.0F : fin(v[2], 0.0F));
      if (rx != 0.0F || ry != 0.0F || rz != 0.0F) {
         m.rotateZYX(rz, ry, rx);
      }

      if (v != null) {
         m.scale(fin(v[6], 1.0F), fin(v[7], 1.0F), fin(v[8], 1.0F));
      }

      m.translate(-b.getPivotX() / 16.0F, -b.getPivotY() / 16.0F, -b.getPivotZ() / 16.0F);
   }

   private static void applyLocator(Matrix4f m, GeoLocatorData loc) {
      m.translate(loc.offsetX() / 16.0F, loc.offsetY() / 16.0F, loc.offsetZ() / 16.0F);
      if (loc.rotX() != 0.0F || loc.rotY() != 0.0F || loc.rotZ() != 0.0F) {
         m.rotateZYX(loc.rotZ(), loc.rotY(), loc.rotX());
      }
   }

   private static float fin(float f, float fallback) {
      return Float.isFinite(f) ? f : fallback;
   }

   private static GeoLocatorData locator(ResourceLocation id, String raw) {
      Map<String, GeoLocatorData> all = LocatorReader.forModel(id);
      if (all.isEmpty()) {
         return null;
      }

      GeoLocatorData l = all.get(raw);
      if (l != null) {
         return l;
      }

      String n = raw.toLowerCase(Locale.ROOT);
      l = all.get(n);
      if (l != null) {
         return l;
      }

      l = all.get(decode(n));
      if (l != null) {
         return l;
      }

      for (Entry<String, GeoLocatorData> e : all.entrySet()) {
         if (e.getKey().equalsIgnoreCase(raw)) {
            return e.getValue();
         }
      }

      return null;
   }

   /** The bone a locator hangs from, resolved in the model's bone map (GL4 has no loc.getParent()). */
   private static GeoBone parentOf(BakedGeoModel model, GeoLocatorData loc) {
      return BoneLookup.of(model).get(loc.boneName());
   }

   private static String decode(String s) {
      StringBuilder sb = new StringBuilder(s.length());

      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         int idx = "0123456789abcdefghijklmnopqrstuvwxyz".indexOf(c);
         sb.append(idx >= 2 && idx < 28 ? "0123456789abcdefghijklmnopqrstuvwxyz".charAt((idx + 8) % 36) : c);
      }

      return sb.toString();
   }
}
