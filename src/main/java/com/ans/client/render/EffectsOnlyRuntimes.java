package com.ans.client.render;

import com.ans.Ans;
import com.ans.client.bedrock.EntityBundle;
import com.ans.client.bedrock.EntityBundles;
import com.ans.client.bedrock.EntityRuntime;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.bedrock.MobPose;
import com.ans.client.mob.AnimatedMob;
import com.ans.client.mob.AnimatedMobs;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;

public final class EffectsOnlyRuntimes {
   public static final boolean ENABLED = !"false".equalsIgnoreCase(System.getProperty("ans.effectsOnly"));
   private static final long STALE_NANOS = 30000000000L;
   private static final long SWEEP_NANOS = 1000000000L;
   private static final Map<Integer, EffectsOnlyRuntimes.Run> RUNS = new HashMap<>();
   private static final Set<String> FAILED = new HashSet<>();
   private static final LivingEntityRenderState STATE = new LivingEntityRenderState();
   private static WeakReference<Object> level;
   private static long lastSweep;

   private EffectsOnlyRuntimes() {
   }

   public static void extracted(Entity entity, float partialTick) {
      if (ENABLED) {
         AnimatedMob<?> mob = AnimatedMobs.get(entity.getType());
         if (mob != null && mob.effectsOnly() && !FAILED.contains(mob.id())) {
            Object lvl = entity.level();
            if (level == null || level.get() != lvl) {
               RUNS.clear();
               level = new WeakReference<>(lvl);
            }

            long now = System.nanoTime();
            if (now - lastSweep > 1000000000L) {
               sweep(now);
            }

            EntityBundle bundle = EntityBundles.get(mob.id(), mob.bundleFor(entity));
            if (bundle != null) {
               EffectsOnlyRuntimes.Run run = RUNS.get(entity.getId());
               if (run == null || run.entity.get() != entity || run.runtime.bundle() != bundle) {
                  run = new EffectsOnlyRuntimes.Run(entity, new EntityRuntime(mob.id(), bundle));
                  RUNS.put(entity.getId(), run);
               }

               run.usedAt = now;

               try {
                  FrameResult frame = run.runtime.tick(entity, partialTick, STATE);

                  for (String bone : mob.hiddenBones()) {
                     hide(frame.pose(), bone);
                  }
               } catch (RuntimeException e) {
                  FAILED.add(mob.id());
                  RUNS.values().removeIf(r -> r.runtime.mobId().equals(mob.id()));
                  Ans.LOGGER.error("[ans] effects-only runtime of {} failed for {}; disabled for this type", new Object[]{mob, entity, e});
               }
            }
         }
      }
   }

   private static void hide(MobPose pose, String bone) {
      float[] v = pose.bones().computeIfAbsent(bone, k -> new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F});
      v[6] = v[7] = v[8] = 0.0F;
   }

   private static void sweep(long now) {
      lastSweep = now;
      Iterator<EffectsOnlyRuntimes.Run> it = RUNS.values().iterator();

      while (it.hasNext()) {
         EffectsOnlyRuntimes.Run r = it.next();
         Entity e = r.entity.get();
         if (e == null || e.isRemoved() || now - r.usedAt > 30000000000L) {
            it.remove();
         }
      }
   }

   private static final class Run {
      final WeakReference<Entity> entity;
      final EntityRuntime runtime;
      long usedAt;

      Run(Entity entity, EntityRuntime runtime) {
         this.entity = new WeakReference<>(entity);
         this.runtime = runtime;
      }
   }
}
