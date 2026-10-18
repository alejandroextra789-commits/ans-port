package com.ans;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

/** Small Forge platform adapter replacing the original FabricLoader calls. */
public final class AnsPlatform {
    private AnsPlatform() {}
    public static Path gameDir() { return FMLPaths.GAMEDIR.get(); }
    public static Path configDir() { return FMLPaths.CONFIGDIR.get(); }
    public static boolean isModLoaded(String modId) { return ModList.get().isLoaded(modId); }
}
