# Execute AS and AT a staging builder; does not place blocks or RCT trainers.
# A pre-existing staging device anywhere within 32 blocks prevents duplication.
execute if entity @e[type=minecraft:interaction,tag=pride_staging_prop,distance=..32] run tellraw @s {"text":"[Pride Core] Staging finale props already present nearby. Audit or clean up first.","color":"yellow"}
execute unless entity @e[type=minecraft:interaction,tag=pride_staging_prop,distance=..32] run function pridecore:staging/place_engine_props
