package com.ans.client.mob.groups;

import com.ans.client.equipment.MobEquipment;
import com.ans.client.mob.AnimatedMob;
import com.ans.client.mob.misc.EnderDragonRenderer;
import com.ans.client.mob.misc.MiscState;
import com.ans.client.molang.EngineVariables;
import com.ans.client.molang.MolangContext;
import com.ans.client.render.BedrockTextures;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory.Sample;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.item.DyeColor;

public final class MiscMobs {
   private MiscMobs() {
   }

   public static void register() {
      AnimatedMob.builder(EntityType.BAT).query("is_resting", b -> b.isResting() ? 1.0 : 0.0).register();
      AnimatedMob.builder(EntityType.SLIME).query("variant", s -> s.getSize()).register();
      AnimatedMob.builder(EntityType.SILVERFISH).register();
      AnimatedMob.builder(EntityType.ENDERMITE).register();
      AnimatedMob.builder(EntityType.PHANTOM).actorScale(p -> 1.0 + 0.15 * p.getPhantomSize()).register();
      BedrockTextures.composite(
         vanilla("phantom/phantom_bedrock"),
         64,
         64,
         BedrockTextures.part(vanilla("phantom/phantom"), 0, 0, true),
         BedrockTextures.part(vanilla("phantom/phantom_eyes"), 0, 0, false)
      );
      shulker();
      sniffer();
      warden();
      enderDragon();
   }

   private static void shulker() {
      EngineVariables.registerFor(EntityType.SHULKER, "Shulker.PeekAmount", s -> s.getClientPeekAmount(MolangContext.current().partialTick));
      EngineVariables.registerFor(EntityType.SHULKER, "Shulker.FacingDirection", s -> s.getAttachFace().get3DDataValue());
      AnimatedMob.builder(EntityType.SHULKER).query("variant", s -> {
         DyeColor c = s.getColor();
         return c == null ? 16.0 : 15 - c.getId();
      }).query("has_target", s -> MiscState.shulkerRawPeek(s) >= 100 ? 1.0 : 0.0).register();
   }



   private static void sniffer() {
      AnimatedMob.builder(EntityType.SNIFFER)
         .query("timer_flag_3", s -> s.feelingHappyAnimationState.isStarted() ? 1.0 : 0.0)
         .query("timer_flag_2", s -> s.risingAnimationState.isStarted() ? 1.0 : 0.0)
         .query("is_searching", s -> !s.isSearching() && !s.scentingAnimationState.isStarted() && !s.sniffingAnimationState.isStarted() ? 0.0 : 1.0)
         .query("is_digging", s -> s.diggingAnimationState.isStarted() ? 1.0 : 0.0)
         .register();
   }

   private static void warden() {
      AnimatedMob.builder(EntityType.WARDEN)
         .query("is_emerging", w -> w.getPose() == Pose.EMERGING ? 1.0 : 0.0)
         .query("is_digging", w -> w.getPose() == Pose.DIGGING ? 1.0 : 0.0)
         .query("is_roaring", w -> w.getPose() == Pose.ROARING ? 1.0 : 0.0)
         .query("is_sniffing", w -> w.getPose() == Pose.SNIFFING ? 1.0 : 0.0)
         .query("is_sonic_boom", w -> MiscState.remainingTicks(w.sonicBoomAnimationState, w, 60) > 0.0F ? 1.0 : 0.0)
         .query("has_target", w -> w.getClientAngerLevel() >= AngerLevel.ANGRY.getMinimumAnger() ? 1.0 : 0.0)
         .query("heartbeat_phase", w -> w.getHeartAnimation(MolangContext.current().partialTick))
         .query("time_since_last_vibration_detection", w -> {
            float t = w.getTendrilAnimation(MolangContext.current().partialTick);
            return t > 0.0F ? (1.0F - t) * 0.5 : -1.0;
         })
         .changeColor((w, pt) -> 16777215)
         .register();
      EngineVariables.registerFor(
         EntityType.WARDEN, "attack_time", w -> Math.max(w.getAttackAnim(MolangContext.current().partialTick), MiscState.swing(w.attackAnimationState, w))
      );
      ResourceLocation warden = vanilla("warden/warden");
      BedrockTextures.composite(
         vanilla("warden/warden_tendrils"),
         128,
         128,
         BedrockTextures.crop(warden, 52, 32, 32, 16, 52, 32, true),
         BedrockTextures.crop(warden, 58, 0, 32, 16, 58, 0, true)
      );
   }

   private static ResourceLocation vanilla(String entityTexture) {
      return new ResourceLocation("minecraft", "textures/entity/" + entityTexture + ".png");
   }

   private static void enderDragon() {
      for (int i = 0; i < 10; i++) {
         int delay = i;
         EngineVariables.registerFor(EntityType.ENDER_DRAGON, "historical_frame_" + i + ".pos_y", d -> history(d, delay).y());
         EngineVariables.registerFor(EntityType.ENDER_DRAGON, "historical_frame_" + i + ".rot_y", d -> history(d, delay).yRot());
      }

      AnimatedMob.builder(EntityType.ENDER_DRAGON)
         .query(
            "has_target",
            d -> {
               EnderDragonPhase<?> p = d.getPhaseManager().getCurrentPhase().getPhase();
               return p != EnderDragonPhase.STRAFE_PLAYER
                     && p != EnderDragonPhase.CHARGING_PLAYER
                     && p != EnderDragonPhase.SITTING_FLAMING
                     && p != EnderDragonPhase.SITTING_ATTACKING
                  ? 0.0
                  : 1.0;
            }
         )
         .query("death_ticks", d -> d.dragonDeathTime <= 0 ? 0.0 : d.dragonDeathTime + MolangContext.current().partialTick)
         .query("body_y_rotation", d -> history(d, 0).yRot())
         .vanillaDeathRotation(false)
         .renderer(EnderDragonRenderer::new)
         .register();
   }

   private static Sample history(EnderDragon dragon, int delay) {
      return dragon.flightHistory.get(delay, MolangContext.current().partialTick);
   }


}
