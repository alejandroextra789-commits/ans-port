package com.ans.client.attachable;

import com.ans.client.render.BedrockTextures;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public final class VanillaItemTextures {
   public static final ResourceLocation SPYGLASS = new ResourceLocation("minecraft", "textures/item/spyglass_model.png");
   public static final ResourceLocation ZOMBIE_SKULL = new ResourceLocation("minecraft", "textures/entity/zombie/zombie_skull_bedrock.png");

   private VanillaItemTextures() {
   }

   public static void register() {
      BedrockTextures.composite(
         ZOMBIE_SKULL, 64, 32, BedrockTextures.crop(new ResourceLocation("minecraft", "textures/entity/zombie/zombie.png"), 0, 0, 64, 32, 0, 0, true)
      );
      List<BedrockTextures.Part> p = new ArrayList<>();

      for (int i = 0; i < 6; i++) {
         for (int j = 0; j < 2; j++) {
            px(p, j, 12 - i, 2 + i, j);
            px(p, j, 12 - i, 8 + i, j);
            px(p, j, 12 - i, 2 + i, 2 + j);
            px(p, j, 7 + i, 10 + i, 2 + j);
         }
      }

      for (int a = 0; a < 2; a++) {
         for (int b = 0; b < 2; b++) {
            px(p, a, 13 + b, a, 2 + b);
            px(p, a, 5 + b, 8 + a, 2 + b);
         }
      }

      for (int i = 0; i < 5; i++) {
         for (int j = 0; j < 2; j++) {
            px(p, j, 6 - i, 2 + i, 4 + j);
            px(p, j, 6 - i, 7 + i, 4 + j);
            px(p, j, 6 - i, 2 + i, 6 + j);
            px(p, j, 2 + i, 9 + i, 6 + j);
         }
      }

      for (int a = 0; a < 2; a++) {
         for (int b = 0; b < 2; b++) {
            px(p, a, 5 + b, a, 6 + b);
            px(p, a, b, 7 + a, 6 + b);
         }
      }

      BedrockTextures.composite(SPYGLASS, 16, 16, p.toArray(BedrockTextures.Part[]::new));
   }

   private static void px(List<BedrockTextures.Part> parts, int srcX, int srcY, int dstX, int dstY) {
      parts.add(BedrockTextures.crop(SPYGLASS, srcX, srcY, 1, 1, dstX, dstY, true));
   }
}
