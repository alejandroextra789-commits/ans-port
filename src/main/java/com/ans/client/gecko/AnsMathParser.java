package com.ans.client.gecko;

import com.ans.client.molang.Molang;
import com.ans.client.molang.MolangExpr;
import com.geckolib.loading.math.MathParser;
import com.geckolib.loading.math.MathValue;
import com.geckolib.loading.math.MathParser.Deduplicator;

public final class AnsMathParser extends MathParser {
   public AnsMathParser() {
      super(Deduplicator.defaultImpl());
   }

   public MathValue compileMolang(String expression) {
      if (expression.isBlank()) {
         return this.compileConstant(0.0);
      }

      MolangExpr expr = Molang.compile(expression);
      return expr.isConstant() ? this.compileConstant(expr.constantValue()) : new MolangMathValue(expr);
   }
}
