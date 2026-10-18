# Handoff: port de Actions & Stuff a Forge 1.20.1 (sesión 2026-10-17)

Leer primero este archivo. Describe el estado real verificado, no el deseado.

## Repo y ramas

- Repo: `alejandroextra789-commits/ans-port` (público).
- `main`: commit `1e85fdb` "Port ANS a Forge 1.20.1" (subida inicial del usuario, sin compilar).
- Rama de trabajo: `port/geckolib4-renderers`, con 3 commits sobre `main`:
  - `6d4b8a1` Forge 47.3.0 → 47.4.10 en `gradle.properties`.
  - `7e7c972` `build.gradle`: `-Xmaxerrs 5000` en `compilerArgs` para ver todos los errores de javac.
- Ninguna reescritura de código Java todavía. La rama solo tiene cambios de build.

## Versiones (decididas y confirmadas por el usuario)

- Minecraft 1.20.1, Java 17, Forge **47.4.10** (el usuario tiene 47.4.10 instalado).
- GeckoLib Forge 4.8.4 (`geckolib-forge-1.20.1-4.8.4_1.jar`).
- Gradle 8.8 vía wrapper (generado con `gradle wrap --gradle-version 8.8`; `gradlew` y `gradle/wrapper/` están en la carpeta del usuario, NO commiteados aún).

## Estado de compilación (verificado en la PC del usuario)

- `./gradlew compileJava` **resuelve todas las dependencias** (Forge, Minecraft, GeckoLib, librerías). Llega a javac.
- Resultado: **1.110 errores de código** (javac mostró 100 con el límite por defecto). Con `7e7c972` debería listar todos.
- Solo avisos de deprecación (`ResourceLocation(String,String)`, `FMLJavaModLoadingContext.get()`), no errores.
- Log de referencia del usuario: `baseline.log` (516 líneas, 2m30s). Se subió como `2acdbd1c-attachment.txt` en la sesión anterior; puede no estar disponible en la nueva sesión.

## Problemas de entorno ya resueltos (no repetir)

- Gradle del sistema 8.1.1 no corre con JDK 21 (`Unsupported class file major version 65`). Solución: JDK 17 para Gradle, o usar el wrapper 8.8.
- `gradle wrap` debe ejecutarse dentro de la carpeta del proyecto, no en `~`.
- La primera compilación tarda varios minutos (descarga y remapeo de Forge/Minecraft).
- Redirigir `./gradlew` a archivo sin `--console=plain` deja salida con barras de progreso ilegibles. Usar `--console=plain`.

## Verificaciones hechas

- `client.jar` del usuario = Minecraft 1.20.1 oficial (`version.json` id 1.20.1, Java 17, protocolo 763). Las clases están ofuscadas.
- `client.txt` = mapeo oficial de Mojang para 1.20.1 (formato `named -> obf:`), sí contiene `LivingEntityRenderer` (`fpz`), `EntityRenderer` (`fox`), `EntityRendererProvider$Context` (`foy$a`), `MultiBufferSource` (`fjx`), `RenderLayer` (`ftg`).
- `joined.tsrg` que subió el usuario mapea obf → **SRG** (`C_xxxx_`), no a nombres de Mojang. NO usar para nombrar clases.
- Ejemplo verificado con javap: `fpz` (LivingEntityRenderer) → `LivingEntityRenderer(EntityRendererProvider.Context, EntityModel, float)`, `render(LivingEntity, float, float, PoseStack, MultiBufferSource, int)`, `getRenderType(LivingEntity, boolean, boolean, boolean)`.
- GeckoLib 4.8.4 JAR (`software.bernie.geckolib`): `GeoReplacedEntityRenderer<E extends Entity, T extends GeoAnimatable>` extiende `EntityRenderer<E>`; constructor `(Context, GeoModel<T>, T)`; métodos `preRender`, `actuallyRender`, `applyRenderLayers`, `renderFinal`, `renderRecursively`, `doPostRenderCleanup`; `GeoAnimatable` con `registerControllers`, `getAnimatableInstanceCache`, `getTick(Object)`.
- Los paquetes `com.geckolib.*` del código NO existen en GeckoLib 4.8.4.

## Hallazgos sobre el código

- Inventario (medido en la rama): 130 archivos Java; `com.geckolib` 138 líneas en 27 archivos; `EntityRenderState` 84 líneas en 14 archivos; `SubmitNodeCollector` 55 líneas en 16 archivos.
- `reference/compatibility-inventory.md` y `reference/likely-new-api-references.txt` agrupan el resto (DataComponents, Equippable, Creaking, Sulfur Cube, Happy Ghast, Nautilus, Breeze, etc.).
- `ReplacedMobRenderer` (153 líneas) y `ReplacedEntityRenderer` (114 líneas) dependen de `RenderPassInfo`, `BoneSnapshots`, `DataTicket`, `getGeckolibData`, `OrderedSubmitNodeCollector`: todo eso es GeckoLib 5/26.2. No es renombrar: hay que cambiar cómo se guarda el estado por frame (`FrameResult`) porque GeckoLib 4 no lo guarda.
- `LayerSubmitter` (448 líneas) usa `SubmitNodeCollector`; hay que rehacerlo con `PoseStack` + `MultiBufferSource`.
- `MobAnimatable` y `MobGeoModel` ya están adaptados a GeckoLib 4 (de la sesión anterior).

## Qué falta (orden recomendado)

1. Leer la lista completa de 1.110 errores agrupada por archivo.
2. Reescribir `ReplacedEntityRenderer` y `ReplacedMobRenderer` contra `GeoReplacedEntityRenderer` (GeckoLib 4), guardando el estado por frame de otra forma (p. ej. mapa por entidad o campos en el renderer).
3. Reescribir `LayerSubmitter` para renderizado inmediato (`PoseStack`, `MultiBufferSource`, `RenderType`).
4. Adaptar capas (`EquipmentLayer`, `PlayerWingsLayer`, `ShoulderParrotLayer`, `DolphinItemLayer`, etc.) y `EntityRuntime`.
5. Quitar o reemplazar mobs que no existen en 1.20.1 (Creaking, Sulfur Cube, Happy Ghast, Nautilus, Breeze, Bogged, Parched, Armadillo, Camel Husk, Sniffer, Copper Golem).
6. Equipo: `DataComponents`, `Equippable`, `EquipmentAsset`, `ItemUseAnimation`, `ChargedProjectiles` → equivalentes de 1.20.1 o documentar la diferencia.
7. Primera persona (`ItemInHandRendererMixin`, `FirstPerson`), `PlayerRenderer`, `PaperDoll`, mixins (revisar descriptores y `required`).
8. Cuando compile: probar en cliente 1.20.1 con Forge 47.4.10 (animación de una entidad, attachables, jugador, primera persona, partículas).

## Estimación dada al usuario

- Hasta que compile: 3 a 5 sesiones de compilar y corregir.
- Hasta que funcione en juego: 1 a 2 semanas más de pruebas en su PC.

## Notas de flujo con el usuario

- El usuario compila en su PC (Ubuntu, usuario `alejandro`, carpeta `~/Downloads/ans-port-1.20.1-forge-stage`). Yo NO puedo ver su PC.
- Este entorno no tiene acceso a repos de Gradle, Maven ni Forge (proxy), por eso la compilación la hace el usuario.
- Push: el repo se añadió a la sesión con permiso de escritura tras reconectar GitHub; el push funcionó (`7e7c972`).
- Hook `stop-hook-git-check.sh` avisa de commits sin push según la copia del repo que revisa. Si avisa, primero `git pull` en la copia del usuario y confirmar con `git ls-remote origin port/geckolib4-renderers`.
- El usuario prefiere respuestas cortas, en español, sin tecnicismos innecesarios.
