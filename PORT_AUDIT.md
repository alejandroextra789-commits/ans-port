# Actions & Stuff — current port audit (Minecraft 26.2 Fabric → Minecraft 1.20.1 Forge)

## Source and project

- Original artifact: `actionandstuff0_5_0.jar`, recovered from the Minecraft 26.2 / Java 25 / Fabric build.
- Decompiled source is reconstructed bytecode, not the original developer source.
- Current project target: Minecraft 1.20.1, Forge 47.3.0, Java 17, GeckoLib Forge 4.8.4.
- Current Java source count: 130 files.
- Current state: incomplete source port. No working JAR or successful compilation is claimed.

## Completed compatibility work

- Replaced the newer render-pipeline helpers with the closest available 1.20.1 `RenderType` equivalents for the supported paths.
- Moved particle rendering to Forge's `RenderLevelStageEvent` and legacy `PoseStack` / `MultiBufferSource` vertex emission.
- Replaced several newer resource/texture APIs with 1.20.1 `AbstractTexture`, `TextureUtil`, `NativeImage`, and resource-manager access.
- Adapted several player texture lookups to `ResourceLocation`-based skin/cape/elytra APIs available in 1.20.1.
- Replaced the newer sound-engine result hook with Forge 1.20.1 `PlaySoundEvent`.
- Removed Fabric API references and direct registrations for mobs/projectiles that do not exist in 1.20.1.
- JSON validation: 7,884 resource JSON files parsed successfully with zero syntax errors.
- ZIP integrity: latest project archive passes `unzip -t`.

## Remaining blockers

1. **GeckoLib 5 → GeckoLib 4:** 144 `com.geckolib.*` references remain across 29 files. These include the custom animation loader, baked animation structures, animation sampling, model access, and render layers. A package rename is not enough.
2. **Render-state architecture:** 88 `EntityRenderState` references remain across 14 files. Minecraft 1.20.1 uses direct entity renderer calls rather than the newer render-state extraction/submission pipeline.
3. **Deferred submit collectors:** 58 `SubmitNodeCollector` references remain across 16 files. They must be replaced by immediate rendering using `PoseStack`, `MultiBufferSource`, `VertexConsumer`, and 1.20.1-compatible GeckoLib hooks.
4. **Equipment rendering:** a small set of `DataComponents`, equipment-asset, and item-model-resolver calls remains in equipment and first-person rendering code.
5. **First-person rendering and custom layers:** their mixins and rendering paths still use modern signatures and need a structural rewrite.

## Verification limits

- Static migration audit only; it does not prove that Java compiles.
- No dependency-backed Gradle build or in-game launch has been performed in this workspace.
- Do not install this as a finished mod until the remaining API rewrite, a real build, and a client launch succeed.

## Porting strategy

Keep original behavior where an equivalent 1.20.1 API exists. Where the newer renderer or equipment feature has no equivalent, implement the closest legacy rendering path rather than deleting unrelated animation, sound, or resource behavior. The central task is now a renderer/animation runtime rewrite, not a mass rename.

## Decompilation and rights note

Decompiled Java may not perfectly reconstruct source-level intent. The mod's assets may be subject to third-party rights; redistribute them only if permitted.
