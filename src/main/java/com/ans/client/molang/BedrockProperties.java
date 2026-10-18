package com.ans.client.molang;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class BedrockProperties {
   private static final Map<String, Function<Entity, Object>> GLOBAL = new ConcurrentHashMap<>();
   private static final Map<String, Map<EntityType<?>, Function<Entity, Object>>> PER_TYPE = new ConcurrentHashMap<>();
   private BedrockProperties() {
   }

   public static void register(String name, Function<Entity, Object> provider) {
      GLOBAL.put(name, provider);
   }

   public static <E extends Entity> void register(EntityType<E> type, String name, Function<E, Object> provider) {
      PER_TYPE.computeIfAbsent(name, var0x -> new ConcurrentHashMap<>()).put(type, e -> provider.apply((E)e));
   }

   public static Object get(Entity entity, String name) {
      if (entity == null) {
         return null;
      }

      Map<EntityType<?>, Function<Entity, Object>> perType = PER_TYPE.get(name);
      if (perType != null) {
         Function<Entity, Object> f = perType.get(entity.getType());
         if (f != null) {
            return f.apply(entity);
         }
      }

      Function<Entity, Object> f = GLOBAL.get(name);
      return f == null ? null : f.apply(entity);
   }

   public static <T> String keyPath(Holder<T> holder) {
      return holder == null ? null : holder.unwrapKey().map(ResourceKey::location).<String>map(ResourceLocation::getPath).orElse(null);
   }

   private static Object climateVariant(Entity entity) {
      // Cow/pig/chicken climate variants are not part of Minecraft 1.20.1.
      return null;
   }

   static {
      register("minecraft:climate_variant", BedrockProperties::climateVariant);
   }
}
