# Optional staging-only marker. Run AS/AT the operator in the desired world.
execute if entity @e[type=minecraft:interaction,tag=pride_clue_ward,distance=..3] run tellraw @s {"text":"[Pride Core] ward clue marker already exists nearby.","color":"yellow"}
execute unless entity @e[type=minecraft:interaction,tag=pride_clue_ward,distance=..3] run summon minecraft:interaction ~ ~ ~ {width:1.0f,height:1.5f,Tags:["pride_clue_ward","pride_staging_clue"]}
