# Pride Scenes — HUD polish / Sequoia lab camera authoring (Preview 13)

**Rin's storyline, dialogue text, actor animation and Sequoia's native Cobblemon
NPC model/skin are unchanged.** This release changes only the client cinematic
presentation and adds generic shot-reordering commands to the server.

## HUD (client)

Install the Preview 13 client JAR instead of Preview 12. It generates:

`<Minecraft instance>/config/pride_scenes_client/hud.properties`

```properties
letterbox_bars=true
letterbox_height=0.065
dialogue_position=bottom
dialogue_width=320
```

- Subtle translucent top/bottom cinema framing.
- The dialogue appears in a compact card above the bottom bar. Set
  `dialogue_position=top` if you prefer the old placement.
- Rin's actual bundled skin head remains in his dialogue portrait.
- Sequoia remains his **actual existing native Cobblemon NPC model** in the
  world. No new skin/model is assigned. Since his texture is NOT bundled in
  the optional client mod, his subtitle shows his proper speaker name, not
  an invented portrait or an "S" substitute.
- The cinematic still hides chat/hotbar/first-person hands. Real game menus
  and progression remain unaffected. HUD config is read at Minecraft startup.

## Add more camera shots to the lab

Stand at a position looking in the direction the camera should face:

```mcfunction
/pridecore cinema capture sequoia_lab professor_closeup
/pridecore cinema capture sequoia_lab professor_desk_wide
```

The command captures **eye-height** XYZ and yaw/pitch. New shots append at the
END of `sequoia_lab.json`; replacing an existing ID now updates coordinates
and facing while preserving existing timing/transition/tracking settings.

Put your new shot immediately after Rin's entrance, without opening JSON:

```mcfunction
/pridecore cinema move-after sequoia_lab professor_closeup rin_entrance
/pridecore cinema move-after sequoia_lab professor_desk_wide professor_closeup
```

Other useful commands:

```mcfunction
/pridecore cinema move-first sequoia_lab professor_desk_wide
/pridecore cinema timing sequoia_lab rin_entrance 88 0
/pridecore cinema timing sequoia_lab professor_closeup 56 18
/pridecore cinema timing sequoia_lab professor_desk_wide 48 18
/pridecore cinema timing sequoia_lab sequoia_dialogue 140 26
/pridecore cinema list sequoia_lab
/pridecore cinema preview sequoia_lab
```

The example timing sums to **332 ticks** = 88 + 56 + 48 + 140.
The real Rin lab story ends at tick 332. Any camera sequence taking longer is
truncated in actual onboarding, even though a standalone preview may play
longer. This release does NOT change that server-owned timing.

If you only add one shot, a simpler example is Rin entrance 88 ticks,
new closeup 55 ticks, Sequoia dialogue 189 ticks (total 332).

The file is saved on the **server**, normally:
`<world>/data/pride_core/cinematics/sequoia_lab.json`.

Back it up before changing the scene. The Camera Director re-reads it on
each replay; no restart is necessary for shot edits.

## Deployment

Back up the server, replace its previous unified PrideCore JAR with the
new unified JAR (exactly one installed), and replace the optional client
Preview 12 JAR with Preview 13. Nothing needs to change in your native NPC
resource pack. Validate first with `/pridecore cinema preview sequoia_lab`
and then a normal onboarding run.
