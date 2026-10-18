package com.ans.client.bedrock;

import com.eliotlash.mclib.math.IValue;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.EasingType;
import software.bernie.geckolib.core.keyframe.BoneAnimation;
import software.bernie.geckolib.core.keyframe.Keyframe;
import software.bernie.geckolib.core.keyframe.KeyframeStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Samples a GeckoLib 4 {@link Animation} at a given tick, per bone.
 *
 * Uses only data GeckoLib 4 exposes publicly: {@link Animation}, {@link BoneAnimation},
 * {@link KeyframeStack}, {@link Keyframe}, {@link EasingType}.
 *
 * Assumptions (verify in game):
 *  - Keyframe.length() is the keyframe's end time, in ticks, measured from the previous keyframe's end.
 *  - Each keyframe interpolates from startValue() to endValue() across its own span.
 *  - Animation.length() is in ticks.
 *  - IValue.get() evaluates the stored value (constant or Molang).
 */
public final class BedrockSampler {

   private BedrockSampler() {
   }

   /** Per-bone sampled transform. Rotation in degrees, position in model units, scale as multiplier. */
   public record BoneTransform(float rotX, float rotY, float rotZ, float posX, float posY, float posZ,
                               float scaleX, float scaleY, float scaleZ) {
      public static final BoneTransform IDENTITY = new BoneTransform(0, 0, 0, 0, 0, 0, 1, 1, 1);
   }

   /** Samples every bone of the animation at the given tick. Bones not in the animation are absent. */
   public static Map<String, BoneTransform> sample(Animation animation, double tick, boolean step) {
      Map<String, BoneTransform> out = new HashMap<>();
      if (animation == null) {
         return out;
      }
      double t = wrapTime(animation, tick);
      BoneAnimation[] bones = animation.boneAnimations();
      if (bones == null) {
         return out;
      }
      for (BoneAnimation ba : bones) {
         out.put(ba.boneName(), sampleBone(ba, t, step));
      }
      return out;
   }

   /** Maps a tick onto the animation timeline according to its loop type. */
   public static double wrapTime(Animation animation, double tick) {
      double length = animation.length();
      if (length <= 0.0) {
         return 0.0;
      }
      Animation.LoopType loop = animation.loopType();
      if (loop == Animation.LoopType.LOOP) {
         double t = tick % length;
         return t < 0 ? t + length : t;
      }
      // PLAY_ONCE, HOLD_ON_LAST_FRAME, DEFAULT: clamp at the end.
      return Math.max(0.0, Math.min(tick, length));
   }

   static BoneTransform sampleBone(BoneAnimation ba, double t, boolean step) {
      KeyframeStack<Keyframe<IValue>> rot = ba.rotationKeyFrames();
      KeyframeStack<Keyframe<IValue>> pos = ba.positionKeyFrames();
      KeyframeStack<Keyframe<IValue>> scl = ba.scaleKeyFrames();
      return new BoneTransform(
         (float) sampleAxis(rot == null ? null : rot.xKeyframes(), t, 0.0, step),
         (float) sampleAxis(rot == null ? null : rot.yKeyframes(), t, 0.0, step),
         (float) sampleAxis(rot == null ? null : rot.zKeyframes(), t, 0.0, step),
         (float) sampleAxis(pos == null ? null : pos.xKeyframes(), t, 0.0, step),
         (float) sampleAxis(pos == null ? null : pos.yKeyframes(), t, 0.0, step),
         (float) sampleAxis(pos == null ? null : pos.zKeyframes(), t, 0.0, step),
         (float) sampleAxis(scl == null ? null : scl.xKeyframes(), t, 1.0, step),
         (float) sampleAxis(scl == null ? null : scl.yKeyframes(), t, 1.0, step),
         (float) sampleAxis(scl == null ? null : scl.zKeyframes(), t, 1.0, step)
      );
   }

   /** Interpolates one channel (x, y or z) at time t. */
   static double sampleAxis(List<Keyframe<IValue>> frames, double t, double fallback, boolean step) {
      if (frames == null || frames.isEmpty()) {
         return fallback;
      }
      double prevEnd = 0.0;
      for (Keyframe<IValue> k : frames) {
         double end = k.length();
         if (t <= end) {
            double span = end - prevEnd;
            double progress = span <= 0.0 ? 1.0 : (t - prevEnd) / span;
            progress = clamp01(progress);
            progress = step ? (progress < 1.0 ? 0.0 : 1.0) : ease(k.easingType(), progress);
            double from = value(k.startValue());
            double to = value(k.endValue());
            return from + (to - from) * progress;
         }
         prevEnd = end;
      }
      return value(frames.get(frames.size() - 1).endValue());
   }

   /**
    * Applies an easing curve to a progress value in [0, 1].
    * Only LINEAR and STEP are implemented here. Other easings fall back to linear
    * until they are ported; see TODO below.
    */
   static double ease(EasingType easing, double p) {
      if (easing == EasingType.STEP) {
         return 0.0;
      }
      // TODO: port the remaining EasingType curves (sine, quad, cubic, ..., catmull-rom).
      return p;
   }

   static double value(IValue v) {
      return v == null ? 0.0 : v.get();
   }

   private static double clamp01(double v) {
      return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
   }
}
