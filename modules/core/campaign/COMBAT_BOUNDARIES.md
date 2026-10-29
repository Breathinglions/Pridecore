# Pride Core combat architecture — mandatory separation

## Module 02 storyline: **Pokémon battles only**

This is a hard gameplay requirement, not an optional encounter setting. Every
scripted story rival, Eclipse leader, Director Cael, and Resonance Shield
Defender is a normal **Cobblemon Pokémon-vs-Pokémon trainer battle** started
through the trusted RCT BattleManager as **GEN_9_SINGLES**. Storyline trainers
cannot be beaten with a Minecraft sword, bow, fists, spell, explosion, block
trap, mob fight, or any other damage to their physical Minecraft entity.

### Allowed story boss presentation
- Dramatic entrance, dialogue and Pokémon trainer introductions
- Camera-safe actionbar text, vanilla/custom particles and audio
- Reactive cinematic cues when *opponent Pokémon* faint
- Distinct Pokémon rosters, levels, Pokémon-team strategies, RCT trainer AI
- Normal Cobblemon/RCT rules, Pokémon moves, turns and winner/loser events
- Noncombat story exploration: talking to NPCs, examining evidence, activating
  conduits to unlock the three **Pokémon battle** shield defenders

### Not allowed in Module 02 storyline encounters
- Minecraft health-based boss phases, weapons, melee or ranged damage
- Physical mobs that must be killed to progress a scripted trainer encounter
- Hybrid player-vs-mob/Pokémon encounters, arena combat waves or Minecraft
  enemy health bars
- Minecraft-player HP damage or block destruction caused by a story boss script
- Granting a story win from a physical entity kill, a particle, or a clue click

The physical RCT trainer entity is just the interaction anchor and **cannot
take ordinary Minecraft damage when the story module is enabled in an allowed
world**. The scoped `AttackEntityCallback` cancels left-click weapon/hand attacks, and the `ServerLivingEntityEvents.ALLOW_DAMAGE` listener cancels
incoming damage only for valid `pride_battle_<encounter>` actors. It does not protect
unrelated Pokémon, mobs, League NPCs, or other modules' boss entities.
Disabling Module 02 deactivates this protection. Physical trainer entities are
not to be used as combat targets.

## Separate endgame combat modules — **hybrid systems**

These are future, independently controlled **Flashpoints, Operations, Raids,
and World Bosses**. They may combine Minecraft combat with Cobblemon battles
when expressly authored for those endgame experiences. They are **not**
implemented or enabled by this Module 02 commit and must not reuse the Module
02 encounter proof/launch tags to award progress.

Suggested separate namespaces:
- `pride_flashpoint_*`
- `pride_operation_*`
- `pride_raid_*`
- `pride_worldboss_*`

No Module 02 physical actor tag or storyline battle configuration should
function as a hybrid encounter unlock or Minecraft combat trigger.

## Enforced by code and CI

- Story roster JSON requires `combatMode: POKEMON_ONLY` and
  `battleFormat: GEN_9_SINGLES`.
- Every authored encounter has a closed allowlist of roster keys, preventing
  accidental insertion of hybrid mob waves, melee damage, or Minecraft attacks.
- RCT is the sole battle launcher and Cobblemon's authenticated victory event
  is the sole battle success proof.
- The physical trainer damage listener is explicitly scoped to the nine
  story battle markers.
- `StoryCombatPolicyTests` checks valid/invalid protected targets and that
  the future hybrid systems have distinct policy scopes.
- `test_story_combat_boundary.py` audits Module 02's source for physical
  attacks, direct health changes, mob spawning and other forbidden mechanics.

## Staging still required

A green build proves these source constraints but does not prove the client
battle GUI, Pokémon AI turns, player loss, KO event order, or protection from
every third-party mod's nonstandard damage behavior. Confirm all of those on
the isolated Fabric 1.21.1/Cobblemon 1.8.1/RCT staging server before release.
