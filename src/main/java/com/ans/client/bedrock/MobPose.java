package com.ans.client.bedrock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

public final class MobPose {
   private final Map<String, float[]> bones = new HashMap<>();
   private Set<String> entityRelative;
   public Object owner;

   void markEntityRelative(String bone) {
      if (this.entityRelative == null) {
         this.entityRelative = new HashSet<>();
      }

      this.entityRelative.add(bone);
   }

   public Set<String> entityRelative() {
      return this.entityRelative == null ? Set.of() : this.entityRelative;
   }

   float[] bone(String name) {
      float[] v = this.bones.get(name);
      if (v == null) {
         v = new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F};
         this.bones.put(name, v);
      }

      return v;
   }

   public float[] get(String bone) {
      return this.bones.get(bone);
   }

   public boolean isEmpty() {
      return this.bones.isEmpty();
   }

   public Map<String, float[]> bones() {
      return this.bones;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("{");

      for (Entry<String, float[]> e : new TreeMap<>(this.bones).entrySet()) {
         float[] v = e.getValue();
         if (sb.length() > 1) {
            sb.append(", ");
         }

         sb.append(e.getKey())
            .append(
               String.format(
                  Locale.ROOT,
                  "=r(%.1f,%.1f,%.1f) p(%.2f,%.2f,%.2f) s(%.2f,%.2f,%.2f)",
                  Math.toDegrees(v[0]),
                  Math.toDegrees(v[1]),
                  Math.toDegrees(v[2]),
                  v[3],
                  v[4],
                  v[5],
                  v[6],
                  v[7],
                  v[8]
               )
            );
      }

      return sb.append('}').toString();
   }
}
