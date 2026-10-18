package com.ans.client.bedrock;

import java.util.Arrays;
import java.util.Locale;

public final class BonePattern {
   private final String[] parts;
   private final boolean leadingStar;
   private final boolean trailingStar;
   private final String source;

   public BonePattern(String pattern) {
      this.source = pattern.toLowerCase(Locale.ROOT);
      this.leadingStar = this.source.startsWith("*");
      this.trailingStar = this.source.endsWith("*");
      this.parts = Arrays.stream(this.source.split("\\*")).filter(s -> !s.isEmpty()).toArray(String[]::new);
   }

   public boolean matches(String bone) {
      if (this.parts.length == 0) {
         return true;
      }

      if (!this.leadingStar && !this.trailingStar && this.parts.length == 1) {
         return bone.equals(this.source);
      }

      int pos = 0;

      for (int i = 0; i < this.parts.length; i++) {
         String p = this.parts[i];
         if (i == 0 && !this.leadingStar) {
            if (!bone.startsWith(p)) {
               return false;
            }

            pos = p.length();
         } else {
            int found = bone.indexOf(p, pos);
            if (found < 0) {
               return false;
            }

            pos = found + p.length();
         }
      }

      return this.trailingStar || pos == bone.length() || this.parts.length > 0 && bone.endsWith(this.parts[this.parts.length - 1]);
   }

   @Override
   public String toString() {
      return this.source;
   }
}
