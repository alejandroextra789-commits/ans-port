package com.ans.client.mob;

import com.ans.Ans;
import com.ans.client.render.ReplacedEntityRenderer;
import com.ans.client.render.ReplacedMobRenderer;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class AnimatedMobs {
   private static final Map<EntityType<?>, AnimatedMob<?>> MOBS = new LinkedHashMap<>();
   private static boolean renderersRegistered;

   private AnimatedMobs() {
   }

   public static synchronized <E extends Entity> AnimatedMob<E> register(AnimatedMob<E> mob) {
      AnimatedMob<?> previous = MOBS.put(mob.type(), mob);
      if (previous != null) {
         Ans.LOGGER.warn("[ans] {} replaces earlier registration {}", mob, previous);
      }

      if (renderersRegistered) {
         registerRenderer(mob);
      }

      return mob;
   }

   public static AnimatedMob<?> get(EntityType<?> type) {
      return MOBS.get(type);
   }

   public static Collection<AnimatedMob<?>> all() {
      return Collections.unmodifiableCollection(MOBS.values());
   }

   public static synchronized void registerRenderers() {
      for (AnimatedMob<?> mob : MOBS.values()) {
         registerRenderer(mob);
      }

      renderersRegistered = true;
      Ans.LOGGER.info("[ans] Replaced renderers for {} mob type(s): {}", MOBS.size(), MOBS.values());
   }

   private static <E extends Entity> void registerRenderer(AnimatedMob<E> mob) {
      if (!mob.effectsOnly()) {
         EntityRendererProvider<E> vanilla = vanillaProvider(mob.type());
         AnimatedMob.RendererFactory<E> custom = mob.renderer();
         if (custom != null) {
            EntityRenderers.register(mob.type(), context -> custom.create(context, mob, vanilla));
         } else if (mob.living()) {
            AnimatedMob raw = mob;
            EntityRendererProvider rawVanilla = vanilla;
            EntityRenderers.register(mob.type(), context -> new ReplacedMobRenderer(context, raw, rawVanilla));
         } else {
            EntityRenderers.register(mob.type(), context -> new ReplacedEntityRenderer<E>(context, mob, vanilla));
         }
      }
   }

   private static <E extends Entity> EntityRendererProvider<E> vanillaProvider(EntityType<E> type) {
      try {
         Field f = EntityRenderers.class.getDeclaredField("PROVIDERS");
         f.setAccessible(true);
         return (EntityRendererProvider<E>)((Map)f.get(null)).get(type);
      } catch (ReflectiveOperationException | RuntimeException e) {
         Ans.LOGGER.debug("[ans] vanilla renderer provider for {} unavailable: {}", type, e.toString());
         return null;
      }
   }
}
