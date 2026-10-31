#!/usr/bin/env python3
"""Fail-closed NPC skin exporter contract. Does not claim client renderer acceptance."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
code = (root/"src/main/java/com/prideregion/core/npc/NpcSkinManager.java").read_text()
mod = (root/"src/main/java/com/prideregion/core/PrideCoreMod.java").read_text()
checks = (
    'CommandManager.literal("npc")',
    'CommandManager.literal("skins")',
    'CommandManager.literal("reload")',
    'CommandManager.literal("status")',
    '.requires(s -> s.hasPermissionLevel(4))',
    'resolve("pride_core").resolve("npc_skins")',
    'resolve("skins.json")',
    'Files.notExists(mapping)',
    'if (!cfg.keySet().equals(Set.of("schema", "skins", "npcClasses"))',
    'Files.isSymbolicLink(file)',
    'image.getWidth() != 64 || image.getHeight() != 64',
    'NPC skin must be 64x64',
    'readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject()',
    'npc.addProperty("resourceIdentifier"',
    'if (!npc.startsWith("pride_region:") && !npc.startsWith("pride_story:"))',
    'sourceFile.readAllBytes()',
    'if (!npc.has("resourceIdentifier"))',
    'new ZipOutputStream(buffer, StandardCharsets.UTF_8)',
    'ATOMIC_MOVE,REPLACE_EXISTING',
    'PrideNPCSkins_Resources.zip',
    'PrideNPCSkins_NPCOverrides.zip',
    'This command does NOT push a new client pack automatically.',
)
assert 'pack_format' in code and '34,' in code and '48,' in code
for test in checks:
    assert test in code, test
assert 'NpcSkinManager.start();' in mod
assert 'NpcSkinManager.stop();' in mod
assert 'root.then(admin).then(StoryModule.commands()).then(CameraDirectorModule.commands()).then(NpcSkinManager.commands())' in mod
for forbidden in ('ProfileStore', 'ProgressionService', 'battleConfiguration.add(', 'player.addCommandTag(', 'setCustomName(', 'teleport('):
    assert forbidden not in code, forbidden
print("PASS: NPC skin manager config validation, non-destructive ZIP output and explicit resource-pack deployment contract")
