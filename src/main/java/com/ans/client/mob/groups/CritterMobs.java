package com.ans.client.mob.groups;

import com.ans.client.util.DyeColors;
import com.ans.client.mob.AnimatedMob;
import com.ans.client.molang.EngineVariables;
import com.ans.client.render.BedrockTextures;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

public final class CritterMobs {
   private CritterMobs() {
   }

   public static void register() {
      sheep();
      AnimatedMob.builder(EntityType.CHICKEN).register();
      rabbit();
      goat();
      llama();
   }

   private static void rabbit() {
      AnimatedMob.builder(EntityType.RABBIT).query("variant", r -> {
         return switch (r.getVariant()) {
            case BROWN -> 0.0;
            case WHITE, EVIL -> 1.0;
            case BLACK -> 2.0;
            case WHITE_SPLOTCHED -> 3.0;
            case GOLD -> 4.0;
            case SALT -> 5.0;
            default -> throw new IllegalStateException("Unexpected enum value");
         };
      }).actorScale(r -> r.isBaby() ? 0.4 : 0.55).register();
   }

   private static void goat() {
      EngineVariables.registerFor(EntityType.GOAT, "goat_has_left_horn", g -> g.hasLeftHorn() ? 1.0 : 0.0);
      EngineVariables.registerFor(EntityType.GOAT, "goat_has_right_horn", g -> g.hasRightHorn() ? 1.0 : 0.0);
      EngineVariables.registerFor(EntityType.GOAT, "should_bow_head", g -> g.getRammingXHeadRot() > 0.0F ? 1.0 : 0.0);
      AnimatedMob.builder(EntityType.GOAT).register();
   }

   private static void llama() {
      EngineVariables.registerFor(EntityType.LLAMA, "DecorTextureIndex", CritterMobs::carpetIndex);
      AnimatedMob.builder(EntityType.LLAMA).query("variant", l -> l.getVariant().ordinal()).register();
   }

   private static int carpetIndex(LivingEntity entity) {
      // Llama carpets use the native swag field in Minecraft 1.20.1.
      if (entity instanceof Llama llama) {
         DyeColor dye = llama.getSwag();
         int swag = dye == null ? -1 : dye.getId();
         return swag < 0 ? 0 : swag + 1;
      }
      return 0;
   }

   private static void sheep() {
      AnimatedMob.builder(EntityType.SHEEP)
         .query("is_grazing", s -> !(s.getHeadEatPositionScale(0.0F) > 0.0F) && !(s.getHeadEatPositionScale(1.0F) > 0.0F) ? 0.0 : 1.0)
         .changeColor(CritterMobs::woolColor)
         .register();
      BedrockTextures.composite(
         vanilla("sheep/sheep"),
         64,
         64,
         BedrockTextures.part(vanilla("sheep/sheep"), 0, 0, false),
         BedrockTextures.part(vanilla("sheep/sheep_wool_undercoat"), 0, 0, true),
         BedrockTextures.part(vanilla("sheep/sheep_wool"), 0, 32, true)
      );
      BedrockTextures.composite(
         vanilla("sheep/sheep_baby"),
         32,
         32,
         BedrockTextures.crop(vanilla("sheep/sheep_baby"), 0, 0, 32, 32, 0, 0, false),
         BedrockTextures.crop(vanilla("sheep/sheep_wool_baby"), 0, 0, 32, 32, 0, 0, true)
      );
   }

   private static int woolColor(Sheep sheep, float partialTick) {
      if (!hasName(sheep, "jeb_")) {
         return DyeColors.diffuse(sheep.getColor());
      }

      // Vanilla's rainbow sheep easter egg cycles through the dye palette.
      DyeColor[] colors = DyeColor.values();
      int current = (int)((sheep.tickCount + partialTick) / 25.0F) % colors.length;
      int next = (current + 1) % colors.length;
      float blend = ((sheep.tickCount + partialTick) % 25.0F) / 25.0F;
      int a = DyeColors.diffuse(colors[current]);
      int b = DyeColors.diffuse(colors[next]);
      int r = (int)(((a >> 16) & 255) * (1.0F - blend) + ((b >> 16) & 255) * blend);
      int g = (int)(((a >> 8) & 255) * (1.0F - blend) + ((b >> 8) & 255) * blend);
      int bl = (int)((a & 255) * (1.0F - blend) + (b & 255) * blend);
      return 0xFF000000 | r << 16 | g << 8 | bl;
   }

   private static boolean hasName(Entity entity, String name) {
      return entity.getCustomName() != null && name.equals(entity.getCustomName().getString());
   }

   private static ResourceLocation vanilla(String entityTexture) {
      return new ResourceLocation("minecraft", "textures/entity/" + entityTexture + ".png");
   }
}
