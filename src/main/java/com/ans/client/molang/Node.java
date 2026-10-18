package com.ans.client.molang;


public abstract class Node {
   public abstract double eval(MolangContext var1);

   public Object value(MolangContext c) {
      return this.eval(c);
   }

   public Object valueOrNull(MolangContext c) {
      return this.value(c);
   }

   public boolean stringy() {
      return false;
   }

   public boolean isConstant() {
      return false;
   }

   public void exec(MolangContext c) {
      this.eval(c);
   }

   public boolean usesTemps() {
      return false;
   }
}
