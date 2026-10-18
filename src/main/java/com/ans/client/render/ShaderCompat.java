package com.ans.client.render;

import java.lang.reflect.Method;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class ShaderCompat {
   private static final String MODE = System.getProperty("ans.vanillaOutline", "auto").trim().toLowerCase();
   private static boolean irisResolved;
   private static Object irisApi;
   private static Method irisInUse;

   private ShaderCompat() {
   }

   public static boolean shadersActive() {
      if (MODE.equals("on") || MODE.equals("true")) {
         return true;
      }

      if (!MODE.equals("off") && !MODE.equals("false")) {
         if (!irisResolved) {
            irisResolved = true;

            try {
               Class var0 = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
               irisApi = var0.getMethod("getInstance").invoke(null);
               irisInUse = var0.getMethod("isShaderPackInUse");
            } catch (Throwable var2) {
               irisApi = null;
               irisInUse = null;
            }
         }

         if (irisInUse == null) {
            return false;
         }

         try {
            return Boolean.TRUE.equals(irisInUse.invoke(irisApi));
         } catch (Throwable var1) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static RenderType additive(ResourceLocation var0, float var1, float var2, boolean var3) {
      return shadersActive() ? RenderType.eyes(var0) : BedrockRenderTypes.additive(var0, var1, var2, var3);
   }
}
