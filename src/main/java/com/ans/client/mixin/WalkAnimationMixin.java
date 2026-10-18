package com.ans.client.mixin;

import com.ans.client.bedrock.BedrockWalk;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.frog.Frog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Frog.class, Camel.class})
public abstract class WalkAnimationMixin implements BedrockWalk {
   @Unique
   private final WalkAnimationState ans$walk = new WalkAnimationState();

   @Override
   public WalkAnimationState ans$bedrockWalk() {
      return this.ans$walk;
   }

   @Inject(method = "updateWalkAnimation", at = @At("HEAD"))
   private void ans$updateBedrockWalk(float distance, CallbackInfo ci) {
      this.ans$walk.update(Math.min(distance * 4.0F, 1.0F) * (((LivingEntity)this).isBaby() ? 3.0F : 1.0F), 0.4F);
   }
}
