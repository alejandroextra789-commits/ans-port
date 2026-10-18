package com.ans.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Scales and offsets UVs before forwarding to the wrapped consumer (1.20.1 VertexConsumer API). */
public final class UvAnimConsumer implements VertexConsumer {
   private final VertexConsumer out;
   private final float su;
   private final float sv;
   private final float ou;
   private final float ov;

   public UvAnimConsumer(VertexConsumer out, float scaleU, float scaleV, float offsetU, float offsetV) {
      this.out = out;
      this.su = scaleU;
      this.sv = scaleV;
      this.ou = offsetU;
      this.ov = offsetV;
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
      this.out.uv(u * this.su + this.ou, v * this.sv + this.ov);
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
      this.out.normal(x, y, z);
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
