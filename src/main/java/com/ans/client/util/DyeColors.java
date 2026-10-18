package com.ans.client.util;

import net.minecraft.world.item.DyeColor;

/** Opaque ARGB tint of a dye color (1.20.1 exposes the diffuse color as floats). */
public final class DyeColors {
   private DyeColors() {
   }

   public static int diffuse(DyeColor color) {
      float[] c = color.getTextureDiffuseColors();
      return 0xFF000000 | Math.round(c[0] * 255.0F) << 16 | Math.round(c[1] * 255.0F) << 8 | Math.round(c[2] * 255.0F);
   }
}
