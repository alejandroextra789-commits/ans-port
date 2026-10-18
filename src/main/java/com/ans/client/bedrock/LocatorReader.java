package com.ans.client.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * Reads Bedrock locators straight from a .geo.json. GeckoLib 4 does not keep them on
 * BakedGeoModel, so they are parsed here. Accepts both value forms:
 *   "name": [x, y, z]
 *   "name": { "offset": [x,y,z], "rotation": [x,y,z], "ignore_inherited_scale": bool }
 */
public final class LocatorReader {
   private static final Map<ResourceLocation, Map<String, GeoLocatorData>> CACHE = new ConcurrentHashMap<>();
   private static final Map<String, GeoLocatorData> NONE = Collections.emptyMap();

   private LocatorReader() {
   }

   public static Map<String, GeoLocatorData> forModel(ResourceLocation id) {
      if (id == null) {
         return NONE;
      }
      return CACHE.computeIfAbsent(id, LocatorReader::read);
   }

   private static Map<String, GeoLocatorData> read(ResourceLocation id) {
      Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(id);
      if (res.isEmpty()) {
         return NONE;
      }
      try (Reader r = new InputStreamReader(res.get().open())) {
         JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
         JsonObject geo = root.has("minecraft:geometry")
            ? root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
            : root;
         Map<String, GeoLocatorData> out = new HashMap<>();
         if (!geo.has("bones")) {
            return out;
         }
         for (JsonElement be : geo.getAsJsonArray("bones")) {
            JsonObject bone = be.getAsJsonObject();
            String boneName = bone.get("name").getAsString();
            if (!bone.has("locators")) {
               continue;
            }
            for (Map.Entry<String, JsonElement> e : bone.getAsJsonObject("locators").entrySet()) {
               out.put(e.getKey(), parse(boneName, e.getValue()));
            }
         }
         return out;
      } catch (Exception ex) {
         return NONE;
      }
   }

   private static GeoLocatorData parse(String boneName, JsonElement v) {
      float[] offset = new float[3];
      float[] rot = new float[3];
      boolean ignore = false;
      if (v.isJsonArray()) {
         offset = vec(v.getAsJsonArray());
      } else if (v.isJsonObject()) {
         JsonObject o = v.getAsJsonObject();
         if (o.has("offset")) {
            offset = vec(o.getAsJsonArray("offset"));
         }
         if (o.has("rotation")) {
            rot = vec(o.getAsJsonArray("rotation"));
         }
         if (o.has("ignore_inherited_scale")) {
            ignore = o.get("ignore_inherited_scale").getAsBoolean();
         }
      }
      return new GeoLocatorData(boneName,
         offset[0], offset[1], offset[2],
         (float)Math.toRadians(rot[0]), (float)Math.toRadians(rot[1]), (float)Math.toRadians(rot[2]),
         ignore);
   }

   private static float[] vec(JsonArray a) {
      return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
   }
}
