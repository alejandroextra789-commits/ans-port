# Build and API migration status — Minecraft 1.20.1 Forge

## Current status

The current workspace has not had a successful Gradle compilation. The earlier source-recovery stage produced compiler diagnostics, but the latest environment did not have a Gradle executable or project wrapper available, so no new dependency-backed build was run. Static checks must not be described as a compile result.

## Main incompatibility groups

1. **GeckoLib generation mismatch.** The recovered code targets GeckoLib 5 (`com.geckolib.*`) while Minecraft 1.20.1 uses GeckoLib 4 (`software.bernie.geckolib.*`). Animation data structures, resource loading, render layers, and renderer hooks need a real adaptation; global package replacement is unsafe.
2. **Minecraft renderer mismatch.** `EntityRenderState`, `LivingEntityRenderState`, `ItemStackRenderState`, `RenderPassInfo`, and `SubmitNodeCollector` belong to the newer rendering architecture. Minecraft 1.20.1 uses direct entity render methods and immediate rendering through `PoseStack` / `MultiBufferSource` / `VertexConsumer`.
3. **First-person and equipment rendering.** Item rendering, equipment assets, and some mixin targets use later Minecraft signatures and must be rebuilt around 1.20.1 APIs.
4. **Runtime animation sampling.** `EntityRuntime` samples GeckoLib 5 baked animation/controller structures. This is coupled to the renderer mismatch and is one of the central pieces requiring a compatibility implementation.

## Current static audit

- Fabric API references: 0.
- New GeckoLib namespace references: 144 occurrences across 29 files.
- `EntityRenderState` references: 88 occurrences across 14 files.
- `SubmitNodeCollector` references: 58 occurrences across 16 files.
- Unsupported 1.21+ entity constants: 0.
- Resource JSON validation: 7,884 files, 0 syntax errors.
- ZIP integrity: passes.

These are inventory checks only, not evidence of successful compilation or runtime behavior.

## Next implementation sequence

1. Replace the GeckoLib 5 animation sampling layer with a GeckoLib 4-compatible adapter or a self-contained Bedrock animation sampler.
2. Port the core replaced-entity renderer to direct 1.20.1 render calls and make frame data available without modern render-state objects.
3. Rewrite `LayerSubmitter` and attachable/equipment layers for legacy immediate rendering.
4. Port first-person and special entity layers, then update mixin targets.
5. Only after these changes, run Gradle build and fix the actual compiler diagnostics, then launch a client for runtime validation.
