package com.ans.client.settings;

import com.ans.Ans;
import com.ans.client.bedrock.AnimationLookup;
import com.ans.client.molang.PackSettings;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import com.ans.AnsClientEvents;
import net.minecraftforge.client.ClientRegistry;
import com.ans.AnsPlatform;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.resources.Resource;

public final class AnsSettings {
   public static final KeyMapping OPEN_KEY = new KeyMapping("key.ans.settings", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.misc");
   private static final Path FILE = AnsPlatform.configDir().resolve("ans-settings.json");
   private static List<AnsSettings.Setting> settings = List.of();
   private static int loadedGeneration = -1;

   private AnsSettings() {
   }

   public static void init() {
      ClientRegistry.registerKeyBinding(OPEN_KEY);
      AnsClientEvents.registerEndTick(mc -> {
         while (OPEN_KEY.consumeClick()) {
            if (mc.screen == null) {
               mc.setScreen(new AnsSettingsScreen(null));
            }
         }
      });
   }

   public static synchronized List<AnsSettings.Setting> all() {
      int generation = AnimationLookup.generation();
      if (generation == loadedGeneration && !settings.isEmpty()) {
         return settings;
      }

      loadedGeneration = generation;
      List<AnsSettings.Setting> out = new ArrayList<>();
      out.add(
         new AnsSettings.Setting(
            "experience",
            false,
            "full",
            Experience.OPTIONS,
            "experience",
            "ans.settings.experience",
            Experience.OPTIONS.stream().map(o -> "ans.settings.experience." + o).toList()
         )
      );

      try {
         Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(PackSettings.DEFAULTS);
         if (res.isPresent()) {
            try (Reader reader = res.get().openAsReader()) {
               JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();

               for (Entry<String, JsonElement> e : obj.entrySet()) {
                  if (e.getValue().isJsonObject()) {
                     JsonObject s = e.getValue().getAsJsonObject();
                     List<String> options = new ArrayList<>();
                     if (s.has("options")) {
                        s.getAsJsonArray("options").forEach(o -> options.add(o.getAsString()));
                     }

                     List<String> optionTexts = new ArrayList<>();
                     if (s.has("option_texts")) {
                        s.getAsJsonArray("option_texts").forEach(o -> optionTexts.add(o.isJsonNull() ? null : o.getAsString()));
                     }

                     JsonElement def = s.get("default");
                     out.add(
                        new AnsSettings.Setting(
                           e.getKey(),
                           "toggle".equals(str(s, "type")),
                           def != null && !def.isJsonNull() ? def.getAsString() : "",
                           List.copyOf(options),
                           str(s, "readable") == null ? e.getKey() : str(s, "readable"),
                           str(s, "text"),
                           optionTexts
                        )
                     );
                  }
               }
            }
         }
      } catch (Exception e) {
         Ans.LOGGER.warn("[ans] settings: could not read {}: {}", PackSettings.DEFAULTS, e.toString());
      }

      settings = List.copyOf(out);
      return settings;
   }

   public static AnsSettings.Setting find(String idOrReadable) {
      for (AnsSettings.Setting s : all()) {
         if (s.id.equals(idOrReadable) || s.readable.equals(idOrReadable) || s.id.equals("oreville_ans:" + idOrReadable)) {
            return s;
         }
      }

      return null;
   }

   public static void set(AnsSettings.Setting s, String value, boolean save) {
      PackSettings.set(s.id, value);
      if (save) {
         save();
      }
   }

   public static void resetAll() {
      for (AnsSettings.Setting s : all()) {
         PackSettings.set(s.id, s.defaultValue);
      }

      save();
   }

   public static synchronized void save() {
      JsonObject out = new JsonObject();

      try {
         if (Files.isRegularFile(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
               JsonElement old = JsonParser.parseReader(reader);
               if (old.isJsonObject()) {
                  for (Entry<String, JsonElement> e : old.getAsJsonObject().entrySet()) {
                     if (find(e.getKey()) == null) {
                        out.add(e.getKey(), e.getValue());
                     }
                  }
               }
            }
         }
      } catch (Exception e) {
         Ans.LOGGER.warn("[ans] settings: could not read {} (overwriting): {}", FILE, e.toString());
      }

      for (AnsSettings.Setting s : all()) {
         String v = s.value();
         out.add(s.readable, s.toggle ? new JsonPrimitive(Boolean.parseBoolean(v)) : new JsonPrimitive(v));
      }

      try {
         Files.createDirectories(FILE.getParent());
         Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(out) + "\n", StandardCharsets.UTF_8);
      } catch (Exception e) {
         Ans.LOGGER.warn("[ans] settings: could not write {}: {}", FILE, e.toString());
      }
   }

   public static void scenario(Minecraft mc, String[] p) {
      if (p.length >= 2 && p[1].equals("screen")) {
         mc.setScreen(new AnsSettingsScreen(null));
      } else if (p.length >= 2 && p[1].equals("close")) {
         mc.setScreen(null);
      } else if (p.length >= 2 && p[1].equals("save")) {
         save();
      } else if (p.length >= 3 && p[1].equals("scroll")) {
         if (mc.screen instanceof AnsSettingsScreen s) {
            s.scrollTo(Double.parseDouble(p[2]));
         }
      } else {
         AnsSettings.Setting s = p.length >= 3 ? find(p[1]) : null;
         if (s == null) {
            Ans.LOGGER.warn("[ans] settings scenario: unknown setting in {}", String.join(" ", p));
         } else {
            set(s, p[2], false);
            Ans.LOGGER.info("[ans] settings scenario: {} ({}) = {}", new Object[]{s.readable, s.id, s.value()});
         }
      }
   }

   private static String str(JsonObject o, String key) {
      JsonElement e = o.get(key);
      return e != null && !e.isJsonNull() ? e.getAsString() : null;
   }

   static String translate(String key, String fallback) {
      if (key == null) {
         return fallback;
      }

      Language lang = Language.getInstance();
      return lang.has(key) ? lang.getOrDefault(key) : fallback;
   }

   static String[] split(String text) {
      int i = text.indexOf("§8");
      return i < 0 ? new String[]{text.strip(), ""} : new String[]{text.substring(0, i).strip(), text.substring(i + 2).strip()};
   }

   private static String pretty(String readable) {
      String s = readable.replace('_', ' ');
      return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
   }

   public record Setting(
      String id, boolean toggle, String defaultValue, List<String> options, String readable, String text, List<String> optionTexts
   ) {
      public String value() {
         String v = PackSettings.value(this.id);
         return v.isEmpty() ? this.defaultValue : v;
      }

      public String[] label() {
         return AnsSettings.split(AnsSettings.translate(this.text, AnsSettings.pretty(this.readable)));
      }

      public String optionLabel(String option) {
         int i = this.options.indexOf(option);
         String key = i >= 0 && i < this.optionTexts.size() ? this.optionTexts.get(i) : null;
         return AnsSettings.translate(key, option);
      }
   }
}
