# Handoff — port Actions & Stuff a Forge 1.20.1 (estado 2026-10-18)

Continúa `SESSION_HANDOFF_2026-10-17.md`. Este documento es la referencia actual.

## Entorno

- Rama: `port/geckolib4-renderers` (último commit: `8f232da`).
- Versiones: Minecraft 1.20.1, Forge 47.4.10, Java 17, Gradle 8.8 (wrapper), ForgeGradle 6, GeckoLib 4.8.4, mappings `official`.
- Copia de trabajo del usuario: `~/Downloads/ans-port-1.20.1-forge-stage`.
- Compilación: `./gradlew compileJava --console=plain > after_X.log 2>&1`, luego `grep -c "error:" after_X.log`.
- Referencia de nombres 1.20.1 (Mojang): `client.txt` del client.jar (subido por el usuario, no está en el repo).
- Si el hook de git dice "unpushed commits" pero el remoto ya tiene el commit, refrescar la referencia de seguimiento:
  `git fetch origin port/geckolib4-renderers:refs/remotes/origin/port/geckolib4-renderers`.

## Progreso de errores (javac, `compileJava`)

| Punto | Errores |
|---|---|
| Inicio de la sesión anterior (`after_patch11`) | 1110 |
| Consumers de vértices reescritos (`3b773d1`) | 447 |
| Quitados harnesses de prueba (`296ee81`) | 428 |
| Capas Fox, Dolphin, Enderman (`5f0f0c6`) | 346 |
| Grupos de mobs (`ad5afc5`) | 309 |
| PlayerQueries y ExpressionSkins (`b132a69`) | 283 |
| Parrot de hombro (`07e22cf`, `8f232da`) | 260 |

## Patrón acordado

Las capas ya no se registran con `CompileReplacedEntityRenderLayersEvent` ni con render states de GeckoLib 5. Ahora son renders directos llamados desde `MobLayers.render(...)` (en `render/MobLayers.java`), que `ReplacedMobRenderer.render` invoca después del equipo. Para huesos, `MobLayers.bone(frame, model, name, requireVisible)`. Para transformar al hueso, `EquipmentLayer.toBone(...)`.

Otros helpers nuevos:
- `util/DyeColors.diffuse(DyeColor)`: reemplaza `getTextureDiffuseColor()` (1.20.1 solo tiene `getTextureDiffuseColors()`).
- `mob/groups/RegistryIndex.of(order, registry, value)`: reemplaza el índice basado en `Holder`.
- `MolangQueries.registerFactoryFor(type, name, factory)`: para lambdas curry (`var0 -> c -> ...`), evita la ambigüedad con `registerFor`.

Reglas de nombres 1.20.1 confirmadas en `client.txt`:
- `VillagerData`: `getProfession()`, `getType()`, `getLevel()`.
- `DyeColor`: `getId()`, `getTextureDiffuseColors()` (float[]).
- `Wolf.getWetShade(float)`; `Cat.isLying()`; `TropicalFish.getVariant()` devuelve `Pattern`; `Pattern.base()`.
- `Pufferfish.getPuffState()`; `Frog.getVariant()` devuelve `FrogVariant`.
- `ItemCooldowns` 1.20.1 se indexa por `Item`, no por grupo.
- `NativeImage`: `getPixelRGBA` / `setPixelRGBA`.
- Paquetes: `net.minecraft.world.entity.animal.{Pufferfish,Salmon,Squid,TropicalFish,Parrot}`, `net.minecraft.world.entity.vehicle.AbstractMinecart`.

## Commits de esta sesión

- `3b773d1` consumers de vértices (UvAnim, Normalizing, ReversedQuad, CullingQuad, StencilClip) reescritos a la API 1.20.1.
- `296ee81` eliminados `DevSelfTest`, `CritterSelfTest`, `MonsterSelfTest`, `ScenarioTest` y sus llamadas (solo se activaban con archivos bandera).
- `5f0f0c6` capas Fox, Dolphin y Enderman con `MobLayers`.
- `ad5afc5` grupos de mobs: índices, tintes, villager, potion, imports.
- `b132a69` PlayerQueries (cámara, cooldowns por Item, factories) y ExpressionSkins (NativeImage, `var17`).
- `07e22cf` + `8f232da` ShoulderParrotLayer como render directo (`Parrot` en `animal.Parrot`).

## Pendiente, por bloque (aprox. errores)

1. **Jugador (~110)**:
   - `FirstPerson` (24): depende de `RenderPassInfo`, `SubmitNodeCollector`, `PlayerRenderState`, `LayerSubmitter` y `AttachableRenderer` de GeckoLib 5, además del mixin `ItemInHandRendererAccessor` con API de 1.21 (`submitArmWithItem`, `itemModelResolver`, `renderMap`). Rediseñar la ruta de primera persona: el cuerpo con `LayerSubmitter` de GeckoLib 4 y las manos con `ItemInHandRenderer.renderArmWithItem` de 1.20.1.
   - `PlayerWingsLayer` (22): `EquipmentLayerRenderer`, `EquipmentClientInfo`, `DataComponents` (1.21). Portar como render directo.
   - `PlayerScenario` (25): `SkinTextureDownloader` y `Avatar` no existen en 1.20.1.
   - `PlayerRenderState` (5) y `PaperDoll` (2): borrar cuando ya no tengan uso.
2. **Dragón (34)**: `EnderDragonRenderer` usa `com.geckolib.renderer.base` (GeckoLib 5). También `MiscMobs` necesita `DragonFlightHistory` (no existe en 1.20.1).
3. **Guardian (19)**: `GuardianBeamLayer` usa render state y `GuardianRenderer.renderBeam` (no existe en 1.20.1). Reimplementar el rayo con el código de `GuardianRenderer.render` de vanilla 1.20.1.
4. **Adjuntos y equipo (~40)**: `AttachableRenderer` (19), `TextureMeshes` (10), `TrimTextures` (6), `Attachables` (5), `EquipmentQueries` (5), `EquipmentLayer`.
5. **Molang y gecko (~30)**: `BedrockQueries` (10), `BedrockTextures` (7), `AnsMathParser` (7), `AnsGeckoLoader` (6), `MolangMathValue` (4), `EngineQueries` (2).
6. **Resto (~30)**: `AnsSettingsScreen` (6), mixins (`ItemInHandRendererAccessor`, `ItemInHandRendererMixin`, `EntityRenderDispatcherMixin`), `BedrockParticles`, `EffectsOnlyRuntimes`, `Emitter`, `AnsSettings`, `MiscState`, `MiscMobs` (3).

## Decisiones pendientes del usuario

1. **Historial de vuelo del dragón** (`DragonFlightHistory`): propuesta — reconstruir el historial de posiciones dentro del mod, sin perder el efecto. Bloquea `EnderDragonRenderer` y `MiscMobs`.
2. **Salmón `getSalmonScale`**: no existe en 1.20.1. Dejarlo con la escala original fija o pasar la tabla del mod original.
3. **Caballo `getArmor()`**: no encontrado en el mapping. Buscar el método correcto cuando aparezca el error exacto (`PetMobs:165`).
4. **Lingering potion**: quitado (no es tipo de entidad en 1.20.1). ¿Reemplazar por nube de efecto de área?

## Aproximaciones a verificar

- `Cat` `lieonplayer`: usa `isLying()`, que no es exactamente "acostado sobre un jugador dormido".
- `Wolf` `is_shaking_wetness`: usa `getWetShade(1.0F)` en vez de la animación de sacudida original.
- `ExpressionSkins`: se declaró `var17 = false` porque se usaba sin declarar. Revisar que la ruta de éxito lo ponga en `true` como en el original.
- `ShoulderParrotLayer`: la firma `ParrotModel.renderOnShoulder(...)` se tomó de memoria de 1.20.1. Si falla al compilar, verificar en `client.txt`.

## Archivos de referencia en el repo

- `reference/SESSION_HANDOFF_2026-10-17.md`: handoff anterior.
- `reference/ERROR_ANALYSIS_2026-10-17.md`: análisis de errores iniciales.
- `reference/compatibility-inventory.md`, `reference/likely-new-api-references.txt`: inventario de APIs.
- Los logs de compilación y los `.jar` del usuario no están en el repo a propósito.
