# Run at a staging clue as an operator. Only our staging-generated clue props are affected.
execute if entity @e[type=minecraft:interaction,tag=pride_staging_clue,distance=..4,limit=1,sort=nearest] run tellraw @s {"text":"[Pride Core] Removing nearest generated clue hitbox within four blocks.","color":"yellow"}
kill @e[type=minecraft:interaction,tag=pride_staging_clue,distance=..4,limit=1,sort=nearest]
