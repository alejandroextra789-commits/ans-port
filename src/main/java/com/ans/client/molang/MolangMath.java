package com.ans.client.molang;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.Mth;

public final class MolangMath {
   private static final Map<String, MolangMath.Def> FUNCTIONS = new HashMap<>();

   private MolangMath() {
   }

   static MolangMath.Fn get(String name, int argc) {
      MolangMath.Def d = FUNCTIONS.get(name);
      if (d == null) {
         return null;
      }

      if (argc >= d.minArgs) {
         return d.fn;
      }

      MolangMath.Fn fn = d.fn;
      int min = d.minArgs;
      return (c, a) -> {
         Node[] padded = new Node[min];

         for (int i = 0; i < min; i++) {
            padded[i] = i < a.length ? a[i] : new Nodes.Const(0.0);
         }

         return fn.call(c, padded);
      };
   }

   public static boolean exists(String name) {
      return FUNCTIONS.containsKey(name);
   }

   private static void def(String name, int args, MolangMath.Fn fn) {
      FUNCTIONS.put(name, new MolangMath.Def(args, fn));
   }

   private static double rad(double deg) {
      return deg * (float) (Math.PI / 180.0);
   }

   private static double deg(double rad) {
      return rad * 180.0F / (float)Math.PI;
   }

   public static double minAngle(double a) {
      a %= 360.0;
      if (a >= 180.0) {
         a -= 360.0;
      }

      if (a < -180.0) {
         a += 360.0;
      }

      return a;
   }

   static double finite(double v) {
      return Double.isFinite(v) ? v : 0.0;
   }

   private static double randomInt(double lo, double hi) {
      long a = Math.round(Math.min(lo, hi));
      long b = Math.round(Math.max(lo, hi));
      return a + ThreadLocalRandom.current().nextLong(b - a + 1L);
   }

   static double ease(String mode, String family, double t) {
      return switch (mode) {
         case "in" -> easeIn(family, t);
         case "out" -> 1.0 - easeIn(family, 1.0 - t);
         default -> t < 0.5 ? easeIn(family, 2.0 * t) / 2.0 : 1.0 - easeIn(family, 2.0 - 2.0 * t) / 2.0;
      };
   }

   private static double easeIn(String family, double t) {
      return switch (family) {
         case "quad" -> t * t;
         case "cubic" -> t * t * t;
         case "quart" -> t * t * t * t;
         case "quint" -> t * t * t * t * t;
         case "sine" -> 1.0 - Math.cos(t * Math.PI / 2.0);
         case "expo" -> t == 0.0 ? 0.0 : Math.pow(2.0, 10.0 * t - 10.0);
         case "circ" -> 1.0 - Math.sqrt(1.0 - t * t);
         case "back" -> 2.70158 * t * t * t - 1.70158 * t * t;
         case "elastic" -> t == 0.0 ? 0.0 : (t == 1.0 ? 1.0 : -Math.pow(2.0, 10.0 * t - 10.0) * Math.sin((t * 10.0 - 10.75) * (Math.PI * 2.0 / 3.0)));
         case "bounce" -> 1.0 - bounceOut(1.0 - t);
         default -> t;
      };
   }

   private static double bounceOut(double x) {
      double n1 = 7.5625;
      double d1 = 2.75;
      if (x < 1.0 / d1) {
         return n1 * x * x;
      } else if (x < 2.0 / d1) {
         double var8;
         return n1 * (var8 = x - 1.5 / d1) * var8 + 0.75;
      } else {
         double var6;
         double var7;
         return x < 2.5 / d1 ? n1 * (var6 = x - 2.25 / d1) * var6 + 0.9375 : n1 * (var7 = x - 2.625 / d1) * var7 + 0.984375;
      }
   }

   static {
      def("abs", 1, (c, a) -> Math.abs(a[0].eval(c)));
      def("sin", 1, (c, a) -> Math.sin(rad(a[0].eval(c))));
      def("cos", 1, (c, a) -> Math.cos(rad(a[0].eval(c))));
      def("asin", 1, (c, a) -> deg(Math.asin(Mth.clamp(a[0].eval(c), -1.0, 1.0))));
      def("acos", 1, (c, a) -> deg(Math.acos(Mth.clamp(a[0].eval(c), -1.0, 1.0))));
      def("atan", 1, (c, a) -> deg(Math.atan(a[0].eval(c))));
      def("atan2", 2, (c, a) -> deg(Math.atan2(a[0].eval(c), a[1].eval(c))));
      def("ceil", 1, (c, a) -> Math.ceil(a[0].eval(c)));
      def("floor", 1, (c, a) -> Math.floor(a[0].eval(c)));
      def("trunc", 1, (c, a) -> {
         double x = a[0].eval(c);
         return x < 0.0 ? Math.ceil(x) : Math.floor(x);
      });
      def("round", 1, (c, a) -> {
         double x = a[0].eval(c);
         return Math.signum(x) * Math.floor(Math.abs(x) + 0.5);
      });
      def("sqrt", 1, (c, a) -> finite(Math.sqrt(a[0].eval(c))));
      def("exp", 1, (c, a) -> Math.exp(a[0].eval(c)));
      def("ln", 1, (c, a) -> finite(Math.log(a[0].eval(c))));
      def("pow", 2, (c, a) -> finite(Math.pow(a[0].eval(c), a[1].eval(c))));
      def("mod", 2, (c, a) -> {
         double y = a[1].eval(c);
         return y == 0.0 ? 0.0 : finite(a[0].eval(c) % y);
      });
      def("min", 2, (c, a) -> Math.min(a[0].eval(c), a[1].eval(c)));
      def("max", 2, (c, a) -> Math.max(a[0].eval(c), a[1].eval(c)));
      def("clamp", 3, (c, a) -> {
         double x = a[0].eval(c);
         double lo = a[1].eval(c);
         double hi = a[2].eval(c);
         return x < lo ? lo : (x > hi ? hi : x);
      });
      def("sign", 1, (c, a) -> a[0].eval(c) < 0.0 ? -1.0 : 1.0);
      def("copy_sign", 2, (c, a) -> Math.copySign(a[0].eval(c), a[1].eval(c)));
      def("lerp", 3, (c, a) -> {
         double s = a[0].eval(c);
         double e = a[1].eval(c);
         return s + (e - s) * a[2].eval(c);
      });
      def("inverse_lerp", 3, (c, a) -> {
         double s = a[0].eval(c);
         double e = a[1].eval(c);
         double v = a[2].eval(c);
         return e == s ? 0.0 : (v - s) / (e - s);
      });
      def("lerprotate", 3, (c, a) -> {
         double s = minAngle(a[0].eval(c));
         double e = minAngle(a[1].eval(c));
         double diff = minAngle(e - s);
         return s + diff * a[2].eval(c);
      });
      def("min_angle", 1, (c, a) -> minAngle(a[0].eval(c)));
      def("hermite_blend", 1, (c, a) -> {
         double t = a[0].eval(c);
         return 3.0 * t * t - 2.0 * t * t * t;
      });
      def("pi", 0, (c, a) -> Math.PI);
      def("to_deg", 1, (c, a) -> deg(a[0].eval(c)));
      def("to_rad", 1, (c, a) -> rad(a[0].eval(c)));
      def("random", 2, (c, a) -> {
         double lo = a[0].eval(c);
         double hi = a[1].eval(c);
         return lo + (hi - lo) * ThreadLocalRandom.current().nextDouble();
      });
      def("random_integer", 2, (c, a) -> randomInt(a[0].eval(c), a[1].eval(c)));
      def("die_roll", 3, (c, a) -> {
         int n = (int)a[0].eval(c);
         double lo = a[1].eval(c);
         double hi = a[2].eval(c);
         double sum = 0.0;

         for (int i = 0; i < n && i < 1024; i++) {
            sum += lo + (hi - lo) * ThreadLocalRandom.current().nextDouble();
         }

         return sum;
      });
      def("die_roll_integer", 3, (c, a) -> {
         int n = (int)a[0].eval(c);
         double lo = a[1].eval(c);
         double hi = a[2].eval(c);
         double sum = 0.0;

         for (int i = 0; i < n && i < 1024; i++) {
            sum += randomInt(lo, hi);
         }

         return sum;
      });

      for (String family : new String[]{"quad", "cubic", "quart", "quint", "sine", "expo", "circ", "back", "elastic", "bounce"}) {
         for (String mode : new String[]{"in", "out", "in_out"}) {
            String name = "ease_" + mode + "_" + family;
            def(name, 3, (c, a) -> {
               double s = a[0].eval(c);
               double e = a[1].eval(c);
               double t = Mth.clamp(a[2].eval(c), 0.0, 1.0);
               return s + (e - s) * ease(mode, family, t);
            });
         }
      }
   }

   private record Def(int minArgs, MolangMath.Fn fn) {
   }

   @FunctionalInterface
   public interface Fn {
      double call(MolangContext var1, Node[] var2);
   }
}
