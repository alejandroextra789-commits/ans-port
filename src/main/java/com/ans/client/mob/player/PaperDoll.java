package com.ans.client.mob.player;

import com.ans.Ans;
import com.ans.client.bedrock.AnimationLookup;
import com.ans.client.bedrock.EntityBundle;
import com.ans.client.bedrock.EntityBundles;
import com.ans.client.bedrock.EntityRuntime;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.mob.AnimatedMob;
import com.ans.client.molang.MolangContext;
import com.ans.client.molang.MolangVars;
import com.ans.client.render.ReplacedMobRenderer;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;

public final class PaperDoll {
   private static final int V_IS_PAPERDOLL = MolangVars.id("is_paperdoll");
   private static final Map<Entity, EntityRuntime> RUNTIMES = new WeakHashMap<>();
   private static boolean failed;
   private static long lastLog;

   private PaperDoll() {
   }

   static boolean tick(AnimatedMob<?> mob, AbstractClientPlayer player, LivingEntityRenderState state, float partialTick) {
      if (failed) {
         return false;
      }

      EntityBundle bundle = EntityBundles.get(mob.id(), mob.bundleFor(player));
      if (bundle == null) {
         return false;
      }

      try {
         FrameResult frame = runtime(player, bundle).tick(player, partialTick, state);
         state.addGeckolibData(FrameResult.TICKET, frame);
         if (ReplacedMobRenderer.debug && System.currentTimeMillis() - lastLog > 2000L) {
            lastLog = System.currentTimeMillis();
            Ans.LOGGER
               .info(
                  "[ans] paper doll {}: {} layers={}",
                  new Object[]{
                     player.getName().getString(),
                     RUNTIMES.get(player).describe(false),
                     frame.layers().stream().map(l -> l.model().getPath() + "|" + l.texture().getPath()).toList()
                  }
               );
         }

         return true;
      } catch (RuntimeException e) {
         failed = true;
         Ans.LOGGER.error("[ans] paper-doll runtime failed; the doll draws its rest pose from now on (logged once)", e);
         return false;
      }
   }

   private static EntityRuntime runtime(AbstractClientPlayer player, EntityBundle bundle) {
      EntityRuntime rt = RUNTIMES.get(player);
      if (rt == null || rt.bundle() != bundle || rt.generation() != AnimationLookup.generation()) {
         rt = new EntityRuntime("player", bundle).firstPerson(false, PaperDoll::engineVariables).ui(true);
         RUNTIMES.put(player, rt);
      }

      return rt;
   }

   private static void engineVariables(MolangContext c) {
      c.vars.set(V_IS_PAPERDOLL, 1.0);
   }

   public static EntityRuntime runtime(Entity player) {
      return RUNTIMES.get(player);
   }
}
