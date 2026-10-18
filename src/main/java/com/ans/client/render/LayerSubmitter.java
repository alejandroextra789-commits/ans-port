package com.ans.client.render;

import com.ans.client.bedrock.BoneLookup;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.bedrock.MobPose;
import com.ans.client.bedrock.RenderKind;
import com.ans.client.bedrock.RenderLayer;
import com.ans.client.particle.BedrockParticles;
import com.ans.client.util.ArgbCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * Immediate-mode layer submission for GeckoLib 4 / Forge 1.20.1.
 *
 * The 26.2 version queued work on a SubmitNodeCollector; here every layer is drawn in call
 * order against the caller's PoseStack and MultiBufferSource. The pass logic (color masks,
 * emissive split, UV scroll, stencil clip, glint) is unchanged.
 *
 * Only the model draw itself is supplied by the caller through {@link Draw}, because that
 * is where the renderer's GeoRenderer.actuallyRender(...) call lives.
 */
public final class LayerSubmitter {
   static final int FULL_BRIGHT = 15728880;
   private static float fade = 1.0F;

   private LayerSubmitter() {
   }

   /** Draws the model into the given consumer. The caller supplies the GeoRenderer call. */
   @FunctionalInterface
   public interface Draw {
      void draw(BakedGeoModel model, RenderType type, VertexConsumer vc, int light, int overlay, int color);
   }

   public static float fade(float alpha) {
      float previous = fade;
      fade = alpha;
      return previous;
   }

   /** Writes the frame's bone pose into the model's bones. Restore with {@link #restore}. */
   public static void applyPose(FrameResult frame, BakedGeoModel model) {
      if (frame == null || model == null) {
         return;
      }
      Map<String, GeoBone> bones = BoneLookup.of(model);
      for (Entry<String, float[]> e : frame.pose().bones().entrySet()) {
         GeoBone bone = bones.get(e.getKey());
         if (bone != null) {
            set(bone, e.getValue());
         }
      }
   }

   public static void submit(
      PoseStack poseStack,
      MultiBufferSource buffers,
      GeoModel<?> geoModel,
      FrameResult frame,
      int baseColor,
      int light,
      int overlay,
      boolean ghost,
      @Nullable Matrix4fc modelMatrix,
      @Nullable Vec3 camera,
      Draw draw
   ) {
      if (frame == null) {
         return;
      }
      if (modelMatrix != null && camera != null) {
         BedrockParticles.capture(frame.pose(), modelMatrix, camera);
      }

      int color = baseColor;
      boolean isGhost = ghost;
      if (fade < 1.0F) {
         isGhost = true;
         color = ArgbCompat.multiplyAlpha(color, fade);
      }

      List<RenderLayer> layers = frame.layers();
      for (RenderLayer layer : layers) {
         BakedGeoModel model = geoModel.getBakedModel(layer.model());
         if (BoneLookup.isMissing(model)) {
            continue;
         }
         drawLayer(poseStack, buffers, model, frame, layer, color, light, overlay, isGhost, draw);
      }
   }

   private static void drawLayer(
      PoseStack poseStack,
      MultiBufferSource buffers,
      BakedGeoModel model,
      FrameResult frame,
      RenderLayer layer,
      int baseColor,
      int light,
      int overlay,
      boolean ghost,
      Draw draw
   ) {
      Set<LayerSubmitter.Pass> passes = new LinkedHashSet<>();
      for (String bone : BoneLookup.of(model).keySet()) {
         passes.add(new LayerSubmitter.Pass(layer.kindOf(bone), layer.colorMasked(bone), layer.uvAnimated(bone)));
      }
      if (passes.isEmpty()) {
         passes.add(new LayerSubmitter.Pass(RenderKind.CUTOUT, false, layer.hasUvAnim()));
      }

      boolean split = passes.size() > 1;
      int layerColor = ArgbCompat.multiply(baseColor, layer.tint());
      int packedOverlay = layer.hurtOverlay() ? overlay : OverlayTexture.NO_OVERLAY;
      int packedLight = layer.ignoreLighting() ? FULL_BRIGHT : light;

      for (LayerSubmitter.Pass p : passes) {
         LayerSubmitter.Pass filter = split ? p : null;
         RenderKind kind = p.kind();
         if (kind == RenderKind.INVISIBLE) {
            continue;
         }

         boolean mixPass = layer.overlay() != 0 && !ghost && mixable(kind);
         int color = layer.overlay() != 0 && !mixPass ? multiplyOverlay(layerColor, layer.overlay()) : layerColor;

         if (p.colorMasked()) {
            BedrockTextures.Split mask = BedrockTextures.colorMask(layer.texture());
            submitPass(poseStack, buffers, frame, layer, model, filter, kind, mask.plain(), ghost,
               packedLight, packedOverlay, color, p.uvAnimated(), draw);
            if (layer.changeColor() >>> 24 != 0) {
               submitPass(poseStack, buffers, frame, layer, model, filter, kind, mask.tinted(), ghost,
                  packedLight, packedOverlay, ArgbCompat.multiply(color, layer.changeColor()), p.uvAnimated(), draw);
            }
         } else if (isEmissive(kind) && !ghost) {
            boolean cull = kind == RenderKind.EMISSIVE_ALPHA_CULL || kind == RenderKind.EMISSIVE_OPAQUE_CULL;
            BedrockTextures.Split glow = kind != RenderKind.EMISSIVE_OPAQUE && kind != RenderKind.EMISSIVE_OPAQUE_CULL
               ? BedrockTextures.emissiveSplit(layer.texture())
               : BedrockTextures.emissiveOpaqueSplit(layer.texture());
            submitPass(poseStack, buffers, frame, layer, model, filter,
               cull ? RenderKind.CUTOUT_CULL : RenderKind.CUTOUT, glow.plain(), false,
               packedLight, packedOverlay, color, p.uvAnimated(), draw);
            submitPass(poseStack, buffers, frame, layer, model, filter,
               cull ? RenderKind.EMISSIVE_ALPHA_CULL : RenderKind.TRANSLUCENT_EMISSIVE, glow.tinted(), false,
               packedLight, packedOverlay, color, p.uvAnimated(), draw);
         } else {
            submitPass(poseStack, buffers, frame, layer, model, filter, kind,
               BedrockTextures.resolve(layer.texture()), ghost, packedLight, packedOverlay, color, p.uvAnimated(), draw);
         }

         if (mixPass) {
            submitPass(poseStack, buffers, frame, layer, model, filter, RenderKind.TRANSLUCENT,
               BedrockTextures.whiteMask(layer.texture()), false, packedLight, OverlayTexture.NO_OVERLAY,
               ArgbCompat.multiply(baseColor, layer.overlay()), p.uvAnimated(), draw,
               kind == RenderKind.SOLID || kind == RenderKind.CUTOUT_CULL || kind == RenderKind.TRANSLUCENT_CULL);
         }
      }

      if (layer.hasGlint() && !ghost) {
         submitGlint(poseStack, buffers, model, frame, layer, light, draw);
      }
   }

   private static void submitGlint(
      PoseStack poseStack, MultiBufferSource buffers, BakedGeoModel model, FrameResult frame,
      RenderLayer layer, int light, Draw draw
   ) {
      LayerSubmitter.Pass glintBones = new LayerSubmitter.Pass(RenderKind.CUTOUT, false, false, true);
      RenderType type = RenderType.entityGlint();
      VertexConsumer out = new NormalizingVertexConsumer(buffers.getBuffer(type));
      renderPosed(model, frame.pose(), layer, glintBones, () ->
         draw.draw(model, type, out, light, OverlayTexture.NO_OVERLAY, -1));
   }

   private static void submitPass(
      PoseStack poseStack, MultiBufferSource buffers, FrameResult frame, RenderLayer layer,
      BakedGeoModel model, LayerSubmitter.Pass pass, RenderKind kind, ResourceLocation texture,
      boolean ghost, int packedLight, int packedOverlay, int color, boolean uvAnimated, Draw draw
   ) {
      submitPass(poseStack, buffers, frame, layer, model, pass, kind, texture, ghost,
         packedLight, packedOverlay, color, uvAnimated, draw, false);
   }

   private static void submitPass(
      PoseStack poseStack, MultiBufferSource buffers, FrameResult frame, RenderLayer layer,
      BakedGeoModel model, LayerSubmitter.Pass pass, RenderKind kind, ResourceLocation texture,
      boolean ghost, int packedLight, int packedOverlay, int color, boolean uvAnimated, Draw draw,
      boolean cpuCull
   ) {
      RenderType type = ghost
         ? RenderType.entityTranslucentCull(texture)
         : kind.renderType(texture, layer.uvOffsetU(), layer.uvOffsetV());
      boolean matrixOffset = !ghost && kind.textureMatrixOffset();
      boolean uvAnim = layer.hasUvAnim() && (uvAnimated || matrixOffset);
      float su = layer.uvScaleU();
      float sv = layer.uvScaleV();
      float ou = matrixOffset ? 0.0F : layer.uvOffsetU();
      float ov = matrixOffset ? 0.0F : layer.uvOffsetV();
      int passColor = !ghost && kind.fadesByAlpha() ? RenderKind.fadeToAlpha(color) : color;
      boolean inverted = !ghost && kind == RenderKind.STENCIL_OUTLINE;
      boolean invertedCulled = !ghost && kind == RenderKind.STENCIL_OUTLINE_ADDITIVE;

      LayerSubmitter.Pass maskPass = null;
      if (!ghost && kind == RenderKind.CUTOUT_STENCIL_CLIP) {
         for (String bone : BoneLookup.of(model).keySet()) {
            if (layer.kindOf(bone) == RenderKind.INVISIBLE) {
               maskPass = new LayerSubmitter.Pass(RenderKind.INVISIBLE, layer.colorMasked(bone), layer.uvAnimated(bone));
               break;
            }
         }
      }

      VertexConsumer target = buffers.getBuffer(type);
      VertexConsumer base = uvAnim ? new UvAnimConsumer(target, su, sv, ou, ov) : target;

      poseStack.pushPose();
      try {
         if (maskPass != null) {
            StencilClipConsumer.Capture mask = new StencilClipConsumer.Capture();
            renderPosed(model, frame.pose(), layer, maskPass, () ->
               draw.draw(model, type, mask, packedLight, packedOverlay, passColor));
            base = new StencilClipConsumer(base, mask);
         }

         VertexConsumer out = new NormalizingVertexConsumer(
            inverted
               ? new ReversedQuadConsumer(base)
               : (invertedCulled
                  ? new ReversedQuadConsumer(new CullingQuadConsumer(base))
                  : (ghost || !cpuCull && kind != RenderKind.EMISSIVE_ALPHA_CULL ? base : new CullingQuadConsumer(base)))
         );

         renderPosed(model, frame.pose(), layer, pass, () ->
            draw.draw(model, type, out, packedLight, packedOverlay, passColor));
      } finally {
         poseStack.popPose();
      }
   }

   /**
    * Applies the pose for one pass, runs the draw, then restores every touched bone.
    * Bones hidden for this pass are flagged with setHidden(true).
    */
   static void renderPosed(BakedGeoModel model, MobPose pose, RenderLayer layer, LayerSubmitter.Pass pass, Runnable render) {
      List<Object[]> saved = new ArrayList<>();
      try {
         for (GeoBone bone : BoneLookup.of(model).values()) {
            String name = bone.getName();
            float[] v = pose.get(name);
            boolean hidden = !layer.visible(name);
            boolean otherPass = pass != null && !pass.contains(layer, name);
            if (v != null || hidden || otherPass) {
               saved.add(new Object[]{bone, snapshot(bone)});
               if (v != null) {
                  set(bone, v);
               }
               if (hidden || otherPass) {
                  bone.setHidden(true);
               }
            }
         }
         render.run();
      } finally {
         for (Object[] s : saved) {
            restore((GeoBone)s[0], (float[])s[1]);
         }
      }
   }

   static void set(GeoBone bone, float[] v) {
      bone.setRotX(finite(v[0]));
      bone.setRotY(finite(v[1]));
      bone.setRotZ(finite(v[2]));
      bone.setPosX(finite(v[3]));
      bone.setPosY(finite(v[4]));
      bone.setPosZ(finite(v[5]));
      bone.setScaleX(finite1(v[6]));
      bone.setScaleY(finite1(v[7]));
      bone.setScaleZ(finite1(v[8]));
   }

   private static float[] snapshot(GeoBone b) {
      return new float[]{
         b.getRotX(), b.getRotY(), b.getRotZ(),
         b.getPosX(), b.getPosY(), b.getPosZ(),
         b.getScaleX(), b.getScaleY(), b.getScaleZ(),
         b.isHidden() ? 1.0F : 0.0F
      };
   }

   private static void restore(GeoBone b, float[] s) {
      b.setRotX(s[0]);
      b.setRotY(s[1]);
      b.setRotZ(s[2]);
      b.setPosX(s[3]);
      b.setPosY(s[4]);
      b.setPosZ(s[5]);
      b.setScaleX(s[6]);
      b.setScaleY(s[7]);
      b.setScaleZ(s[8]);
      b.setHidden(s[9] != 0.0F);
   }

   private static boolean isEmissive(RenderKind kind) {
      return kind == RenderKind.EMISSIVE_ALPHA
         || kind == RenderKind.EMISSIVE_ALPHA_CULL
         || kind == RenderKind.EMISSIVE_OPAQUE
         || kind == RenderKind.EMISSIVE_OPAQUE_CULL;
   }

   private static boolean mixable(RenderKind kind) {
      return kind == RenderKind.SOLID
         || kind == RenderKind.CUTOUT
         || kind == RenderKind.CUTOUT_CULL
         || kind == RenderKind.TRANSLUCENT
         || kind == RenderKind.TRANSLUCENT_CULL;
   }

   private static int multiplyOverlay(int color, int overlay) {
      float a = ArgbCompat.alpha(overlay) / 255.0F;
      float r = 1.0F + (ArgbCompat.red(overlay) / 255.0F - 1.0F) * a;
      float g = 1.0F + (ArgbCompat.green(overlay) / 255.0F - 1.0F) * a;
      float b = 1.0F + (ArgbCompat.blue(overlay) / 255.0F - 1.0F) * a;
      return ArgbCompat.multiply(color, ArgbCompat.colorFromFloat(1.0F, r, g, b));
   }

   private static float finite(float f) {
      return Float.isFinite(f) ? f : 0.0F;
   }

   private static float finite1(float f) {
      return Float.isFinite(f) ? f : 1.0F;
   }

   record Pass(RenderKind kind, boolean colorMasked, boolean uvAnimated, boolean glintOnly) {
      Pass(RenderKind kind, boolean colorMasked, boolean uvAnimated) {
         this(kind, colorMasked, uvAnimated, false);
      }

      boolean contains(RenderLayer layer, String bone) {
         return this.glintOnly
            ? layer.glint(bone)
            : layer.kindOf(bone) == this.kind && layer.colorMasked(bone) == this.colorMasked && layer.uvAnimated(bone) == this.uvAnimated;
      }
   }
}
