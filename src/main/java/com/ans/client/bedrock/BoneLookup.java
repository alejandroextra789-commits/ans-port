package com.ans.client.bedrock;

import java.util.HashMap;
import java.util.Map;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

/** GeckoLib 4 replacements for the GL5 BakedGeoModel.boneLookup() and isMissingno(). */
public final class BoneLookup {
   private BoneLookup() {
   }

   /** Name -> bone map built from the model's flat bone list. Empty if model is null. */
   public static Map<String, GeoBone> of(BakedGeoModel model) {
      Map<String, GeoBone> out = new HashMap<>();
      if (model != null) {
         for (Object o : model.getBones()) {
            GeoBone b = (GeoBone)o;
            out.put(b.getName(), b);
         }
      }
      return out;
   }

   /** GL5 "missingno" meant the model failed to load: null, or no bones at all. */
   public static boolean isMissing(BakedGeoModel model) {
      return model == null || model.getBones().isEmpty();
   }
}
