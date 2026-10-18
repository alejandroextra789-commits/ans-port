package com.ans.client.particle;

import com.ans.client.molang.MolangContext;
import com.ans.client.molang.MolangQueries;
import com.ans.client.molang.MolangStruct;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

final class SurfaceQueries {
   private SurfaceQueries() {
   }

   static void install() {
      MolangQueries.registerString("surface_particle_texture_coordinate", var0 -> new SurfaceQueries.Struct(0));
      MolangQueries.registerString("surface_particle_texture_size", var0 -> new SurfaceQueries.Struct(1));
      MolangQueries.registerString("surface_particle_color", var0 -> new SurfaceQueries.Struct(2));
   }

   private record Struct(int kind) implements MolangQueries.Bound {
      @Override
      public double eval(MolangContext c) {
         return 0.0;
      }

      @Override
      public Object value(MolangContext c) {
         Entity actor = c.actor;
         Level level = actor == null ? null : actor.level();
         BlockPos pos = null;
         BlockState state = null;
         if (level != null) {
            pos = BlockPos.containing(actor.getX(), actor.getY() - 0.2, actor.getZ());
            state = level.getBlockState(pos);
            if (state.isAir()) {
               pos = pos.below();
               state = level.getBlockState(pos);
            }
         }

         MolangStruct s = new MolangStruct();
         if (this.kind == 2) {
            float[] rgb = color(state, level, pos);
            return s.put("r", rgb[0]).put("g", rgb[1]).put("b", rgb[2]).put("a", 1.0);
         } else {
            TextureAtlasSprite sprite = state != null && !state.isAir()
               ? Minecraft.getInstance().getBlockRenderer().getBlockModel(state).getParticleIcon()
               : null;
            if (sprite == null) {
               return s.put("u", 0.0).put("v", 0.0);
            } else {
               return this.kind == 0
                  ? s.put("u", sprite.getU0()).put("v", sprite.getV0())
                  : s.put("u", sprite.getU1() - sprite.getU0()).put("v", sprite.getV1() - sprite.getV0());
            }
         }
      }

      private static float[] color(BlockState state, Level level, BlockPos pos) {
         if (state != null && level != null && pos != null) {
            int col = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
            return new float[]{(col >> 16 & 0xFF) / 255.0F, (col >> 8 & 0xFF) / 255.0F, (col & 0xFF) / 255.0F};
         } else {
            return new float[]{1.0F, 1.0F, 1.0F};
         }
      }
   }
}
