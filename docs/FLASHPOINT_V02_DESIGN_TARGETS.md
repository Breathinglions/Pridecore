# Pride Core — Flashpoint v0.2 design targets

## Agreed target
- **Standard Flashpoint completion time: 15–25 minutes**, including Pokémon encounters, exploration, objectives, and boss fights.
- This is a gameplay target, **not** a mandate to fill the dungeon with empty corridors. Actual timing must be measured with a party during in-game tests.
- Preserve the validated v0.1 dungeon generator and **Ancient Deepslate Ruins** visual style as a baseline; do not silently replace known-good generation or cleanup.

## Proposed dimensions (NOT YET APPROVED OR IMPLEMENTED)
The user reported that the v0.1 layout was traversed too quickly and suggested larger rooms and corridors. Initial engineering proposals, all subject to performance and playtest verification:
- Standard footprint: roughly 180 × 150 blocks (v0.1 was 100 × 76).
- Room widths: around 25–35 blocks; boss arena widths: 40–50 blocks.
- Corridors: about 7–9 blocks wide; interior vertical clearance: 9–12 blocks.
- Approximately 14–18 rooms with varied routes, optional branches and side objectives.
- Retain variation in encounters and structure instead of padding playtime with empty traversal.

## v0.2 development gates
1. Persist per-instance block/change journals and crash-safe cleanup, tested across server restarts.
2. Chunk boundaries, generation tick budgets, staging-world safety checks, performance measurements.
3. Add scalable dungeon layout settings and room metrics without breaking v0.1 behavior.
4. Validate navigability for players and large Cobblemon models.
5. Add encounter markers, room goals, miniboss and boss fight time budgets; test with a real party.
6. Time real sessions to tune room count and challenge until Standard Flashpoints typically take 15–25 minutes.

## Existing test status
- v0.1 compiled as Fabric 1.21.1 mod and passed 1,000 seeded geometry tests.
- The server operator confirmed successful dungeon generation, room connectivity, interior traversal, and `/pridefp clear` cleanup in the dedicated Multiworld test environment.
- Restart persistence and multi-party isolation are **not verified**.

## Future presets (concepts)
- Small Flashpoint: shorter content than Standard; exact target not approved.
- Standard Flashpoint: **15–25 minutes (agreed)**.
- Operation: longer multi-stage raid content; exact target not approved.
