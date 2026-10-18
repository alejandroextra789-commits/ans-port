package com.ans.client.mixin;

import com.ans.client.mob.player.PlayerRendering;
import com.ans.client.render.EffectsOnlyRuntimes;
import java.util.Map;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
   @Shadow
   private Map<EntityType<?>, EntityRenderer<?, ?>> renderers;

   @Inject(
      method = "getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
      at = @At("HEAD"),
      cancellable = true
   )
   private void ans$playerRenderer(Entity entity, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
      if (entity instanceof AbstractClientPlayer player && PlayerRendering.replaces(player)) {
         EntityRenderer<?, ?> renderer = this.renderers.get(entity.getType());
         if (renderer != null) {
            cir.setReturnValue(renderer);
         }
      }
   }

   /**
    * Minecraft 1.20.1 renders entities directly instead of extracting a render-state
    * object. Keep the effects-only animation tick on the legacy render entry point.
    */
   @Inject(
      method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
      at = @At("HEAD")
   )
   private void ans$effectsOnly(Entity entity, double x, double y, double z, float yaw, float partialTick,
                                PoseStack poseStack, MultiBufferSource buffers, int packedLight, CallbackInfo ci) {
      EffectsOnlyRuntimes.extracted(entity, partialTick);
   }
}
