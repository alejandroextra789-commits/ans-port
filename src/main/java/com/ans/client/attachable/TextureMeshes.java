package com.ans.client.attachable;

import com.ans.Ans;
import com.ans.client.bedrock.AnimationLookup;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.geckolib.loading.definition.geometry.Geometry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import com.ans.client.render.TextureImageUtil;
import net.minecraft.resources.ResourceLocation;

public final class TextureMeshes {
   private static final boolean ENABLED = !"false".equalsIgnoreCase(System.getProperty("ans.runtimeTextureMeshes"));
   private static final int MAX_SIZE = 256;
   private static final Map<String, Map<String, GeoCube[]>> CACHE = new HashMap<>();
   private static int seenGeneration = -1;

   private TextureMeshes() {
   }

   static Map<ResourceLocation, TextureMeshes.Spec> parse(JsonElement json) {
      if (json != null && json.isJsonObject()) {
         Map<ResourceLocation, TextureMeshes.Spec> out = new LinkedHashMap<>();

         for (Entry<String, JsonElement> e : json.getAsJsonObject().entrySet()) {
            ResourceLocation model = ResourceLocation.tryParse(e.getKey());
            if (model != null && e.getValue().isJsonObject()) {
               JsonObject o = e.getValue().getAsJsonObject();
               if (o.has("texture") && o.has("mask") && o.has("image")) {
                  Map<String, TextureMeshes.BoneSpec> bones = new LinkedHashMap<>();

                  for (Entry<String, JsonElement> b : o.getAsJsonObject("bones").entrySet()) {
                     JsonObject bo = b.getValue().getAsJsonObject();
                     List<double[][]> meshes = new ArrayList<>();

                     for (JsonElement m : bo.getAsJsonArray("meshes")) {
                        JsonObject mo = m.getAsJsonObject();
                        meshes.add(new double[][]{vec(mo, "position", 0.0), vec(mo, "local_pivot", 0.0), vec(mo, "rotation", 0.0), vec(mo, "scale", 1.0)});
                     }

                     bones.put(
                        b.getKey(),
                        new TextureMeshes.BoneSpec(
                           bo.get("cubes_before").getAsInt(),
                           bo.get("pivot_y").getAsDouble(),
                           bo.has("mirror") ? bo.get("mirror").getAsBoolean() : null,
                           bo.has("inflate") ? bo.get("inflate").getAsDouble() : null,
                           List.copyOf(meshes)
                        )
                     );
                  }

                  JsonArray img = o.getAsJsonArray("image");
                  out.put(
                     model,
                     new TextureMeshes.Spec(
                        o.get("texture").getAsString(),
                        o.get("texture_width").getAsInt(),
                        o.get("texture_height").getAsInt(),
                        img.get(0).getAsInt(),
                        img.get(1).getAsInt(),
                        o.get("mask").getAsString(),
                        Map.copyOf(bones)
                     )
                  );
               }
            }
         }

         return Map.copyOf(out);
      } else {
         return Map.of();
      }
   }

   private static double[] vec(JsonObject o, String key, double fallback) {
      double[] v = new double[]{fallback, fallback, fallback};
      if (o.has(key) && o.get(key).isJsonArray()) {
         JsonArray a = o.getAsJsonArray(key);

         for (int i = 0; i < 3 && i < a.size(); i++) {
            v[i] = a.get(i).getAsDouble();
         }
      }

      return v;
   }

   public static synchronized Map<String, GeoCube[]> overrides(BakedGeoModel model, ResourceLocation modelId, TextureMeshes.Spec spec, ResourceLocation texture) {
      if (!ENABLED) {
         return Map.of();
      }

      int generation = AnimationLookup.generation();
      if (generation != seenGeneration) {
         seenGeneration = generation;
         CACHE.clear();
      }

      String key = modelId + "|" + texture;
      Map<String, GeoCube[]> cached = CACHE.get(key);
      if (cached == null) {
         try {
            cached = build(model, modelId, spec, texture);
         } catch (Exception e) {
            Ans.LOGGER.warn("[ans] texture mesh of {} with {} could not be rebuilt; the converter's cubes are drawn", new Object[]{modelId, texture, e});
            cached = Map.of();
         }

         CACHE.put(key, cached);
      }

      return cached;
   }

   private static Map<String, GeoCube[]> build(BakedGeoModel model, ResourceLocation modelId, TextureMeshes.Spec spec, ResourceLocation texture) throws Exception {
      if (Minecraft.getInstance().getResourceManager().getResource(texture).isEmpty()) {
         return Map.of();
      }

      NativeImage image = TextureImageUtil.load(Minecraft.getInstance().getResourceManager(), texture);

      Map<String, GeoCube[]> out;
      label105: {
         Map var23;
         label106: {
            try {
               int w = image.getWidth();
               int h = image.getHeight();
               boolean[] opaque = new boolean[w * h];
               byte[] maskBytes = new byte[w * h];

               for (int v = 0; v < h; v++) {
                  for (int u = 0; u < w; u++) {
                     boolean o = image.getPixel(u, v) >>> 24 != 0;
                     opaque[v * w + u] = o;
                     maskBytes[v * w + u] = (byte)(o ? 1 : 0);
                  }
               }

               if (w == spec.imageWidth()
                  && h == spec.imageHeight()
                  && HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(maskBytes)).equals(spec.mask())) {
                  out = Map.of();
                  break label105;
               }

               if (w <= 256 && h <= 256) {
                  out = new HashMap<>();

                  for (Entry<String, TextureMeshes.BoneSpec> e : spec.bones().entrySet()) {
                     GeoBone bone = (GeoBone)((Map)model.boneLookup().get()).get(e.getKey());
                     if (bone instanceof CuboidGeoBone cb) {
                        GeoCube[] mesh = extrude(modelId, spec, e.getValue(), opaque, w, h);
                        GeoCube[] kept = Arrays.copyOf(cb.cubes, Math.min(e.getValue().cubesBefore(), cb.cubes.length));
                        GeoCube[] all = Arrays.copyOf(kept, kept.length + mesh.length);
                        System.arraycopy(mesh, 0, all, kept.length, mesh.length);
                        out.put(e.getKey(), all);
                     }
                  }

                  Ans.LOGGER.info("[ans] texture mesh {} re-extruded from {} ({}x{}, {} bones)", new Object[]{modelId, texture, w, h, out.size()});
                  var23 = Map.copyOf(out);
                  break label106;
               }

               Ans.LOGGER.warn("[ans] texture mesh texture {} is {}x{} (max {}): the converter's cubes are drawn", new Object[]{texture, w, h, 256});
               out = Map.of();
            } catch (Throwable var18) {
               if (image != null) {
                  try {
                     image.close();
                  } catch (Throwable var17) {
                     var18.addSuppressed(var17);
                  }
               }

               throw var18;
            }

            if (image != null) {
               image.close();
            }

            return out;
         }

         if (image != null) {
            image.close();
         }

         return var23;
      }

      if (image != null) {
         image.close();
      }

      return out;
   }

   private static GeoCube[] extrude(ResourceLocation modelId, TextureMeshes.Spec spec, TextureMeshes.BoneSpec bone, boolean[] opaque, int w, int h) {
      int tw = spec.textureWidth();
      int th = spec.textureHeight();
      double ku = (double)tw / w;
      double kv = (double)th / h;
      JsonArray cubes = new JsonArray();

      for (double[][] mesh : bone.meshes()) {
         double[] p = mesh[0];
         double[] lp = mesh[1];
         double[] rot = mesh[2];
         double[] sc = mesh[3];
         double[] pos = new double[]{p[0], 2.0 * bone.pivotY() - p[1], p[2]};

         for (int v = 0; v < h; v++) {
            for (int u = 0; u < w; u++) {
               if (opaque[v * w + u]) {
                  JsonObject uv = new JsonObject();
                  addFace(uv, "up", (double)u * tw / w, (double)v * th / h, (double)tw / w, (double)th / h);
                  addFace(uv, "down", (double)u * tw / w, (double)v * th / h, (double)tw / w, (double)th / h);
                  String[] names = new String[]{"east", "west", "north", "south"};
                  int[][] d = new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

                  for (int i = 0; i < 4; i++) {
                     int uu = u + d[i][0];
                     int vv = v + d[i][1];
                     if (uu < 0 || uu >= w || vv < 0 || vv >= h || !opaque[vv * w + uu]) {
                        addFace(uv, names[i], (double)u * tw / w, (double)v * th / h, (double)tw / w, (double)th / h);
                     }
                  }

                  JsonObject cube = new JsonObject();
                  cube.add("origin", arr(pos[0] + (u * ku - lp[0]) * sc[0], pos[1] + (lp[1] - 1.0) * sc[1], pos[2] + (v * kv - lp[2]) * sc[2]));
                  cube.add("size", arr(ku * sc[0], sc[1], kv * sc[2]));
                  cube.add("pivot", arr(pos[0], pos[1], pos[2]));
                  cube.add("rotation", arr(rot[0], rot[1], rot[2]));
                  cube.add("uv", uv);
                  cubes.add(cube);
               }
            }
         }
      }

      JsonObject b = new JsonObject();
      b.addProperty("name", "mesh");
      if (bone.mirror() != null) {
         b.addProperty("mirror", bone.mirror());
      }

      if (bone.inflate() != null) {
         b.addProperty("inflate", bone.inflate());
      }

      b.add("cubes", cubes);
      JsonArray bones = new JsonArray();
      bones.add(b);
      JsonObject desc = new JsonObject();
      desc.addProperty("identifier", "geometry.ans.runtime_texture_mesh");
      desc.addProperty("texture_width", tw);
      desc.addProperty("texture_height", th);
      JsonObject def = new JsonObject();
      def.add("description", desc);
      def.add("bones", bones);
      JsonArray defs = new JsonArray();
      defs.add(def);
      JsonObject geo = new JsonObject();
      geo.addProperty("format_version", "1.12.0");
      geo.add("minecraft:geometry", defs);
      BakedGeoModel baked = ((Geometry)Geometry.GSON.fromJson(geo, Geometry.class)).bake(modelId);
      return baked.topLevelBones().length > 0 && baked.topLevelBones()[0] instanceof CuboidGeoBone cb ? cb.cubes : new GeoCube[0];
   }

   private static void addFace(JsonObject uv, String face, double u, double v, double su, double sv) {
      JsonObject f = new JsonObject();
      f.add("uv", arr(u, v));
      f.add("uv_size", arr(su, sv));
      uv.add(face, f);
   }

   private static JsonArray arr(double... values) {
      JsonArray a = new JsonArray();

      for (double d : values) {
         a.add(d);
      }

      return a;
   }

   record BoneSpec(int cubesBefore, double pivotY, Boolean mirror, Double inflate, List<double[][]> meshes) {
   }

   public record Spec(
      String textureKey, int textureWidth, int textureHeight, int imageWidth, int imageHeight, String mask, Map<String, TextureMeshes.BoneSpec> bones
   ) {
   }
}
