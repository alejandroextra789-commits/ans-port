package com.ans.client.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Minecraft 1.20.1-compatible render type adapter.
 *
 * The source mod used the newer pipeline-builder/RenderSetup API, which does not
 * exist in 1.20.1. Use the closest legacy render types instead of dropping the
 * effect path entirely. Additive blending is approximated with the vanilla eyes
 * pass; texture scrolling is retained where vanilla exposes it.
 */
public final class BedrockRenderTypes {
    private BedrockRenderTypes() {}

    public static RenderType additive(ResourceLocation texture, float uOffset, float vOffset, boolean one) {
        // The legacy renderer has no public equivalent of the newer pipeline's
        // arbitrary shader/texture-transform setup. EYES provides an emissive,
        // additive-like pass; energySwirl retains vanilla UV scrolling semantics.
        if (uOffset != 0.0F || vOffset != 0.0F) {
            return RenderType.energySwirl(texture, uOffset, vOffset);
        }
        return one ? RenderType.eyes(texture) : RenderType.entityTranslucent(texture);
    }
}
