package com.ans.client.bedrock;

import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
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
         Map<String, GeoBone> bones = (Map<String, GeoBone>)model.boneLookup().get();
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

            for (GeoBone p = bone.parent(); p != null; p = p.parent()) {
               parents.add(p);
            }

            for (int i = parents.size() - 1; i >= 0; i--) {
               chain.mul(rotation(parents.get(i), pose, new Quaternionf()));
            }

            rotation(bone, pose, desired);
            Quaternionf local = chain.conjugate().mul(desired);
            new Matrix3f().set(local).getEulerAnglesZYX(euler);
            float[] v = pose.bone(bone.name());
            v[0] = euler.x - bone.baseRotX();
            v[1] = euler.y - bone.baseRotY();
            v[2] = euler.z - bone.baseRotZ();
         }
      }
   }

   private static int depth(GeoBone b) {
      int d = 0;

      for (GeoBone p = b.parent(); p != null; p = p.parent()) {
         d++;
      }

      return d;
   }

   private static Quaternionf rotation(GeoBone b, MobPose pose, Quaternionf out) {
      float[] v = pose.get(b.name());
      float x = b.baseRotX() + (v == null ? 0.0F : fin(v[0]));
      float y = b.baseRotY() + (v == null ? 0.0F : fin(v[1]));
      float z = b.baseRotZ() + (v == null ? 0.0F : fin(v[2]));
      return out.rotationZYX(z, y, x);
   }

   private static float fin(float f) {
      return Float.isFinite(f) ? f : 0.0F;
   }
}
