package com.ans.client.render;

import com.ans.Ans;
import com.ans.client.attachable.AtlasSources;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntUnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public final class BedrockTextures {
   private static final Map<ResourceLocation, BedrockTextures.Recipe> COMPOSITES = new ConcurrentHashMap<>();
   private static final Map<ResourceLocation, ResourceLocation> RESOLVED = new ConcurrentHashMap<>();
   private static final Map<ResourceLocation, BedrockTextures.Split> SPLITS = new ConcurrentHashMap<>();
   private static final Map<ResourceLocation, BedrockTextures.Split> EMISSIVE_SPLITS = new ConcurrentHashMap<>();
   private static final Map<ResourceLocation, BedrockTextures.Split> EMISSIVE_OPAQUE_SPLITS = new ConcurrentHashMap<>();
   private static final Map<ResourceLocation, ResourceLocation> WHITE_MASKS = new ConcurrentHashMap<>();

   public static BedrockTextures.Split emissiveOpaqueSplit(ResourceLocation texture) {
      return EMISSIVE_OPAQUE_SPLITS.computeIfAbsent(
         texture,
         t -> new BedrockTextures.Split(
            register(derivedId(t, "olit"), t, BedrockTextures.Mode.OPAQUE_LIT), register(derivedId(t, "oglow"), t, BedrockTextures.Mode.OPAQUE_GLOW)
         )
      );
   }

   public static ResourceLocation whiteMask(ResourceLocation texture) {
      return WHITE_MASKS.computeIfAbsent(texture, t -> register(derivedId(t, "white"), t, BedrockTextures.Mode.WHITE));
   }

   private BedrockTextures() {
   }

   public static BedrockTextures.Part part(ResourceLocation texture, int dstX, int dstY, boolean tinted) {
      return new BedrockTextures.Part(texture, 0, 0, -1, -1, dstX, dstY, tinted);
   }

   public static BedrockTextures.Part crop(ResourceLocation texture, int srcX, int srcY, int width, int height, int dstX, int dstY, boolean tinted) {
      return new BedrockTextures.Part(texture, srcX, srcY, width, height, dstX, dstY, tinted);
   }

   public static void composite(ResourceLocation bedrockTexture, int width, int height, BedrockTextures.Part... parts) {
      COMPOSITES.put(bedrockTexture, new BedrockTextures.Recipe(width, height, List.of(parts)));
   }

   public static ResourceLocation resolve(ResourceLocation texture) {
      if (!COMPOSITES.containsKey(texture)) {
         return texture.getPath().startsWith("textures/attachable/") && AtlasSources.has(texture)
            ? RESOLVED.computeIfAbsent(texture, t -> register(derivedId(t, "atlas"), t, BedrockTextures.Mode.RAW))
            : texture;
      } else {
         return RESOLVED.computeIfAbsent(texture, t -> register(derivedId(t, "all"), t, BedrockTextures.Mode.ALL));
      }
   }

   public static BedrockTextures.Split colorMask(ResourceLocation texture) {
      return SPLITS.computeIfAbsent(
         texture,
         t -> new BedrockTextures.Split(
            register(derivedId(t, "plain"), t, BedrockTextures.Mode.PLAIN), register(derivedId(t, "tinted"), t, BedrockTextures.Mode.TINTED)
         )
      );
   }

   public static BedrockTextures.Split emissiveSplit(ResourceLocation texture) {
      return EMISSIVE_SPLITS.computeIfAbsent(
         texture,
         t -> new BedrockTextures.Split(
            register(derivedId(t, "lit"), t, BedrockTextures.Mode.LIT), register(derivedId(t, "glow"), t, BedrockTextures.Mode.GLOW)
         )
      );
   }

   private static ResourceLocation derivedId(ResourceLocation source, String suffix) {
      String path = source.getPath().replaceAll("\\.png$", "");
      return Ans.id("derived/" + source.getNamespace() + "/" + path + "_" + suffix + ".png");
   }

   private static ResourceLocation register(ResourceLocation id, ResourceLocation source, BedrockTextures.Mode mode) {
      Minecraft.getInstance().getTextureManager().register(id, new BedrockTextures.Derived(source, mode));
      return id;
   }

   private static NativeImage load(ResourceManager resources, ResourceLocation id) throws IOException {
      return resources.getResource(id).isEmpty()
            && Minecraft.getInstance().getTextureManager().getTexture(id) instanceof DynamicTexture d
            && d.getPixels() != null
         ? d.getPixels().mappedCopy(IntUnaryOperator.identity())
         : AtlasSources.patch(resources, id, TextureImageUtil.load(resources, id));
   }

   private static NativeImage bedrockImage(ResourceManager resources, ResourceLocation texture) throws IOException {
      BedrockTextures.Recipe recipe = COMPOSITES.get(texture);
      if (recipe == null) {
         return load(resources, texture);
      }

      NativeImage out = new NativeImage(recipe.width(), recipe.height(), true);
      out.fillRect(0, 0, recipe.width(), recipe.height(), 0);

      for (BedrockTextures.Part p : recipe.parts()) {
         NativeImage src = load(resources, p.texture());

         try {
            int w = p.width() < 0 ? src.getWidth() : p.width();
            int h = p.height() < 0 ? src.getHeight() : p.height();
            int alpha = p.tinted() ? 255 : 3;

            for (int y = 0; y < h; y++) {
               for (int x = 0; x < w; x++) {
                  int sx = p.srcX() + x;
                  int sy = p.srcY() + y;
                  int dx = p.dstX() + x;
                  int dy = p.dstY() + y;
                  if (sx < src.getWidth() && sy < src.getHeight() && dx < out.getWidth() && dy < out.getHeight()) {
                     int c = src.getPixel(sx, sy);
                     if (c >>> 24 >= 128) {
                        out.setPixel(dx, dy, alpha << 24 | c & 16777215);
                     }
                  }
               }
            }
         } catch (Throwable var18) {
            if (src != null) {
               try {
                  src.close();
               } catch (Throwable var17) {
                  var18.addSuppressed(var17);
               }
            }

            throw var18;
         }

         if (src != null) {
            src.close();
         }
      }

      return out;
   }

   private static final class Derived extends AbstractTexture {
      private final ResourceLocation source;
      private final BedrockTextures.Mode mode;

      Derived(ResourceLocation source, BedrockTextures.Mode mode) {
         this.source = source;
         this.mode = mode;
      }

      @Override
      public void load(ResourceManager resources) throws IOException {
         NativeImage image = BedrockTextures.bedrockImage(resources, this.source);
         try {
         if (this.mode != BedrockTextures.Mode.RAW) {
         for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
               int c = image.getPixel(x, y);
               int a = c >>> 24;
               if (this.mode == BedrockTextures.Mode.WHITE) {
                  image.setPixel(x, y, a > 0 ? a << 24 | 16777215 : 0);
               } else if (this.mode == BedrockTextures.Mode.GLOW) {
                  image.setPixel(x, y, a > 0 && a < 255 ? 255 - a << 24 | c & 16777215 : 0);
               } else if (this.mode == BedrockTextures.Mode.OPAQUE_GLOW) {
                  image.setPixel(x, y, a < 255 ? 255 - a << 24 | c & 16777215 : 0);
               } else {
                  boolean keep = switch (this.mode) {
                     case ALL, LIT -> a > 0;
                     case PLAIN -> {
                        if (a > 0 && a < 128) {
                           yield true;
                        }

                        yield false;
                     }
                     case TINTED -> a >= 128;
                     case GLOW, WHITE, OPAQUE_GLOW -> false;
                     case OPAQUE_LIT, RAW -> true;
                  };
                  image.setPixel(x, y, keep ? 0xFF000000 | c & 16777215 : 0);
               }
            }
         }
         }
         TextureUtil.prepareImage(this.getId(), image.getWidth(), image.getHeight());
         image.upload(0, 0, 0, false);
         } finally {
            image.close();
         }
      }

      public String toString() {
         return "BedrockTextures.Derived[" + this.source + " " + this.mode.name().toLowerCase(Locale.ROOT) + "]";
      }
   }

   private enum Mode {
      ALL,
      PLAIN,
      TINTED,
      LIT,
      GLOW,
      WHITE,
      OPAQUE_LIT,
      OPAQUE_GLOW,
      RAW;
   }

   public record Part(ResourceLocation texture, int srcX, int srcY, int width, int height, int dstX, int dstY, boolean tinted) {
   }

   private record Recipe(int width, int height, List<BedrockTextures.Part> parts) {
   }

   public record Split(ResourceLocation plain, ResourceLocation tinted) {
   }
}
