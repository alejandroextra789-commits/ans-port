package com.ans.client.bedrock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToIntBiFunction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class ChangeColors {
   public static final int NONE = -1;
   private static final Map<EntityType<?>, ToIntBiFunction<Entity, Float>> PROVIDERS = new ConcurrentHashMap<>();
   private static final Map<EntityType<?>, ChangeColors.TextureTint<Entity>> TEXTURE_TINTS = new ConcurrentHashMap<>();

   private ChangeColors() {
   }

   public static <E extends Entity> void register(EntityType<E> type, ToIntBiFunction<E, Float> provider) {
      PROVIDERS.put(type, (e, pt) -> provider.applyAsInt((E)e, pt));
   }

   public static int get(Entity entity, float partialTick) {
      ToIntBiFunction<Entity, Float> p = PROVIDERS.get(entity.getType());
      return p == null ? -1 : p.applyAsInt(entity, partialTick);
   }

   public static <E extends Entity> void registerTextureTints(EntityType<E> type, ChangeColors.TextureTint<E> provider) {
      TEXTURE_TINTS.put(type, (e, pt, i) -> provider.argb((E)e, pt, i));
   }

   public static int textureTint(Entity entity, float partialTick, int index) {
      ChangeColors.TextureTint<Entity> p = TEXTURE_TINTS.get(entity.getType());
      return p == null ? -1 : p.argb(entity, partialTick, index);
   }

   @FunctionalInterface
   public interface TextureTint<E extends Entity> {
      int argb(E var1, float var2, int var3);
   }
}
