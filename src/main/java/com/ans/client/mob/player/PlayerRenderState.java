package com.ans.client.mob.player;

import net.minecraft.util.Mth;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.animal.parrot.Parrot.Variant;
import net.minecraft.resources.ResourceLocation;

public final class PlayerRenderState extends HumanoidRenderState {
   public ResourceLocation skin;
   public ResourceLocation cape;
   public ResourceLocation elytra;
   public String modelName = "default";
   public boolean showCape = true;
   public boolean isSpectator;
   public float fallFlyingTimeInTicks;
   public float cameraFade = 1.0F;
   public Variant parrotOnLeftShoulder;
   public Variant parrotOnRightShoulder;

   public float fallFlyingScale() {
      return Mth.clamp(this.fallFlyingTimeInTicks * this.fallFlyingTimeInTicks / 100.0F, 0.0F, 1.0F);
   }
}
