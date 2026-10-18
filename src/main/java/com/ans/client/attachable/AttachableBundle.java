package com.ans.client.attachable;

import com.ans.client.bedrock.EntityBundle;
import com.ans.client.molang.Molang;
import com.ans.client.molang.MolangExpr;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;

public final class AttachableBundle {
   public final ResourceLocation id;
   public final String identifier;
   public final EntityBundle bundle;
   public final MolangExpr parentSetup;
   public final Map<ResourceLocation, Map<String, MolangExpr>> bindings;
   public final Map<ResourceLocation, TextureMeshes.Spec> textureMeshes;
   public final Map<String, Integer> itemAux;
   public final String auxVariable;

   private AttachableBundle(ResourceLocation id, JsonObject root) {
      this.id = id;
      this.identifier = root.has("identifier") ? root.get("identifier").getAsString() : id.toString();
      this.bundle = EntityBundle.parse("attachable:" + this.identifier, root, id.toString());
      JsonObject scripts = root.has("scripts") && root.get("scripts").isJsonObject() ? root.getAsJsonObject("scripts") : new JsonObject();
      this.parentSetup = Molang.compile(scripts.get("parent_setup"), null);
      Map<ResourceLocation, Map<String, MolangExpr>> binds = new LinkedHashMap<>();
      JsonObject b = root.has("bone_bindings") && root.get("bone_bindings").isJsonObject() ? root.getAsJsonObject("bone_bindings") : new JsonObject();

      for (Entry<String, JsonElement> e : b.entrySet()) {
         ResourceLocation model = ResourceLocation.tryParse(e.getKey());
         if (model != null && e.getValue().isJsonObject()) {
            Map<String, MolangExpr> bones = new LinkedHashMap<>();

            for (Entry<String, JsonElement> bone : e.getValue().getAsJsonObject().entrySet()) {
               MolangExpr expr = Molang.compile(bone.getValue(), null);
               if (expr != null) {
                  bones.put(bone.getKey().toLowerCase(Locale.ROOT), expr);
               }
            }

            binds.put(model, Collections.unmodifiableMap(bones));
         }
      }

      this.bindings = Collections.unmodifiableMap(binds);
      this.textureMeshes = TextureMeshes.parse(root.get("texture_meshes"));
      Map<String, Integer> aux = new LinkedHashMap<>();
      if (root.has("item_aux") && root.get("item_aux").isJsonObject()) {
         for (Entry<String, JsonElement> e : root.getAsJsonObject("item_aux").entrySet()) {
            aux.put(e.getKey(), e.getValue().getAsInt());
         }
      }

      this.itemAux = Collections.unmodifiableMap(aux);
      this.auxVariable = root.has("aux_variable") ? root.get("aux_variable").getAsString() : null;
   }

   static AttachableBundle parse(ResourceLocation id, JsonObject root) {
      return new AttachableBundle(id, root);
   }

   public boolean wornOnly() {
      for (Map<String, MolangExpr> b : this.bindings.values()) {
         if (!b.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public boolean drawable() {
      return !this.bundle.geometry.isEmpty() && !this.bundle.renderControllers.isEmpty();
   }

   @Override
   public String toString() {
      return "AttachableBundle[" + this.identifier + "]";
   }
}
