package com.ans.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

public final class UvAnimConsumer implements VertexConsumer {
   private final VertexConsumer out;

   @Override
   public void defaultColor(int r, int g, int b, int a) {
      this.out.defaultColor(r, g, b, a);
   }

   @Override
   public void unsetDefaultColor() {
      this.out.unsetDefaultColor();
   }

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

   public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
      this.out.addVertex(x, y, z, color, u * this.su + this.ou, v * this.sv + this.ov, overlay, light, nx, ny, nz);
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
      this.out.setUv(u * this.su + this.ou, v * this.sv + this.ov);
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
      this.out.setNormal(x, y, z);
      return this;
   }

   public VertexConsumer setLineWidth(float width) {
      this.out.setLineWidth(width);
      return this;
   }
}
