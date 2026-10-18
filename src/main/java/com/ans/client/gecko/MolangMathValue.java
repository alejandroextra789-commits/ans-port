package com.ans.client.gecko;

import com.ans.client.molang.MolangContext;
import com.ans.client.molang.MolangExpr;
import com.geckolib.animation.state.ControllerState;
import com.geckolib.loading.math.MathValue;

public record MolangMathValue(MolangExpr expr) implements MathValue {
   public double get(ControllerState controllerState) {
      return this.expr.eval(MolangContext.current());
   }

   public boolean isMutable() {
      return true;
   }

   @Override
   public String toString() {
      return "molang(" + this.expr.source() + ")";
   }
}
