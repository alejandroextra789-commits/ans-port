package com.ans.client.bedrock;

import com.ans.Ans;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BedrockRules {
   private static final Set<String> HIERARCHICAL = ConcurrentHashMap.newKeySet();
   private static final Set<String> SUBTRACT = ConcurrentHashMap.newKeySet();
   private static final Set<String> NEGATIVE_SEEN = ConcurrentHashMap.newKeySet();
   private static volatile boolean legacy = "legacy".equalsIgnoreCase(System.getProperty("ans.rules", "bedrock"));
   public static volatile boolean logNegativeWeights;
   private static final boolean ANIM_THIS = !"false".equalsIgnoreCase(System.getProperty("ans.animThis", "true"));
   private static final boolean ANIM_THIS_REST = !"false".equalsIgnoreCase(System.getProperty("ans.animThisRest", "true"));
   private static final boolean DEFAULT_MATERIAL = !"false".equalsIgnoreCase(System.getProperty("ans.defaultMaterial", "true"));
   private static final boolean STEP_KEYFRAMES = !"false".equalsIgnoreCase(System.getProperty("ans.stepKeyframes", "true"));

   private BedrockRules() {
   }

   public static void keepHierarchicalRotation(String mobId) {
      HIERARCHICAL.add(mobId);
   }

   public static void keepNegativeWeights(String mobId) {
      SUBTRACT.add(mobId);
   }

   public static void setLegacy(boolean value) {
      legacy = value;
      Ans.LOGGER.info("[ans] Bedrock rules: {}", value ? "legacy (off)" : "bedrock (on)");
   }

   public static boolean relativeToEntity(String mobId) {
      return !legacy && !HIERARCHICAL.contains(mobId);
   }

   public static boolean clampNegativeWeights(String mobId) {
      return !legacy && !SUBTRACT.contains(mobId);
   }

   public static boolean absoluteHeadYaw() {
      return !legacy;
   }

   public static boolean animationThis() {
      return ANIM_THIS;
   }

   public static boolean animationThisRest() {
      return ANIM_THIS_REST && !legacy;
   }

   public static boolean defaultMaterialFallback() {
      return DEFAULT_MATERIAL;
   }

   public static boolean stepKeyframes() {
      return STEP_KEYFRAMES && !legacy;
   }

   static void negativeWeight(String mobId, String entry, double value) {
      if (logNegativeWeights && NEGATIVE_SEEN.size() < 2000 && NEGATIVE_SEEN.add(mobId + "|" + entry)) {
         Ans.LOGGER.info("[ans] negative weight: {} '{}' = {}", new Object[]{mobId, entry, String.format(Locale.ROOT, "%.3f", value)});
      }
   }
}
