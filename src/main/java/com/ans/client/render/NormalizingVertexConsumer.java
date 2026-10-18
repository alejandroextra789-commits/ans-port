package com.ans.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

public final class NormalizingVertexConsumer implements VertexConsumer {
   private final VertexConsumer out;

   public NormalizingVertexConsumer(VertexConsumer out) {
      this.out = out;
   }

   public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
      float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
      if (len > 1.0E-8F && Math.abs(len - 1.0F) > 1.0E-4F) {
         nx /= len;
         ny /= len;
         nz /= len;
      }

      this.out.addVertex(x, y, z, color, u, v, overlay, light, nx, ny, nz);
   }

   public VertexConsumer addVertex(float x, float y, float z) {
      this.out.addVertex(x, y, z);
      return this;
   }

   public VertexConsumer setColor(int r, int g, int b, int a) {
      this.out.setColor(r, g, b, a);
      return this;
   }

   public VertexConsumer setColor(int argb) {
      this.out.setColor(argb);
      return this;
   }

   public VertexConsumer setUv(float u, float v) {
      this.out.setUv(u, v);
      return this;
   }

   public VertexConsumer setUv1(int u, int v) {
      this.out.setUv1(u, v);
      return this;
   }

   public VertexConsumer setUv2(int u, int v) {
      this.out.setUv2(u, v);
      return this;
   }

   public VertexConsumer setNormal(float x, float y, float z) {
      float len = (float)Math.sqrt(x * x + y * y + z * z);
      if (len > 1.0E-8F) {
         this.out.setNormal(x / len, y / len, z / len);
      } else {
         this.out.setNormal(x, y, z);
      }

      return this;
   }

   public VertexConsumer setLineWidth(float width) {
      this.out.setLineWidth(width);
      return this;
   }
}
