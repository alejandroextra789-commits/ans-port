package com.ans.client.bedrock;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;

public interface BedrockWalk {
   boolean ENABLED = !"false".equalsIgnoreCase(System.getProperty("ans.bedrockWalk", "true"));

   WalkAnimationState ans$bedrockWalk();

   static WalkAnimationState of(LivingEntity e) {
      if (ENABLED && e instanceof BedrockWalk w) {
         return w.ans$bedrockWalk();
      } else {
         return e == null ? null : e.walkAnimation;
      }
   }
}
