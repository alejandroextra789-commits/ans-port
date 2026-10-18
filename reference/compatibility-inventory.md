# Focused compatibility inventory (Minecraft 26.2 source -> 1.20.1 target)

This file is generated from source-text searches; it is a triage aid, not proof that every listed use is incompatible or every incompatibility is listed.

## new render-state / submit APIs

- `src/main/java/com/ans/client/attachable/AttachableRenderer.java` — `SubmitNodeCollector`
- `src/main/java/com/ans/client/equipment/EquipmentLayer.java` — `SubmitNodeCollector`, `EntityRenderState`, `LivingEntityRenderState`, `HumanoidRenderState`, `ItemStackRenderState`
- `src/main/java/com/ans/client/mixin/EntityRenderDispatcherMixin.java` — `EntityRenderState`
- `src/main/java/com/ans/client/mixin/InventoryScreenMixin.java` — `EntityRenderState`
- `src/main/java/com/ans/client/mixin/ItemInHandRendererAccessor.java` — `SubmitNodeCollector`
- `src/main/java/com/ans/client/mixin/ItemInHandRendererMixin.java` — `SubmitNodeCollector`
- `src/main/java/com/ans/client/mob/aquatic/DolphinItemLayer.java` — `SubmitNodeCollector`, `EntityRenderState`, `LivingEntityRenderState`, `ItemStackRenderState`
- `src/main/java/com/ans/client/mob/aquatic/GuardianBeamLayer.java` — `SubmitNodeCollector`, `EntityRenderState`, `LivingEntityRenderState`
- `src/main/java/com/ans/client/mob/misc/CreakingRenderer.java` — `EntityRenderState`, `LivingEntityRenderState`
- `src/main/java/com/ans/client/mob/misc/EnderDragonRenderer.java` — `SubmitNodeCollector`, `OrderedSubmitNodeCollector`, `EntityRenderState`, `LivingEntityRenderState`, `CameraRenderState`
- `src/main/java/com/ans/client/mob/misc/SulfurCubeBlockLayer.java` — `SubmitNodeCollector`, `EntityRenderState`, `LivingEntityRenderState`
- `src/main/java/com/ans/client/mob/monster/EndermanCarriedBlockLayer.java` — `SubmitNodeCollector`, `EntityRenderState`, `LivingEntityRenderState`
- `src/main/java/com/ans/client/mob/nether/HappyGhastHarnessLayer.java` — `SubmitNodeCollector`, `EntityRenderState`, `LivingEntityRenderState`
- `src/main/java/com/ans/client/mob/pets/FoxHeldItemLayer.java` — `SubmitNodeCollector`, `EntityRenderState`, `LivingEntityRenderState`, `ItemStackRenderState`
- `src/main/java/com/ans/client/mob/pets/WolfArmorLayer.java` — `SubmitNodeCollector`, `EntityRenderState`, `LivingEntityRenderState`
- `src/main/java/com/ans/client/mob/player/FirstPerson.java` — `SubmitNodeCollector`, `EntityRenderState`, `LivingEntityRenderState`, `CameraRenderState`
- `src/main/java/com/ans/client/mob/player/PaperDoll.java` — `EntityRenderState`, `LivingEntityRenderState`
- `src/main/java/com/ans/client/mob/player/PlayerRenderState.java` — `HumanoidRenderState`
- `src/main/java/com/ans/client/mob/player/PlayerRenderer.java` — `SubmitNodeCollector`, `EntityRenderState`, `LivingEntityRenderState`, `CameraRenderState`
- `src/main/java/com/ans/client/mob/player/PlayerWingsLayer.java` — `SubmitNodeCollector`, `EntityRenderState`, `LivingEntityRenderState`
- `src/main/java/com/ans/client/mob/player/ShoulderParrotLayer.java` — `SubmitNodeCollector`, `EntityRenderState`, `LivingEntityRenderState`
- `src/main/java/com/ans/client/particle/BedrockParticles.java` — `CameraRenderState`
- `src/main/java/com/ans/client/particle/ParticleRenderTypes.java` — `RenderSetup`
- `src/main/java/com/ans/client/render/BedrockRenderTypes.java` — `RenderSetup`
- `src/main/java/com/ans/client/render/EffectsOnlyRuntimes.java` — `EntityRenderState`, `LivingEntityRenderState`
- `src/main/java/com/ans/client/render/LayerSubmitter.java` — `SubmitNodeCollector`, `OrderedSubmitNodeCollector`
- `src/main/java/com/ans/client/render/ReplacedEntityRenderer.java` — `SubmitNodeCollector`, `OrderedSubmitNodeCollector`, `EntityRenderState`
- `src/main/java/com/ans/client/render/ReplacedMobRenderer.java` — `SubmitNodeCollector`, `OrderedSubmitNodeCollector`, `EntityRenderState`, `LivingEntityRenderState`

## 1.20.5+ item data components / equipment assets

- `src/main/java/com/ans/client/attachable/Attachables.java` — `DataComponents`, `DyedItemColor`
- `src/main/java/com/ans/client/attachable/TrimTextures.java` — `DataComponents`, `Equippable`, `EquipmentAsset`
- `src/main/java/com/ans/client/equipment/EquipmentLayer.java` — `DataComponents`, `Equippable`, `EquipmentAsset`
- `src/main/java/com/ans/client/equipment/EquipmentQueries.java` — `DataComponents`
- `src/main/java/com/ans/client/mob/groups/ObjectMobs.java` — `DataComponents`
- `src/main/java/com/ans/client/mob/groups/PetMobs.java` — `DataComponents`
- `src/main/java/com/ans/client/mob/misc/SulfurCubeBlockLayer.java` — `DataComponents`, `BlockItemStateProperties`
- `src/main/java/com/ans/client/mob/nether/HappyGhastHarnessLayer.java` — `DataComponents`, `Equippable`
- `src/main/java/com/ans/client/mob/pets/WolfArmorLayer.java` — `DataComponents`, `Equippable`, `EquipmentAsset`, `EquipmentAssetManager`
- `src/main/java/com/ans/client/mob/player/FirstPerson.java` — `DataComponents`
- `src/main/java/com/ans/client/mob/player/PlayerQueries.java` — `ItemUseAnimation`
- `src/main/java/com/ans/client/mob/player/PlayerRenderer.java` — `DataComponents`, `Equippable`, `EquipmentAsset`, `EquipmentAssetManager`
- `src/main/java/com/ans/client/mob/player/PlayerWingsLayer.java` — `DataComponents`, `Equippable`
- `src/main/java/com/ans/client/molang/BedrockProperties.java` — `DataComponents`
- `src/main/java/com/ans/client/molang/EngineQueries.java` — `DataComponents`, `ChargedProjectiles`, `DyedItemColor`

## newer entity types absent in 1.20.1

- `src/main/java/com/ans/client/mixin/WalkAnimationMixin.java` — `Creaking`
- `src/main/java/com/ans/client/mob/groups/AquaticMobs.java` — `AbstractNautilus`, `EntityType.NAUTILUS`, `EntityType.ZOMBIE_NAUTILUS`
- `src/main/java/com/ans/client/mob/groups/MiscMobs.java` — `Creaking`, `SulfurCube`, `EntityType.PARCHED`, `EntityType.BREEZE`, `EntityType.CREAKING`, `EntityType.SNIFFER`
- `src/main/java/com/ans/client/mob/groups/NetherMobs.java` — `HappyGhast`
- `src/main/java/com/ans/client/mob/groups/ObjectMobs.java` — `EntityType.BREEZE`, `EntityType.WIND_CHARGE`, `EntityType.BREEZE_WIND_CHARGE`
- `src/main/java/com/ans/client/mob/groups/PetMobs.java` — `EntityType.CAMEL_HUSK`, `EntityType.ARMADILLO`
- `src/main/java/com/ans/client/mob/groups/UndeadMobs.java` — `EntityType.BOGGED`
- `src/main/java/com/ans/client/mob/groups/VillageMobs.java` — `CopperGolem`
- `src/main/java/com/ans/client/mob/misc/CreakingRenderer.java` — `Creaking`
- `src/main/java/com/ans/client/mob/misc/SulfurCubeBlockLayer.java` — `SulfurCube`
- `src/main/java/com/ans/client/mob/nether/HappyGhastHarnessLayer.java` — `HappyGhast`
- `src/main/java/com/ans/client/molang/EngineQueries.java` — `AbstractNautilus`

## newer texture/color/light APIs

- `src/main/java/com/ans/client/attachable/TextureMeshes.java` — `TextureContents`
- `src/main/java/com/ans/client/attachable/TrimTextures.java` — `TextureContents`
- `src/main/java/com/ans/client/bedrock/EntityRuntime.java` — `net.minecraft.util.ARGB`
- `src/main/java/com/ans/client/mob/groups/AquaticMobs.java` — `net.minecraft.util.ARGB`
- `src/main/java/com/ans/client/mob/groups/CritterMobs.java` — `ColorLerper`
- `src/main/java/com/ans/client/mob/misc/EnderDragonRenderer.java` — `net.minecraft.util.ARGB`
- `src/main/java/com/ans/client/mob/player/ExpressionSkins.java` — `ClientAsset`
- `src/main/java/com/ans/client/mob/player/PlayerRenderer.java` — `ClientAsset`
- `src/main/java/com/ans/client/mob/player/PlayerRendering.java` — `ClientAsset`
- `src/main/java/com/ans/client/particle/BedrockParticles.java` — `LightCoordsUtil`
- `src/main/java/com/ans/client/particle/SurfaceQueries.java` — `BlockTintSource`
- `src/main/java/com/ans/client/render/BedrockTextures.java` — `TextureContents`
- `src/main/java/com/ans/client/render/LayerSubmitter.java` — `net.minecraft.util.ARGB`
