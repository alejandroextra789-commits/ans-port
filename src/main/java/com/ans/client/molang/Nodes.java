package com.ans.client.molang;

import java.util.List;
import java.util.Objects;

final class Nodes {
   private Nodes() {
   }

   static boolean truthy(double v) {
      return v != 0.0 && !Double.isNaN(v);
   }

   static double toNum(Object o) {
      return o instanceof Number n ? n.doubleValue() : 0.0;
   }

   static boolean equal(Object a, Object b) {
      if (a instanceof Number x && b instanceof Number y) {
         return x.doubleValue() == y.doubleValue();
      } else {
         return a instanceof String x && b instanceof String y ? x.equals(y) : Objects.equals(a, b);
      }
   }

   static final class And extends Node {
      final Node a;
      final Node b;

      And(Node a, Node b) {
         this.a = a;
         this.b = b;
      }

      @Override
      public double eval(MolangContext c) {
         return Nodes.truthy(this.a.eval(c)) && Nodes.truthy(this.b.eval(c)) ? 1.0 : 0.0;
      }

      @Override
      public boolean usesTemps() {
         return this.a.usesTemps() || this.b.usesTemps();
      }
   }

   static final class ArrayIndex extends Node {
      final String array;
      final Node index;

      ArrayIndex(String array, Node index) {
         this.array = array;
         this.index = index;
      }

      @Override
      public double eval(MolangContext c) {
         return 0.0;
      }

      @Override
      public Object value(MolangContext c) {
         if (c.arrays == null) {
            return 0.0;
         } else {
            List<String> list = c.arrays.get(this.array);
            if (list != null && !list.isEmpty()) {
               int i = (int)Math.floor(this.index.eval(c));
               int n = list.size();
               return list.get((i % n + n) % n);
            } else {
               return 0.0;
            }
         }
      }

      @Override
      public boolean stringy() {
         return true;
      }

      @Override
      public boolean usesTemps() {
         return this.index.usesTemps();
      }
   }

   static final class Arrow extends Node {
      final Node left;
      final Node right;

      Arrow(Node left, Node right) {
         this.left = left;
         this.right = right;
      }

      @Override
      public double eval(MolangContext c) {
         return this.left.value(c) instanceof MolangActorRef ref ? ref.eval(this.right) : 0.0;
      }

      @Override
      public Object value(MolangContext c) {
         return this.left.value(c) instanceof MolangActorRef ref ? ref.value(this.right) : 0.0;
      }

      @Override
      public Object valueOrNull(MolangContext c) {
         return this.left.value(c) instanceof MolangActorRef ref ? ref.valueOrNull(this.right) : null;
      }

      @Override
      public boolean stringy() {
         return true;
      }

      @Override
      public boolean usesTemps() {
         return this.left.usesTemps();
      }
   }

   static final class Assign extends Node {
      final Nodes.Var target;
      final Node value;
      private int[] members = new int[0];
      private int membersAt = -1;

      Assign(Nodes.Var target, Node value) {
         this.target = target;
         this.value = value;
      }

      @Override
      public double eval(MolangContext c) {
         MolangScope scope = this.target.temp ? c.temps : c.vars;
         if (this.value.stringy()) {
            Object v = this.value.value(c);
            if (v instanceof MolangStruct s) {
               s.assignTo(scope, MolangVars.name(this.target.slot));
               scope.set(this.target.slot, 0.0);
               return 0.0;
            }

            scope.set(this.target.slot, v);
            if (this.value instanceof Nodes.Var src) {
               this.copyMembers(src, src.temp ? c.temps : c.vars, scope);
            }
         } else {
            double v = this.value.eval(c);
            scope.set(this.target.slot, Double.isNaN(v) ? 0.0 : v);
         }

         return 0.0;
      }

      private void copyMembers(Nodes.Var src, MolangScope from, MolangScope to) {
         int size = MolangVars.size();
         if (size != this.membersAt) {
            this.members = MolangVars.memberPairs(src.slot, this.target.slot);
            this.membersAt = size;
         }

         for (int i = 0; i + 1 < this.members.length; i += 2) {
            Object v = from.getOrNull(this.members[i]);
            if (v != null) {
               to.set(this.members[i + 1], v);
            }
         }
      }

      @Override
      public boolean usesTemps() {
         return this.target.temp || this.value.usesTemps();
      }
   }

   static final class Bin extends Node {
      final char op;
      final Node a;
      final Node b;

      Bin(char op, Node a, Node b) {
         this.op = op;
         this.a = a;
         this.b = b;
      }

      @Override
      public double eval(MolangContext c) {
         double x = this.a.eval(c);
         double y = this.b.eval(c);

         return switch (this.op) {
            case '%' -> y == 0.0 ? 0.0 : x % y;
            case '*' -> x * y;
            case '+' -> x + y;
            case '-' -> x - y;
            case '/' -> y == 0.0 ? 0.0 : x / y;
            case '<' -> x < y ? 1.0 : 0.0;
            case '>' -> x > y ? 1.0 : 0.0;
            case 'g' -> x >= y ? 1.0 : 0.0;
            case 'l' -> x <= y ? 1.0 : 0.0;
            default -> 0.0;
         };
      }

      @Override
      public boolean usesTemps() {
         return this.a.usesTemps() || this.b.usesTemps();
      }
   }

   static final class Block extends Node {
      final Node[] statements;

      Block(List<Node> statements) {
         this.statements = statements.toArray(new Node[0]);
      }

      @Override
      public double eval(MolangContext c) {
         this.exec(c);
         return 0.0;
      }

      @Override
      public void exec(MolangContext c) {
         for (Node s : this.statements) {
            s.exec(c);
            if (c.flow != 0) {
               return;
            }
         }
      }

      @Override
      public boolean usesTemps() {
         for (Node s : this.statements) {
            if (s.usesTemps()) {
               return true;
            }
         }

         return false;
      }
   }

   static final class Coalesce extends Node {
      final Node a;
      final Node b;

      Coalesce(Node a, Node b) {
         this.a = a;
         this.b = b;
      }

      @Override
      public double eval(MolangContext c) {
         Object v = this.a.valueOrNull(c);
         return v != null ? Nodes.toNum(v) : this.b.eval(c);
      }

      @Override
      public Object value(MolangContext c) {
         Object v = this.a.valueOrNull(c);
         return v != null ? v : this.b.value(c);
      }

      @Override
      public boolean stringy() {
         return this.a.stringy() || this.b.stringy();
      }

      @Override
      public boolean usesTemps() {
         return this.a.usesTemps() || this.b.usesTemps();
      }
   }

   static final class Const extends Node {
      final double v;

      Const(double v) {
         this.v = v;
      }

      @Override
      public double eval(MolangContext c) {
         return this.v;
      }

      @Override
      public boolean isConstant() {
         return true;
      }

      @Override
      public String toString() {
         return Double.toString(this.v);
      }
   }

   static final class ContextVar extends Node {
      final String name;

      ContextVar(String name) {
         this.name = name;
      }

      @Override
      public double eval(MolangContext c) {
         Object a = c.attachable == null ? null : c.attachable.value(this.name);
         if (a != null) {
            return a instanceof Number n ? n.doubleValue() : 0.0;
         } else {
            return c.contextVars.getOrDefault(this.name, 0.0);
         }
      }

      @Override
      public Object value(MolangContext c) {
         Object a = c.attachable == null ? null : c.attachable.value(this.name);
         return a != null ? a : c.contextVars.getOrDefault(this.name, 0.0);
      }

      @Override
      public Object valueOrNull(MolangContext c) {
         Object a = c.attachable == null ? null : c.attachable.value(this.name);
         if (a != null) {
            return a;
         } else {
            return c.contextVars.containsKey(this.name) ? c.contextVars.getDouble(this.name) : null;
         }
      }

      @Override
      public boolean stringy() {
         return true;
      }
   }

   static final class Eq extends Node {
      final boolean negate;
      final Node a;
      final Node b;
      final boolean strings;

      Eq(boolean negate, Node a, Node b) {
         this.negate = negate;
         this.a = a;
         this.b = b;
         this.strings = a.stringy() || b.stringy();
      }

      @Override
      public double eval(MolangContext c) {
         boolean eq = this.strings ? Nodes.equal(this.a.value(c), this.b.value(c)) : this.a.eval(c) == this.b.eval(c);
         return eq != this.negate ? 1.0 : 0.0;
      }

      @Override
      public boolean usesTemps() {
         return this.a.usesTemps() || this.b.usesTemps();
      }
   }

   static final class Flow extends Node {
      final int flow;

      Flow(int flow) {
         this.flow = flow;
      }

      @Override
      public double eval(MolangContext c) {
         c.flow = this.flow;
         return 0.0;
      }
   }

   static final class Loop extends Node {
      final Node count;
      final Node body;

      Loop(Node count, Node body) {
         this.count = count;
         this.body = body;
      }

      @Override
      public double eval(MolangContext c) {
         int n = (int)Math.min(1024.0, Math.max(0.0, this.count.eval(c)));

         for (int i = 0; i < n; i++) {
            this.body.exec(c);
            if (c.flow == 2) {
               c.flow = 0;
               break;
            }

            if (c.flow == 3) {
               c.flow = 0;
            }

            if (c.flow == 1) {
               break;
            }
         }

         return 0.0;
      }

      @Override
      public boolean usesTemps() {
         return this.count.usesTemps() || this.body.usesTemps();
      }
   }

   static final class MathCall extends Node {
      final String name;
      final MolangMath.Fn fn;
      final Node[] args;

      MathCall(String name, MolangMath.Fn fn, Node[] args) {
         this.name = name;
         this.fn = fn;
         this.args = args;
      }

      @Override
      public double eval(MolangContext c) {
         return this.fn.call(c, this.args);
      }

      @Override
      public boolean usesTemps() {
         for (Node a : this.args) {
            if (a.usesTemps()) {
               return true;
            }
         }

         return false;
      }
   }

   static final class Neg extends Node {
      final Node x;

      Neg(Node x) {
         this.x = x;
      }

      @Override
      public double eval(MolangContext c) {
         return -this.x.eval(c);
      }

      @Override
      public boolean usesTemps() {
         return this.x.usesTemps();
      }
   }

   static final class Not extends Node {
      final Node x;

      Not(Node x) {
         this.x = x;
      }

      @Override
      public double eval(MolangContext c) {
         return Nodes.truthy(this.x.eval(c)) ? 0.0 : 1.0;
      }

      @Override
      public boolean usesTemps() {
         return this.x.usesTemps();
      }
   }

   static final class Or extends Node {
      final Node a;
      final Node b;

      Or(Node a, Node b) {
         this.a = a;
         this.b = b;
      }

      @Override
      public double eval(MolangContext c) {
         return !Nodes.truthy(this.a.eval(c)) && !Nodes.truthy(this.b.eval(c)) ? 0.0 : 1.0;
      }

      @Override
      public boolean usesTemps() {
         return this.a.usesTemps() || this.b.usesTemps();
      }
   }

   static final class Program extends Node {
      final Node[] statements;
      final boolean simple;
      final boolean temps;

      Program(List<Node> statements, boolean simple) {
         this.statements = statements.toArray(new Node[0]);
         this.simple = simple && this.statements.length == 1 && !(this.statements[0] instanceof Nodes.Return);
         boolean t = false;

         for (Node s : this.statements) {
            t |= s.usesTemps();
         }

         this.temps = t;
      }

      @Override
      public double eval(MolangContext c) {
         if (this.temps && !c.keepTemps) {
            c.temps.clear();
         }

         if (this.simple) {
            double v = this.statements[0].eval(c);
            Object r = consumeFlow(c);
            return r == null ? v : Nodes.toNum(r);
         } else {
            Object r = this.run(c);
            return r == null ? 0.0 : Nodes.toNum(r);
         }
      }

      @Override
      public Object value(MolangContext c) {
         if (this.temps && !c.keepTemps) {
            c.temps.clear();
         }

         if (this.simple) {
            Object v = this.statements[0].value(c);
            Object r = consumeFlow(c);
            return r == null ? v : r;
         } else {
            Object r = this.run(c);
            return r == null ? 0.0 : r;
         }
      }

      @Override
      public void exec(MolangContext c) {
         if (this.temps && !c.keepTemps) {
            c.temps.clear();
         }

         if (this.simple) {
            this.statements[0].exec(c);
            consumeFlow(c);
         } else {
            this.run(c);
         }
      }

      private static Object consumeFlow(MolangContext c) {
         if (c.flow == 0) {
            return null;
         }

         Object r = c.flow == 1 ? c.returned : null;
         c.flow = 0;
         c.returned = null;
         return r;
      }

      private Object run(MolangContext c) {
         c.flow = 0;
         c.returned = null;

         for (Node s : this.statements) {
            s.exec(c);
            if (c.flow != 0) {
               break;
            }
         }

         Object r = c.flow == 1 ? c.returned : null;
         c.flow = 0;
         c.returned = null;
         return r;
      }

      @Override
      public boolean stringy() {
         return this.simple ? this.statements[0].stringy() : true;
      }

      @Override
      public boolean isConstant() {
         return this.simple && this.statements[0].isConstant();
      }
   }

   static final class Query extends Node {
      final String name;
      final Node[] args;
      private volatile MolangQueries.Bound bound;
      private volatile boolean resolved;

      Query(String name, Node[] args) {
         this.name = name;
         this.args = args;
      }

      private MolangQueries.Bound bound() {
         if (!this.resolved) {
            this.bound = MolangQueries.bind(this.name, this.args);
            this.resolved = true;
         }

         return this.bound;
      }

      @Override
      public double eval(MolangContext c) {
         MolangQueries.Bound b = this.bound();
         return b == null ? 0.0 : b.eval(c);
      }

      @Override
      public Object value(MolangContext c) {
         MolangQueries.Bound b = this.bound();
         return b == null ? 0.0 : b.value(c);
      }

      @Override
      public boolean stringy() {
         return MolangQueries.isStringQuery(this.name);
      }

      @Override
      public boolean usesTemps() {
         for (Node a : this.args) {
            if (a.usesTemps()) {
               return true;
            }
         }

         return false;
      }

      @Override
      public String toString() {
         return "q." + this.name;
      }
   }

   static final class Return extends Node {
      final Node x;

      Return(Node x) {
         this.x = x;
      }

      @Override
      public double eval(MolangContext c) {
         this.exec(c);
         return 0.0;
      }

      @Override
      public void exec(MolangContext c) {
         c.returned = this.x.stringy() ? this.x.value(c) : this.x.eval(c);
         c.flow = 1;
      }

      @Override
      public boolean usesTemps() {
         return this.x.usesTemps();
      }
   }

   static final class Str extends Node {
      final String s;

      Str(String s) {
         this.s = s;
      }

      @Override
      public double eval(MolangContext c) {
         return 0.0;
      }

      @Override
      public Object value(MolangContext c) {
         return this.s;
      }

      @Override
      public boolean stringy() {
         return true;
      }

      @Override
      public boolean isConstant() {
         return true;
      }

      @Override
      public String toString() {
         return "'" + this.s + "'";
      }
   }

   static final class Ternary extends Node {
      final Node cond;
      final Node a;
      final Node b;

      Ternary(Node cond, Node a, Node b) {
         this.cond = cond;
         this.a = a;
         this.b = b;
      }

      @Override
      public double eval(MolangContext c) {
         if (Nodes.truthy(this.cond.eval(c))) {
            return this.a.eval(c);
         } else {
            return this.b != null ? this.b.eval(c) : 0.0;
         }
      }

      @Override
      public Object value(MolangContext c) {
         if (Nodes.truthy(this.cond.eval(c))) {
            return this.a.value(c);
         } else {
            return this.b != null ? this.b.value(c) : 0.0;
         }
      }

      @Override
      public void exec(MolangContext c) {
         if (Nodes.truthy(this.cond.eval(c))) {
            this.a.exec(c);
         } else if (this.b != null) {
            this.b.exec(c);
         }
      }

      @Override
      public boolean stringy() {
         return this.a.stringy() || this.b != null && this.b.stringy();
      }

      @Override
      public boolean usesTemps() {
         return this.cond.usesTemps() || this.a.usesTemps() || this.b != null && this.b.usesTemps();
      }
   }

   static final class This extends Node {
      @Override
      public double eval(MolangContext c) {
         return c.thisValue;
      }
   }

   static final class Var extends Node {
      final boolean temp;
      final int slot;

      Var(boolean temp, int slot) {
         this.temp = temp;
         this.slot = slot;
      }

      private MolangScope scope(MolangContext c) {
         return this.temp ? c.temps : c.vars;
      }

      @Override
      public double eval(MolangContext c) {
         return this.scope(c).getNum(this.slot);
      }

      @Override
      public Object value(MolangContext c) {
         return this.scope(c).get(this.slot);
      }

      @Override
      public Object valueOrNull(MolangContext c) {
         return this.scope(c).getOrNull(this.slot);
      }

      @Override
      public boolean stringy() {
         return true;
      }

      @Override
      public boolean usesTemps() {
         return this.temp;
      }

      @Override
      public String toString() {
         return (this.temp ? "t." : "v.") + MolangVars.name(this.slot);
      }
   }
}
