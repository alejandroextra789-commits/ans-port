package com.ans.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/** Texture resource loading helper for Minecraft 1.20.1's NativeImage API. */
public final class TextureImageUtil {
   private TextureImageUtil() {}

   public static NativeImage load(ResourceManager resources, ResourceLocation id) throws IOException {
      Resource resource = resources.getResource(id)
         .orElseThrow(() -> new IOException("Missing texture resource: " + id));
      try (InputStream stream = resource.open()) {
         return NativeImage.read(stream);
      }
   }
}
