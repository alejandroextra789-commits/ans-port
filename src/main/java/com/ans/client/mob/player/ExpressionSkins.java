package com.ans.client.mob.player;

import com.ans.Ans;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;

public final class ExpressionSkins {
   public static final String SETTING = "oreville_ans:wzqdjp";
   public static boolean enabled = !"false".equalsIgnoreCase(System.getProperty("ans.expressionSkins", "true"));
   private static final int[][] RECTS = new int[][]{
      {0, 0, 8, 8},
      {24, 0, 16, 8},
      {56, 0, 8, 8},
      {0, 16, 4, 4},
      {12, 16, 8, 4},
      {36, 16, 8, 4},
      {52, 16, 4, 4},
      {0, 32, 4, 4},
      {12, 32, 8, 4},
      {36, 32, 4, 2},
      {40, 32, 2, 1}
   };
   private static final Map<String, int[]> PENDING = new ConcurrentHashMap<>();
   private static final Map<String, Boolean> COMPATIBLE = new ConcurrentHashMap<>();

   private ExpressionSkins() {
   }

   public static void captureOriginal(NativeImage image, String url) {
      if (enabled && url != null) {
         boolean legacy = image.getHeight() == 32;
         int[] px = new int[count()];
         int i = 0;

         for (int[] r : RECTS) {
            for (int y = r[1]; y < r[1] + r[3]; y++) {
               for (int x = r[0]; x < r[0] + r[2]; i++) {
                  px[i] = x < image.getWidth() && y < image.getHeight() ? image.getPixel(x, y) : 0;
                  x++;
               }
            }
         }

         PENDING.put(url, px);
         boolean compatible = !legacy && classify(px);
         COMPATIBLE.put(url, compatible);
         Ans.LOGGER.info("[ans] expressions: skin {} is {}expression-compatible", url, compatible ? "" : "not ");
      }
   }

   public static void restore(NativeImage processed, String url) {
      int[] px = url == null ? null : PENDING.remove(url);
      if (px != null && processed != null && processed.getWidth() == 64 && processed.getHeight() == 64) {
         int i = 0;

         for (int[] r : RECTS) {
            for (int y = r[1]; y < r[1] + r[3]; y++) {
               for (int x = r[0]; x < r[0] + r[2]; i++) {
                  processed.setPixel(x, y, px[i]);
                  x++;
               }
            }
         }
      }
   }

   private static int count() {
      int n = 0;

      for (int[] r : RECTS) {
         n += r[2] * r[3];
      }

      return n;
   }

   private static boolean classify(int[] px) {
      Set<Integer> colours = new HashSet<>();

      for (int argb : px) {
         if (argb >>> 24 >= 128) {
            colours.add(argb | 0xFF000000);
         }
      }

      return colours.size() >= 2;
   }

   public static boolean allows(Entity actor, String setting) {
      return enabled && "oreville_ans:wzqdjp".equals(setting) && actor instanceof AbstractClientPlayer p ? compatible(PlayerRendering.skinBody(p)) : true;
   }

   public static boolean compatible(ResourceLocation skin) {
      return skin != null && COMPATIBLE.computeIfAbsent(skin.toString(), ignored -> classifyResource(skin));
   }

   private static boolean classifyImage(NativeImage image) {
      if (image.getWidth() != 64 || image.getHeight() != 64) {
         return false;
      }
      int[] px = new int[count()];
      int i = 0;
      for (int[] rect : RECTS) {
         for (int y = rect[1]; y < rect[1] + rect[3]; y++) {
            for (int x = rect[0]; x < rect[0] + rect[2]; x++) {
               px[i++] = image.getPixel(x, y);
            }
         }
      }
      return classify(px);
   }

   private static boolean classifyResource(ResourceLocation path) {
      try {
         Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(path);
         if (res.isEmpty()) {
            if (Minecraft.getInstance().getTextureManager().getTexture(path) instanceof DynamicTexture dynamic
               && dynamic.getPixels() != null) {
               return classifyImage(dynamic.getPixels());
            }
            return false;
         }

         try (InputStream in = res.get().open()) {
            NativeImage image = NativeImage.read(in);

            label131: {
               boolean var18;
               try {
                  if (image.getWidth() != 64 || image.getHeight() != 64) {
                     var17 = false;
                     break label131;
                  }

                  int[] px = new int[count()];
                  int i = 0;

                  for (int[] r : RECTS) {
                     for (int y = r[1]; y < r[1] + r[3]; y++) {
                        for (int x = r[0]; x < r[0] + r[2]; i++) {
                           px[i] = image.getPixel(x, y);
                           x++;
                        }
                     }
                  }

                  var18 = classify(px);
               } catch (Throwable var14) {
                  if (image != null) {
                     try {
                        image.close();
                     } catch (Throwable var13) {
                        var14.addSuppressed(var13);
                     }
                  }

                  throw var14;
               }

               if (image != null) {
                  image.close();
               }

               return var18;
            }

            if (image != null) {
               image.close();
            }
         }

         return var17;
      } catch (Exception e) {
         Ans.LOGGER.warn("[ans] expressions: could not read skin {}: {}", path, e.toString());
         return false;
      }
   }
}
