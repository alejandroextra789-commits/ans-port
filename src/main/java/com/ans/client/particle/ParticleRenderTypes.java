package com.ans.client.particle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Legacy RenderType mapping for Bedrock particle quads on Minecraft 1.20.1. */
final class ParticleRenderTypes {
    private static final Map<Key, RenderType> CACHE = new ConcurrentHashMap<>();

    private ParticleRenderTypes() {}

    static RenderType get(ResourceLocation texture, ParticleDef.Render render) {
        if (texture == null || render == null) return null;
        return CACHE.computeIfAbsent(new Key(texture, render), key -> switch (key.render()) {
            case ADD -> RenderType.eyes(key.texture());
            case BLEND -> RenderType.entityTranslucent(key.texture());
            default -> RenderType.entityCutoutNoCull(key.texture());
        });
    }

    private record Key(ResourceLocation texture, ParticleDef.Render render) {}
}
