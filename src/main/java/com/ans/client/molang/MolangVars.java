package com.ans.client.molang;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;

public final class MolangVars {
   private static final Object2IntOpenHashMap<String> IDS = new Object2IntOpenHashMap();
   private static final List<String> NAMES = new ArrayList<>();

   private MolangVars() {
   }

   public static synchronized int id(String name) {
      int id = IDS.getInt(name);
      if (id < 0) {
         id = NAMES.size();
         NAMES.add(name);
         IDS.put(name, id);
      }

      return id;
   }

   public static synchronized String name(int id) {
      return id >= 0 && id < NAMES.size() ? NAMES.get(id) : "?";
   }

   public static synchronized int size() {
      return NAMES.size();
   }

   public static synchronized int[] memberPairs(int src, int dst) {
      String s = name(src) + ".";
      String d = name(dst) + ".";
      List<String> paths = new ArrayList<>();

      for (String n : NAMES) {
         if (n.startsWith(s)) {
            paths.add(n.substring(s.length()));
         }
      }

      int[] out = new int[paths.size() * 2];

      for (int i = 0; i < paths.size(); i++) {
         out[2 * i] = id(s + paths.get(i));
         out[2 * i + 1] = id(d + paths.get(i));
      }

      return out;
   }

   static {
      IDS.defaultReturnValue(-1);
   }
}
