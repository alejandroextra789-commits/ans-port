package com.ans.client.mixin;

import com.ans.client.render.UiRender;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bridges the inventory entity preview to the runtime's UI animation context.
 * Minecraft 1.20.1 renders the preview directly and has no extractRenderState method.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
   @Inject(method = "renderEntityInInventory", at = @At("HEAD"))
   private static void ans$uiRenderBegin(CallbackInfo ci) {
      UiRender.begin();
   }

   @Inject(method = "renderEntityInInventory", at = @At("RETURN"))
   private static void ans$uiRenderEnd(CallbackInfo ci) {
      UiRender.end();
   }
}
