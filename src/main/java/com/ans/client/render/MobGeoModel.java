package com.ans.client.render;

import com.ans.Ans;
import com.ans.client.bedrock.EntityBundle;
import com.ans.client.bedrock.EntityBundles;
import com.ans.client.mob.AnimatedMob;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** GeckoLib 4 model resource adapter for an Actions & Stuff mob definition. */
public final class MobGeoModel extends GeoModel<MobAnimatable> {
    private final AnimatedMob<?> mob;

    public MobGeoModel(AnimatedMob<?> mob) {
        this.mob = mob;
    }

    @Override
    public ResourceLocation getModelResource(MobAnimatable animatable) {
        EntityBundle bundle = EntityBundles.get(this.mob.id(), this.mob.bundleFile());
        ResourceLocation geo = bundle == null ? null : bundle.geometry.get("default");
        return geo != null ? geo : AnimatedMob.geoId(this.mob.id());
    }

    @Override
    public ResourceLocation getTextureResource(MobAnimatable animatable) {
        EntityBundle bundle = EntityBundles.get(this.mob.id(), this.mob.bundleFile());
        ResourceLocation texture = bundle == null ? null : bundle.textures.get("default");
        return texture != null ? texture : Ans.id("textures/entity/" + this.mob.id() + "/default.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MobAnimatable animatable) {
        EntityBundle bundle = EntityBundles.get(this.mob.id(), this.mob.bundleFile());
        return bundle != null && bundle.animationFile != null ? bundle.animationFile : AnimatedMob.geoId(this.mob.id());
    }
}
