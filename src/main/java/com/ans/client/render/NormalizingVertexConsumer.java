package com.ans.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Normalizes normals before forwarding to the wrapped consumer (1.20.1 VertexConsumer API). */
public final class NormalizingVertexConsumer implements VertexConsumer {
   private final VertexConsumer out;

   public NormalizingVertexConsumer(VertexConsumer out) {
      this.out = out;
   }

   @Override
   public VertexConsumer vertex(double x, double y, double z) {
      this.out.vertex(x, y, z);
      return this;
   }

   @Override
   public VertexConsumer color(int r, int g, int b, int a) {
      this.out.color(r, g, b, a);
      return this;
   }

   @Override
   public VertexConsumer uv(float u, float v) {
      this.out.uv(u, v);
      return this;
   }

   @Override
   public VertexConsumer overlayCoords(int u, int v) {
      this.out.overlayCoords(u, v);
      return this;
   }

   @Override
   public VertexConsumer uv2(int u, int v) {
      this.out.uv2(u, v);
      return this;
   }

   @Override
   public VertexConsumer normal(float x, float y, float z) {
      float len = (float)Math.sqrt(x * x + y * y + z * z);
      if (len > 1.0E-8F) {
         this.out.normal(x / len, y / len, z / len);
      } else {
         this.out.normal(x, y, z);
      }
      return this;
   }

   @Override
   public void endVertex() {
      this.out.endVertex();
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
