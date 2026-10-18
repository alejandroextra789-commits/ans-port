package com.ans.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Re-emits each quad with reversed winding (vertices 0,3,2,1), ported to the 1.20.1 VertexConsumer API.
 * A quad is complete after the fourth vertex's endVertex(); that is when it is written out.
 */
public final class ReversedQuadConsumer implements VertexConsumer {
   private static final int DEFAULT_LIGHT = 15728880;
   private final VertexConsumer out;
   private final float[] pos = new float[12];
   private final float[] uv = new float[8];
   private final float[] normal = new float[12];
   private final int[] color = new int[4];
   private final int[] overlay = new int[4];
   private final int[] light = new int[4];
   private int n = -1;

   public ReversedQuadConsumer(VertexConsumer out) {
      this.out = out;
   }

   @Override
   public VertexConsumer vertex(double x, double y, double z) {
      this.n++;
      int i = this.n;
      this.pos[i * 3] = (float)x;
      this.pos[i * 3 + 1] = (float)y;
      this.pos[i * 3 + 2] = (float)z;
      this.color[i] = -1;
      this.overlay[i] = 0;
      this.light[i] = DEFAULT_LIGHT;
      return this;
   }

   @Override
   public VertexConsumer color(int r, int g, int b, int a) {
      if (this.n >= 0) {
         this.color[this.n] = (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
      }
      return this;
   }

   @Override
   public VertexConsumer uv(float u, float v) {
      if (this.n >= 0) {
         this.uv[this.n * 2] = u;
         this.uv[this.n * 2 + 1] = v;
      }
      return this;
   }

   @Override
   public VertexConsumer overlayCoords(int u, int v) {
      if (this.n >= 0) {
         this.overlay[this.n] = u & 65535 | v << 16;
      }
      return this;
   }

   @Override
   public VertexConsumer uv2(int u, int v) {
      if (this.n >= 0) {
         this.light[this.n] = u & 65535 | v << 16;
      }
      return this;
   }

   @Override
   public VertexConsumer normal(float x, float y, float z) {
      if (this.n >= 0) {
         this.normal[this.n * 3] = x;
         this.normal[this.n * 3 + 1] = y;
         this.normal[this.n * 3 + 2] = z;
      }
      return this;
   }

   @Override
   public void endVertex() {
      if (this.n == 3) {
         for (int i : new int[]{0, 3, 2, 1}) {
            int c = this.color[i];
            int ov = this.overlay[i];
            int li = this.light[i];
            this.out.vertex(this.pos[i * 3], this.pos[i * 3 + 1], this.pos[i * 3 + 2])
               .color(c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF, c >>> 24)
               .uv(this.uv[i * 2], this.uv[i * 2 + 1])
               .overlayCoords(ov & 0xFFFF, ov >>> 16)
               .uv2(li & 0xFFFF, li >>> 16)
               .normal(this.normal[i * 3], this.normal[i * 3 + 1], this.normal[i * 3 + 2])
               .endVertex();
         }
         this.n = -1;
      }
   }

   @Override
   public void defaultColor(int r, int g, int b, int a) {
      this.out.defaultColor(r, g, b, a);
   }

   @Override
   public void unsetDefaultColor() {
      this.out.unsetDefaultColor();
   }
}
