package com.ans.client.particle;

import com.ans.Ans;
import com.ans.client.bedrock.AnimationLookup;
import com.ans.client.settings.Experience;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

public final class ParticleDefs {
   private static final Map<String, Optional<ParticleDef>> CACHE = new HashMap<>();
   private static int seenGeneration = -1;
   private static String seenExperience = "";

   private ParticleDefs() {
   }

   public static synchronized ParticleDef get(String identifier) {
      int generation = AnimationLookup.generation();
      String experience = Experience.current();
      if (generation != seenGeneration || !experience.equals(seenExperience)) {
         seenGeneration = generation;
         seenExperience = experience;
         CACHE.clear();
      }

      return CACHE.computeIfAbsent(identifier.toLowerCase(Locale.ROOT), ParticleDefs::load).orElse(null);
   }

   private static Optional<ParticleDef> load(String identifier) {
      int colon = identifier.indexOf(58);
      String ns = colon < 0 ? "minecraft" : identifier.substring(0, colon);
      String name = colon < 0 ? identifier : identifier.substring(colon + 1);
      ResourceLocation file = ResourceLocation.tryParse("ans:particle/" + ns + "/" + name + ".json");
      if (file == null) {
         return Optional.empty();
      }

      file = Experience.resolve(file);
      Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
      if (resource.isEmpty()) {
         Ans.LOGGER.warn("[ans] particle effect {} is not defined by the pack ({} missing); not emitted", identifier, file);
         return Optional.empty();
      }

      try (Reader reader = resource.get().openAsReader()) {
         JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
         ParticleDef def = new ParticleDef(identifier, root);
         if (!def.unsupported().isEmpty()) {
            Ans.LOGGER.warn("[ans] particle effect {}: unsupported components ignored: {}", identifier, def.unsupported());
         }

         return Optional.of(def);
      } catch (Exception e) {
         Ans.LOGGER.error("[ans] could not read particle effect {}", file, e);
         return Optional.empty();
      }
   }
}
