package com.ans.client.gecko;

import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.loading.math.MathParser;
import com.geckolib.util.GeckoLibUtil;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class AnsGeckoLoader extends GeckoLibGsonLoader {
   private static final AnsMathParser PARSER = new AnsMathParser();
   private static boolean installed;

   public static synchronized void install() {
      if (!installed) {
         installed = true;
         GeckoLibUtil.addResourceLoader((id, var1) -> "ans".equals(id.getNamespace()), new AnsGeckoLoader());
      }
   }

   public BakedAnimations bakeGeckoLibAnimationsFile(ResourceLocation resourcePath, JsonObject raw, MathParser mathParser) throws RuntimeException {
      return deserializeJsonAnimations(resourcePath, raw).bake(resourcePath, PARSER);
   }
}
