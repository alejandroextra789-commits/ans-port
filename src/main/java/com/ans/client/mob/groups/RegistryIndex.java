package com.ans.client.mob.groups;

import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

/** Index of a registry value's path within an ordered list of names (0 when unknown). */
final class RegistryIndex {
   private RegistryIndex() {
   }

   static <T> int of(List<String> order, Registry<T> registry, T value) {
      if (value == null) {
         return 0;
      }
      ResourceLocation key = registry.getKey(value);
      return key == null ? 0 : Math.max(0, order.indexOf(key.getPath()));
   }
}
