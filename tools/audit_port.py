#!/usr/bin/env python3
"""Static migration audit for the Actions & Stuff Forge 1.20.1 workspace.

This tool deliberately does not claim to replace a Gradle compile or an in-game test.
It reports source markers known to be incompatible with the requested target.
"""
from pathlib import Path
from collections import Counter
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java"
files = sorted(JAVA.rglob("*.java"))
checks = {
    "Fabric API references": ("net.fabricmc.", "must be migrated to Forge"),
    "new GeckoLib namespace references": ("com.geckolib.", "must be ported to GeckoLib 4 API; package renames alone are unsafe"),
    "new rendering pipeline references": ("RenderPipeline", "not available in Minecraft 1.20.1's rendering API"),
    "new render-state references": ("EntityRenderState", "must be rewritten for 1.20.1 renderer signatures"),
    "new submit-collector references": ("SubmitNodeCollector", "not available in Minecraft 1.20.1"),
    "new tint API references": ("BlockTintSource", "not available in Minecraft 1.20.1"),
    "new item component API references": ("DataComponents", "must be migrated to legacy item/NBT APIs"),
    "new equipment asset API references": ("EquipmentAsset", "must be migrated to legacy armor rendering"),
    "new player skin object references": ("PlayerSkin", "not part of the 1.20.1 player texture API"),
    "new sound engine result API references": ("PlayResult", "SoundEngine.play returns void in 1.20.1"),
}
print(f"Java source files: {len(files)}")
print("Static migration findings (occurrences, unique files):")
for label, (needle, note) in checks.items():
    hits = [(p, p.read_text(errors="replace").count(needle)) for p in files]
    hits = [(p, n) for p, n in hits if n]
    print(f"- {label}: {sum(n for _, n in hits)} occurrence(s) across {len(hits)} file(s) — {note}")
    for path, count in hits[:12]:
        print(f"    {path.relative_to(ROOT)} ({count})")
    if len(hits) > 12:
        print(f"    ... and {len(hits) - 12} more file(s)")
unsupported_entities = [
    "EntityType.ARMADILLO", "EntityType.BOGGED", "EntityType.BREEZE", "EntityType.BREEZE_WIND_CHARGE",
    "EntityType.CAMEL_HUSK", "EntityType.COPPER_GOLEM", "EntityType.CREAKING", "EntityType.HAPPY_GHAST",
    "EntityType.NAUTILUS", "EntityType.PARCHED", "EntityType.SULFUR_CUBE", "EntityType.WIND_CHARGE",
    "EntityType.ZOMBIE_NAUTILUS",
]
remaining = []
for path in files:
    content = path.read_text(errors="replace")
    for entity in unsupported_entities:
        if entity in content:
            remaining.append((path.relative_to(ROOT), entity))
print(f"\nUnsupported 1.21+ entity constants remaining in Java: {len(remaining)}")
for path, entity in remaining[:40]:
    print(f"    {path}: {entity}")
print("\nBuild/runtime validation: NOT performed by this static audit.")
print("Do not install this workspace as a mod until Gradle compilation and client launch pass.")

# Validate resource JSON independently of Java/API migration findings.
resources = ROOT / "src/main/resources"
json_files = sorted(resources.rglob("*.json"))
json_errors = []
for path in json_files:
    try:
        json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        json_errors.append((path.relative_to(ROOT), str(exc)))
print(f"Resource JSON validation: {len(json_files)} file(s), {len(json_errors)} syntax error(s)")
for path, error in json_errors[:30]:
    print(f"    {path}: {error}")
if len(json_errors) > 30:
    print(f"    ... and {len(json_errors) - 30} more")
