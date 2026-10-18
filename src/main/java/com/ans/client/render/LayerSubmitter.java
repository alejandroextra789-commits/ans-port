package com.ans.client.render;

import com.ans.client.bedrock.FrameResult;
import com.ans.client.bedrock.MobPose;
import com.ans.client.bedrock.RenderKind;
import com.ans.client.bedrock.RenderLayer;
import com.ans.client.particle.BedrockParticles;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import com.ans.client.util.ArgbCompat;

public final class LayerSubmitter {
   static final int FULL_BRIGHT = 15728880;
   private static float fade = 1.0F;
   private static final int MIX_ORDER = 1000;
   private static final int AFTER_MIX_ORDER = 2000;

   private LayerSubmitter() {
   }

   public static float fade(float alpha) {
      float previous = fade;
      fade = alpha;
      return previous;
   }

   public static void applyPose(FrameResult frame, BoneSnapshots snapshots) {
      if (frame != null) {
         for (Entry<String, float[]> e : frame.pose().bones().entrySet()) {
            snapshots.get(e.getKey()).ifPresent(s -> set(s, e.getValue()));
         }
      }
   }

   public static <R extends GeoRenderState> void submit(
      GeoModel<?> geoModel, RenderPassInfo<R> renderPassInfo, OrderedSubmitNodeCollector renderTasks, FrameResult frame, boolean ghost
   ) {
      submit(geoModel, renderPassInfo, renderTasks, frame, ghost, null);
   }

   public static <R extends GeoRenderState> void submit(
      GeoModel<?> geoModel,
      RenderPassInfo<R> renderPassInfo,
      OrderedSubmitNodeCollector renderTasks,
      FrameResult frame,
      boolean ghost,
      LayerSubmitter.ModelDrawer drawer
   ) {
      if (drawer == null) {
         BedrockParticles.capture(frame.pose(), renderPassInfo.getModelRenderMatrixPose().pose(), renderPassInfo.cameraState().pos);
      }

      int baseColor = renderPassInfo.renderColor();
      if (fade < 1.0F) {
         ghost = true;
         baseColor = ArgbCompat.multiplyAlpha(baseColor, fade);
      }

      int light = renderPassInfo.packedLight();
      int overlay = renderPassInfo.packedOverlay();
      List<RenderLayer> layers = frame.layers();

      for (int index = 0; index < layers.size(); index++) {
         RenderLayer layer = layers.get(index);
         BakedGeoModel model = geoModel.getBakedModel(layer.model());
         if (!model.isMissingno()) {
            OrderedSubmitNodeCollector tasks = index > 0 && renderTasks instanceof SubmitNodeCollector c ? c.order(index) : renderTasks;
            Set<LayerSubmitter.Pass> passes = new LinkedHashSet<>();

            for (String bone : ((Map)model.boneLookup().get()).keySet()) {
               passes.add(new LayerSubmitter.Pass(layer.kindOf(bone), layer.colorMasked(bone), layer.uvAnimated(bone)));
            }

            if (passes.isEmpty()) {
               passes.add(new LayerSubmitter.Pass(RenderKind.CUTOUT, false, layer.hasUvAnim()));
            }

            boolean split = passes.size() > 1;
            int layerColor = ArgbCompat.multiply(baseColor, layer.tint());
            int packedOverlay = layer.hurtOverlay() ? overlay : OverlayTexture.NO_OVERLAY;
            int packedLight = layer.ignoreLighting() ? 15728880 : light;
            boolean anyMix = false;
            if (layer.overlay() != 0 && !ghost) {
               for (LayerSubmitter.Pass p : passes) {
                  anyMix |= mixable(p.kind());
               }
            }

            OrderedSubmitNodeCollector layerTasks = tasks;
            OrderedSubmitNodeCollector afterMixTasks = anyMix && renderTasks instanceof SubmitNodeCollector c ? c.order(2000 + index) : tasks;

            for (LayerSubmitter.Pass p : passes) {
               LayerSubmitter.Pass filter = split ? p : null;
               RenderKind kind = p.kind();
               if (kind != RenderKind.INVISIBLE) {
                  tasks = anyMix && !mixable(kind) ? afterMixTasks : layerTasks;
                  boolean mixPass = layer.overlay() != 0 && !ghost && mixable(kind);
                  int color = layer.overlay() != 0 && !mixPass ? multiplyOverlay(layerColor, layer.overlay()) : layerColor;
                  if (p.colorMasked()) {
                     BedrockTextures.Split mask = BedrockTextures.colorMask(layer.texture());
                     submitPass(
                        renderPassInfo,
                        tasks,
                        frame,
                        layer,
                        model,
                        filter,
                        kind,
                        mask.plain(),
                        ghost,
                        packedLight,
                        packedOverlay,
                        color,
                        p.uvAnimated(),
                        drawer
                     );
                     if (layer.changeColor() >>> 24 != 0) {
                        submitPass(
                           renderPassInfo,
                           tasks,
                           frame,
                           layer,
                           model,
                           filter,
                           kind,
                           mask.tinted(),
                           ghost,
                           packedLight,
                           packedOverlay,
                           ArgbCompat.multiply(color, layer.changeColor()),
                           p.uvAnimated(),
                           drawer
                        );
                     }
                  } else if ((
                        kind == RenderKind.EMISSIVE_ALPHA
                           || kind == RenderKind.EMISSIVE_ALPHA_CULL
                           || kind == RenderKind.EMISSIVE_OPAQUE
                           || kind == RenderKind.EMISSIVE_OPAQUE_CULL
                     )
                     && !ghost) {
                     boolean cull = kind == RenderKind.EMISSIVE_ALPHA_CULL || kind == RenderKind.EMISSIVE_OPAQUE_CULL;
                     BedrockTextures.Split glow = kind != RenderKind.EMISSIVE_OPAQUE && kind != RenderKind.EMISSIVE_OPAQUE_CULL
                        ? BedrockTextures.emissiveSplit(layer.texture())
                        : BedrockTextures.emissiveOpaqueSplit(layer.texture());
                     submitPass(
                        renderPassInfo,
                        tasks,
                        frame,
                        layer,
                        model,
                        filter,
                        cull ? RenderKind.CUTOUT_CULL : RenderKind.CUTOUT,
                        glow.plain(),
                        false,
                        packedLight,
                        packedOverlay,
                        color,
                        p.uvAnimated(),
                        drawer
                     );
                     submitPass(
                        renderPassInfo,
                        tasks,
                        frame,
                        layer,
                        model,
                        filter,
                        cull ? RenderKind.EMISSIVE_ALPHA_CULL : RenderKind.TRANSLUCENT_EMISSIVE,
                        glow.tinted(),
                        false,
                        packedLight,
                        packedOverlay,
                        color,
                        p.uvAnimated(),
                        drawer
                     );
                  } else {
                     submitPass(
                        renderPassInfo,
                        tasks,
                        frame,
                        layer,
                        model,
                        filter,
                        kind,
                        BedrockTextures.resolve(layer.texture()),
                        ghost,
                        packedLight,
                        packedOverlay,
                        color,
                        p.uvAnimated(),
                        drawer
                     );
                  }

                  if (mixPass) {
                     OrderedSubmitNodeCollector mixTasks = renderTasks instanceof SubmitNodeCollector c ? c.order(1000 + index) : tasks;
                     submitPass(
                        renderPassInfo,
                        mixTasks,
                        frame,
                        layer,
                        model,
                        filter,
                        RenderKind.TRANSLUCENT,
                        BedrockTextures.whiteMask(layer.texture()),
                        false,
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        ArgbCompat.multiply(baseColor, layer.overlay()),
                        p.uvAnimated(),
                        drawer,
                        kind == RenderKind.SOLID || kind == RenderKind.CUTOUT_CULL || kind == RenderKind.TRANSLUCENT_CULL
                     );
                  }
               }
            }

            if (layer.hasGlint() && !ghost) {
               submitGlint(renderPassInfo, afterMixTasks, frame, layer, model, packedLight, drawer);
            }
         }
      }
   }

   private static <R extends GeoRenderState> void submitGlint(
      RenderPassInfo<R> renderPassInfo,
      OrderedSubmitNodeCollector renderTasks,
      FrameResult frame,
      RenderLayer layer,
      BakedGeoModel model,
      int packedLight,
      LayerSubmitter.ModelDrawer drawer
   ) {
      LayerSubmitter.Pass glintBones = new LayerSubmitter.Pass(RenderKind.CUTOUT, false, false, true);
      renderTasks.submitCustomGeometry(renderPassInfo.poseStack(), RenderTypes.entityGlint(), (pose, vertexConsumer) -> {
         PoseStack poseStack = renderPassInfo.poseStack();
         poseStack.pushPose();
         poseStack.last().set(pose);
         VertexConsumer out = new NormalizingVertexConsumer(vertexConsumer);
         renderPosed(model, frame.pose(), layer, glintBones, () -> {
            if (drawer != null) {
               drawer.draw(model, renderPassInfo, out, packedLight, OverlayTexture.NO_OVERLAY, -1);
            } else {
               model.render(renderPassInfo, out, packedLight, OverlayTexture.NO_OVERLAY, -1);
            }
         });
         poseStack.popPose();
      });
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

   private static <R extends GeoRenderState> void submitPass(
      RenderPassInfo<R> renderPassInfo,
      OrderedSubmitNodeCollector renderTasks,
      FrameResult frame,
      RenderLayer layer,
      BakedGeoModel model,
      LayerSubmitter.Pass pass,
      RenderKind kind,
      ResourceLocation texture,
      boolean ghost,
      int packedLight,
      int packedOverlay,
      int color,
      boolean uvAnimated,
      LayerSubmitter.ModelDrawer drawer
   ) {
      submitPass(renderPassInfo, renderTasks, frame, layer, model, pass, kind, texture, ghost, packedLight, packedOverlay, color, uvAnimated, drawer, false);
   }

   private static <R extends GeoRenderState> void submitPass(
      RenderPassInfo<R> renderPassInfo,
      OrderedSubmitNodeCollector renderTasks,
      FrameResult frame,
      RenderLayer layer,
      BakedGeoModel model,
      LayerSubmitter.Pass pass,
      RenderKind kind,
      ResourceLocation texture,
      boolean ghost,
      int packedLight,
      int packedOverlay,
      int color,
      boolean uvAnimated,
      LayerSubmitter.ModelDrawer drawer,
      boolean cpuCull
   ) {
      RenderType type = ghost ? RenderTypes.entityTranslucentCullItemTarget(texture) : kind.renderType(texture, layer.uvOffsetU(), layer.uvOffsetV());
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
         for (String bone : ((Map)model.boneLookup().get()).keySet()) {
            if (layer.kindOf(bone) == RenderKind.INVISIBLE) {
               maskPass = new LayerSubmitter.Pass(RenderKind.INVISIBLE, layer.colorMasked(bone), layer.uvAnimated(bone));
               break;
            }
         }
      }

      LayerSubmitter.Pass stencilMask = maskPass;
      renderTasks.submitCustomGeometry(
         renderPassInfo.poseStack(),
         type,
         (pose, vertexConsumer) -> {
            PoseStack poseStack = renderPassInfo.poseStack();
            poseStack.pushPose();
            poseStack.last().set(pose);
            VertexConsumer base = uvAnim ? new UvAnimConsumer(vertexConsumer, su, sv, ou, ov) : vertexConsumer;
            if (stencilMask != null) {
               StencilClipConsumer.Capture mask = new StencilClipConsumer.Capture();
               renderPosed(model, frame.pose(), layer, stencilMask, () -> {
                  if (drawer != null) {
                     drawer.draw(model, renderPassInfo, mask, packedLight, packedOverlay, passColor);
                  } else {
                     model.render(renderPassInfo, mask, packedLight, packedOverlay, passColor);
                  }
               });
               base = new StencilClipConsumer(base, mask);
            }

            VertexConsumer out = new NormalizingVertexConsumer(
               inverted
                  ? new ReversedQuadConsumer(base)
                  : (
                     invertedCulled
                        ? new ReversedQuadConsumer(new CullingQuadConsumer(base))
                        : (ghost || !cpuCull && kind != RenderKind.EMISSIVE_ALPHA_CULL ? base : new CullingQuadConsumer(base))
                  )
            );
            renderPosed(model, frame.pose(), layer, pass, () -> {
               if (drawer != null) {
                  drawer.draw(model, renderPassInfo, out, packedLight, packedOverlay, passColor);
               } else {
                  model.render(renderPassInfo, out, packedLight, packedOverlay, passColor);
               }
            });
            poseStack.popPose();
         }
      );
   }

   static void renderPosed(BakedGeoModel model, MobPose pose, RenderLayer layer, LayerSubmitter.Pass pass, Runnable render) {
      List<BoneSnapshot> applied = new ArrayList<>();

      try {
         for (GeoBone bone : ((Map)model.boneLookup().get()).values()) {
            String name = bone.name();
            float[] v = pose.get(name);
            boolean hidden = !layer.visible(name);
            boolean otherPass = pass != null && !pass.contains(layer, name);
            if (v != null || hidden || otherPass) {
               BoneSnapshot s = BoneSnapshot.create(bone);
               if (v != null) {
                  set(s, v);
               }

               if (hidden || otherPass) {
                  s.skipRender(true);
               }

               s.apply();
               applied.add(s);
            }
         }

         render.run();
      } finally {
         for (BoneSnapshot s : applied) {
            s.cleanup();
         }
      }
   }

   static void set(BoneSnapshot s, float[] v) {
      s.setRotation(finite(v[0]), finite(v[1]), finite(v[2]));
      s.setTranslation(finite(v[3]), finite(v[4]), finite(v[5]));
      s.setScale(finite1(v[6]), finite1(v[7]), finite1(v[8]));
   }

   private static float finite(float f) {
      return Float.isFinite(f) ? f : 0.0F;
   }

   private static float finite1(float f) {
      return Float.isFinite(f) ? f : 1.0F;
   }

   @FunctionalInterface
   public interface ModelDrawer {
      void draw(BakedGeoModel var1, RenderPassInfo<?> var2, VertexConsumer var3, int var4, int var5, int var6);
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
