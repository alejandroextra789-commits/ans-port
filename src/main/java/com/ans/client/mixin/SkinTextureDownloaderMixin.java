package com.ans.client.mixin;

import com.ans.client.mob.player.ExpressionSkins;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkinTextureDownloader.class)
public abstract class SkinTextureDownloaderMixin {
   @Inject(method = "processLegacySkin", at = @At("HEAD"))
   private static void ans$captureExpressionTexels(NativeImage image, String url, CallbackInfoReturnable<NativeImage> cir) {
      ExpressionSkins.captureOriginal(image, url);
   }

   @Inject(method = "processLegacySkin", at = @At("RETURN"))
   private static void ans$restoreExpressionTexels(NativeImage image, String url, CallbackInfoReturnable<NativeImage> cir) {
      ExpressionSkins.restore((NativeImage)cir.getReturnValue(), url);
   }
}
