package com.ans.client.bedrock;

import com.ans.Ans;
import com.geckolib.cache.BakedAnimationCache;
import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

public final class AnimationLookup {
   private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
   private static BakedAnimationCache seenCache;
   private static int generation;

   private AnimationLookup() {
   }

   public static synchronized int generation() {
      BakedAnimationCache cache = GeckoLibResources.getBakedAnimations();
      if (cache != seenCache) {
         seenCache = cache;
         WARNED.clear();
         generation++;
      }

      return generation;
   }

   public static Animation baked(ResourceLocation file, String name) {
      if (file == null) {
         return null;
      }

      BakedAnimations animations = (BakedAnimations)GeckoLibResources.getBakedAnimations().cache().get(file);
      Animation animation = animations == null ? null : animations.getAnimation(name);
      if (animation == null && WARNED.add(file + "|" + name)) {
         Ans.LOGGER.warn("[ans] Animation '{}' not found in GeckoLib animation file {}", name, file);
      }

      return animation;
   }
}
