package com.ans.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

public final class CullingQuadConsumer implements VertexConsumer {
   private final VertexConsumer out;

   @Override
   public void defaultColor(int r, int g, int b, int a) {
      this.out.defaultColor(r, g, b, a);
   }

   @Override
   public void unsetDefaultColor() {
      this.out.unsetDefaultColor();
   }

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

   private void flushIfComplete() {
      if (this.n == 3) {
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
               this.out
                  .addVertex(
                     p[i * 3],
                     p[i * 3 + 1],
                     p[i * 3 + 2],
                     this.color[i],
                     this.uv[i * 2],
                     this.uv[i * 2 + 1],
                     this.overlay[i],
                     this.light[i],
                     this.normal[i * 3],
                     this.normal[i * 3 + 1],
                     this.normal[i * 3 + 2]
                  );
            }
         }
      }
   }

   public VertexConsumer addVertex(float x, float y, float z) {
      this.flushIfComplete();
      this.n++;
      int i = this.n;
      this.pos[i * 3] = x;
      this.pos[i * 3 + 1] = y;
      this.pos[i * 3 + 2] = z;
      this.color[i] = -1;
      this.overlay[i] = 0;
      this.light[i] = 15728880;
      return this;
   }

   public VertexConsumer setColor(int r, int g, int b, int a) {
      return this.setColor((a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF);
   }

   public VertexConsumer setColor(int argb) {
      if (this.n >= 0) {
         this.color[this.n] = argb;
      }

      return this;
   }

   public VertexConsumer setUv(float u, float v) {
      if (this.n >= 0) {
         this.uv[this.n * 2] = u;
         this.uv[this.n * 2 + 1] = v;
      }

      return this;
   }

   public VertexConsumer setUv1(int u, int v) {
      if (this.n >= 0) {
         this.overlay[this.n] = u & 65535 | v << 16;
      }

      return this;
   }

   public VertexConsumer setUv2(int u, int v) {
      if (this.n >= 0) {
         this.light[this.n] = u & 65535 | v << 16;
      }

      return this;
   }

   public VertexConsumer setNormal(float x, float y, float z) {
      if (this.n >= 0) {
         this.normal[this.n * 3] = x;
         this.normal[this.n * 3 + 1] = y;
         this.normal[this.n * 3 + 2] = z;
      }

      this.flushIfComplete();
      return this;
   }

   public VertexConsumer setLineWidth(float width) {
      return this;
   }
}
