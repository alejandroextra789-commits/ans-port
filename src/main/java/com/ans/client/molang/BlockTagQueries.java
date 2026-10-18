package com.ans.client.molang;

import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockTagQueries {
   private static final Map<String, Predicate<BlockState>> TAGS = Map.ofEntries(
      Map.entry("water", s -> s.getFluidState().is(FluidTags.WATER)),
      Map.entry("lava", s -> s.getFluidState().is(FluidTags.LAVA)),
      Map.entry("rail", s -> s.is(BlockTags.RAILS)),
      Map.entry(
         "wood",
         s -> s.is(BlockTags.LOGS)
            || s.is(BlockTags.PLANKS)
            || s.is(BlockTags.WOODEN_SLABS)
            || s.is(BlockTags.WOODEN_STAIRS)
            || s.is(BlockTags.WOODEN_FENCES)
            || s.is(BlockTags.WOODEN_DOORS)
            || s.is(BlockTags.WOODEN_TRAPDOORS)
      ),
      Map.entry("stone", s -> s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.STONE_ORE_REPLACEABLES) || s.is(Blocks.COBBLESTONE)),
      Map.entry("snow", s -> s.is(Blocks.SNOW)),
      Map.entry("dirt", s -> s.is(BlockTags.DIRT)),
      Map.entry("grass", s -> s.is(Blocks.GRASS_BLOCK)),
      Map.entry("sand", s -> s.is(BlockTags.SAND)),
      Map.entry("trapdoors", s -> s.is(BlockTags.TRAPDOORS)),
      Map.entry("one_way_collidable", s -> s.is(BlockTags.SLABS) || s.is(BlockTags.TRAPDOORS)),
      Map.entry("not_feature_replaceable", s -> s.is(BlockTags.FEATURES_CANNOT_REPLACE)),
      Map.entry("is_pickaxe_item_destructible", s -> s.is(BlockTags.MINEABLE_WITH_PICKAXE)),
      Map.entry("is_axe_item_destructible", s -> s.is(BlockTags.MINEABLE_WITH_AXE)),
      Map.entry("is_shovel_item_destructible", s -> s.is(BlockTags.MINEABLE_WITH_SHOVEL)),
      Map.entry("is_hoe_item_destructible", s -> s.is(BlockTags.MINEABLE_WITH_HOE))
   );

   private BlockTagQueries() {
   }

   static void install() {
      MolangQueries.register("relative_block_has_any_tag", a -> c -> {
         BlockState s = block(a, c);
         if (s == null) {
            return 0.0;
         }

         for (String t : MolangQueries.strings(a, 3, c)) {
            if (has(s, t)) {
               return 1.0;
            }
         }

         return 0.0;
      });
      MolangQueries.register("block_has_any_tag", a -> c -> {
         BlockState s = blockAt(a, c);
         if (s == null) {
            return 0.0;
         }

         for (String t : MolangQueries.strings(a, 3, c)) {
            if (has(s, t)) {
               return 1.0;
            }
         }

         return 0.0;
      });
      MolangQueries.register("block_has_all_tags", a -> c -> {
         BlockState s = blockAt(a, c);
         if (s != null && a.length >= 4) {
            for (String t : MolangQueries.strings(a, 3, c)) {
               if (!has(s, t)) {
                  return 0.0;
               }
            }

            return 1.0;
         } else {
            return 0.0;
         }
      });
      MolangQueries.register("relative_block_has_all_tags", a -> c -> {
         BlockState s = block(a, c);
         if (s != null && a.length >= 4) {
            for (String t : MolangQueries.strings(a, 3, c)) {
               if (!has(s, t)) {
                  return 0.0;
               }
            }

            return 1.0;
         } else {
            return 0.0;
         }
      });
   }

   private static BlockState block(Node[] a, MolangContext c) {
      Entity e = c.actor;
      if (e != null && a.length >= 3) {
         double x = a[0].eval(c);
         double y = a[1].eval(c);
         double z = a[2].eval(c);
         BlockPos pos = BlockPos.containing(e.getX() + x, e.getY() + BedrockQueries.bedrockPositionOffset(e) + y, e.getZ() + z);
         return e.level().getBlockState(pos);
      } else {
         return null;
      }
   }

   private static BlockState blockAt(Node[] a, MolangContext c) {
      Entity e = c.actor;
      return e != null && a.length >= 3 ? e.level().getBlockState(BlockPos.containing(a[0].eval(c), a[1].eval(c), a[2].eval(c))) : null;
   }

   private static boolean has(BlockState s, String tag) {
      String t = tag.toLowerCase(Locale.ROOT).replace("minecraft:", "");
      Predicate<BlockState> p = TAGS.get(t);
      return p != null && p.test(s);
   }
}
