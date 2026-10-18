package com.ans;

import com.ans.client.AnsClient;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import com.ans.client.settings.AnsSettingsScreen;

/** Forge entry point. The actual renderer/API migration is still in progress. */
@Mod(Ans.MOD_ID)
public final class AnsForge {
    public AnsForge() {
        FMLJavaModLoadingContext.get().getModEventBus().register(ClientSetup.class);
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new AnsSettingsScreen(parent)));
    }

    public static final class ClientSetup {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(AnsClient::initialize);
        }
    }
}
