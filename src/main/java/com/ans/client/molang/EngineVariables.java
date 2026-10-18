package com.ans.client.molang;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.ToDoubleFunction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public final class EngineVariables {
   private static final List<EngineVariables.Var> GLOBAL = new CopyOnWriteArrayList<>();
   private static final Map<EntityType<?>, List<EngineVariables.Var>> PER_TYPE = new ConcurrentHashMap<>();

   private EngineVariables() {
   }

   private static int slot(String name) {
      return MolangVars.id(name.replaceFirst("^(variable|v)\\.", "").toLowerCase(Locale.ROOT));
   }

   public static void register(String name, EngineVariables.Provider provider) {
      int slot = slot(name);
      GLOBAL.removeIf(v -> v.slot == slot);
      GLOBAL.add(new EngineVariables.Var(slot, (e, pt) -> e instanceof LivingEntity l ? provider.value(l, pt) : 0.0));
   }

   public static <E extends Entity> void registerFor(EntityType<E> type, String name, ToDoubleFunction<E> fn) {
      int slot = slot(name);
      List<EngineVariables.Var> list = PER_TYPE.computeIfAbsent(type, var0x -> new CopyOnWriteArrayList<>());
      list.removeIf(v -> v.slot == slot);
      list.add(new EngineVariables.Var(slot, (e, var2x) -> fn.applyAsDouble((E)e)));
   }

   public static void apply(Entity entity, float partialTick, MolangScope vars) {
      if (entity instanceof LivingEntity living) {
         apply(living, partialTick, vars);
      } else {
         List<EngineVariables.Var> own = PER_TYPE.get(entity.getType());
         if (own != null) {
            for (EngineVariables.Var v : own) {
               vars.set(v.slot, v.provider.value(entity, partialTick));
            }
         }
      }
   }

   public static <E extends Entity> void registerForPartial(EntityType<E> type, String name, EngineVariables.PartialProvider<E> fn) {
      int slot = slot(name);
      List<EngineVariables.Var> list = PER_TYPE.computeIfAbsent(type, var0x -> new CopyOnWriteArrayList<>());
      list.removeIf(v -> v.slot == slot);
      list.add(new EngineVariables.Var(slot, (e, pt) -> fn.value((E)e, pt)));
   }

   public static void apply(LivingEntity entity, float partialTick, MolangScope vars) {
      List<EngineVariables.Var> own = PER_TYPE.get(entity.getType());

      for (EngineVariables.Var v : GLOBAL) {
         if (own == null || !overridden(own, v.slot)) {
            vars.set(v.slot, v.provider.value(entity, partialTick));
         }
      }

      if (own != null) {
         for (EngineVariables.Var v : own) {
            vars.set(v.slot, v.provider.value(entity, partialTick));
         }
      }
   }

   private static boolean overridden(List<EngineVariables.Var> own, int slot) {
      for (EngineVariables.Var v : own) {
         if (v.slot == slot) {
            return true;
         }
      }

      return false;
   }

   static {
      register("attack_time", LivingEntity::getAttackAnim);
   }

   private interface AnyProvider {
      double value(Entity var1, float var2);
   }

   public interface PartialProvider<E extends Entity> {
      double value(E var1, float var2);
   }

   @FunctionalInterface
   public interface Provider {
      double value(LivingEntity var1, float var2);
   }

   private record Var(int slot, EngineVariables.AnyProvider provider) {
   }
}
