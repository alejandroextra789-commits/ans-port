package com.ans.client.attachable;

import com.ans.Ans;
import com.ans.client.bedrock.AnimationLookup;
import com.ans.client.settings.Experience;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import com.ans.client.render.TextureImageUtil;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;

public final class TrimTextures {
   public static final String PREFIX = "ans:derived/trim/";
   private static final ResourceLocation INDEX = Ans.id("attachable/_trims.json");
   private static final ResourceLocation KEY_PALETTE = new ResourceLocation("minecraft", "textures/trims/color_palettes/trim_palette.png");
   private static final Map<ResourceLocation, TrimTextures.Index> INDEXES = new HashMap<>();
   private static final Map<ResourceLocation, ResourceLocation> COMPOSED = new HashMap<>();
   private static final Map<ResourceLocation, Boolean> EXISTS = new HashMap<>();
   private static int seenGeneration = -1;

   private TrimTextures() {
   }

   public static synchronized ResourceLocation texture(ItemStack stack, EquipmentSlot slot) {
      return texture(stack, slot, null);
   }

   public static synchronized ResourceLocation texture(ItemStack stack, EquipmentSlot slot, ResourceLocation base) {
      ArmorTrim trim = Minecraft.getInstance().level == null ? null
         : ArmorTrim.getTrim(Minecraft.getInstance().level.registryAccess(), stack).orElse(null);
      if (trim == null) {
         return null;
      }

      int generation = AnimationLookup.generation();
      if (generation != seenGeneration) {
         seenGeneration = generation;
         INDEXES.clear();
         EXISTS.clear();
      }

      TrimTextures.Index index = INDEXES.computeIfAbsent(Experience.resolve(INDEX), TrimTextures::load);
      ResourceLocation pattern = pattern(index, ((TrimPattern)trim.pattern().value()).assetId(), slot == EquipmentSlot.LEGS);
      if (pattern == null) {
         return null;
      }

      // 1.20.1 trim materials expose one palette asset name; equipment-specific asset groups were added later.
      String suffix = ((TrimMaterial)trim.material().value()).assetName();
      ResourceLocation palette = palette(index, suffix);

      if (palette == null) {
         return null;
      }

      ResourceLocation id = new ResourceLocation(
         "ans:derived/trim/"
            + pattern.getNamespace()
            + "/"
            + strip(pattern.getPath())
            + "/"
            + palette.getNamespace()
            + "/"
            + strip(palette.getPath())
            + (base != null ? "/on/" + base.getNamespace() + "/" + strip(base.getPath()) : "")
            + ".png"
      );
      ResourceLocation p = pattern;
      ResourceLocation pal = palette;
      return COMPOSED.computeIfAbsent(id, i -> {
         Minecraft.getInstance().getTextureManager().register(i, new TrimTextures.Composed(p, pal, base));
         return (ResourceLocation)i;
      });
   }

   private static String strip(String path) {
      return path.replaceAll("\\.png$", "");
   }

   private static ResourceLocation pattern(TrimTextures.Index index, ResourceLocation asset, boolean leggings) {
      String name = asset.getPath() + (leggings ? "_leggings" : "");
      if ("minecraft".equals(asset.getNamespace()) && index.patterns().containsKey(name)) {
         return index.patterns().get(name);
      }

      if (!index.javaPatterns()) {
         return null;
      }

      ResourceLocation java = asset.withPath("textures/trims/entity/" + (leggings ? "humanoid_leggings/" : "humanoid/") + asset.getPath() + ".png");
      return exists(java) ? java : null;
   }

   private static ResourceLocation palette(TrimTextures.Index index, String suffix) {
      if (index.palettes().containsKey(suffix)) {
         return index.palettes().get(suffix);
      }

      ResourceLocation java = ResourceLocation.tryParse("minecraft:textures/trims/color_palettes/" + suffix + ".png");
      return java != null && exists(java) ? java : null;
   }

   private static boolean exists(ResourceLocation id) {
      return EXISTS.computeIfAbsent(id, i -> Minecraft.getInstance().getResourceManager().getResource(i).isPresent());
   }

   private static TrimTextures.Index load(ResourceLocation file) {
      Map<String, ResourceLocation> patterns = new HashMap<>();
      Map<String, ResourceLocation> palettes = new HashMap<>();
      boolean javaPatterns = true;
      Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(file);
      if (res.isPresent()) {
         try (Reader reader = res.get().openAsReader()) {
            JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
            javaPatterns = !o.has("java_patterns") || o.get("java_patterns").getAsBoolean();
            read(o, "patterns", patterns);
            read(o, "palettes", palettes);
         } catch (Exception e) {
            Ans.LOGGER.error("[ans] Could not read {}", file, e);
         }
      }

      return new TrimTextures.Index(javaPatterns, Map.copyOf(patterns), Map.copyOf(palettes));
   }

   private static void read(JsonObject o, String key, Map<String, ResourceLocation> out) {
      if (o.has(key) && o.get(key).isJsonObject()) {
         for (Entry<String, JsonElement> e : o.getAsJsonObject(key).entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(e.getValue().getAsString());
            if (id != null) {
               out.put(e.getKey(), id);
            }
         }
      }
   }

   private static final class Composed extends AbstractTexture {
      private final ResourceLocation pattern;
      private final ResourceLocation palette;
      private final ResourceLocation base;

      Composed(ResourceLocation pattern, ResourceLocation palette, ResourceLocation base) {
         this.pattern = pattern;
         this.palette = palette;
         this.base = base;
      }

      @Override
      public void load(ResourceManager resources) throws IOException {
         NativeImage image = TextureImageUtil.load(resources, this.pattern);
         try {
         NativeImage under = null;
         if (this.base != null && resources.getResource(this.base).isPresent()) {
            NativeImage b = TextureImageUtil.load(resources, this.base);
            if (b.getWidth() == image.getWidth() && b.getHeight() == image.getHeight()) {
               under = b;
            } else {
               b.close();
            }
         }

         try {
            NativeImage key = TextureImageUtil.load(resources, TrimTextures.KEY_PALETTE);

            try {
               NativeImage pal = TextureImageUtil.load(resources, this.palette);

               try {
                  int n = Math.min(key.getWidth(), pal.getWidth());
                  int[] from = new int[n];
                  int[] to = new int[n];

                  for (int i = 0; i < n; i++) {
                     from[i] = key.getPixel(i, 0) & 16777215;
                     to[i] = pal.getPixel(i, 0) & 16777215;
                  }

                  for (int y = 0; y < image.getHeight(); y++) {
                     for (int x = 0; x < image.getWidth(); x++) {
                        int c = image.getPixel(x, y);
                        if (c >>> 24 == 0 && under != null) {
                           image.setPixel(x, y, under.getPixel(x, y));
                        } else if (c >>> 24 < 128) {
                           image.setPixel(x, y, 0);
                        } else {
                           int rgb = c & 16777215;

                           for (int i = 0; i < n; i++) {
                              if (from[i] == rgb) {
                                 rgb = to[i];
                                 break;
                              }
                           }

                           image.setPixel(x, y, 0xFF000000 | rgb);
                        }
                     }
                  }
               } catch (Throwable var22) {
                  if (pal != null) {
                     try {
                        pal.close();
                     } catch (Throwable var21) {
                        var22.addSuppressed(var21);
                     }
                  }

                  throw var22;
               }

               if (pal != null) {
                  pal.close();
               }
            } catch (Throwable var23) {
               if (key != null) {
                  try {
                     key.close();
                  } catch (Throwable var20) {
                     var23.addSuppressed(var20);
                  }
               }

               throw var23;
            }

            if (key != null) {
               key.close();
            }
         } finally {
            if (under != null) {
               under.close();
            }
         }

         TextureUtil.prepareImage(this.getId(), image.getWidth(), image.getHeight());
         image.upload(0, 0, 0, false);
         } finally {
            image.close();
         }
      }

      public String toString() {
         return "TrimTextures.Composed[" + this.pattern + " + " + this.palette + (this.base != null ? " over " + this.base : "") + "]";
      }
   }

   private record Index(boolean javaPatterns, Map<String, ResourceLocation> patterns, Map<String, ResourceLocation> palettes) {
   }
}
