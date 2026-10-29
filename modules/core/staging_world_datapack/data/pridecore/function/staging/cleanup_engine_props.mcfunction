# Removes ONLY the invisible props created by the staging setup function.
# The operation is local to the executing builder's present position/dimension.
execute if entity @e[type=minecraft:interaction,tag=pride_staging_prop,distance=..32] run tellraw @s {"text":"[Pride Core] Removing nearby generated staging interaction props only.","color":"yellow"}
kill @e[type=minecraft:interaction,tag=pride_staging_prop,distance=..32]
