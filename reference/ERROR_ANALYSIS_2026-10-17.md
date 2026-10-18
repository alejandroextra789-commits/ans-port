# Análisis de errores de compilación (javac, 1.20.1 + Forge 47.4.10 + GeckoLib 4.8.4)

Fuente: `errores_por_archivo.txt` generado por el usuario con `./gradlew compileJava --console=plain` en la rama `port/geckolib4-renderers` (commit `59ba2ef`).

Total de líneas agrupadas: 921 (≈1.110 errores según javac).

## Por tipo de error

- 663 `cannot find symbol`: mayormente clases de GeckoLib 5 y 26.2 que no existen en 1.20.1 o en GeckoLib 4.
- 32 `package com.geckolib.cache.model does not exist`
- 27 `package com.geckolib.renderer.base does not exist`
- 17 `package net.minecraft.client.renderer.entity.state does not exist` (EntityRenderState, LivingEntityRenderState, HumanoidRenderState: no existen en 1.20.1)
- 10 `package com.geckolib.constant.dataticket does not exist`
- 10 `reference to registerFor is ambiguous`
- 10 `incompatible types: cannot infer type-variable(s) E`
- 7 `package net.minecraft.client.renderer.rendertype does not exist` (RenderType en 1.20.1 está en `net.minecraft.client.renderer.RenderType`)
- 7 `package com.geckolib.renderer.layer does not exist`
- 6 `wrong number of type arguments; required 1`
- 6 `package com.geckolib.cache.animation does not exist`
- 6 `package com.geckolib.animation.state does not exist`
- 4 `package com.geckolib.event.replacedentity...` (`CompileReplacedEntityRenderLayersEvent` no existe en GeckoLib 4.8.4)
- 3 `package net.minecraft.world.item.equipment does not exist` (1.20.5+)
- 3 `package net.minecraft.client.renderer.state.level does not exist`
- 3 `package net.minecraft.world.entity.animal.parrot.Parrot does not exist` (en 1.20.1 la clase es `Parrot` en `net.minecraft.world.entity.animal.Parrot`)
- 2 `package net.minecraft.world.entity.animal.fish does not exist` (en 1.20.1: `net.minecraft.world.entity.animal.*`)
- 2 `package net.minecraft.core.component does not exist` (1.20.5+)
- 2 `package net.minecraft.client.resources.model.EquipmentClientInfo does not exist` (1.20.5+)
- 2 `incompatible types: PoseStack cannot be converted to GuiGraphics` (API de GUI distinta)
- 2 `method getPattern in class TropicalFish cannot be applied` (firma distinta en 1.20.1)
- 7 `variable f is already defined in method run(Minecraft,IntegratedServer,String)` y 2 más en el mismo método: probablemente mixin o copia; revisar.

## Por archivo (top)

| Errores | Archivo | Causa principal |
|---|---|---|
| 138 | `equipment/EquipmentLayer.java` | Equipo 1.20.5+ (DataComponents, Equippable, EquipmentAsset) + GeckoLib 5 |
| 76 | `attachable/AttachableRenderer.java` | SubmitNodeCollector + GeckoLib 5 |
| 65 | `mob/player/PlayerRenderer.java` | Render state 26.2 + GeckoLib 5 |
| 54 | `render/LayerSubmitter.java` | SubmitNodeCollector (render pipeline 26.2) |
| 53 | `bedrock/EntityRuntime.java` | Probablemente acoplado a EntityRenderState / FrameResult |
| 51 | `render/ReplacedMobRenderer.java` | GeckoLib 5 (RenderPassInfo, BoneSnapshots, DataTicket) |
| 48 | `mob/misc/EnderDragonRenderer.java` | SubmitNodeCollector, CameraRenderState |
| 41 | `mob/monster/EndermanCarriedBlockLayer.java` | Render state + SubmitNodeCollector |
| 39 | `render/ReplacedEntityRenderer.java` | GeckoLib 5 |
| 37 | `mob/pets/FoxHeldItemLayer.java` | Render state + ItemStackRenderState |
| 37 | `mob/aquatic/DolphinItemLayer.java` | Render state + ItemStackRenderState |
| 30 | `mob/player/ShoulderParrotLayer.java` | Render state + Parrot path |
| 26 | `attachable/TextureMeshes.java` | TextureContents (API 26.2) |
| 25 | `mob/player/PlayerWingsLayer.java` | DataComponents + Equippable |
| 25 | `mob/player/PlayerScenario.java` | Probablemente acoplado a PlayerRenderState |
| 24 | `mob/player/FirstPerson.java` | Render state + CameraRenderState |
| 24 | `mob/aquatic/GuardianBeamLayer.java` | Render state |
| 22 | `particle/RuntimeEffects.java` | Por revisar |
| 20 | `mob/player/PlayerQueries.java` | ItemUseAnimation (1.20.5+) |

## Orden recomendado

1. Crear una capa de compatibilidad mínima (un paquete propio, p. ej. `com.ans.compat`) con los tipos que necesitan los renderizadores, implementados en 1.20.1 vanilla y GeckoLib 4. Así las 10+ clases que usan render state dejan de depender de 26.2.
2. Reescribir `ReplacedEntityRenderer` y `ReplacedMobRenderer` contra `GeoReplacedEntityRenderer` (GeckoLib 4).
3. Reescribir `LayerSubmitter` para `PoseStack` + `MultiBufferSource`.
4. Quitar mobs que no existen en 1.20.1 (ver `SESSION_HANDOFF_2026-10-17.md`).
5. Capas y equipo (`EquipmentLayer` es el de mayor impacto).

No reescribir sin ver los errores de la línea siguiente de cada archivo: muchas firmas dependen de la versión exacta.
