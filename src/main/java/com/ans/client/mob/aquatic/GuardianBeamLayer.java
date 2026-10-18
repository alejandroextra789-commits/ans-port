package com.ans.client.mob.aquatic;

import com.ans.Ans;
import com.ans.client.render.MobAnimatable;
import com.ans.client.render.ReplacedMobRenderer;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.event.replacedentity.CompileReplacedEntityRenderLayersEvent;
import com.geckolib.event.replacedentity.CompileReplacedEntityRenderLayersEvent.Listener;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.GuardianRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.phys.Vec3;

public final class GuardianBeamLayer<E extends LivingEntity> extends GeoRenderLayer<MobAnimatable, E, LivingEntityRenderState> {
   private static final DataTicket<GuardianBeamLayer.Beam> BEAM = DataTicket.create("ans_guardian_beam", GuardianBeamLayer.Beam.class);
   private static final GuardianBeamLayer.Beam NONE = new GuardianBeamLayer.Beam(false, Vec3.ZERO, 0.0F, 0.0F, 0.0F);
   private static MethodHandle renderBeam;
   private static boolean failed;

   private GuardianBeamLayer(GeoRenderer<MobAnimatable, E, LivingEntityRenderState> renderer) {
      super(renderer);
   }

   public static void install() {
      CompileReplacedEntityRenderLayersEvent.EVENT.register((Listener)event -> {
         if (event.getRenderer() instanceof ReplacedMobRenderer<?> r) {
            EntityType<?> type = ((MobAnimatable)r.getAnimatable()).mob().type();
            if (type == EntityType.GUARDIAN || type == EntityType.ELDER_GUARDIAN) {
               r.withRenderLayer(new GuardianBeamLayer(r));
            }
         }
      });
   }

   public void addRenderData(MobAnimatable animatable, E entity, LivingEntityRenderState renderState, float partialTick) {
      LivingEntity target = entity instanceof Guardian g ? g.getActiveAttackTarget() : null;
      if (target == null) {
         if (renderState.hasGeckolibData(BEAM)) {
            renderState.addGeckolibData(BEAM, NONE);
         }
      } else {
         Guardian g = (Guardian)entity;
         Vec3 at = new Vec3(
            Mth.lerp(partialTick, target.xOld, target.getX()),
            Mth.lerp(partialTick, target.yOld, target.getY()) + target.getBbHeight() * 0.5,
            Mth.lerp(partialTick, target.zOld, target.getZ())
         );
         renderState.addGeckolibData(
            BEAM,
            new GuardianBeamLayer.Beam(
               true,
               at.subtract(g.getEyePosition(partialTick)),
               g.getEyeHeight(),
               g.getClientSideAttackTime() + partialTick,
               g.getAttackAnimationScale(partialTick)
            )
         );
      }
   }

   public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
      GuardianBeamLayer.Beam beam = (GuardianBeamLayer.Beam)info.getGeckolibData(BEAM);
      if (beam != null && beam.active) {
         MethodHandle mh = handle();
         if (mh != null) {
            PoseStack ps = info.poseStack();
            ps.pushPose();
            ps.last().set(info.getPreRenderMatrixPose());
            ps.translate(0.0F, beam.eyeHeight, 0.0F);

            try {
               mh.invokeExact(
                  (PoseStack)ps,
                  (SubmitNodeCollector)collector,
                  (Vec3)beam.relative,
                  (float)beam.attackTime,
                  (float)beam.attackScale,
                  (float)(beam.attackTime * 0.5F % 1.0F)
               );
            } catch (Throwable t) {
               if (!failed) {
                  Ans.LOGGER.error("[ans] guardian beam failed (logged once)", t);
               }

               failed = true;
            } finally {
               ps.popPose();
            }
         }
      }
   }

   private static MethodHandle handle() {
      if (renderBeam == null && !failed) {
         try {
            Lookup lookup = MethodHandles.privateLookupIn(GuardianRenderer.class, MethodHandles.lookup());
            renderBeam = lookup.findStatic(
               GuardianRenderer.class,
               "renderBeam",
               MethodType.methodType(void.class, PoseStack.class, SubmitNodeCollector.class, Vec3.class, float.class, float.class, float.class)
            );
         } catch (ReflectiveOperationException | RuntimeException e) {
            Ans.LOGGER.error("[ans] GuardianRenderer.renderBeam not found; guardian beams are not drawn", e);
            failed = true;
         }

         return renderBeam;
      } else {
         return renderBeam;
      }
   }

   private record Beam(boolean active, Vec3 relative, float eyeHeight, float attackTime, float attackScale) {
   }
}
