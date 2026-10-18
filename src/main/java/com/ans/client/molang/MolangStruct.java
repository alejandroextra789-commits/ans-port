package com.ans.client.molang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public final class MolangStruct {
   private final Map<String, Double> members = new LinkedHashMap<>();

   public MolangStruct put(String path, double value) {
      this.members.put(path, value);
      return this;
   }

   public Map<String, Double> members() {
      return this.members;
   }

   public static MolangStruct trs(double[] v) {
      MolangStruct s = new MolangStruct();
      String[] groups = new String[]{"t", "r", "s"};
      String[] axes = new String[]{"x", "y", "z"};

      for (int g = 0; g < 3; g++) {
         for (int a = 0; a < 3; a++) {
            s.put(groups[g] + "." + axes[a], v[g * 3 + a]);
         }
      }

      return s;
   }

   void assignTo(MolangScope scope, String name) {
      for (Entry<String, Double> e : this.members.entrySet()) {
         scope.set(MolangVars.id(name + "." + e.getKey()), e.getValue().doubleValue());
      }
   }

   @Override
   public String toString() {
      return "struct" + this.members;
   }
}
