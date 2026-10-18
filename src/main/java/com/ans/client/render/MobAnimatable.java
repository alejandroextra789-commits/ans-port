package com.ans.client.render;

import com.ans.client.mob.AnimatedMob;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * GeckoLib 4 animatable facade for an Actions & Stuff mob definition.
 * The replaced Minecraft entity remains the source of per-entity animation state;
 * this facade only owns the definition and GeckoLib's controller/cache plumbing.
 */
public final class MobAnimatable implements GeoAnimatable {
    private final AnimatedMob<?> mob;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MobAnimatable(AnimatedMob<?> mob) {
        this.mob = mob;
    }

    public AnimatedMob<?> mob() {
        return this.mob;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Bedrock animation controllers are sampled by EntityRuntime, rather than
        // being translated into GeckoLib's named-controller abstraction.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public double getTick(Object relatedObject) {
        if (relatedObject instanceof net.minecraft.world.entity.Entity entity) {
            return entity.tickCount;
        }
        return 0;
    }
}
