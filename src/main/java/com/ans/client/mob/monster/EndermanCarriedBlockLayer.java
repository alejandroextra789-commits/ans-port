package com.ans.client.mob.monster;

import com.ans.client.bedrock.FrameResult;
import com.ans.client.bedrock.RenderLayer;
import com.ans.client.equipment.EquipmentLayer;
import com.ans.client.render.MobAnimatable;
import com.ans.client.render.ReplacedMobRenderer;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.event.replacedentity.CompileReplacedEntityRenderLayersEvent;
import com.geckolib.event.replacedentity.CompileReplacedEntityRenderLayersEvent.Listener;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.block.state.BlockState;

public final class EndermanCarriedBlockLayer<E extends LivingEntity> extends GeoRenderLayer<MobAnimatable, E, LivingEntityRenderState> {
   private static final DataTicket<BlockModelRenderState> BLOCK = DataTicket.create("ans_enderman_block", BlockModelRenderState.class);
   private static final String BONE = "fakearms";
   private static final float DY = 0.03125F;
   private static final float DZ = -0.125F;
   private static final float ADULT_PIVOT_Y = 13.5F;
   private BlockModelResolver blocks;

   private EndermanCarriedBlockLayer(GeoRenderer<MobAnimatable, E, LivingEntityRenderState> renderer) {
      super(renderer);
   }

   public static void install() {
      CompileReplacedEntityRenderLayersEvent.EVENT.register((Listener)event -> {
         if (event.getRenderer() instanceof ReplacedMobRenderer<?> r && ((MobAnimatable)r.getAnimatable()).mob().type() == EntityType.ENDERMAN) {
            r.withRenderLayer(new EndermanCarriedBlockLayer(r));
         }
      });
   }

   public void addRenderData(MobAnimatable animatable, E entity, LivingEntityRenderState renderState, float partialTick) {
      BlockState carried = entity instanceof EnderMan e ? e.getCarriedBlock() : null;
      if (carried == null) {
         if (renderState.hasGeckolibData(BLOCK)) {
            renderState.addGeckolibData(BLOCK, new BlockModelRenderState());
         }
      } else {
         if (this.blocks == null) {
            this.blocks = new BlockModelResolver(Minecraft.getInstance().getModelManager());
         }

         BlockModelRenderState state = new BlockModelRenderState();
         this.blocks.update(state, carried, EndermanRenderer.BLOCK_DISPLAY_CONTEXT);
         renderState.addGeckolibData(BLOCK, state);
      }
   }

   public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
      BlockModelRenderState block = (BlockModelRenderState)info.getGeckolibData(BLOCK);
      FrameResult frame = (FrameResult)info.getGeckolibData(FrameResult.TICKET);
      if (block != null && !block.isEmpty() && frame != null) {
         GeoBone bone = null;

         for (RenderLayer layer : frame.layers()) {
            BakedGeoModel model = this.getGeoModel().getBakedModel(layer.model());
            if (!model.isMissingno() && layer.visible("fakearms")) {
               Optional<GeoBone> b = model.getBone("fakearms");
               if (b.isPresent()) {
                  bone = b.get();
                  break;
               }
            }
         }

         if (bone != null) {
            PoseStack ps = info.poseStack();
            ps.pushPose();
            ps.last().set(info.getModelRenderMatrixPose());
            EquipmentLayer.toBone(ps, bone, frame.pose());
            ps.scale(-1.0F, -1.0F, 1.0F);
            float s = Math.abs(bone.pivotY()) / 13.5F;
            if (s > 0.01F && Math.abs(s - 1.0F) > 0.05F) {
               ps.scale(s, s, s);
            }

            ps.translate(0.0F, 0.03125F, -0.125F);
            ps.mulPose(Axis.XP.rotationDegrees(20.0F));
            ps.mulPose(Axis.YP.rotationDegrees(45.0F));
            ps.translate(0.25F, 0.1875F, 0.25F);
            ps.scale(-0.5F, -0.5F, 0.5F);
            ps.mulPose(Axis.YP.rotationDegrees(90.0F));
            block.submit(ps, collector, info.packedLight(), OverlayTexture.NO_OVERLAY, ((LivingEntityRenderState)info.renderState()).outlineColor);
            ps.popPose();
         }
      }
   }
}
