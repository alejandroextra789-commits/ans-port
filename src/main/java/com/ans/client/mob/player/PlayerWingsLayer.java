package com.ans.client.mob.player;

import com.ans.client.attachable.Attachables;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.equipment.EquipmentLayer;
import com.ans.client.render.MobAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import com.geckolib.renderer.base.RenderPassInfo;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

final class PlayerWingsLayer extends GeoRenderLayer<MobAnimatable, Player, LivingEntityRenderState> {
   private final ElytraModel model;
   private final EquipmentLayerRenderer equipment;

   PlayerWingsLayer(PlayerRenderer renderer, Context context) {
      super(renderer);
      this.model = new ElytraModel(context.bakeLayer(ModelLayers.ELYTRA));
      this.equipment = context.getEquipmentRenderer();
   }

   public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
      if (info.renderState() instanceof PlayerRenderState s && !s.isInvisible && !s.isSpectator) {
         ItemStack chest = s.chestEquipment;
         Equippable equippable = (Equippable)chest.get(DataComponents.EQUIPPABLE);
         if (equippable != null && !equippable.assetId().isEmpty()) {
            Attachables.FrameList attached = (Attachables.FrameList)info.getGeckolibData(Attachables.FrameList.TICKET);
            if (attached == null || !attached.frames().stream().anyMatch(f -> f.javaSlot() == EquipmentSlot.CHEST)) {
               FrameResult frame = (FrameResult)info.getGeckolibData(FrameResult.TICKET);
               GeoBone body = frame == null ? null : PlayerRenderer.bone(this.getGeoModel(), frame, "body");
               if (body != null) {
                  PoseStack ps = info.poseStack();
                  ps.pushPose();
                  ps.last().set(info.getModelRenderMatrixPose());
                  EquipmentLayer.toBone(ps, body, frame.pose());
                  ps.scale(-1.0F, -1.0F, 1.0F);
                  ps.translate(0.0F, 0.0F, 0.125F);
                  this.equipment
                     .renderLayers(
                        LayerType.WINGS,
                        (ResourceKey)equippable.assetId().get(),
                        this.model,
                        s,
                        chest,
                        ps,
                        collector,
                        info.packedLight(),
                        texture(s),
                        s.outlineColor,
                        0
                     );
                  ps.popPose();
               }
            }
         }
      }
   }

   private static ResourceLocation texture(PlayerRenderState s) {
      if (s.elytra != null) {
         return s.elytra;
      }
      return s.cape != null && s.showCape ? s.cape : null;
   }
}
