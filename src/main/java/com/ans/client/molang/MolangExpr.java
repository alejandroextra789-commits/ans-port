package com.ans.client.molang;

public final class MolangExpr {
   private final String source;
   private final Node root;

   MolangExpr(String source, Node root) {
      this.source = source;
      this.root = root;
   }

   public static MolangExpr constant(double v) {
      return new MolangExpr(Double.toString(v), new Nodes.Const(v));
   }

   public double eval(MolangContext c) {
      try {
         return this.root.eval(c);
      } catch (RuntimeException e) {
         Molang.warn("Molang runtime error in \"" + this.source + "\": " + e);
         return 0.0;
      }
   }

   public Object value(MolangContext c) {
      try {
         return this.root.value(c);
      } catch (RuntimeException e) {
         Molang.warn("Molang runtime error in \"" + this.source + "\": " + e);
         return 0.0;
      }
   }

   public void exec(MolangContext c) {
      try {
         this.root.exec(c);
      } catch (RuntimeException e) {
         Molang.warn("Molang runtime error in \"" + this.source + "\": " + e);
      }
   }

   public boolean test(MolangContext c) {
      return Nodes.truthy(this.eval(c));
   }

   public boolean isConstant() {
      return this.root.isConstant();
   }

   public double constantValue() {
      return this.root.isConstant() ? this.root.eval(MolangContext.current()) : 0.0;
   }

   public String source() {
      return this.source;
   }

   @Override
   public String toString() {
      return this.source;
   }
}
