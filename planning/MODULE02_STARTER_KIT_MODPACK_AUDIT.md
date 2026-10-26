# Module 02 — Starter Kit and Official Modpack Onboarding Audit

**Assessed:** 2026-10-25. **Sources:** League v7.10.10 ZIP uploaded in chat, published The Pride Region [Official] v1.1.2 on CurseForge, and the September 25 server mods+config audit. Public v1.1.2 mod list is NOT proof of the exact server runtime or the player's locally installed ZIP as of today. Do not change rewards without a staging check.

## Actual current one-time onboarding rewards and authorities

| Step | Current actual server-side owner | Items/progression |
|---|---|---|
| Starter choice | Cobblemon native Sequoia dialogue and starter UI | one starter Pokémon, not a manual give command |
| Technician Theo | `prideregion:onboarding/trainer_card` | `rctmod:trainer_card` ×1, `knowlogy:knowlogy_book` ×1; FTB Quest + `trainer_card_received`, `pokepedia_received` tags |
| Avery | `prideregion:onboarding/trainer_guide` | `cobblemon:poke_ball` ×10, `minecraft:bread` ×8, `rctmod player set series pride_region`, FTB Quest + `trainer_guide_received`, `pride_avery_kit` tags |
| Currency | configured Impactor economy | Existing starting balance previously documented as ₽2,500; verify active profile/server config before deciding any additional starting money |

**Keep these sources authoritative.** Do not duplicate item grants from Pride Core story dialogue or starter FTB Quests.

## Official published client modpack context

The Pride Region [Official] **v1.1.2** was published 2026-10-10 for Minecraft 1.21.1/Fabric/Cobblemon 1.8.1. CurseForge explicitly says it contains map/navigation, inventory, backpack, Pokémon information and progression-oriented mods. Modpack Index catalogs ~131 mods and lists client Pokémon helper and storage/UI features; this public catalog may not exactly match the server's enabled mods, local config or a future update.

Useful onboarding tutorials:
- How to open the **FTB Quests** journal, its personal quest progress, and next objective (FTB Quests Per Player is installed per the server owner).
- How to use the existing **Trainer Card** and **Poképedia** after Theo issues them.
- How to find/use the **map**, route to the next location, and read location hints. No free physical filled map needed if Xaero/world map is already in the client.
- Pokémon party and PC storage, healing at a Pokémon Center, and catching wild Pokémon.
- Apricorn gathering and Poké Ball crafting (we should teach/quest this rather than hand out a large stack).
- Backpacks and the player's storage upgrade progression as a side quest, **not** a mandatory free starter item.
- The in-game transit/resource world/economy/shops can be introduced later with NPCs after the first route, not all at spawn.

## Recommended starter kit policy (proposal, no code/reward changes authorized yet)

| Item | Qty | Policy |
|---|---:|---|
| Starter Pokémon | 1 | KEEP with Sequoia; native Cobblemon ownership |
| RCT Trainer Card | 1 | KEEP with Theo; already exists |
| Poképedia / Knowlogy book | 1 | KEEP with Theo; already exists |
| Poké Balls | 10 | KEEP Avery existing grant |
| Bread | 8 | KEEP Avery existing grant |
| Potions | 5 | **OPTIONAL ADD**, after verifying the exact item ID, actual battle-healing behavior, stack caps and no already-existing FTB reward |
| Beginner backpack | 0 | EARN via an optional early quest after first catch or first Gym; avoid undermining upgrades |
| Portable PC or portable healing | 0 | Do not issue on login; keep upgrade rewards and Pokémon Centers relevant |
| Great/Ultra/Master Balls, Rare Candies, expensive battle items | 0 | Do not hand out; protect crafting/economy/progression |
| Currency grant | 0 incremental | No additional money until active starting Impactor ₽2,500 balance and economy sinks are verified |

Recommended early FTB Quests:
1. `First Steps`: choose starter, register, obtain field kit.
2. `Your First Catch`: catch one Pokémon; tutorial points at Balls and party/storage.
3. `An Apricorn in Hand`: collect specified Apricorns and craft Poké Balls; a modest reward with no premium items.
4. `Healing Up`: visit Pokémon Center and optionally earn 5 Potions (if not already in starter kit)—**only one source**, never both.
5. `Pack for the Road`: optional backpack introduction; reward basic pack only if starter-level backpack is safe for server's storage/economy configuration.
6. `Route 1`: find Ranger contact, battle/catch, investigate the Resonance clue.
7. `Verdant Preparations`: unlock Thorn Gym narrative once first catch/battle/League flags are genuinely completed.

**Prefer 5 Potions as the early `Healing Up` quest reward** instead of automatically enlarging Avery's kit; gives players a purpose to visit the Center and teaches recovery. This is the final current recommendation unless playtesting demonstrates early healing problems.

## Core compatibility and exploit gates

- FTB Quests Per Player installed: quest journal/task progress must remain individually owned, not shared by FTB Teams. Module 02 must not manually mark rewards in two places.
- Existing datapack used `/ftbquests change_progress @s complete <ID>` and `reset`; preserve existing IDs and test user context.
- The original `trainer_guide.mcfunction` can reset the first catch and first battle FTB Quest projection if corresponding authoritative tags are missing. Confirm returning players don't lose legitimate quest markers.
- Cobblemon 1.8.1 starter system's native `has_chosen_starter` is separate from the datapack's `starter_chosen` tag. The latter is the current conservative gate available to story content; final native adapter needs to verify live starter ownership too.
- No auto-items/Impactor deposits from test dialogue; all one-time rewards require a single authoritative owner and non-OP staging/restart checks.
- Do not package client mods or protected upstream jar contents into the story datapack.

## Sources for current public modpack listing

- https://www.curseforge.com/minecraft/modpacks/the-pride-region-official/files/9123617
- https://www.modpackindex.com/modpack/163079/the-pride-region-official

**No player inventory change, datapack overwrite or new item grant was performed in this audit.**
