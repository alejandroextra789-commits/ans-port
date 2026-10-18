package com.ans.client.bedrock;

import com.ans.Ans;
import com.ans.client.molang.PackSettings;
import com.ans.client.settings.Experience;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

public final class EntityBundles {
   private static final Map<ResourceLocation, Optional<EntityBundle>> CACHE = new HashMap<>();
   private static int seenGeneration = -1;

   private EntityBundles() {
   }

   public static synchronized EntityBundle get(String mobId, ResourceLocation file) {
      int generation = AnimationLookup.generation();
      if (generation != seenGeneration) {
         seenGeneration = generation;
         CACHE.clear();
         PackSettings.invalidate();
      }

      ResourceLocation resolved = Experience.resolve(file);
      return CACHE.computeIfAbsent(resolved, f -> Optional.ofNullable(load(mobId, f))).orElse(null);
   }

   private static EntityBundle load(String mobId, ResourceLocation file) {
      Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
      if (resource.isEmpty()) {
         Ans.LOGGER.error("[ans] Entity bundle {} not found: run the converter (actions-and-stuff/work/converter) for '{}'", file, mobId);
         return null;
      }

      try (Reader reader = resource.get().openAsReader()) {
         EntityBundle bundle = EntityBundle.parse(mobId, JsonParser.parseReader(reader).getAsJsonObject(), file.toString());
         Ans.LOGGER
            .info(
               "[ans] Loaded {}: {} animations, {} controllers, {} render controllers",
               new Object[]{file, bundle.animations.size(), bundle.controllers.size(), bundle.renderControllers.size()}
            );
         return bundle;
      } catch (Exception e) {
         Ans.LOGGER.error("[ans] Could not read entity bundle {}", file, e);
         return null;
      }
   }
}
