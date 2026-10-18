# Renderer/animation migration work map (fresh audit 2026-10-17)

This is a task map for the next implementation pass, not a claim that the port is complete.

## Current inventory

- 130 Java source files.
- 7,884 JSON files under `src/main/resources` (all parsed without syntax errors); the ZIP has 7,887 `.json` entries including reference/audit JSON, and its integrity check passed.
- GeckoLib-5-era namespace: 144 occurrences / 29 files.
- Minecraft newer renderer state: `EntityRenderState` 88 / 14 files.
- Deferred submission API: `SubmitNodeCollector` 58 / 16 files.
- Gecko render pass/state glue: `RenderPassInfo` 49 / 14 files.
- Modern item render-state use: `ItemStackRenderState` 25 / 3 files.
- Modern equipment component/asset references: `DataComponents` 4 / 2; `EquipmentAsset` 2 / 1; `Equippable` 6 / 2.

## Required dependency and access constraint

The target artifact is GeckoLib Forge 1.20.1 4.8.4 (`software.bernie.geckolib:geckolib-forge-1.20.1:4.8.4`). Its published coordinates and availability were confirmed from the public package listing, but the binary is not present in this workspace or local Gradle cache. Direct Maven/Gradle hosts cannot be resolved from this execution environment. Do not claim binary inspection or compile validation.

## Rewrite order and files

### A. Runtime/animation adapter (foundational; no mechanical namespace replacement)
- `bedrock/EntityRuntime.java`
- `bedrock/AnimationLookup.java`
- `bedrock/BoneTransforms.java`
- `bedrock/RelativeBones.java`
- `bedrock/FrameResult.java`
- `gecko/AnsGeckoLoader.java`, `gecko/AnsMathParser.java`, `gecko/MolangMathValue.java`
- `render/MobAnimatable.java`, `render/MobGeoModel.java`

Target: preserve pack animation definitions, Molang evaluation, blend/controller semantics, bone transforms and frame caching while moving to GeckoLib 4's `GeoAnimatable`/`AnimatableManager`/`GeoModel`/`AnimationProcessor` interfaces. If GeckoLib 4 cannot represent a recovered custom loader feature, implement a local Bedrock sampler and state that feature-specific limitation.

### B. Central entity render entry points
- `render/ReplacedMobRenderer.java`
- `render/ReplacedEntityRenderer.java`
- `render/LayerSubmitter.java`
- `render/EffectsOnlyRuntimes.java`

Target: replace render-state extraction and deferred submit collectors with 1.20.1 direct render signatures and `PoseStack` + `MultiBufferSource` + `VertexConsumer`; retain shadow, visibility, ghost/fade, per-bone render modes, particle capture, animation frame association, and vanilla renderer fallbacks.

### C. Attachment/equipment
- `attachable/AttachableRenderer.java`, `attachable/Attachables.java`, `attachable/TextureMeshes.java`
- `equipment/EquipmentLayer.java`
- all custom mob item/armor layers listed in static audit.

Target: use actual entity/ItemStack at render time, legacy item renderer contexts and vanilla armor model/layer APIs. Preserve attachment-to-bone matrices and custom geo armor where GeckoLib 4 supports it.

### D. Player/first person and mixins
- `mob/player/PlayerRenderer.java`, `FirstPerson.java`, `PlayerWingsLayer.java`, `ShoulderParrotLayer.java`, `PaperDoll.java`
- `mixin/ItemInHandRendererMixin.java`, `ItemInHandRendererAccessor.java`
- special layers: dragon, enderman, fox, dolphin, guardian.

Target: convert modern state inputs to live 1.20.1 entities/players, re-target mixins to verified 1.20.1 methods, and render immediately with vanilla buffers. Check mixin injection descriptors and required injection counts against target mappings.

## Safe acceptance gates

1. No `com.geckolib.*` imports/classes that are absent in GeckoLib 4.
2. No 1.21+/26.x renderer state, submit collector, item render-state, equipment asset types in production Java.
3. No empty/no-op replacements for formerly functional render or animation paths.
4. JSON/resource references and model/animation/texture identifiers audited after source rewrites.
5. Dependency-backed Gradle build and client test are separate gates; static audit must not be reported as either.
