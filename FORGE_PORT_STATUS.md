# Forge 1.20.1 port status

The workspace contains recovered Actions & Stuff Java source/resources with a Forge 1.20.1 build configuration. It remains an in-progress source port; no working JAR is claimed.

## Latest adaptation pass
- Moved the effects-only animation hook to Minecraft 1.20.1's direct `EntityRenderDispatcher.render` call and replaced the inventory preview's newer render-state wrapper with the 1.20.1 inventory preview entry point.
- Replaced the newer texture-content/reloadable-texture APIs with 1.20.1 `AbstractTexture`, `TextureUtil`, `NativeImage`, and a shared resource-image loader for composite textures, trim textures, and texture-mesh analysis.
- Added ARGB compatibility helpers and replaced newer entity rotation overloads with explicit interpolation against `xRotO` / `yRotO`.
- Replaced the newer vehicle base class with 1.20.1 boat/minecart checks; adapted player settings screen/key binding APIs and registered the settings screen through Forge's config-screen extension point instead of Fabric ModMenu.
- Replaced later wolf/body equipment-slot assumptions with 1.20.1 horse armor and llama carpet access. The optional wolf-armor equipment layer is intentionally inactive because wolf armor/body equipment is not available in 1.20.1.
- Removed the later `Leashable`, `ColorLerper`, `ItemUseAnimation`, `VehicleEntity`, `ItemTags.SPEARS`, and related API references where equivalent 1.20.1 behavior is available.
- Rechecked all 7,884 resource JSON files: zero JSON syntax errors. ZIP integrity is checked before delivery.

## Remaining blockers before the source port can be considered adapted
1. The recovered code is built around GeckoLib 5 (`com.geckolib.*`) while the target uses GeckoLib 4.8.4 (`software.bernie.geckolib.*`). This includes the animation data model/runtime and custom resource loader, not just imports.
2. The custom renderers/layers still depend on Minecraft's newer `EntityRenderState`, `SubmitNodeCollector`, `RenderPassInfo`, `ItemStackRenderState`, and equipment asset APIs. These need a real rewrite to 1.20.1 entity renderer signatures and `PoseStack`/`MultiBufferSource` rendering.
3. The player renderer and first-person rendering path use the newer skin, camera, item-model, and render-state contracts.
4. No Gradle executable or project wrapper is present in this environment; the environment also cannot resolve Maven hosts. A dependency-backed compile and in-game launch have not been performed.

## Validation performed
- JSON parsing: 7,884 files, 0 syntax errors.
- Java source syntax scan: no parser-pattern errors detected, but `javac` cannot resolve Minecraft/Forge/GeckoLib because their dependencies are not installed; its symbol diagnostics are not a compilation result.
- Build/runtime validation: NOT performed.

Do not install this workspace as a finished mod until the remaining API rewrite, a real Gradle build, and a client launch are complete.

## Additional compatibility pass
- Player cape/skin/elytra texture state now uses legacy `ResourceLocation` values and `AbstractClientPlayer` texture getters instead of the newer `PlayerSkin` data object.
- Player cape suppression checks the actual chest item for Elytra/armor, avoiding the 1.21+ equipment-asset component API.
- Shader compatibility uses legacy `RenderType.eyes`.
- JSON validation still passes (7,884/7,884).

Remaining architecture blockers are unchanged: custom rendering and animation sampling still rely on GeckoLib 5 classes and Minecraft's newer render-state/submit-collector APIs. This is not yet a completed or compile-verified port.
- Sound muting/replacement now hooks Forge's 1.20.1 `PlaySoundEvent`; removed the incompatible `SoundEngine.PlayResult` mixin and switched to `SoundInstance.getLocation()`.
