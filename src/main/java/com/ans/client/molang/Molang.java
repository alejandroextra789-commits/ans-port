package com.ans.client.molang;

import com.ans.Ans;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Molang {
   private static final Map<String, MolangExpr> CACHE = new ConcurrentHashMap<>();
   private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
   public static final MolangExpr ZERO = MolangExpr.constant(0.0);
   public static final MolangExpr ONE = MolangExpr.constant(1.0);

   private Molang() {
   }

   public static MolangExpr compile(String source) {
      MolangExpr cached = CACHE.get(source);
      if (cached != null) {
         return cached;
      }

      MolangExpr expr;
      try {
         expr = new MolangExpr(source, MolangParser.parse(source));
      } catch (RuntimeException e) {
         warn("Molang syntax error, expression reads 0: " + e.getMessage());
         expr = new MolangExpr(source, new Nodes.Const(0.0));
      }

      CACHE.putIfAbsent(source, expr);
      return expr;
   }

   public static MolangExpr compile(JsonElement e, MolangExpr fallback) {
      if (e == null || e.isJsonNull()) {
         return fallback;
      }

      if (e.isJsonPrimitive()) {
         JsonPrimitive p = e.getAsJsonPrimitive();
         if (p.isBoolean()) {
            return p.getAsBoolean() ? ONE : ZERO;
         }

         if (p.isNumber()) {
            return MolangExpr.constant(p.getAsDouble());
         }

         String s = p.getAsString();
         return s.isBlank() ? fallback : compile(s);
      } else {
         warn("expected a number or Molang string, got " + e);
         return fallback;
      }
   }

   public static void warn(String message) {
      if (WARNED.size() < 5000 && WARNED.add(message)) {
         Ans.LOGGER.warn("[ans] {}", message);
      }
   }

   public static void clearCache() {
      CACHE.clear();
   }
}
