# Native Cobblemon NPC Dialogue Overlay — Staging Fix

## Bug
Mom opened vanilla Minecraft chat dialogue rather than Cobblemon's native dialogue overlay. The chat choice still advanced the story, but bypassed the intended NPC screen.

## Root cause
StoryModule's UseEntityCallback consumed tagged real Cobblemon NPC clicks and called the old clickable-chat prototype even though the unified JAR already includes a full native pride_story:mom NPC and dialogue definition.

## Correction
- Native tagged cobblemon:npc story actors now return ActionResult.PASS so Cobblemon owns the GUI.
- Cancel stale prototype chat sessions without altering player facts.
- Original Sequoia, Theo, Avery and RCT Gym NPC interactions remain passed through.
- The protected pridecore story native <npc> <choice> handler validates actual NPC proximity, world and story prerequisites.
- Tagged development test villagers retain the prototype chat UI.
- Added a regression guard and workflow test step to prevent chat rendering from returning to actual Cobblemon NPCs.

## Staging
Corrected unified JAR: pride-core-0.4.3-rc1-UNIFIED-NATIVE-DIALOGUE-STAGING.jar, SHA-256 b3a3792ec356a3190c0ce03557b8700aab0ae232fa2117666aeeeb4cb4fed480. Built from a successful CI artifact with original hashed League ZIP and native Story ZIP. All 13 League trainers and 20 story native NPCs were packaged.

Stop staging; replace the previous Pride Core server JAR (one Pride Core mod ID only); restart. No standalone duplicate League/Story datapack. Native Mom dialogue begins with: Good morning! Professor Sequoia has been expecting you at his laboratory in Hearthleaf. Native buttons include I'm ready for my adventure!, What should I do first?, and Not yet, Mom.

If the NPC still does not open Cobblemon's overlay, verify the physical entity has the authored pride_story:mom NPC type, not merely the pride_npc_mom marker on a generic NPC. Do not spawn a second Mom. Verify the actual GUI and native action in-game; compile/package checks are not Minecraft gameplay acceptance.