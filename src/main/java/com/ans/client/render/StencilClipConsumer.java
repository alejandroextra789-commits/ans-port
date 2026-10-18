package com.ans.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;

/**
 * Clips each incoming quad against the planes of a captured mask (1.20.1 VertexConsumer API).
 * A quad is clipped when its fourth vertex calls endVertex(); the surviving polygon is re-emitted as quads.
 */
public final class StencilClipConsumer implements VertexConsumer {
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
   private final List<float[]> planes = new ArrayList<>();
   private final float[][] quad = new float[4][SIZE];
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

   @Override
   public VertexConsumer vertex(double x, double y, double z) {
      this.n++;
      float[] v = this.quad[this.n];
      v[0] = (float)x;
      v[1] = (float)y;
      v[2] = (float)z;
      v[A] = 255.0F;
      v[R] = 255.0F;
      v[G] = 255.0F;
      v[B] = 255.0F;
      v[U] = 0.0F;
      v[U + 1] = 0.0F;
      v[NX] = 0.0F;
      v[NX + 1] = 0.0F;
      v[NX + 2] = 0.0F;
      v[OV] = 0.0F;
      v[LU] = 240.0F;
      v[LV] = 240.0F;
      return this;
   }

   @Override
   public VertexConsumer color(int r, int g, int b, int a) {
      if (this.n >= 0) {
         float[] v = this.quad[this.n];
         v[A] = a & 0xFF;
         v[R] = r & 0xFF;
         v[G] = g & 0xFF;
         v[B] = b & 0xFF;
      }
      return this;
   }

   @Override
   public VertexConsumer uv(float u, float v) {
      if (this.n >= 0) {
         this.quad[this.n][U] = u;
         this.quad[this.n][U + 1] = v;
      }
      return this;
   }

   @Override
   public VertexConsumer overlayCoords(int u, int v) {
      if (this.n >= 0) {
         this.quad[this.n][OV] = u & 0xFFFF | v << 16;
      }
      return this;
   }

   @Override
   public VertexConsumer uv2(int u, int v) {
      if (this.n >= 0) {
         this.quad[this.n][LU] = u & 0xFFFF;
         this.quad[this.n][LV] = v & 0xFFFF;
      }
      return this;
   }

   @Override
   public VertexConsumer normal(float x, float y, float z) {
      if (this.n >= 0) {
         this.quad[this.n][NX] = x;
         this.quad[this.n][NX + 1] = y;
         this.quad[this.n][NX + 2] = z;
      }
      return this;
   }

   @Override
   public void endVertex() {
      if (this.n == 3) {
         this.n = -1;
         this.flush();
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

   private void flush() {
      for (float[] p : this.planes) {
         List<float[]> poly = new ArrayList<>(8);

         for (float[] v : this.quad) {
            poly.add(v.clone());
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
            float[] c = new float[SIZE];

            for (int k = 0; k < SIZE; k++) {
               c[k] = a[k] + (b[k] - a[k]) * t;
            }

            res.add(c);
         }
      }

      return res;
   }

   private void emit(float[] v) {
      int overlay = Math.round(v[OV]);
      this.out.vertex(v[0], v[1], v[2])
         .color(Math.round(v[R]), Math.round(v[G]), Math.round(v[B]), Math.round(v[A]))
         .uv(v[U], v[U + 1])
         .overlayCoords(overlay & 0xFFFF, overlay >>> 16)
         .uv2(Math.round(v[LU]), Math.round(v[LV]))
         .normal(v[NX], v[NX + 1], v[NX + 2])
         .endVertex();
   }

   public static final class Capture implements VertexConsumer {
      final List<float[]> quads = new ArrayList<>();
      private float[] cur = new float[12];
      private int n = -1;

      public boolean isEmpty() {
         return this.quads.isEmpty();
      }

      @Override
      public VertexConsumer vertex(double x, double y, double z) {
         this.n++;
         this.cur[this.n * 3] = (float)x;
         this.cur[this.n * 3 + 1] = (float)y;
         this.cur[this.n * 3 + 2] = (float)z;
         return this;
      }

      @Override
      public VertexConsumer color(int r, int g, int b, int a) {
         return this;
      }

      @Override
      public VertexConsumer uv(float u, float v) {
         return this;
      }

      @Override
      public VertexConsumer overlayCoords(int u, int v) {
         return this;
      }

      @Override
      public VertexConsumer uv2(int u, int v) {
         return this;
      }

      @Override
      public VertexConsumer normal(float x, float y, float z) {
         return this;
      }

      @Override
      public void endVertex() {
         if (this.n == 3) {
            this.quads.add(this.cur);
            this.cur = new float[12];
            this.n = -1;
         }
      }

      @Override
      public void defaultColor(int r, int g, int b, int a) {
      }

      @Override
      public void unsetDefaultColor() {
      }
   }
}
