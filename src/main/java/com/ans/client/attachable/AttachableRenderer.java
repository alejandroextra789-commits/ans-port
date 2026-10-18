package com.ans.client.attachable;

import com.ans.client.bedrock.BoneLookup;

import com.ans.client.bedrock.BoneTransforms;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.bedrock.MobPose;
import com.ans.client.bedrock.RenderLayer;
import com.ans.client.particle.BedrockParticles;
import com.ans.client.render.LayerSubmitter;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class AttachableRenderer {
   public static boolean relativeAllLayers = !"false".equalsIgnoreCase(System.getProperty("ans.relativeAllLayers"));
   public static boolean relativeDropsParentScale = !"false".equalsIgnoreCase(System.getProperty("ans.relativeScale"));

   private AttachableRenderer() {
   }

   private static GeoBone holderBone(GeoModel<?> geoModel, FrameResult holder, String name) {
      for (RenderLayer layer : holder.layers()) {
         BakedGeoModel model = geoModel.getBakedModel(layer.model());
         if (!BoneLookup.isMissing(model)) {
            GeoBone b = BoneTransforms.resolve(model, layer.model(), name);
            if (b != null) {
               return b;
            }
         }
      }

      return null;
   }

   private static boolean holderHasExact(GeoModel<?> geoModel, FrameResult holder, String name) {
      for (RenderLayer layer : holder.layers()) {
         BakedGeoModel model = geoModel != null ? geoModel.getBakedModel(layer.model()) : BoneTransforms.model(layer.model());
         if (model != null && !BoneLookup.isMissing(model) && ((Map)BoneLookup.of(model)).containsKey(name)) {
            return true;
         }
      }

      return false;
   }

   private static GeoBone holderBone(FrameResult holder, String name) {
      for (RenderLayer layer : holder.layers()) {
         BakedGeoModel model = BoneTransforms.model(layer.model());
         if (model != null) {
            GeoBone b = BoneTransforms.resolve(model, layer.model(), name);
            if (b != null) {
               return b;
            }
         }
      }

      return null;
   }

   private static String anchorName(GeoBone bone, Map<String, String> bindings, boolean[] bound, Predicate<String> holderHas) {
      String b = bindings.get(bone.getName());
      if (b != null) {
         bound[0] = true;
         return b;
      } else {
         bound[0] = false;
         return holderHas.test(bone.getName()) ? bone.getName() : null;
      }
   }

   static Matrix4f holderMatrix(GeoBone bone, MobPose pose) {
      List<GeoBone> chain = new ArrayList<>();

      for (GeoBone b = bone; b != null; b = b.getParent()) {
         chain.add(b);
      }

      Matrix4f m = new Matrix4f();

      for (int i = chain.size() - 1; i >= 0; i--) {
         GeoBone b = chain.get(i);
         float[] v = pose.get(b.getName());
         if (v != null) {
            m.translate(-fin(v[3], 0.0F) / 16.0F, fin(v[4], 0.0F) / 16.0F, fin(v[5], 0.0F) / 16.0F);
         }

         m.translate(b.getPivotX() / 16.0F, b.getPivotY() / 16.0F, b.getPivotZ() / 16.0F);
         float rx = b.getInitialSnapshot().getRotX() + (v == null ? 0.0F : fin(v[0], 0.0F));
         float ry = b.getInitialSnapshot().getRotY() + (v == null ? 0.0F : fin(v[1], 0.0F));
         float rz = b.getInitialSnapshot().getRotZ() + (v == null ? 0.0F : fin(v[2], 0.0F));
         if (rx != 0.0F || ry != 0.0F || rz != 0.0F) {
            m.rotate(new Quaternionf().rotationZYX(rz, ry, rx));
         }

         if (v != null) {
            m.scale(fin(v[6], 1.0F), fin(v[7], 1.0F), fin(v[8], 1.0F));
         }

         m.translate(-b.getPivotX() / 16.0F, -b.getPivotY() / 16.0F, -b.getPivotZ() / 16.0F);
      }

      m.translate(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);
      return m;
   }

   private static float fin(float f, float fallback) {
      return Float.isFinite(f) ? f : fallback;
   }

   private static Quaternionf chainRotation(GeoBone bone, MobPose pose, Quaternionf out) {
      List<GeoBone> chain = new ArrayList<>();

      for (GeoBone b = bone; b != null; b = b.getParent()) {
         chain.add(b);
      }

      out.identity();

      for (int i = chain.size() - 1; i >= 0; i--) {
         out.mul(rotation(chain.get(i), pose, new Quaternionf()));
      }

      return out;
   }

   private static Quaternionf rotation(GeoBone b, MobPose pose, Quaternionf out) {
      float[] v = pose.get(b.getName());
      float x = b.getInitialSnapshot().getRotX() + (v == null ? 0.0F : fin(v[0], 0.0F));
      float y = b.getInitialSnapshot().getRotY() + (v == null ? 0.0F : fin(v[1], 0.0F));
      float z = b.getInitialSnapshot().getRotZ() + (v == null ? 0.0F : fin(v[2], 0.0F));
      return out.rotationZYX(z, y, x);
   }

   public static void resolveRelative(Attachables.Frame f, FrameResult holder) {
      f.runtime().boneTrsSource(name -> holderTrs(f, holder, name));
      MobPose pose = f.frame().pose();
      if (!pose.entityRelative().isEmpty() && !f.frame().layers().isEmpty()) {
         Map<String, GeoBone> found = new LinkedHashMap<>();
         Map<GeoBone, Map<String, String>> bindsOf = new IdentityHashMap<>();

         for (RenderLayer layer : f.frame().layers()) {
            BakedGeoModel model = BoneTransforms.model(layer.model());
            if (model != null) {
               Map<String, GeoBone> bones = (Map<String, GeoBone>)BoneLookup.of(model);

               for (String n : pose.entityRelative()) {
                  GeoBone b = found.containsKey(n) ? null : bones.get(n);
                  if (b != null) {
                     found.put(n, b);
                     bindsOf.put(b, f.bindings().getOrDefault(layer.model(), Map.of()));
                  }
               }

               if (!relativeAllLayers) {
                  break;
               }
            }
         }

         List<GeoBone> todo = new ArrayList<>(found.values());
         todo.sort((a, bx) -> Integer.compare(depth(a), depth(bx)));
         boolean[] bound = new boolean[1];
         Quaternionf chain = new Quaternionf();
         Quaternionf desired = new Quaternionf();
         Quaternionf tmp = new Quaternionf();
         Vector3f euler = new Vector3f();

         for (GeoBone bone : todo) {
            Map<String, String> binds = bindsOf.get(bone);
            List<GeoBone> parents = new ArrayList<>();
            GeoBone holderAnchor = null;
            String self = anchorName(bone, binds, bound, n -> holderHasExact(null, holder, n));
            if (self != null) {
               holderAnchor = holderBone(holder, self);
            } else {
               for (GeoBone p = bone.getParent(); p != null; p = p.getParent()) {
                  parents.add(p);
                  String a = anchorName(p, binds, bound, n -> holderHasExact(null, holder, n));
                  if (a != null) {
                     holderAnchor = holderBone(holder, a);
                     break;
                  }
               }
            }

            if (holderAnchor != null) {
               chainRotation(holderAnchor, holder.pose(), chain);
            } else {
               chain.identity();
            }

            for (int i = parents.size() - 1; i >= 0; i--) {
               chain.mul(rotation(parents.get(i), pose, tmp));
            }

            rotation(bone, pose, desired);
            Quaternionf local = chain.conjugate().mul(desired);
            new Matrix3f().set(local).getEulerAnglesZYX(euler);
            float[] v = pose.get(bone.getName());
            if (v != null) {
               v[0] = euler.x - bone.getInitialSnapshot().getRotX();
               v[1] = euler.y - bone.getInitialSnapshot().getRotY();
               v[2] = euler.z - bone.getInitialSnapshot().getRotZ();
               if (relativeDropsParentScale) {
                  float sx = 1.0F;
                  float sy = 1.0F;
                  float sz = 1.0F;
                  if (holderAnchor != null) {
                     Vector3f hs = holderMatrix(holderAnchor, holder.pose()).getScale(new Vector3f());
                     sx = hs.x;
                     sy = hs.y;
                     sz = hs.z;
                  }

                  for (GeoBone p : parents) {
                     float[] pv = pose.get(p.getName());
                     if (pv != null) {
                        sx *= fin(pv[6], 1.0F);
                        sy *= fin(pv[7], 1.0F);
                        sz *= fin(pv[8], 1.0F);
                     }
                  }

                  if (Math.abs(sx) > 0.001F
                     && Math.abs(sy) > 0.001F
                     && Math.abs(sz) > 0.001F
                     && (Math.abs(sx - 1.0F) > 1.0E-4F || Math.abs(sy - 1.0F) > 1.0E-4F || Math.abs(sz - 1.0F) > 1.0E-4F)) {
                     v[6] = fin(v[6], 1.0F) / sx;
                     v[7] = fin(v[7], 1.0F) / sy;
                     v[8] = fin(v[8], 1.0F) / sz;
                  }
               }
            }
         }
      }
   }

   static double [] holderTrs(Attachables.Frame f, FrameResult holder, String name) {
      MobPose pose = f.frame().pose();
      boolean[] bound = new boolean[1];

      for (RenderLayer layer : f.frame().layers()) {
         BakedGeoModel model = BoneTransforms.model(layer.model());
         GeoBone bone = model == null ? null : BoneTransforms.resolve(model, layer.model(), name);
         if (bone != null) {
            Map<String, String> binds = f.bindings().getOrDefault(layer.model(), Map.of());
            List<GeoBone> chain = new ArrayList<>();
            Matrix4f m = new Matrix4f();

            for (GeoBone b = bone; b != null; b = b.getParent()) {
               chain.add(b);
               String a = anchorName(b, binds, bound, n -> holderHasExact(null, holder, n));
               if (a != null) {
                  GeoBone hb = holderBone(holder, a);
                  if (hb == null) {
                     return null;
                  }

                  m = holderMatrix(hb, holder.pose());
                  if (bound[0] && b.getParent() == null) {
                     m.translate(0.0F, -1.5F, 0.0F);
                     break;
                  }

                  anchorOffset(m, b, bound[0]);
                  break;
               }
            }

            for (int i = chain.size() - 1; i >= 0; i--) {
               GeoBone b = chain.get(i);
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

               if (i > 0) {
                  m.translate(-b.getPivotX() / 16.0F, -b.getPivotY() / 16.0F, -b.getPivotZ() / 16.0F);
               }
            }

            Vector3f t = m.getTranslation(new Vector3f());
            Vector3f s = m.getScale(new Vector3f());
            Vector3f r = new Matrix4f(m).normalize3x3().getEulerAnglesZYX(new Vector3f());
            return new double[]{-t.x * 16.0F, t.y * 16.0F, t.z * 16.0F, -Math.toDegrees(r.x), -Math.toDegrees(r.y), Math.toDegrees(r.z), s.x, s.y, s.z};
         }
      }

      return null;
   }

   static String describeAnchors(Attachables.Frame f, FrameResult holder) {
      StringBuilder sb = new StringBuilder();
      Iterator var3 = f.bindings().entrySet().iterator();
      if (var3.hasNext()) {
         Entry<ResourceLocation, Map<String, String>> e = (Entry<ResourceLocation, Map<String, String>>)var3.next();

         for (Entry<String, String> b : e.getValue().entrySet()) {
            GeoBone hb = holderBone(holder, b.getValue());
            sb.append(' ').append(b.getKey()).append("->").append(b.getValue());
            if (hb == null) {
               sb.append("(missing)");
            } else {
               Matrix4f m = holderMatrix(hb, holder.pose());
               Vector3f t = m.getTranslation(new Vector3f());
               Vector3f sc = m.getScale(new Vector3f());
               sb.append(String.format(Locale.ROOT, "[t=(%.2f,%.2f,%.2f) s=(%.2f,%.2f,%.2f) chain:", t.x * 16.0F, t.y * 16.0F, t.z * 16.0F, sc.x, sc.y, sc.z));

               for (GeoBone p = hb; p != null; p = p.getParent()) {
                  float[] v = holder.pose().get(p.getName());
                  sb.append(' ').append(p.getName());
                  if (v != null) {
                     sb.append(String.format(Locale.ROOT, "{p(%.1f,%.1f,%.1f)s(%.2f,%.2f,%.2f)}", v[3], v[4], v[5], v[6], v[7], v[8]));
                  }
               }

               sb.append(']');
            }
         }
      }

      return sb.toString();
   }

   private static int depth(GeoBone b) {
      int d = 0;

      for (GeoBone p = b.getParent(); p != null; p = p.getParent()) {
         d++;
      }

      return d;
   }

   private static Map<String, Matrix4f> anchors(GeoModel<?> geoModel, BakedGeoModel model, Map<String, String> binds, FrameResult holder) {
      Map<String, Matrix4f> out = new HashMap<>();
      boolean[] bound = new boolean[1];

      for (GeoBone bone : ((Map)BoneLookup.of(model)).values()) {
         String target = anchorName(bone, binds, bound, n -> holderHasExact(geoModel, holder, n));
         if (target != null) {
            GeoBone hb = holderBone(geoModel, holder, target);
            if (hb == null) {
               out.put(bone.getName(), null);
            } else {
               Matrix4f m = holderMatrix(hb, holder.pose());
               if (bound[0] && bone.getParent() == null) {
                  m.translate(0.0F, -1.5F, 0.0F);
               } else {
                  anchorOffset(m, bone, bound[0]);
               }

               out.put(bone.getName(), m);
            }
         }
      }

      return out;
   }

   static void anchorOffset(Matrix4f m, GeoBone bone, boolean bound) {
      GeoBone ref = bound && bone.getParent() != null ? bone.getParent() : bone;
      m.translate(-ref.getPivotX() / 16.0F, -ref.getPivotY() / 16.0F, -ref.getPivotZ() / 16.0F);
   }

   public static <R extends GeoRenderState> void submit(
      GeoModel<?> geoModel, RenderPassInfo<R> info, SubmitNodeCollector collector, FrameResult holder, List<Attachables.Frame> frames
   ) {
      for (Attachables.Frame f : frames) {
         Map<BakedGeoModel, Map<String, Matrix4f>> anchors = new IdentityHashMap<>();
         Map<BakedGeoModel, Map<String, GeoCube[]>> meshes = new IdentityHashMap<>();

         for (RenderLayer layer : f.frame().layers()) {
            BakedGeoModel model = geoModel.getBakedModel(layer.model());
            if (!BoneLookup.isMissing(model) && !anchors.containsKey(model)) {
               anchors.put(model, anchors(geoModel, model, f.bindings().getOrDefault(layer.model(), Map.of()), holder));
               TextureMeshes.Spec spec = f.bundle().textureMeshes.get(layer.model());
               ResourceLocation meshTexture = spec == null ? null : f.bundle().bundle.textures.get(spec.textureKey());
               if (meshTexture != null) {
                  meshes.put(model, TextureMeshes.overrides(model, layer.model(), spec, meshTexture));
               }
            }
         }

         BedrockParticles.captureAttachable(f.runtime(), info.getModelRenderMatrixPose().pose(), info.cameraState().pos, f.frame(), anchors);
         LayerSubmitter.submit(
            geoModel,
            info,
            collector,
            f.frame(),
            false,
            (modelx, rinfo, out, light, overlay, color) -> draw(
               modelx, anchors.getOrDefault(modelx, Map.of()), meshes.getOrDefault(modelx, Map.of()), rinfo, out, light, overlay, color
            )
         );
      }
   }

   private static void draw(
      BakedGeoModel model,
      Map<String, Matrix4f> anchors,
      Map<String, GeoCube[]> meshes,
      RenderPassInfo<?> info,
      VertexConsumer out,
      int light,
      int overlay,
      int color
   ) {
      PoseStack ps = info.poseStack();
      Pose base = ps.last().copy();

      for (GeoBone top : model.topLevelBones()) {
         renderTree(top, ps, base, anchors, meshes, info, out, light, overlay, color);
      }
   }

   private static void renderTree(
      GeoBone bone,
      PoseStack ps,
      Pose base,
      Map<String, Matrix4f> anchors,
      Map<String, GeoCube[]> meshes,
      RenderPassInfo<?> info,
      VertexConsumer out,
      int light,
      int overlay,
      int color
   ) {
      boolean anchored = anchors.containsKey(bone.getName());
      Matrix4f anchor = anchors.get(bone.getName());
      if (!anchored || anchor != null) {
         ps.pushPose();
         if (anchor != null) {
            ps.last().set(base);
            ps.mulPose(anchor);
         }

         RenderUtil.prepMatrixForBone(ps, bone);
         GeoCube[] meshCubes = meshes.get(bone.getName());
         if (meshCubes == null) {
            bone.render(info, ps, out, light, overlay, color);
         } else if (bone.frameSnapshot == null || !bone.frameSnapshot.isHidden()) {
            for (GeoCube cube : meshCubes) {
               ps.pushPose();
               cube.render(ps, out, light, overlay, color);
               ps.popPose();
            }
         }

         for (GeoBone child : bone.children()) {
            renderTree(child, ps, base, anchors, meshes, info, out, light, overlay, color);
         }

         ps.popPose();
      }
   }
}
