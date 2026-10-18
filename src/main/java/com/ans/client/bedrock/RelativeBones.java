package com.ans.client.bedrock;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class RelativeBones {
   private RelativeBones() {
   }

   public static void enable(String mobId) {
   }

   static boolean enabled(String mobId) {
      return BedrockRules.relativeToEntity(mobId);
   }

   static void resolve(MobPose pose, BakedGeoModel model) {
      Set<String> marked = pose.entityRelative();
      if (!marked.isEmpty()) {
         Map<String, GeoBone> bones = (Map<String, GeoBone>)BoneLookup.of(model);
         List<GeoBone> todo = new ArrayList<>();

         for (String name : marked) {
            GeoBone b = bones.get(name);
            if (b != null) {
               todo.add(b);
            }
         }

         todo.sort((a, b) -> Integer.compare(depth(a), depth(b)));
         Quaternionf chain = new Quaternionf();
         Quaternionf desired = new Quaternionf();
         Vector3f euler = new Vector3f();

         for (GeoBone bone : todo) {
            chain.identity();
            List<GeoBone> parents = new ArrayList<>();

            for (GeoBone p = bone.getParent(); p != null; p = p.getParent()) {
               parents.add(p);
            }

            for (int i = parents.size() - 1; i >= 0; i--) {
               chain.mul(rotation(parents.get(i), pose, new Quaternionf()));
            }

            rotation(bone, pose, desired);
            Quaternionf local = chain.conjugate().mul(desired);
            new Matrix3f().set(local).getEulerAnglesZYX(euler);
            float[] v = pose.bone(bone.getName());
            v[0] = euler.x - bone.getInitialSnapshot().getRotX();
            v[1] = euler.y - bone.getInitialSnapshot().getRotY();
            v[2] = euler.z - bone.getInitialSnapshot().getRotZ();
         }
      }
   }

   private static int depth(GeoBone b) {
      int d = 0;

      for (GeoBone p = b.getParent(); p != null; p = p.getParent()) {
         d++;
      }

      return d;
   }

   private static Quaternionf rotation(GeoBone b, MobPose pose, Quaternionf out) {
      float[] v = pose.get(b.getName());
      float x = b.getInitialSnapshot().getRotX() + (v == null ? 0.0F : fin(v[0]));
      float y = b.getInitialSnapshot().getRotY() + (v == null ? 0.0F : fin(v[1]));
      float z = b.getInitialSnapshot().getRotZ() + (v == null ? 0.0F : fin(v[2]));
      return out.rotationZYX(z, y, x);
   }

   private static float fin(float f) {
      return Float.isFinite(f) ? f : 0.0F;
   }
}
