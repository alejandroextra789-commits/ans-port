package com.ans.client.bedrock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class ResourceOverrides {
   private static final Map<EntityType<?>, ResourceOverrides.Provider> GEOMETRY = new ConcurrentHashMap<>();
   private static final Map<EntityType<?>, ResourceOverrides.Provider> TEXTURE = new ConcurrentHashMap<>();

   private ResourceOverrides() {
   }

   public static void geometry(EntityType<?> type, ResourceOverrides.Provider provider) {
      GEOMETRY.put(type, provider);
   }

   public static void texture(EntityType<?> type, ResourceOverrides.Provider provider) {
      TEXTURE.put(type, provider);
   }

   static ResourceLocation resolve(Entity entity, String prefix, String key) {
      if (entity == null) {
         return null;
      } else {
         Map<EntityType<?>, ResourceOverrides.Provider> map = prefix.startsWith("geometry") ? GEOMETRY : (prefix.startsWith("texture") ? TEXTURE : null);
         if (map != null && !map.isEmpty()) {
            ResourceOverrides.Provider p = map.get(entity.getType());
            return p == null ? null : p.resolve(entity, key);
         } else {
            return null;
         }
      }
   }

   @FunctionalInterface
   public interface Provider {
      ResourceLocation resolve(Entity var1, String var2);
   }
}
