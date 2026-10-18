package com.ans.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;

public final class StencilClipConsumer implements VertexConsumer {
   private static final int X = 0;
   private static final int U = 3;
   private static final int NX = 5;
   private static final int A = 8;
   private static final int R = 9;
   private static final int G = 10;
   private static final int B = 11;
   private static final int OV = 12;
   private static final int LU = 13;
   private static final int LV = 14;
   private static final int SIZE = 15;
   private final VertexConsumer out;

   @Override
   public void defaultColor(int r, int g, int b, int a) {
      this.out.defaultColor(r, g, b, a);
   }

   @Override
   public void unsetDefaultColor() {
      this.out.unsetDefaultColor();
   }

   private final List<float[]> planes = new ArrayList<>();
   private final float[][] quad = new float[4][15];
   private int n = -1;

   public StencilClipConsumer(VertexConsumer out, StencilClipConsumer.Capture mask) {
      this.out = out;

      for (float[] q : mask.quads) {
         float cx = (q[0] + q[3] + q[6] + q[9]) / 4.0F;
         float cy = (q[1] + q[4] + q[7] + q[10]) / 4.0F;
         float cz = (q[2] + q[5] + q[8] + q[11]) / 4.0F;
         float[] p = new float[12];
         boolean ok = true;

         for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            float ax = q[i * 3];
            float ay = q[i * 3 + 1];
            float az = q[i * 3 + 2];
            float bx = q[j * 3];
            float by = q[j * 3 + 1];
            float bz = q[j * 3 + 2];
            float nx = ay * bz - az * by;
            float ny = az * bx - ax * bz;
            float nz = ax * by - ay * bx;
            float s = nx * cx + ny * cy + nz * cz;
            if (s == 0.0F) {
               ok = false;
            }

            if (s < 0.0F) {
               nx = -nx;
               ny = -ny;
               nz = -nz;
            }

            p[i * 3] = nx;
            p[i * 3 + 1] = ny;
            p[i * 3 + 2] = nz;
         }

         if (ok) {
            this.planes.add(p);
         }
      }
   }

   private void flushIfComplete() {
      if (this.n == 3) {
         this.n = -1;

         for (float[] p : this.planes) {
            List<float[]> poly = new ArrayList<>(8);

            for (float[] v : this.quad) {
               poly.add((float[])v.clone());
            }

            for (int i = 0; i < 4 && poly.size() >= 3; i++) {
               poly = clip(poly, p[i * 3], p[i * 3 + 1], p[i * 3 + 2]);
            }

            if (poly.size() >= 3) {
               for (int k = 1; k + 1 < poly.size(); k++) {
                  this.emit(poly.get(0));
                  this.emit(poly.get(k));
                  this.emit(poly.get(k + 1));
                  this.emit(poly.get(k + 1));
               }
            }
         }
      }
   }

   private static List<float[]> clip(List<float[]> poly, float nx, float ny, float nz) {
      List<float[]> res = new ArrayList<>(poly.size() + 2);

      for (int i = 0; i < poly.size(); i++) {
         float[] a = poly.get(i);
         float[] b = poly.get((i + 1) % poly.size());
         float da = a[0] * nx + a[1] * ny + a[2] * nz;
         float db = b[0] * nx + b[1] * ny + b[2] * nz;
         if (da >= 0.0F) {
            res.add(a);
         }

         if (da >= 0.0F != db >= 0.0F) {
            float t = da / (da - db);
            float[] c = new float[15];

            for (int k = 0; k < 15; k++) {
               c[k] = a[k] + (b[k] - a[k]) * t;
            }

            res.add(c);
         }
      }

      return res;
   }

   private void emit(float[] v) {
      int color = Math.round(v[8]) << 24 | Math.round(v[9]) << 16 | Math.round(v[10]) << 8 | Math.round(v[11]);
      int overlay = Math.round(v[12]);
      int light = Math.round(v[13]) & 65535 | Math.round(v[14]) << 16;
      this.out.addVertex(v[0], v[1], v[2], color, v[3], v[4], overlay, light, v[5], v[6], v[7]);
   }

   public VertexConsumer addVertex(float x, float y, float z) {
      this.flushIfComplete();
      this.n++;
      float[] v = this.quad[this.n];
      v[0] = x;
      v[1] = y;
      v[2] = z;
      v[8] = v[9] = v[10] = v[11] = 255.0F;
      v[12] = 0.0F;
      v[13] = 240.0F;
      v[14] = 240.0F;
      return this;
   }

   public VertexConsumer setColor(int r, int g, int b, int a) {
      return this.setColor((a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF);
   }

   public VertexConsumer setColor(int argb) {
      if (this.n >= 0) {
         float[] v = this.quad[this.n];
         v[8] = argb >>> 24;
         v[9] = argb >> 16 & 0xFF;
         v[10] = argb >> 8 & 0xFF;
         v[11] = argb & 0xFF;
      }

      return this;
   }

   public VertexConsumer setUv(float u, float v) {
      if (this.n >= 0) {
         this.quad[this.n][3] = u;
         this.quad[this.n][4] = v;
      }

      return this;
   }

   public VertexConsumer setUv1(int u, int v) {
      if (this.n >= 0) {
         this.quad[this.n][12] = u & 65535 | v << 16;
      }

      return this;
   }

   public VertexConsumer setUv2(int u, int v) {
      if (this.n >= 0) {
         this.quad[this.n][13] = u & 65535;
         this.quad[this.n][14] = v & 65535;
      }

      return this;
   }

   public VertexConsumer setNormal(float x, float y, float z) {
      if (this.n >= 0) {
         this.quad[this.n][5] = x;
         this.quad[this.n][6] = y;
         this.quad[this.n][7] = z;
      }

      this.flushIfComplete();
      return this;
   }

   public VertexConsumer setLineWidth(float width) {
      return this;
   }

   public static final class Capture implements VertexConsumer {
      final List<float[]> quads = new ArrayList<>();

      @Override
      public void defaultColor(int r, int g, int b, int a) {
      }

      @Override
      public void unsetDefaultColor() {
      }

      private float[] cur = new float[12];
      private int n = -1;

      public boolean isEmpty() {
         return this.quads.isEmpty();
      }

      public VertexConsumer addVertex(float x, float y, float z) {
         this.n++;
         this.cur[this.n * 3] = x;
         this.cur[this.n * 3 + 1] = y;
         this.cur[this.n * 3 + 2] = z;
         if (this.n == 3) {
            this.quads.add(this.cur);
            this.cur = new float[12];
            this.n = -1;
         }

         return this;
      }

      public VertexConsumer setColor(int r, int g, int b, int a) {
         return this;
      }

      public VertexConsumer setColor(int argb) {
         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         return this;
      }

      public VertexConsumer setLineWidth(float width) {
         return this;
      }
   }
}
