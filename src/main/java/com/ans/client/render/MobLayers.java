package com.ans.client.render;

import com.ans.client.bedrock.BoneLookup;
import com.ans.client.bedrock.FrameResult;
import com.ans.client.bedrock.RenderLayer;
import com.ans.client.mob.aquatic.DolphinItemLayer;
import com.ans.client.mob.monster.EndermanCarriedBlockLayer;
import com.ans.client.mob.pets.FoxHeldItemLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/** Per-mob extra layers drawn after the body (GeckoLib 4 port: called directly, no render-layer registry). */
public final class MobLayers {
   private MobLayers() {
   }

   public static void render(FrameResult frame, LivingEntity entity, GeoModel<?> model, PoseStack ps, MultiBufferSource buffers, int light, float partialTick) {
      if (frame == null || entity == null) {
         return;
      }
      EntityType<?> type = entity.getType();
      if (type == EntityType.FOX) {
         FoxHeldItemLayer.render(frame, entity, model, ps, buffers, light);
      } else if (type == EntityType.DOLPHIN) {
         DolphinItemLayer.render(frame, entity, model, ps, buffers, light);
      } else if (type == EntityType.ENDERMAN) {
         EndermanCarriedBlockLayer.render(frame, entity, model, ps, buffers, light);
      }
   }

   /** First bone with this name in any visible layer of the frame, or null. */
   public static GeoBone bone(FrameResult frame, GeoModel<?> model, String name, boolean requireVisible) {
      for (RenderLayer layer : frame.layers()) {
         if (requireVisible && !layer.visible(name)) {
            continue;
         }
         BakedGeoModel baked = model.getBakedModel(layer.model());
         if (!BoneLookup.isMissing(baked)) {
            Optional<GeoBone> b = baked.getBone(name);
            if (b.isPresent()) {
               return b.get();
            }
         }
      }
      return null;
   }
}
