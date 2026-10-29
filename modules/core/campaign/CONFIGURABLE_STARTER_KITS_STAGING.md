# Pride Core configurable onboarding kits — staging release notes

**Feature branch:** `feature/pridecore-module02-private-rin-scenes`  
**Deployment:** isolated Minecraft 1.21.1 / Cobblemon 1.8.1 staging only.

## What is now implemented

`config/pride_core/onboarding_rewards.json` is created automatically by Pride Core if absent. The file is now **actually read by Java on server startup** and through operator reload; it is not a decorative example.

```json
{
  "schema": 1,
  "theo_registration": {
    "items": [
      {"id": "rctmod:trainer_card", "count": 1},
      {"id": "knowlogy:knowlogy_book", "count": 1}
    ]
  },
  "avery_field_kit": {
    "items": [
      {"id": "cobblemon:poke_ball", "count": 10},
      {"id": "minecraft:bread", "count": 8}
    ]
  }
}
```

To add a modded backpack to Avery, **only if its actual registered item ID exists in the installed server modpack**, add an entry to Avery's `items` array such as `{"id":"yourmod:example_backpack","count":1}`. That ID is illustrative; replace it with the real one.

Each kit requires 1–32 unique registered items, each quantity 1–64, total configured quantity <=512. Oversized files, unregistered items, invalid IDs, duplicates, unknown keys and unsupported schema are rejected. Items with smaller max stacks are automatically split.

The Java code checks the original nearby `cobblemon:npc` service entity with both `pride_service_npc` and its original `pride_npc_technician_theo` / `pride_npc_avery` marker, the original prerequisite tags and absence of the original first-time receipt tags. Full-inventory preflight refuses granting without advancing the original receipt tags. The exact original native starter GUI is never replaced by the grant service.

The unified-jar packaging script rewires only these TWO original native item-grant functions to:
- `pridecore onboarding claim theo`, or
- `pridecore onboarding claim avery`.

Native NPC dialogues, original FTB quest IDs, starter gating, legacy completion tags and Avery's original RCT series setting remain in force. The existing `prideregion:onboarding/sync_player` legacy Poképedia recovery remains unchanged; new Theo kit claims set `pokepedia_received` to avoid a second recovery grant.

## Operator actions (OP level 4)

```mcfunction
/pridecore onboarding config-status
/pridecore onboarding reload
```

On invalid JSON or missing required mods, the loader **fails closed**: the custom kit cannot be claimed and no first-time tags are written. The rest of Pride Core should continue to run. The invalid reload does **not** silently revert to an old kit. Fix the file and run `/pridecore onboarding reload` again.

**Do not grant the same item twice:** The old raw `give` lines must not remain active alongside the new claim command. Use only the specifically marked `UNIFIED-CONFIG-KITS` staging JAR, not the base CI JAR or the original older `UNIFIED-STAGING` JAR alongside the new one. The older JAR remains a rollback checkpoint.

## Staging checks before production

1. On a **fresh non-OP staging player**, do Mom → native Sequoia starter GUI → Theo. Confirm one copy each of Theo's configured items, `trainer_card_received` + `pokepedia_received`, original trainer-card FTB task. Click Theo again: no duplicate.
2. Do Avery. Confirm specified items, `trainer_guide_received` + `field_kit_received` + `pride_avery_kit`, RCT `pride_region` series and original First Steps FTB task. Click again: no duplicate.
3. Fill all 36 inventory slots, retry with a fresh eligible staging player; no partial item grant nor receipt progress. Free space and retry successfully.
4. Add a **real installed mod item** to Avery's list; operator reload; confirm next fresh player receives it. Unknown ID must be rejected.
5. Repeat with two concurrent players, restart and relog. No duplicates, no cross-player grants, no missing independent League rewards.
6. Test an untagged NPC and a remote `/pridecore onboarding claim avery` request: both must refuse. Only original bound service NPCs qualify.
7. Confirm the original Professor Sequoia native starter selector still works, existing RCT Thorn and all 13 League victory/reward definitions are unchanged.

### Integrity limitations

Inventory preflight and per-player legacy receipt tags protect routine duplicate claims and full-inventory failures. A server-process crash **during the tiny window between item insertion and tag persistence** may still require manual operator reconciliation; this is not yet a fully transactional persisted-delivery ledger. Production requires testing/recovery design. This is also not a general configuration system for native RCT Gym rewards or Fabric-mod asset files. The existing configuration audit enumerates those as separate changes.

## Other configuration decisions

- **Already editable in current Pride Core:** `story.json`, `story.properties`, optional `rin_scene_anchors.properties`; `config/pride_core.properties`; presentation-only particle registry overrides.
- **Now editable in this new branch:** Theo and Avery first-time reward item lists.
- **External-provider-owned:** FTB SNBT tasks/IDs, RCT native League trainer teams and first-win payouts, Impactor balances, LuckPerms, Flan and mod-specific vendor configurations. Pride Core should never run a duplicate reward or balance authority.
- **Next config engineering:** Native dialogue **text** externalization without weakening Cobblemon native service actions; validated non-League story team specs; bounded cinematic sound/camera/timing options; provider-compatible RCT data asset override lifecycle. These are **not implemented** in this update.
