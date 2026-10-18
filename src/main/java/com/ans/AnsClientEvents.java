package com.ans;

import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Adapts Fabric's end-client-tick callback style to Forge 1.20.1. */
public final class AnsClientEvents {
    private static final CopyOnWriteArrayList<Consumer<Minecraft>> END_TICK = new CopyOnWriteArrayList<>();
    private static boolean registered;

    private AnsClientEvents() {}

    public static synchronized void registerEndTick(Consumer<Minecraft> callback) {
        END_TICK.add(callback);
        if (!registered) {
            MinecraftForge.EVENT_BUS.register(AnsClientEvents.class);
            registered = true;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        for (Consumer<Minecraft> callback : END_TICK) {
            callback.accept(minecraft);
        }
    }
}
