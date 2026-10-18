package com.ans.client.molang;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class MolangParser {
   private final String src;
   private final List<MolangParser.Tok> toks = new ArrayList<>();
   private int pos;
   private static final MolangContext FOLD = new MolangContext(new MolangScope());

   private MolangParser(String src) {
      this.src = src;
      this.tokenize();
   }

   static Node parse(String src) {
      return new MolangParser(src).program();
   }

   private void tokenize() {
      String s = this.src;
      int i = 0;
      int n = s.length();

      while (i < n) {
         char ch = s.charAt(i);
         if (Character.isWhitespace(ch)) {
            i++;
         } else if (Character.isDigit(ch) || ch == '.' && i + 1 < n && Character.isDigit(s.charAt(i + 1))) {
            int st = i;

            while (i < n && Character.isDigit(s.charAt(i))) {
               i++;
            }

            if (i < n && s.charAt(i) == '.') {
               i++;

               while (i < n && Character.isDigit(s.charAt(i))) {
                  i++;
               }
            }

            if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
               int save = i++;
               if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                  i++;
               }

               if (i < n && Character.isDigit(s.charAt(i))) {
                  while (i < n && Character.isDigit(s.charAt(i))) {
                     i++;
                  }
               } else {
                  i = save;
               }
            }

            String num = s.substring(st, i);
            if (i < n && (s.charAt(i) == 'f' || s.charAt(i) == 'F') && (i + 1 >= n || !Character.isLetterOrDigit(s.charAt(i + 1)))) {
               i++;
            }

            this.toks.add(new MolangParser.Tok(MolangParser.T.NUM, num));
         } else if (ch == '\'') {
            int end = s.indexOf(39, i + 1);
            if (end < 0) {
               throw this.error("unterminated string");
            }

            this.toks.add(new MolangParser.Tok(MolangParser.T.STR, s.substring(i + 1, end)));
            i = end + 1;
         } else if (!Character.isLetter(ch) && ch != '_') {
            String two = i + 1 < n ? s.substring(i, i + 2) : "";
            if (!two.equals("->")
               && !two.equals("&&")
               && !two.equals("||")
               && !two.equals("==")
               && !two.equals("!=")
               && !two.equals("<=")
               && !two.equals(">=")
               && !two.equals("??")) {
               if ("-+*/%!?:=<>(){}[],;".indexOf(ch) < 0) {
                  throw this.error("bad character '" + ch + "'");
               }

               this.toks.add(new MolangParser.Tok(MolangParser.T.OP, String.valueOf(ch)));
               i++;
            } else {
               this.toks.add(new MolangParser.Tok(MolangParser.T.OP, two));
               i += 2;
            }
         } else {
            int st = i;

            while (true) {
               if (i < n) {
                  char c = s.charAt(i);
                  if (Character.isLetterOrDigit(c) || c == '_') {
                     i++;
                     continue;
                  }

                  if (c == '.' && i + 1 < n && (Character.isLetter(s.charAt(i + 1)) || s.charAt(i + 1) == '_')) {
                     i++;
                     continue;
                  }
               }

               this.toks.add(new MolangParser.Tok(MolangParser.T.ID, s.substring(st, i)));
               break;
            }
         }
      }

      this.toks.add(new MolangParser.Tok(MolangParser.T.EOF, ""));
   }

   private MolangParser.Tok peek() {
      return this.toks.get(this.pos);
   }

   private MolangParser.Tok peek(int ahead) {
      return this.toks.get(Math.min(this.pos + ahead, this.toks.size() - 1));
   }

   private MolangParser.Tok next() {
      return this.toks.get(this.pos++);
   }

   private boolean isOp(MolangParser.Tok t, String op) {
      return t.type == MolangParser.T.OP && t.text.equals(op);
   }

   private boolean at(String op) {
      return this.isOp(this.peek(), op);
   }

   private void expect(String op) {
      MolangParser.Tok t = this.next();
      if (!this.isOp(t, op)) {
         throw this.error("expected '" + op + "' but found '" + t.text + "'");
      }
   }

   private IllegalArgumentException error(String msg) {
      return new IllegalArgumentException(msg + " in \"" + this.src + "\"");
   }

   private Node program() {
      List<Node> statements = this.statements(null);
      if (this.peek().type != MolangParser.T.EOF) {
         throw this.error("unexpected '" + this.peek().text + "'");
      }

      boolean simple = this.src.indexOf(59) < 0;
      return new Nodes.Program(statements, simple);
   }

   private List<Node> statements(String end) {
      List<Node> out = new ArrayList<>();

      while (true) {
         MolangParser.Tok t = this.peek();
         if (t.type == MolangParser.T.EOF || end != null && this.isOp(t, end)) {
            return out;
         }

         if (this.isOp(t, ";")) {
            this.next();
         } else {
            out.add(this.statement());
            if (this.at(";")) {
               this.next();
            }
         }
      }
   }

   private Node statement() {
      MolangParser.Tok t = this.peek();
      if (t.type == MolangParser.T.ID) {
         String low = t.text.toLowerCase(Locale.ROOT);
         switch (low) {
            case "return":
               this.next();
               return new Nodes.Return(this.expr());
            case "break":
               this.next();
               return new Nodes.Flow(2);
            case "continue":
               this.next();
               return new Nodes.Flow(3);
            case "loop":
               if (this.isOp(this.peek(1), "(")) {
                  this.next();
                  this.expect("(");
                  Node count = this.expr();
                  this.expect(",");
                  Node body = this.expr();
                  this.expect(")");
                  return new Nodes.Loop(count, body);
               }
         }
      }

      return this.expr();
   }

   private Node expr() {
      Node left = this.ternary();
      if (this.at("=")) {
         this.next();
         Node rhs = this.expr();
         if (left instanceof Nodes.Var v) {
            return new Nodes.Assign(v, rhs);
         } else {
            throw this.error("can only assign to variable.* or temp.*");
         }
      } else {
         return left;
      }
   }

   private Node ternary() {
      Node cond = this.binary(0);
      if (this.at("?")) {
         this.next();
         Node a = this.ternary();
         if (this.at(":")) {
            this.next();
            Node b = this.ternary();
            return fold(new Nodes.Ternary(cond, a, b));
         } else {
            return new Nodes.Ternary(cond, a, null);
         }
      } else {
         return cond;
      }
   }

   private static int prec(MolangParser.Tok t) {
      if (t.type != MolangParser.T.OP) {
         return -1;
      }

      return switch (t.text) {
         case "??" -> 3;
         case "||" -> 4;
         case "&&" -> 5;
         case "==", "!=" -> 6;
         case "<", "<=", ">", ">=" -> 7;
         case "+", "-" -> 8;
         case "*", "/", "%" -> 9;
         default -> -1;
      };
   }

   private Node binary(int minPrec) {
      Node left = this.unary();

      while (true) {
         MolangParser.Tok t = this.peek();
         int p = prec(t);
         if (p < 0 || p < minPrec) {
            return left;
         }

         this.next();
         Node right = this.binary(p + 1);

         left = fold(switch (t.text) {
            case "??" -> new Nodes.Coalesce(left, right);
            case "||" -> new Nodes.Or(left, right);
            case "&&" -> new Nodes.And(left, right);
            case "==" -> new Nodes.Eq(false, left, right);
            case "!=" -> new Nodes.Eq(true, left, right);
            case "<" -> new Nodes.Bin('<', left, right);
            case "<=" -> new Nodes.Bin('l', left, right);
            case ">" -> new Nodes.Bin('>', left, right);
            case ">=" -> new Nodes.Bin('g', left, right);
            default -> new Nodes.Bin(t.text.charAt(0), left, right);
         });
      }
   }

   private Node unary() {
      if (this.at("!")) {
         this.next();
         return fold(new Nodes.Not(this.unary()));
      } else if (this.at("-")) {
         this.next();
         Node x = this.unary();
         return x instanceof Nodes.Const k ? new Nodes.Const(-k.v) : new Nodes.Neg(x);
      } else if (this.at("+")) {
         this.next();
         return this.unary();
      } else {
         return this.postfix();
      }
   }

   private Node postfix() {
      Node x = this.primary();

      while (true) {
         while (!this.at("->")) {
            if (!this.at("[")) {
               return x;
            }

            this.next();
            Node index = this.expr();
            this.expect("]");
            x = x instanceof Nodes.Str s && s.s.startsWith("array.") ? new Nodes.ArrayIndex(s.s, index) : new Nodes.Const(0.0);
         }

         this.next();
         x = new Nodes.Arrow(x, this.primary());
      }
   }

   private Node primary() {
      MolangParser.Tok t = this.next();
      switch (t.type) {
         case NUM:
            try {
               return new Nodes.Const(Double.parseDouble(t.text));
            } catch (NumberFormatException e) {
               throw this.error("bad number '" + t.text + "'");
            }
         case STR:
            return new Nodes.Str(t.text);
         case ID:
            return this.identifier(normalize(t.text));
         case OP:
            if (t.text.equals("(")) {
               Node e = this.expr();
               this.expect(")");
               return e;
            } else {
               if (t.text.equals("{")) {
                  List<Node> body = this.statements("}");
                  this.expect("}");
                  return new Nodes.Block(body);
               }

               throw this.error("unexpected '" + t.text + "'");
            }
         default:
            throw this.error("unexpected end of expression");
      }
   }

   static String normalize(String raw) {
      String v = raw.toLowerCase(Locale.ROOT);
      if (v.startsWith("query.")) {
         return "q." + v.substring(6);
      } else if (v.startsWith("variable.")) {
         return "v." + v.substring(9);
      } else if (v.startsWith("temp.")) {
         return "t." + v.substring(5);
      } else {
         return v.startsWith("context.") ? "c." + v.substring(8) : v;
      }
   }

   private Node[] args() {
      this.expect("(");
      List<Node> args = new ArrayList<>();
      if (!this.at(")")) {
         while (true) {
            args.add(this.expr());
            if (!this.at(",")) {
               break;
            }

            this.next();
         }
      }

      this.expect(")");
      return args.toArray(new Node[0]);
   }

   private Node identifier(String name) {
      boolean call = this.at("(");
      switch (name) {
         case "true":
            return new Nodes.Const(1.0);
         case "false":
            return new Nodes.Const(0.0);
         case "this":
            return new Nodes.This();
         default:
            if (name.startsWith("q.")) {
               return new Nodes.Query(name.substring(2), call ? this.args() : new Node[0]);
            } else if (name.startsWith("math.")) {
               String fn = name.substring(5);
               Node[] args = call ? this.args() : new Node[0];
               if (!fn.equals("pi") && !fn.equals("e")) {
                  MolangMath.Fn f = MolangMath.get(fn, args.length);
                  if (f == null) {
                     Molang.warn("unknown function 'math." + fn + "' (reads 0) in \"" + this.src + "\"");
                     return new Nodes.Const(0.0);
                  } else {
                     Nodes.MathCall node = new Nodes.MathCall(fn, f, args);
                     return foldCall(node, fn);
                  }
               } else {
                  return new Nodes.Const(fn.equals("pi") ? Math.PI : Math.E);
               }
            } else if (call) {
               this.args();
               Molang.warn("unknown function '" + name + "' (reads 0) in \"" + this.src + "\"");
               return new Nodes.Const(0.0);
            } else if (name.startsWith("v.")) {
               return new Nodes.Var(false, MolangVars.id(name.substring(2)));
            } else if (name.startsWith("t.")) {
               return new Nodes.Var(true, MolangVars.id(name.substring(2)));
            } else if (name.startsWith("c.")) {
               return new Nodes.ContextVar(name.substring(2));
            } else if (!name.startsWith("geometry.") && !name.startsWith("texture.") && !name.startsWith("material.") && !name.startsWith("array.")) {
               Molang.warn("unknown identifier '" + name + "' (reads 0) in \"" + this.src + "\"");
               return new Nodes.Const(0.0);
            } else {
               return new Nodes.Str(name);
            }
      }
   }

   private static Node fold(Node n) {
      // Java 17-compatible replacement for the pattern-switch used by the original Java 25 build.
      boolean constant = false;
      if (n instanceof Nodes.Bin b) {
         constant = b.a.isConstant() && b.b.isConstant() && !b.a.stringy() && !b.b.stringy();
      } else if (n instanceof Nodes.Eq e) {
         constant = e.a.isConstant() && e.b.isConstant();
      } else if (n instanceof Nodes.And a) {
         constant = a.a.isConstant() && a.b.isConstant();
      } else if (n instanceof Nodes.Or o) {
         constant = o.a.isConstant() && o.b.isConstant();
      } else if (n instanceof Nodes.Not x) {
         constant = x.x.isConstant();
      } else if (n instanceof Nodes.Ternary t) {
         constant = t.cond.isConstant() && !t.cond.stringy();
      }
      if (!constant) {
         return n;
      } else if (n instanceof Nodes.Ternary t) {
         Node chosen = Nodes.truthy(t.cond.eval(FOLD)) ? t.a : t.b;
         return chosen != null ? chosen : new Nodes.Const(0.0);
      } else {
         return new Nodes.Const(n.eval(FOLD));
      }
   }

   private static Node foldCall(Nodes.MathCall call, String fn) {
      if (!fn.startsWith("random") && !fn.startsWith("die_roll")) {
         for (Node a : call.args) {
            if (!a.isConstant() || a.stringy()) {
               return call;
            }
         }

         return new Nodes.Const(call.eval(FOLD));
      } else {
         return call;
      }
   }

   private enum T {
      NUM,
      STR,
      ID,
      OP,
      EOF;
   }

   private record Tok(MolangParser.T type, String text) {
   }
}
