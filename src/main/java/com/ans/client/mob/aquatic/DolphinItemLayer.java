package com.ans.client.mob.aquatic;

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
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class DolphinItemLayer<E extends LivingEntity> extends GeoRenderLayer<MobAnimatable, E, LivingEntityRenderState> {
   private static final DataTicket<ItemStackRenderState> ITEM = DataTicket.create("ans_dolphin_item", ItemStackRenderState.class);
   private static final String BONE = "nose";

   private DolphinItemLayer(GeoRenderer<MobAnimatable, E, LivingEntityRenderState> renderer) {
      super(renderer);
   }

   public static void install() {
      CompileReplacedEntityRenderLayersEvent.EVENT.register((Listener)event -> {
         if (event.getRenderer() instanceof ReplacedMobRenderer<?> r && ((MobAnimatable)r.getAnimatable()).mob().type() == EntityType.DOLPHIN) {
            r.withRenderLayer(new DolphinItemLayer(r));
         }
      });
   }

   public void addRenderData(MobAnimatable animatable, E entity, LivingEntityRenderState renderState, float partialTick) {
      ItemStack stack = entity == null ? ItemStack.EMPTY : entity.getMainHandItem();
      ItemStackRenderState state = new ItemStackRenderState();
      if (!stack.isEmpty()) {
         Minecraft.getInstance().getItemModelResolver().updateForLiving(state, stack, ItemDisplayContext.GROUND, entity);
      }

      if (!state.isEmpty() || renderState.hasGeckolibData(ITEM)) {
         renderState.addGeckolibData(ITEM, state);
      }
   }

   public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
      ItemStackRenderState item = (ItemStackRenderState)info.getGeckolibData(ITEM);
      FrameResult frame = (FrameResult)info.getGeckolibData(FrameResult.TICKET);
      if (item != null && !item.isEmpty() && frame != null) {
         GeoBone bone = null;

         for (RenderLayer layer : frame.layers()) {
            BakedGeoModel model = this.getGeoModel().getBakedModel(layer.model());
            if (!model.isMissingno()) {
               Optional<GeoBone> b = model.getBone("nose");
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
            ps.translate(0.0F, 0.0625F, -0.25F);
            item.submit(ps, collector, info.packedLight(), OverlayTexture.NO_OVERLAY, ((LivingEntityRenderState)info.renderState()).outlineColor);
            ps.popPose();
         }
      }
   }
}
