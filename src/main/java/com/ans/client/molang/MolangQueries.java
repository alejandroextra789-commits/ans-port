package com.ans.client.molang;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public final class MolangQueries {
   private static final Map<String, MolangQueries.Factory> FACTORIES = new ConcurrentHashMap<>();
   private static final Set<String> STRING_QUERIES = ConcurrentHashMap.newKeySet();
   private static final Map<String, Map<EntityType<?>, MolangQueries.Factory>> PER_TYPE = new ConcurrentHashMap<>();

   private MolangQueries() {
   }

   public static void register(String name, MolangQueries.Factory factory) {
      FACTORIES.put(name, factory);
   }

   public static void registerString(String name, MolangQueries.Factory factory) {
      STRING_QUERIES.add(name);
      FACTORIES.put(name, factory);
   }

   public static void simple(String name, ToDoubleFunction<MolangContext> fn) {
      MolangQueries.Bound bound = fn::applyAsDouble;
      register(name, var1x -> bound);
   }

   public static void living(String name, ToDoubleFunction<LivingEntity> fn) {
      simple(name, c -> c.entity == null ? 0.0 : fn.applyAsDouble(c.entity));
   }

   public static void flag(String name, Predicate<LivingEntity> fn) {
      simple(name, c -> c.entity != null && fn.test(c.entity) ? 1.0 : 0.0);
   }

   public static void actor(String name, ToDoubleFunction<Entity> fn) {
      simple(name, c -> c.actor == null ? 0.0 : fn.applyAsDouble(c.actor));
   }

   public static void actorFlag(String name, Predicate<Entity> fn) {
      simple(name, c -> c.actor != null && fn.test(c.actor) ? 1.0 : 0.0);
   }

   public static <E extends Entity> void registerFor(EntityType<E> type, String name, ToDoubleFunction<E> fn) {
      PER_TYPE.computeIfAbsent(name, var0x -> new ConcurrentHashMap<>()).put(type, var1x -> c -> c.actor == null ? 0.0 : fn.applyAsDouble((E)c.actor));
   }

   public static <E extends Entity> void registerFactoryFor(EntityType<E> type, String name, MolangQueries.Factory factory) {
      registerFor(type, name, factory);
   }

   public static <E extends Entity> void registerFor(EntityType<E> type, String name, MolangQueries.Factory factory) {
      PER_TYPE.computeIfAbsent(name, var0x -> new ConcurrentHashMap<>()).put(type, factory);
   }

   public static boolean isStringQuery(String name) {
      return STRING_QUERIES.contains(name);
   }

   public static boolean exists(String name) {
      return FACTORIES.containsKey(name) || PER_TYPE.containsKey(name);
   }

   public static Set<String> names() {
      Set<String> all = new TreeSet<>(FACTORIES.keySet());
      all.addAll(PER_TYPE.keySet());
      return Collections.unmodifiableSet(all);
   }

   static MolangQueries.Bound bind(String name, Node[] args) {
      MolangQueries.Factory global = FACTORIES.get(name);
      Map<EntityType<?>, MolangQueries.Factory> perType = PER_TYPE.get(name);
      if (global == null && perType == null) {
         Molang.warn("unsupported query 'query." + name + "' (reads 0)");
         return null;
      }

      final MolangQueries.Bound fallback = global != null ? global.bind(args) : null;
      if (perType == null) {
         return fallback;
      }

      final Map<EntityType<?>, MolangQueries.Bound> bound = new ConcurrentHashMap<>();

      for (Entry<EntityType<?>, MolangQueries.Factory> e : perType.entrySet()) {
         bound.put(e.getKey(), e.getValue().bind(args));
      }

      return new MolangQueries.Bound() {
         private MolangQueries.Bound pick(MolangContext c) {
            MolangQueries.Bound b = c.actor == null ? null : bound.get(c.actor.getType());
            return b != null ? b : fallback;
         }

         @Override
         public double eval(MolangContext c) {
            MolangQueries.Bound b = this.pick(c);
            return b == null ? 0.0 : b.eval(c);
         }

         @Override
         public Object value(MolangContext c) {
            MolangQueries.Bound b = this.pick(c);
            return b == null ? 0.0 : b.value(c);
         }
      };
   }

   public static String [] constantStrings(Node[] args, int from) {
      String[] out = new String[Math.max(0, args.length - from)];

      for (int i = from; i < args.length; i++) {
         if (!args[i].isConstant() || !args[i].stringy()) {
            return null;
         }

         out[i - from] = (String)args[i].value(MolangContext.current());
      }

      return out;
   }

   public static String string(Node arg, MolangContext c) {
      return arg.value(c) instanceof String s ? s : "";
   }

   public static String[] strings(Node[] args, int from, MolangContext c) {
      String[] out = new String[Math.max(0, args.length - from)];

      for (int i = from; i < args.length; i++) {
         out[i - from] = string(args[i], c);
      }

      return out;
   }

   public interface Bound {
      double eval(MolangContext var1);

      default Object value(MolangContext c) {
         return this.eval(c);
      }
   }

   @FunctionalInterface
   public interface Factory {
      MolangQueries.Bound bind(Node[] var1);
   }
}
