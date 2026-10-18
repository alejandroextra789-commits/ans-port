package com.ans.client.settings;

import com.ans.client.bedrock.AnimationLookup;
import com.ans.client.molang.PackSettings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class Experience {
   public static final String SETTING = "experience";
   public static final String FULL = "full";
   public static final String VANILLA = "vanilla";
   public static final List<String> OPTIONS = List.of("full", "vanilla");
   private static final String DEFAULT_LAYERS = "vanilla";
   private static final String PREFIX = "experience/";
   private static final Map<ResourceLocation, ResourceLocation> RESOLVED = new HashMap<>();
   private static int seenGeneration = -1;
   private static String seenExperience = "";

   private Experience() {
   }

   public static String current() {
      String v = PackSettings.value("experience");
      return OPTIONS.contains(v) ? v : "full";
   }

   public static synchronized ResourceLocation resolve(ResourceLocation file) {
      String exp = current();
      int generation = AnimationLookup.generation();
      if (generation != seenGeneration || !exp.equals(seenExperience)) {
         seenGeneration = generation;
         seenExperience = exp;
         RESOLVED.clear();
      }

      return !"vanilla".equals(exp) && "ans".equals(file.getNamespace()) ? RESOLVED.computeIfAbsent(file, f -> {
         ResourceLocation alt = f.withPath("experience/" + exp + "/" + f.getPath());
         return Minecraft.getInstance().getResourceManager().getResource(alt).isPresent() ? alt : f;
      }) : file;
   }
}
