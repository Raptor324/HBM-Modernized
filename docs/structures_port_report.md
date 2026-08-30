# Отчёт: портирование структур 1.7.10 -> 1.20.1/1.21.1

Конвертер: `tools/structure_converter/convert.py` (запуск из корня репозитория).

## Конвертированные файлы

* `aircraft_carrier.nbt`
* `beached_patrol.nbt`
* `broadcasting_tower.nbt`
* `crane_mod.nbt`
* `crashed-vertibird.nbt`
* `crashed_plane_1.nbt`
* `crashed_plane_2.nbt`
* `dead_dish_small.nbt`
* `desert_shack_1.nbt`
* `desert_shack_2.nbt`
* `desert_shack_3.nbt`
* `dish.nbt`
* `factory.nbt`
* `forest_chem.nbt`
* `forest_post.nbt`
* `laboratory.nbt`
* `lighthouse.nbt`
* `meteor/loot3x3/meteor-3-bale.nbt`
* `meteor/loot3x3/meteor-3-blank.nbt`
* `meteor/loot3x3/meteor-3-block.nbt`
* `meteor/loot3x3/meteor-3-book.nbt`
* `meteor/loot3x3/meteor-3-crab-tesla.nbt`
* `meteor/loot3x3/meteor-3-crab.nbt`
* `meteor/loot3x3/meteor-3-crate.nbt`
* `meteor/loot3x3/meteor-3-dirt.nbt`
* `meteor/loot3x3/meteor-3-glow.nbt`
* `meteor/loot3x3/meteor-3-lead.nbt`
* `meteor/loot3x3/meteor-3-mku.nbt`
* `meteor/loot3x3/meteor-3-ooze.nbt`
* `meteor/loot3x3/meteor-3-pillar.nbt`
* `meteor/loot3x3/meteor-3-star.nbt`
* `meteor/loot3x3/meteor-3-statue.nbt`
* `meteor/loot3x3/meteor-3-tesla.nbt`
* `meteor/meteor-core.nbt`
* `meteor/meteor-corner.nbt`
* `meteor/meteor-fallback.nbt`
* `meteor/meteor-spike.nbt`
* `meteor/meteor-stairs.nbt`
* `meteor/meteor-t.nbt`
* `meteor/room10/headloot/loot-chest.nbt`
* `meteor/room10/headloot/loot-crate-crab.nbt`
* `meteor/room10/headloot/loot-fallback.nbt`
* `meteor/room10/headloot/loot-tesla.nbt`
* `meteor/room10/headloot/loot-trap.nbt`
* `meteor/room10/room-balcony.nbt`
* `meteor/room10/room-base-end.nbt`
* `meteor/room10/room-base-thru.nbt`
* `meteor/room10/room-basic.nbt`
* `meteor/room10/room-dragon.nbt`
* `meteor/room10/room-fallback.nbt`
* `meteor/room10/room-ladder.nbt`
* `meteor/room10/room-ooze.nbt`
* `meteor/room10/room-split.nbt`
* `meteor/room10/room-stairs.nbt`
* `meteor/room10/room-triple.nbt`
* `ntmruinsA.nbt`
* `ntmruinsB.nbt`
* `ntmruinsC.nbt`
* `ntmruinsD.nbt`
* `ntmruinsE.nbt`
* `ntmruinsF.nbt`
* `ntmruinsG.nbt`
* `ntmruinsH.nbt`
* `ntmruinsI.nbt`
* `ntmruinsJ.nbt`
* `oil_rig.nbt`
* `radio_house.nbt`
* `repeater_radio.nbt`
* `spire.nbt`
* `tower_base.nbt`
* `vertibird.nbt`
* `water_pump.nbt`

## Предупреждения/замены

* TE tileentity_cable отброшен (блок сохранён)
* TE tileentity_geiger отброшен (блок сохранён)
* TE tileentity_radio_receiver отброшен (блок сохранён)
* ore_oil_sand: ore_oil_sand -> ore_oil
* filing_cabinet: filing_cabinet -> crate_metal
* bobblehead: bobblehead -> flower_pot (порт голов-фигурок отложен)
* TE tileentity_ntm_bobblehead отброшен (блок сохранён)
* TE tileentity_fluid_barrel отброшен (блок сохранён)
* crate_supply: crate_supply -> crate_steel
* turret_howard_damaged: turret_howard_damaged -> deco_steel (турели требуют живого BE)
* TE tileentity_turret_howard_damaged отброшен (блок сохранён)
* TE tileentity_satellitereceicer отброшен (блок сохранён)
* spotlight_incandescent: spotlight -> cage_lamp (ориентация = сторона крепления meta>>1)
* TE tileentity_electric_furnace отброшен (блок сохранён)
* файл пропущен (не используется/битый): crane.nbt
* dungeon_chain: hbm-цепь -> ванильная chain
* charger: charger -> machine_battery
* TE tileentity_ntm_charger отброшен (блок сохранён)
* skeleton_holder: skeleton_holder -> skeleton_skull
* TE tileentity_ntm_skeleton отброшен (блок сохранён)
* TE tileentity_diesel_generator отброшен (блок сохранён)
* TE tileentity_connector_redwire отброшен (блок сохранён)
* steel_beam: в оригинале beam.obj всегда вертикален
* TE tileentity_data отброшен (блок сохранён)
* machine_weapon_table: weapon_table отсутствует, ближайший - armor_table
* machine_controller: machine_controller (дисгайз спавнера) -> deco_steel
* TE tileentity_battery отброшен (блок сохранён)
* turret_sentry_damaged: turret_sentry_damaged -> deco_steel
* TE tileentity_turret_sentry_damaged отброшен (блок сохранён)
* TE Sign отброшен (блок сохранён)
* TE Skull отброшен (блок сохранён)
* TE tileentity_deco отброшен (блок сохранён)
* ore_coal_oil: ore_coal_oil -> ore_oil
* TE tileentity_funnel отброшен (блок сохранён)
* tile_lab: tile_lab -> concrete_tile
* TE tileentity_pipe_gauge отброшен (блок сохранён)
* TE FlowerPot отброшен (блок сохранён)
* floodlight: floodlight -> flood_lamp
* TE tileentity_floodlight отброшен (блок сохранён)
* TE tileentity_ntm_loot отброшен (блок сохранён)
* machine_rtg_grey: rtg отсутствует - замена machine_battery
* leaves_layer: leaves_layer (листовой ковёр) -> воздух
* safe: safe -> crate_iron
* TE tileentity_crabs отброшен (блок сохранён)
* meteor_battery: meteor_battery -> machine_battery
* TE tileentity_tesla_coil отброшен (блок сохранён)
* ntm_dirt: ntm_dirt =~ dirt_dead
* hbm-блок #undef не найден в маппинге -> air
* TE tileentity_deco_f отброшен (блок сохранён)
* tnt_ntm: tnt_ntm -> ванильный TNT
* предмет hbm:item.pipe не портирован - пропущен
* ladder_tungsten: ladder_tungsten -> ladder_steel
* TE tileentity_proxy_combo отброшен (блок сохранён)
* TE tileentity_fluid_tank отброшен (блок сохранён)
* TE tileentity_microwave отброшен (блок сохранён)
* dungeon_spawner: dungeon_spawner -> meteor_spawner
* TE tileentity_ntm_dungeon_spawner отброшен (блок сохранён)
* файл пропущен (не используется/битый): test-jigsaw-core.nbt
* файл пропущен (не используется/битый): test-jigsaw-hall.nbt
* файл пропущен (не используется/битый): test-jigsaw.nbt
* файл пропущен (не используется/битый): test-rot.nbt
* файл пропущен (не используется/битый): test-tandem-core.nbt
* файл пропущен (не используется/битый): test-tandem.nbt
* TE tileentity_rtty_telex отброшен (блок сохранён)
* gas_asbestos: газовый блок асбеста -> воздух
