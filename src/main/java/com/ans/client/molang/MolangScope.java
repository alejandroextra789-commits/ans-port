package com.ans.client.molang;

import java.util.Arrays;
import java.util.Locale;

public final class MolangScope {
   private double[] nums = new double[32];
   private Object[] objs = new Object[32];
   private boolean[] defined = new boolean[32];
   private boolean dirty;

   private void ensure(int slot) {
      if (slot >= this.nums.length) {
         int size = Math.max(slot + 1, this.nums.length * 2);
         this.nums = Arrays.copyOf(this.nums, size);
         this.objs = Arrays.copyOf(this.objs, size);
         this.defined = Arrays.copyOf(this.defined, size);
      }
   }

   public boolean isDefined(int slot) {
      return slot < this.defined.length && this.defined[slot];
   }

   public double getNum(int slot) {
      return slot < this.nums.length ? this.nums[slot] : 0.0;
   }

   public Object get(int slot) {
      if (slot < this.nums.length && this.defined[slot]) {
         Object o = this.objs[slot];
         return o != null ? o : this.nums[slot];
      } else {
         return 0.0;
      }
   }

   public Object getOrNull(int slot) {
      if (slot < this.nums.length && this.defined[slot]) {
         Object o = this.objs[slot];
         return o != null ? o : this.nums[slot];
      } else {
         return null;
      }
   }

   public void set(int slot, double value) {
      this.ensure(slot);
      this.nums[slot] = value;
      this.objs[slot] = null;
      this.defined[slot] = true;
      this.dirty = true;
   }

   public void set(int slot, Object value) {
      if (value == null) {
         this.set(slot, 0.0);
      } else if (value instanceof Number n) {
         this.set(slot, n.doubleValue());
      } else {
         this.ensure(slot);
         this.nums[slot] = 0.0;
         this.objs[slot] = value;
         this.defined[slot] = true;
         this.dirty = true;
      }
   }

   public void clear() {
      if (this.dirty) {
         Arrays.fill(this.nums, 0.0);
         Arrays.fill(this.objs, null);
         Arrays.fill(this.defined, false);
         this.dirty = false;
      }
   }

   public String describe(String prefix) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < this.nums.length; i++) {
         if (this.defined[i]) {
            if (!sb.isEmpty()) {
               sb.append(", ");
            }

            Object v = this.get(i);
            sb.append(prefix).append(MolangVars.name(i)).append('=').append(v instanceof Double d ? String.format(Locale.ROOT, "%.3f", d) : "'" + v + "'");
         }
      }

      return sb.toString();
   }
}
