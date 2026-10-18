package com.ans.client.mob.pets;

/**
 * Wolf armour rendering was added after Minecraft 1.20.1. The 1.20.1 target has
 * neither the wolf body equipment slot nor the equipment-asset renderer, so this
 * optional layer deliberately installs no renderer instead of linking against
 * newer client classes. Wolf animation, collars, and all other wolf behavior are
 * still registered by PetMobs.
 */
public final class WolfArmorLayer {
   private WolfArmorLayer() {}

   public static void install() {
      // No equivalent wolf armour item/slot exists in Minecraft 1.20.1.
   }
}
