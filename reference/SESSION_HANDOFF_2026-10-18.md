# Handoff — port Actions & Stuff a Forge 1.20.1 (actualizado 2026-10-18, 00:33)

Continúa `SESSION_HANDOFF_2026-10-17.md`. Este documento reemplaza el anterior de la misma fecha.

## Instrucciones para el siguiente paso (usuario)

1. Estar en la copia de trabajo: `~/Downloads/ans-port-1.20.1-forge-stage`.
2. Traer cambios: `git pull` (rama `port/geckolib4-renderers`, último commit `8706df6` o posterior).
3. Compilar y guardar el log:
   `./gradlew compileJava --console=plain > after_X.log 2>&1`
4. Conteo: `grep -c "error:" after_X.log`
5. Desglose por archivo: `grep "error:" after_X.log | cut -d: -f1 | sort | uniq -c | sort -rn | head -30`
6. Para un archivo concreto (con símbolos): `grep -A5 "NOMBRE.java" after_X.log | grep -E "error:|symbol:|location:" | head -150`
7. Pasar el resultado al asistente. No hace falta subir el log completo (si el chat no lo acepta).

Si el hook de git dice "unpushed commits" pero el remoto ya tiene el commit:
`git fetch origin port/geckolib4-renderers:refs/remotes/origin/port/geckolib4-renderers`

## Entorno

- Versiones: Minecraft 1.20.1, Forge 47.4.10, Java 17, Gradle 8.8 (wrapper), ForgeGradle 6, GeckoLib 4.8.4, mappings `official`.
- Referencia de nombres 1.20.1: `client.txt` del client.jar (subido por el usuario, NO está en el repo). Los logs y jars tampoco están en el repo a propósito.
- En 1.20.1 existe `GuiGraphics` (la pantalla usa `render(GuiGraphics, ...)`), pero NO existe `SubmitNodeCollector`, `RenderPassInfo`, `LivingEntityRenderState`, `EquipmentLayerRenderer`, `DataComponents`, `ItemModelResolver`, ni `EntityRenderer<E, S>` (un solo parámetro).

## Progreso de errores (javac)

| Punto | Errores |
|---|---|
| Inicio (sesión anterior) | 1110 |
| Consumers de vértices (`3b773d1`) | 447 |
| Quitados harnesses de prueba (`296ee81`) | 428 |
| Capas Fox, Dolphin, Enderman (`5f0f0c6`) | 346 |
| Grupos de mobs (`ad5afc5`) | 309 |
| PlayerQueries, ExpressionSkins (`b132a69`) | 283 |
| Parrot de hombro (`07e22cf`, `8f232da`) | 260 |
| Settings, config factory, Molang (`cf8391b`) | 239 |
| Registro, mixins, emitter, crossbow, pesca (`aed7347`, `8706df6`) | 228 |

## Patrón acordado

- Las capas ya no usan `CompileReplacedEntityRenderLayersEvent` ni render states de GeckoLib 5. Son renders directos llamados desde `MobLayers.render(...)` (`render/MobLayers.java`), que `ReplacedMobRenderer.render` invoca después del equipo. Huesos: `MobLayers.bone(frame, model, name, requireVisible)`. Transformación: `EquipmentLayer.toBone(...)`.
- Para lambdas curry (`var0 -> c -> ...`) usar `MolangQueries.registerFactoryFor(...)` (evita la ambigüedad con `registerFor`). Ya aplicado en PlayerQueries, NetherMobs y en las demás llamadas con lambda ambigua con cast `(MolangQueries.Factory)`.
- Helpers nuevos: `util/DyeColors.diffuse(DyeColor)`, `mob/groups/RegistryIndex.of(order, registry, value)`.
- Reglas 1.20.1 confirmadas en `client.txt`: `VillagerData.getProfession/getType/getLevel`; `DyeColor.getId/getTextureDiffuseColors`; `Wolf.getWetShade(float)`; `Cat.isLying()`; `TropicalFish.getVariant()` → `Pattern`, `Pattern.base()`; `Pufferfish.getPuffState()`; `Frog.getVariant()` → `FrogVariant`; `ItemCooldowns` por `Item`; `NativeImage.getPixelRGBA/setPixelRGBA`; `Camera.getPosition/getXRot/getYRot`; `GameRenderer.getMainCamera()`; `Level.getDayTime/getMoonPhase`; `WalkAnimationState.update(float,float)`; `Saddleable`; `ElytraItem`.

## Hecho en esta sesión

- `3b773d1` consumers de vértices (UvAnim, Normalizing, ReversedQuad, CullingQuad, StencilClip) a la API 1.20.1.
- `296ee81` eliminados DevSelfTest, CritterSelfTest, MonsterSelfTest, ScenarioTest y sus llamadas (solo se activaban con archivo bandera en la carpeta del juego).
- `5f0f0c6` capas Fox, Dolphin, Enderman con `MobLayers`.
- `ad5afc5` grupos de mobs.
- `b132a69` PlayerQueries, ExpressionSkins, `MolangQueries.registerFactoryFor`.
- `07e22cf` + `8f232da` ShoulderParrotLayer como render directo (`net.minecraft.world.entity.animal.Parrot`).
- `cf8391b` AnsSettingsScreen (GuiGraphics, CycleButton 1.20.1), AnsForge (`ConfigScreenHandler.ConfigScreenFactory`), BedrockQueries (cámara, reloj, luna, entidades, `Saddleable`).
- `aed7347` + `8706df6`: ClientRegistry (`net.minecraftforge.fml.client.registry`), crossbow (`ChargedProjectiles` de NBT), pesca (`holdingArm` propio), emitter (variable `rad`, `collideBoundingBox(null, ...)`), WalkAnimationMixin, `EntityRenderer<?>` (un parámetro), `SoundSource.MASTER` en vez de `UI`.

## Pendiente, por bloque (errores aprox., log del último conteo)

1. **Jugador (~90)**:
   - `FirstPerson` (24): depende de `RenderPassInfo`, `SubmitNodeCollector`, `PlayerRenderState`, `LayerSubmitter` y `AttachableRenderer` de GeckoLib 5, y del mixin `ItemInHandRendererAccessor` (`submitArmWithItem`, `itemModelResolver`, `renderMap`) con API 1.21. Rediseñar la primera persona: cuerpo con `LayerSubmitter` de GeckoLib 4 y manos con `ItemInHandRenderer.renderArmWithItem` de 1.20.1.
   - `PlayerWingsLayer` (22): usa `Equippable`/`DataComponents`. Decidir de dónde salen las texturas de alas (¿Elytra vanilla o mapeo del mod?) y portarlo como render directo.
   - `PlayerScenario` (25): `SkinTextureDownloader`, `Avatar` (no existen en 1.20.1). También la mixin `SkinTextureDownloaderMixin` (2) depende de esa clase: hay que quitarla de `ans.client.mixins.json` y borrar el archivo cuando `PlayerScenario` ya no la use.
   - `PlayerRenderState` (5) y `PaperDoll` (2): borrar cuando nada los use.
   - `ItemInHandRendererMixin` (3), `ItemInHandRendererAccessor` (5): mismo bloque que `FirstPerson`.
2. **Dragón (34)**: `EnderDragonRenderer` usa `com.geckolib.renderer.base`. `MiscMobs` (3) necesita `DragonFlightHistory` (no existe en 1.20.1).
3. **Guardian (19)**: `GuardianBeamLayer` usa render state y `GuardianRenderer.renderBeam` (no existe). Reimplementar el rayo con el `GuardianRenderer.render` de vanilla 1.20.1.
4. **Adjuntos y equipo (~40)**: `AttachableRenderer` (19), `TextureMeshes` (10), `TrimTextures` (6), `Attachables` (5), `EquipmentQueries` (5).
5. **Gecko y Molang (~25)**: `BedrockTextures` (7), `AnsMathParser` (7), `AnsGeckoLoader` (6), `MolangMathValue` (4), `EngineQueries` (restante), `BedrockQueries`.
6. **Sonido**: `LocalPlayerSounds` (1): usa `extractEntity` (1.21) y `getDeltaTracker` (no existe). Rediseñar cómo se disparan los sonidos del jugador local.
7. **Resto**: `BedrockParticles` (3), `EffectsOnlyRuntimes` (3), `MiscState` (1), `AnsSettings` (revisar si `ClientRegistry` ya compila), `PaperDoll`.

## Decisiones pendientes del usuario

1. **Historial de vuelo del dragón** (`DragonFlightHistory`): propuesta — reconstruir el historial de posiciones dentro del mod. Bloquea `EnderDragonRenderer` y `MiscMobs`.
2. **Salmón `getSalmonScale`**: no existe en 1.20.1. ¿Escala fija del mod original?
3. **Caballo `getArmor()`**: no está en el mapping. Buscar el método correcto cuando salga el error exacto (`PetMobs:165`).
4. **Lingering potion**: quitado (no es tipo de entidad en 1.20.1). ¿Reemplazar por nube de área?
5. **Alas**: ¿de dónde sale la textura (Elytra vanilla o mapeo del mod)?

## Aproximaciones a verificar al probar

- `Cat` `lieonplayer`: `isLying()` no es "acostado sobre un jugador dormido".
- `Wolf` `is_shaking_wetness`: `getWetShade(1.0F)` en vez de la sacudida original.
- `WalkAnimationMixin`: el multiplicador de bebé va sobre la velocidad objetivo.
- `SoundSource.UI` → `MASTER`: los sonidos de interfaz ahora responden al volumen maestro.
- `ExpressionSkins`: `var17` se declaró en `false`; revisar que la ruta de éxito lo ponga en `true` como en el original.
- `ShoulderParrotLayer`: la firma `ParrotModel.renderOnShoulder(...)` se tomó de memoria; si falla, verificar en `client.txt`.
- Lingering potion quitado; Dolphin/Fox/Enderman usan `renderStatic` de 1.20.1.
- Commit `aed7347` dice que incluye emitter y mixins, pero esos cambios están en `8706df6`.

## Archivos de referencia

- `reference/SESSION_HANDOFF_2026-10-17.md`: handoff anterior.
- `reference/ERROR_ANALYSIS_2026-10-17.md`: análisis de errores iniciales.
- `reference/compatibility-inventory.md`, `reference/likely-new-api-references.txt`: inventario de APIs.
