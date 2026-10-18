package com.ans.client.bedrock;

import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BoneTransforms {
   private static final String AL = "0123456789abcdefghijklmnopqrstuvwxyz";
   private static final Map<String, String> RESOLVED = new ConcurrentHashMap<>();

   private BoneTransforms() {
   }

   public static BakedGeoModel model(ResourceLocation id) {
      BakedGeoModel m = GeckoLibResources.getBakedModels().getModel(id);
      return m != null && !m.isMissingno() ? m : null;
   }

   public static GeoBone resolve(BakedGeoModel model, ResourceLocation modelId, String raw) {
      Map<String, GeoBone> bones = (Map<String, GeoBone>)model.boneLookup().get();
      String key = modelId + "|" + raw;
      String known = RESOLVED.get(key);
      if (known != null) {
         return bones.get(known);
      }

      for (String candidate : candidates(raw)) {
         GeoBone b = bones.get(candidate);
         if (b != null) {
            RESOLVED.put(key, candidate);
            return b;
         }
      }

      return null;
   }

   static List<String> candidates(String raw) {
      String n = raw.toLowerCase(Locale.ROOT);
      List<String> out = new ArrayList<>();
      out.add(n);
      out.add(dec(n));
      int digits = 0;

      while (digits < n.length() && Character.isDigit(n.charAt(n.length() - 1 - digits))) {
         digits++;
      }

      for (int k = 1; k <= digits && k < n.length(); k++) {
         out.add(dec(n.substring(0, n.length() - k)) + n.substring(n.length() - k));
      }

      return out;
   }

   private static String dec(String s) {
      StringBuilder sb = new StringBuilder(s.length());

      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         int idx = "0123456789abcdefghijklmnopqrstuvwxyz".indexOf(c);
         sb.append(idx >= 2 && idx < 28 ? "0123456789abcdefghijklmnopqrstuvwxyz".charAt((idx + 8) % 36) : c);
      }

      return sb.toString();
   }

   public static double[] trs(GeoBone bone, MobPose pose) {
      List<GeoBone> chain = new ArrayList<>();

      for (GeoBone b = bone; b != null; b = b.parent()) {
         chain.add(b);
      }

      Matrix4f m = new Matrix4f();

      for (int i = chain.size() - 1; i >= 0; i--) {
         GeoBone b = chain.get(i);
         float[] v = pose == null ? null : pose.get(b.name());
         if (v != null) {
            m.translate(-v[3] / 16.0F, v[4] / 16.0F, v[5] / 16.0F);
         }

         m.translate(b.pivotX() / 16.0F, b.pivotY() / 16.0F, b.pivotZ() / 16.0F);
         float rx = b.baseRotX() + (v == null ? 0.0F : v[0]);
         float ry = b.baseRotY() + (v == null ? 0.0F : v[1]);
         float rz = b.baseRotZ() + (v == null ? 0.0F : v[2]);
         if (rx != 0.0F || ry != 0.0F || rz != 0.0F) {
            m.rotateZYX(rz, ry, rx);
         }

         if (v != null) {
            m.scale(v[6], v[7], v[8]);
         }

         if (i > 0) {
            m.translate(-b.pivotX() / 16.0F, -b.pivotY() / 16.0F, -b.pivotZ() / 16.0F);
         }
      }

      Vector3f t = m.getTranslation(new Vector3f());
      Vector3f s = m.getScale(new Vector3f());
      Vector3f r = new Matrix4f(m).normalize3x3().getEulerAnglesZYX(new Vector3f());
      return new double[]{-t.x * 16.0F, t.y * 16.0F, t.z * 16.0F, -Math.toDegrees(r.x), -Math.toDegrees(r.y), Math.toDegrees(r.z), s.x, s.y, s.z};
   }

   public static double[] origin(GeoBone bone) {
      return new double[]{-bone.pivotX(), 24.0F - bone.pivotY(), bone.pivotZ()};
   }
}
