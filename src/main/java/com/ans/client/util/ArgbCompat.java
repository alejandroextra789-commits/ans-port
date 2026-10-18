package com.ans.client.util;

/** Small ARGB helper matching the utility methods used by newer Minecraft versions. */
public final class ArgbCompat {
   private ArgbCompat() {}

   public static int alpha(int color) { return color >>> 24 & 255; }
   public static int red(int color) { return color >>> 16 & 255; }
   public static int green(int color) { return color >>> 8 & 255; }
   public static int blue(int color) { return color & 255; }
   public static int opaque(int rgb) { return rgb | 0xFF000000; }
   public static int white(float alpha) { return colorFromFloat(alpha, 1.0F, 1.0F, 1.0F); }

   public static int colorFromFloat(float a, float r, float g, float b) {
      int ai = channel(a), ri = channel(r), gi = channel(g), bi = channel(b);
      return ai << 24 | ri << 16 | gi << 8 | bi;
   }

   public static int multiplyAlpha(int color, float alpha) {
      int a = channel(alpha * alpha(color) / 255.0F);
      return color & 0x00FFFFFF | a << 24;
   }

   public static int multiply(int first, int second) {
      int a = alpha(first) * alpha(second) / 255;
      int r = red(first) * red(second) / 255;
      int g = green(first) * green(second) / 255;
      int b = blue(first) * blue(second) / 255;
      return a << 24 | r << 16 | g << 8 | b;
   }

   private static int channel(float value) {
      return Math.max(0, Math.min(255, Math.round(value * 255.0F)));
   }
}
