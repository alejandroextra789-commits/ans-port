package com.ans.client.molang;

import com.ans.Ans;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import com.ans.AnsPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

public final class PackSettings {
   public static final ResourceLocation DEFAULTS = Ans.id("entity/_settings.json");
   private static final Map<String, String> VALUES = new ConcurrentHashMap<>();
   private static volatile boolean loaded;

   private PackSettings() {
   }

   public static void invalidate() {
      loaded = false;
   }

   private static void ensureLoaded() {
      if (!loaded) {
         synchronized (PackSettings.class) {
            if (!loaded) {
               VALUES.clear();
               Map<String, String> readable = new ConcurrentHashMap<>();

               try {
                  Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(DEFAULTS);
                  if (res.isPresent()) {
                     try (Reader reader = res.get().openAsReader()) {
                        JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();

                        for (Entry<String, JsonElement> e : obj.entrySet()) {
                           if (e.getValue().isJsonObject()) {
                              JsonObject s = e.getValue().getAsJsonObject();
                              JsonElement def = s.get("default");
                              VALUES.put(e.getKey(), def != null && !def.isJsonNull() ? def.getAsString() : "");
                              if (s.has("readable")) {
                                 readable.put(s.get("readable").getAsString(), e.getKey());
                              }
                           }
                        }
                     }
                  }
               } catch (Exception e) {
                  Ans.LOGGER.warn("[ans] Could not read pack settings {}: {}", DEFAULTS, e.toString());
               }

               Path override = AnsPlatform.configDir().resolve("ans-settings.json");
               if (Files.isRegularFile(override)) {
                  try (Reader reader = Files.newBufferedReader(override)) {
                     JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();

                     for (Entry<String, JsonElement> e : obj.entrySet()) {
                        String id = readable.getOrDefault(e.getKey(), e.getKey());
                        VALUES.put(id, e.getValue().getAsString());
                     }
                  } catch (Exception e) {
                     Ans.LOGGER.warn("[ans] Could not read {}: {}", override, e.toString());
                  }
               }

               loaded = true;
            }
         }
      }
   }

   public static String value(String id) {
      ensureLoaded();
      return VALUES.getOrDefault(id, "");
   }

   public static boolean selected(String id, String option) {
      return value(id).equals(option);
   }

   public static boolean enabled(String id) {
      String v = value(id);
      return !v.isEmpty() && !v.equals("false") && !v.equals("disabled") && !v.equals("off") && !v.equals("0");
   }

   public static void set(String id, String value) {
      ensureLoaded();
      VALUES.put(id, value);
   }
}
