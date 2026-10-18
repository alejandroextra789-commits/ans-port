package com.ans.client.render;

public final class UiRender {
   public static boolean enabled = !"false".equalsIgnoreCase(System.getProperty("ans.paperDoll", "true"));
   private static int depth;

   private UiRender() {
   }

   public static boolean active() {
      return depth > 0;
   }

   public static void begin() {
      if (enabled) {
         depth++;
      }
   }

   public static void end() {
      if (depth > 0) {
         depth--;
      }
   }
}
