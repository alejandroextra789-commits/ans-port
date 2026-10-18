package com.ans.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Drops back-facing quads before forwarding (1.20.1 VertexConsumer API).
 * A quad is tested after the fourth vertex's endVertex(); front-facing quads are re-emitted unchanged.
 */
public final class CullingQuadConsumer implements VertexConsumer {
   private static final int DEFAULT_LIGHT = 15728880;
   private final VertexConsumer out;
   private final float[] pos = new float[12];
   private final float[] uv = new float[8];
   private final float[] normal = new float[12];
   private final int[] color = new int[4];
   private final int[] overlay = new int[4];
   private final int[] light = new int[4];
   private int n = -1;

   public CullingQuadConsumer(VertexConsumer out) {
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
      if (this.n != 3) {
         return;
      }
      this.n = -1;

      float[] p = this.pos;
      float ax = p[3] - p[0];
      float ay = p[4] - p[1];
      float az = p[5] - p[2];
      float bx = p[6] - p[0];
      float by = p[7] - p[1];
      float bz = p[8] - p[2];
      float nx = ay * bz - az * by;
      float ny = az * bx - ax * bz;
      float nz = ax * by - ay * bx;
      if (nx == 0.0F && ny == 0.0F && nz == 0.0F) {
         bx = p[9] - p[0];
         by = p[10] - p[1];
         bz = p[11] - p[2];
         ax = p[6] - p[0];
         ay = p[7] - p[1];
         az = p[8] - p[2];
         nx = ay * bz - az * by;
         ny = az * bx - ax * bz;
         nz = ax * by - ay * bx;
      }

      if (!(nx * p[0] + ny * p[1] + nz * p[2] >= 0.0F)) {
         for (int i = 0; i < 4; i++) {
            int c = this.color[i];
            int ov = this.overlay[i];
            int li = this.light[i];
            this.out.vertex(p[i * 3], p[i * 3 + 1], p[i * 3 + 2])
               .color(c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF, c >>> 24)
               .uv(this.uv[i * 2], this.uv[i * 2 + 1])
               .overlayCoords(ov & 0xFFFF, ov >>> 16)
               .uv2(li & 0xFFFF, li >>> 16)
               .normal(this.normal[i * 3], this.normal[i * 3 + 1], this.normal[i * 3 + 2])
               .endVertex();
         }
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
