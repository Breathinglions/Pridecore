# Center core and cardinal conduits, all deliberately invisible interaction hitboxes.
# The builder must add actual world geometry; these are gameplay click targets.
summon minecraft:interaction ~ ~ ~ {width:1.5f,height:2.0f,Tags:["pride_finale_core","pride_staging_prop"]}
execute positioned ~ ~ ~-12 run summon minecraft:interaction ~ ~ ~ {width:1.5f,height:2.0f,Tags:["pride_finale_conduit_north","pride_staging_prop"]}
execute positioned ~ ~ ~12 run summon minecraft:interaction ~ ~ ~ {width:1.5f,height:2.0f,Tags:["pride_finale_conduit_south","pride_staging_prop"]}
execute positioned ~12 ~ ~ run summon minecraft:interaction ~ ~ ~ {width:1.5f,height:2.0f,Tags:["pride_finale_conduit_east","pride_staging_prop"]}
execute positioned ~-12 ~ ~ run summon minecraft:interaction ~ ~ ~ {width:1.5f,height:2.0f,Tags:["pride_finale_conduit_west","pride_staging_prop"]}
tellraw @s {"text":"[Pride Core] Five staging finale hitboxes placed. Bind three shield trainers separately.","color":"aqua"}
