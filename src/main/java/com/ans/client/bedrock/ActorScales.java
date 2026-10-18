package com.ans.client.bedrock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public final class ActorScales {
   private static final Map<EntityType<?>, ToDoubleFunction<LivingEntity>> PROVIDERS = new ConcurrentHashMap<>();

   private ActorScales() {
   }

   public static <E extends Entity> void register(EntityType<E> type, ToDoubleFunction<E> scale) {
      PROVIDERS.put(type, e -> scale.applyAsDouble((E)e));
   }

   public static float correction(Entity entity) {
      ToDoubleFunction<LivingEntity> p = PROVIDERS.get(entity.getType());
      if (p != null && entity instanceof LivingEntity living) {
         float age = living.getAgeScale();
         return age > 0.0F ? (float)p.applyAsDouble(living) / age : 1.0F;
      } else {
         return 1.0F;
      }
   }
}
