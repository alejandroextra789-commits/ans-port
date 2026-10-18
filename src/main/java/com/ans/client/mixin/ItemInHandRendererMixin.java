package com.ans.client.mixin;

import com.ans.client.mob.player.FirstPerson;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
   @Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
   private void ans$firstPerson(float frameInterp, PoseStack poseStack, SubmitNodeCollector collector, LocalPlayer player, int lightCoords, CallbackInfo ci) {
      if (FirstPerson.submit((ItemInHandRenderer)this, frameInterp, poseStack, collector, player, lightCoords)) {
         ci.cancel();
      }
   }
}
