# GeckoLib Forge 1.20.1 / 4.8.4 API inspection

Inspected the user-supplied binary `geckolib-forge-1.20.1-4.8.4.jar` (443 archive entries; GeckoLib renderer/model/cache classes present).

## Confirmed GeckoLib 4 render contracts (`javap -p`)

- `GeoReplacedEntityRenderer<E extends Entity, T extends GeoAnimatable>` extends vanilla `EntityRenderer<E>` and constructor is `(EntityRendererProvider.Context, GeoModel<T>, T)`.
- `GeoEntityRenderer<T extends Entity & GeoAnimatable>` constructors accept either `(Context, EntityType<? extends T>)` or `(Context, GeoModel<T>)`.
- GeckoLib 4 renderer work is expressed through `PoseStack`, `MultiBufferSource`, `RenderType`, `BakedGeoModel`, `GeoBone`, and the legacy entity render callback `(entity, yaw, partialTick, poseStack, buffers, packedLight)`.
- `GeoRenderer<T>` exposes `defaultRender`, `reRender`, `actuallyRender`, `preRender`, `renderFinal`, `renderRecursively`, `applyRenderLayers`, and `doPostRenderCleanup`; render-layer type is `software.bernie.geckolib.renderer.layer.GeoRenderLayer<T>`.
- `GeoModel<T>` defines model/texture/animation resource getters and `handleAnimations(T, long, AnimationState<T>)`; baked model is `software.bernie.geckolib.cache.object.BakedGeoModel`.
- `GeoAnimatable` uses `registerControllers(AnimatableManager.ControllerRegistrar)`, `getAnimatableInstanceCache()`, and `getTick(Object)`.

## Port implication

The current sources import GeckoLib 5-style packages such as `com.geckolib.renderer.base.GeoRenderState`, `RenderPassInfo`, `com.geckolib.cache.model.BakedGeoModel`, and `com.geckolib.animation.state.*`. These do not correspond to GeckoLib 4.8.4's confirmed renderer/model/state surface. This is not a safe package-only rename: renderer entry points, animation state capture, model bone access, render layer dispatch, and per-frame state flow must be rewritten around the 1.20.1 callback/API.

This report records API facts from the supplied jar; it does not claim that source has been migrated or compiled.
