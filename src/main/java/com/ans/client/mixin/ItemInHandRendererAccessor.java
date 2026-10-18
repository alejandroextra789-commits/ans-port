package com.ans.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor {
   @Accessor("mainHandHeight")
   float ans$mainHandHeight();

   @Accessor("oMainHandHeight")
   float ans$oMainHandHeight();

   @Accessor("offHandHeight")
   float ans$offHandHeight();

   @Accessor("oOffHandHeight")
   float ans$oOffHandHeight();

   @Accessor("mainHandItem")
   ItemStack ans$mainHandItem();

   @Accessor("offHandItem")
   ItemStack ans$offHandItem();

   @Accessor("itemModelResolver")
   ItemModelResolver ans$itemModelResolver();

   @Invoker("submitArmWithItem")
   void ans$submitArmWithItem(
      AbstractClientPlayer var1,
      float var2,
      float var3,
      InteractionHand var4,
      float var5,
      ItemStack var6,
      float var7,
      PoseStack var8,
      SubmitNodeCollector var9,
      int var10
   );

   @Invoker("renderMap")
   void ans$renderMap(PoseStack var1, SubmitNodeCollector var2, int var3, ItemStack var4);

   @Invoker("calculateMapTilt")
   float ans$calculateMapTilt(float var1);
}
