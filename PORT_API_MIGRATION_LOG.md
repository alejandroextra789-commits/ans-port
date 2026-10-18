# API migration log — Forge 1.20.1

## Changes in this pass

- Replaced the modern `RenderPipeline` / `RenderSetup` implementation in `BedrockRenderTypes` with Minecraft 1.20.1 `RenderType` factories. Additive effects use the closest legacy vanilla passes; scrolling textures use `RenderType.energySwirl`.
- Replaced the modern pipeline builder in `ParticleRenderTypes` with 1.20.1 render types (`entityCutoutNoCull`, `entityTranslucent`, `eyes`) while keeping the per-texture/per-render-mode cache.
- Removed the Fabric `LevelRenderEvents.COLLECT_SUBMITS` dependency from `BedrockParticles`. Particle updates/rendering are now scheduled through Forge `RenderLevelStageEvent` at `AFTER_ENTITIES` and emitted through `MultiBufferSource`/`VertexConsumer` using the 1.20.1 pose stack.
- Kept emitter simulation, Molang evaluation, particle spawning, flipbooks, UVs, per-particle color/light and camera-facing quad construction intact. The old custom shader and arbitrary blend/pipeline controls are approximated by vanilla render types because the newer pipeline API is absent in 1.20.1.
- Existing `SurfaceQueries` and block-light lookup changes from earlier passes remain in place.
- Replaced several post-1.20.1 item-component reads with 1.20.1 equivalents: edible-item checks use `Item.isEdible`, map detection uses `MapItem.getMapId`, dyed leather uses `DyeableLeatherItem`, potion projectiles use `PotionUtils`, and charged crossbows use `CrossbowItem.getChargedProjectiles`.
- Replaced newer `ResourceKey.identifier()` calls with the 1.20.1 `ResourceKey.location()` accessor in biome/variant query helpers.
- Adapted trim lookup to 1.20.1 `ArmorTrim.getTrim(registryAccess, stack)` and the legacy `TrimMaterial.assetName` / `TrimPattern.assetId` model. Equipment-specific trim palette assets are approximated because `Equippable` / equipment asset groups do not exist in this target.
- Replaced modern cow/pig/chicken climate-variant component reads with a documented 1.20.1 fallback, and wolf coat-variant query now uses the single vanilla wolf appearance available in 1.20.1. Egg climate query defaults to `temperate`.
- Reworked clock/compass frame calculation to a legacy approximation instead of the newer item-property classes. Recovery-compass animation currently falls back to frame zero; lodestone compass handling depends on legacy stack NBT.

## Remaining blockers

1. **GeckoLib 5 to GeckoLib 4**: 155 references to `com.geckolib.*` remain across 30 files. GeckoLib 4.8.4 for Forge 1.20.1 uses `software.bernie.geckolib.*` and has different render/model/animation contracts. A package-only rename would not be safe.
2. **New render-state architecture**: `EntityRenderState` references remain across 17 files and `SubmitNodeCollector` references remain across 17 files. These classes must be rewritten against 1.20.1 renderer signatures, entity instances, `PoseStack`, and `MultiBufferSource`.
3. **Post-1.20.1 equipment renderer APIs**: `DataComponents`, `Equippable`, `EquipmentAsset`, `EquipmentLayerRenderer`, and `EquipmentClientInfo` still remain in the custom armor/player/wolf renderer path. This is not a simple item-component replacement: the complete equipment asset renderer needs a 1.20.1 implementation.
4. Mixin targets and custom UI methods must be checked against the 1.20.1 classes and Forge's runtime environment.

## Validation

- Static audit: run after the changes; no Fabric API references or unsupported post-1.20.1 entity constants remain in Java source.
- Gradle compile attempt: **blocked before Gradle could start** because this execution environment has neither a `gradle` executable nor a project `gradlew` wrapper. No compiler result is available.
- Client launch/in-game test: not run.

This is still an incomplete source port, not a validated release JAR. Do not install it as a finished mod until a real Gradle build and client launch pass.

## Additional 1.20.1 compatibility pass

- Reworked `EntityRenderDispatcherMixin` to hook the legacy direct render method rather than the unavailable render-state extraction method.
- Replaced the inventory-screen render-state wrapper with head/return hooks around the 1.20.1 entity preview render method.
- Migrated derived textures to `AbstractTexture.load(ResourceManager)`, `TextureUtil.prepareImage`, and `NativeImage.upload`; added `TextureImageUtil` for loading resource images with the 1.20.1 API.
- Added `ArgbCompat` for the newer ARGB utility methods.
- Replaced `Entity.getXRot(float)` / `getYRot(float)` assumptions with explicit 1.20.1 interpolation and added vehicle compatibility helpers for boats and minecarts.
- Ported the settings screen to the older `Screen`/`PoseStack` API and registered it using Forge's config-screen extension point rather than Fabric ModMenu.
- Replaced post-1.20.1 wolf body-equipment rendering with an inactive optional layer, because vanilla 1.20.1 has no wolf armor item/body equipment slot.
- Removed newer player skin data types from `PlayerRendering` and changed skin overrides to use `ResourceLocation` and legacy `AbstractClientPlayer` accessors.

This pass does not resolve the GeckoLib 5 runtime/model migration or the newer renderer-state architecture. Those remain the critical blockers and are not represented as completed.

## Follow-up compatibility pass (2026-10-17)
- Removed the 1.21+ equipment-asset manager/component dependency from the player cape attachment decision. On 1.20.1, the equivalent decision now checks the actual chest item (`ElytraItem` / `ArmorItem`) rather than `DataComponents.EQUIPPABLE` and equipment asset layer types.
- Replaced the post-1.20.1 `PlayerSkin` value-object usage in the custom player render state with 1.20.1 `ResourceLocation` texture references for skin, cape, and elytra, using `AbstractClientPlayer`'s legacy texture accessors. Test cape overrides now only replace the cape texture.
- Switched `ShaderCompat` to Minecraft 1.20.1's `RenderType.eyes` API instead of the newer `RenderTypes` package.
- Re-ran resource JSON validation: 7,884 files, 0 syntax errors.

These changes remove several isolated post-1.20.1 API calls but do not complete the render-system port. The central renderer, custom layer, and animation sampling code still depends on GeckoLib 5 and the 1.21+/26.x render-state/submit-collector architecture; those sections require a structural rewrite for GeckoLib 4 + Minecraft 1.20.1.
- Replaced the newer `SoundEngine.play`/`PlayResult` mixin hook with Forge 1.20.1's `PlaySoundEvent`; the sound filter now uses `SoundInstance.getLocation()` and legacy entity positions. This preserves pack sound muting/replacement while avoiding a method signature that does not exist in 1.20.1.
