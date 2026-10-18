package com.ans;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Ans {
   public static final String MOD_ID = "ans";
   public static final Logger LOGGER = LoggerFactory.getLogger("Actions & Stuff");

   private Ans() {
   }

   public static ResourceLocation id(String path) {
      return new ResourceLocation("ans", path);
   }
}
