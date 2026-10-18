package com.ans.client.bedrock;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record RenderLayer(
   ResourceLocation model,
   ResourceLocation texture,
   List<RenderLayer.MaterialRule> materials,
   List<RenderLayer.VisibilityRule> visibility,
   int tint,
   boolean hurtOverlay,
   boolean ignoreLighting,
   float uvOffsetU,
   float uvOffsetV,
   int changeColor,
   int overlay,
   float uvScaleU,
   float uvScaleV
) {
   public RenderLayer(
      ResourceLocation model,
      ResourceLocation texture,
      List<RenderLayer.MaterialRule> materials,
      List<RenderLayer.VisibilityRule> visibility,
      int tint,
      boolean hurtOverlay,
      boolean ignoreLighting,
      float uvOffsetU,
      float uvOffsetV,
      int changeColor
   ) {
      this(model, texture, materials, visibility, tint, hurtOverlay, ignoreLighting, uvOffsetU, uvOffsetV, changeColor, 0);
   }

   public RenderLayer(
      ResourceLocation model,
      ResourceLocation texture,
      List<RenderLayer.MaterialRule> materials,
      List<RenderLayer.VisibilityRule> visibility,
      int tint,
      boolean hurtOverlay,
      boolean ignoreLighting,
      float uvOffsetU,
      float uvOffsetV,
      int changeColor,
      int overlay
   ) {
      this(model, texture, materials, visibility, tint, hurtOverlay, ignoreLighting, uvOffsetU, uvOffsetV, changeColor, overlay, 1.0F, 1.0F);
   }

   public RenderLayer(
      ResourceLocation model,
      ResourceLocation texture,
      List<RenderLayer.MaterialRule> materials,
      List<RenderLayer.VisibilityRule> visibility,
      int tint,
      boolean hurtOverlay,
      boolean ignoreLighting
   ) {
      this(model, texture, materials, visibility, tint, hurtOverlay, ignoreLighting, 0.0F, 0.0F, 0);
   }

   public RenderLayer(
      ResourceLocation model,
      ResourceLocation texture,
      List<RenderLayer.MaterialRule> materials,
      List<RenderLayer.VisibilityRule> visibility,
      int tint,
      boolean hurtOverlay,
      boolean ignoreLighting,
      float uvOffsetU,
      float uvOffsetV
   ) {
      this(model, texture, materials, visibility, tint, hurtOverlay, ignoreLighting, uvOffsetU, uvOffsetV, 0);
   }

   public boolean hasUvAnim() {
      return this.uvOffsetU != 0.0F || this.uvOffsetV != 0.0F || this.uvScaleU != 1.0F || this.uvScaleV != 1.0F;
   }

   public RenderKind kindOf(String bone) {
      RenderKind kind = null;

      for (RenderLayer.MaterialRule r : this.materials) {
         if (r.pattern().matches(bone)) {
            kind = r.kind();
         }
      }

      return kind != null ? kind : (this.materials.isEmpty() ? RenderKind.CUTOUT : this.materials.get(0).kind());
   }

   public boolean colorMasked(String bone) {
      if (this.changeColor == 0) {
         return false;
      }

      RenderLayer.MaterialRule rule = null;

      for (RenderLayer.MaterialRule r : this.materials) {
         if (r.pattern().matches(bone)) {
            rule = r;
         }
      }

      if (rule == null) {
         rule = this.materials.isEmpty() ? null : this.materials.get(0);
      }

      return rule != null && rule.colorMask();
   }

   public boolean uvAnimated(String bone) {
      if (!this.hasUvAnim()) {
         return false;
      }

      RenderLayer.MaterialRule rule = null;

      for (RenderLayer.MaterialRule r : this.materials) {
         if (r.pattern().matches(bone)) {
            rule = r;
         }
      }

      if (rule == null) {
         rule = this.materials.isEmpty() ? null : this.materials.get(0);
      }

      return rule == null || rule.uvAnim();
   }

   public boolean glint(String bone) {
      RenderLayer.MaterialRule rule = null;

      for (RenderLayer.MaterialRule r : this.materials) {
         if (r.pattern().matches(bone)) {
            rule = r;
         }
      }

      if (rule == null) {
         rule = this.materials.isEmpty() ? null : this.materials.get(0);
      }

      return rule != null && rule.glint();
   }

   public boolean hasGlint() {
      for (RenderLayer.MaterialRule r : this.materials) {
         if (r.glint()) {
            return true;
         }
      }

      return false;
   }

   public boolean visible(String bone) {
      boolean visible = true;

      for (RenderLayer.VisibilityRule r : this.visibility) {
         if (r.pattern().matches(bone)) {
            visible = r.visible();
         }
      }

      return visible;
   }

   public record MaterialRule(BonePattern pattern, RenderKind kind, boolean colorMask, boolean uvAnim, boolean glint) {
      public MaterialRule(BonePattern pattern, RenderKind kind, boolean colorMask, boolean uvAnim) {
         this(pattern, kind, colorMask, uvAnim, false);
      }

      public MaterialRule(BonePattern pattern, RenderKind kind) {
         this(pattern, kind, false);
      }

      public MaterialRule(BonePattern pattern, RenderKind kind, boolean colorMask) {
         this(pattern, kind, colorMask, true);
      }
   }

   public record VisibilityRule(BonePattern pattern, boolean visible) {
   }
}
