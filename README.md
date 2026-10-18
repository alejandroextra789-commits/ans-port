# Actions & Stuff — Forge 1.20.1 port workspace

This is an experimental source-reconstruction workspace targeting **Minecraft 1.20.1 + Forge 47.3.x + Java 17**.

## Current status

**Not yet compilable or playable.** This is a migration workspace, not a finished mod JAR. The original mod targets a much newer Minecraft/client rendering API and uses a newer GeckoLib API. Porting requires code-level rewrites; changing dependency names alone is not enough.

## What has been prepared

- ForgeGradle build configuration for Minecraft 1.20.1 / Forge 47.3.0.
- Forge mod metadata (`META-INF/mods.toml`) and a Forge `@Mod` entry point.
- Initial replacements for FabricLoader paths/mod checks and Fabric end-client-tick callbacks.
- Recovered Java source and resource JSON files retained for continued migration.
- Original Fabric metadata retained under `reference/` for audit purposes.

## Major remaining work

1. Replace remaining Fabric rendering hooks and APIs with Forge 1.20.1 equivalents.
2. Port the newer GeckoLib API/data model to GeckoLib 4.8.4 for 1.20.1.
3. Rewrite newer render-pipeline/state/submit-collector code to 1.20.1 rendering (`RenderType`, `PoseStack`, `MultiBufferSource`, Forge render events).
4. Port remaining Fabric keybinding/resource-pack hooks and verify mixin loading on Forge.
5. Compile, fix errors iteratively, then launch a Forge client and test rendering/animation/particles.

## Build environment

Use Java 17 and a compatible modern Gradle installation (the system Gradle 4.4.1 is too old). A build is not expected to succeed until the migration blockers above are resolved.

## Important

Do not install this workspace as a mod. There is no verified working JAR yet.

## Latest cleanup pass

A source cleanup pass removed Java 21-only `List.getFirst()` and `Math.clamp()` usage from the Java 17-targeted source, added Mixin compile-time processing configuration, and added `tools/audit_port.py` to list unresolved cross-version APIs. Run it with:

```bash
python3 tools/audit_port.py
```

This is not a clean build yet: the remaining GeckoLib 5/new Minecraft renderer API references must be ported before the project can compile against Forge 1.20.1. The project must not be installed as a working mod until Gradle compilation and a client launch both pass.

### Compatibility cleanup

The current workspace removes direct support code for mobs/projectiles introduced after 1.20.1 (Armadillo, Bogged, Breeze, Camel Husk, Copper Golem, Creaking, Happy Ghast, Nautilus variants, Parched, Sulfur Cube, Wind Charge). The current ZIP is still an intermediate source port, not a compiled or playable mod. GeckoLib and rendering/item APIs still require a full 1.20.1 rewrite.
