package com.hbm_m.world.gen;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.config.StructureConfig;
import com.hbm_m.main.StructureManager;
import com.hbm_m.world.gen.LegacyBiome.Type;
import com.hbm_m.world.gen.component.Component.CrabSpawners;
import com.hbm_m.world.gen.component.Component.GreenOoze;
import com.hbm_m.world.gen.component.Component.MeteorBricks;
import com.hbm_m.world.gen.component.Component.SupplyCrates;
import com.hbm_m.world.gen.nbt.BlockSelector;
import com.hbm_m.world.gen.nbt.JigsawPiece;
import com.hbm_m.world.gen.nbt.JigsawPool;
import com.hbm_m.world.gen.nbt.NBTStructure;
import com.hbm_m.world.gen.nbt.SpawnCondition;

import net.minecraft.world.level.block.Block;

/**
 * 1:1 {@code com.hbm.world.gen.NTMWorldGenerator}: registriert alle Weltgen-Strukturen der Oberwelt mit
 * Biombedingungen, Gewichten, Hoehengrenzen und Pools. Erzeugt werden sie ueber
 * {@link com.hbm_m.world.gen.nbt.NBTStructureGen} (eine 1.20-Struktur {@code hbm_m:ntm_structures}).
 *
 * <p>Die 1.7.10-Biomeigenschaften (Hoehenvarianz, Wurzelhoehe, BiomeDictionary-Typen) liefert
 * {@link LegacyBiome}. Registriert wird beim ersten Zugriff (Bloecke muessen registriert sein).</p>
 */
public final class NTMWorldGenerator {

    private static boolean initialized = false;

    private NTMWorldGenerator() { }

    /** Umfasst alle als Ozean oder Fluss markierten Biome. */
    public static boolean isWaterBiome(LegacyBiome biome) {
        return biome.isOfType(Type.WATER);
    }

    /** Biome mit wenig Hoehenunterschied und spaerlichem Bewuchs, ohne Wasserbiome. */
    public static boolean isFlatBiome(LegacyBiome biome) {
        return biome.heightVariation <= 0.2F && !isWaterBiome(biome) && biome.isOfType(Type.SPARSE);
    }

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        /// SPIRE ///
        NBTStructure.registerStructure(0, new SpawnCondition("spire") {{
            canSpawn = biome -> biome.heightVariation <= 0.05F && !isWaterBiome(biome);
            structure = new JigsawPiece("spire", StructureManager.spire, -1);
            spawnWeight = StructureConfig.spireSpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("features") {{
            canSpawn = biome -> !isWaterBiome(biome);
            start = d -> MapGenNTMFeatures.Start.create(d);
            spawnWeight = StructureConfig.featuresSpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("bunker") {{
            canSpawn = biome -> !isWaterBiome(biome);
            start = d -> com.hbm_m.world.gen.component.BunkerComponents.BunkerStart.create(d);
            spawnWeight = StructureConfig.bunkerSpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("vertibird") {{
            canSpawn = biome -> !isWaterBiome(biome) && biome.isOfType(Type.SANDY);
            structure = new JigsawPiece("vertibird", StructureManager.vertibird, -3);
            spawnWeight = StructureConfig.vertibirdSpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("crashed_vertibird") {{
            canSpawn = biome -> !isWaterBiome(biome) && biome.isOfType(Type.SANDY);
            structure = new JigsawPiece("crashed_vertibird", StructureManager.crashed_vertibird, -10);
            spawnWeight = StructureConfig.vertibirdCrashedSpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("beached_patrol") {{
            canSpawn = biome -> biome.isOfType(Type.BEACH);
            structure = new JigsawPiece("beached_patrol", StructureManager.beached_patrol, -5);
            minHeight = 58;
            maxHeight = 67;
            spawnWeight = StructureConfig.beachedPatrolSpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("aircraft_carrier") {{
            canSpawn = biome -> biome.isOfType(Type.OCEAN);
            structure = new JigsawPiece("aircraft_carrier", StructureManager.aircraft_carrier, -6);
            maxHeight = 42;
            spawnWeight = StructureConfig.enableOceanStructures ? StructureConfig.aircraftCarrierSpawnWeight : 0;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("oil_rig") {{
            canSpawn = biome -> biome.isOfType(Type.OCEAN) && biome.rootHeight >= -1.5F;
            structure = new JigsawPiece("oil_rig", StructureManager.oil_rig, -20);
            maxHeight = 12;
            minHeight = 11;
            spawnWeight = StructureConfig.enableOceanStructures ? StructureConfig.oilRigSpawnWeight : 0;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("lighthouse") {{
            canSpawn = biome -> biome.isOfType(Type.OCEAN) || biome.isOfType(Type.BEACH);
            structure = new JigsawPiece("lighthouse", StructureManager.lighthouse, -40);
            maxHeight = 29;
            minHeight = 28;
            spawnWeight = StructureConfig.enableOceanStructures ? StructureConfig.lighthouseSpawnWeight : 0;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("dish") {{
            canSpawn = biome -> biome.isOfType(Type.PLAINS);
            structure = new JigsawPiece("dish", StructureManager.dish, -10);
            minHeight = 53;
            maxHeight = 65;
            spawnWeight = StructureConfig.dishSpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("water_pump") {{
            canSpawn = biome -> biome.isOfType(Type.PLAINS) || biome.isOfType(Type.SWAMP);
            structure = new JigsawPiece("water_pump", StructureManager.water_pump, -10);
            spawnWeight = StructureConfig.waterPumpSpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("forestchem") {{
            canSpawn = biome -> biome.heightVariation <= 0.3F && !isWaterBiome(biome);
            structure = new JigsawPiece("forest_chem", StructureManager.forest_chem, -9);
            spawnWeight = StructureConfig.forestChemSpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("labolatory") {{
            canSpawn = biome -> isFlatBiome(biome);
            structure = new JigsawPiece("laboratory", StructureManager.laboratory, -10);
            minHeight = 53;
            maxHeight = 65;
            spawnWeight = StructureConfig.laboratorySpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("forest_post") {{
            canSpawn = biome -> biome.heightVariation <= 0.3F && !isWaterBiome(biome);
            structure = new JigsawPiece("forest_post", StructureManager.forest_post, -10);
            spawnWeight = StructureConfig.forestPostSpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("radio") {{
            canSpawn = biome -> isFlatBiome(biome);
            structure = new JigsawPiece("radio_house", StructureManager.radio_house, -6);
            spawnWeight = StructureConfig.radioSpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("factory") {{
            canSpawn = biome -> biome.heightVariation <= 0.2F && !isWaterBiome(biome);
            structure = new JigsawPiece("factory", StructureManager.factory, -10);
            spawnWeight = StructureConfig.factorySpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("crane") {{
            canSpawn = biome -> biome.heightVariation <= 0.2F && !isWaterBiome(biome);
            structure = new JigsawPiece("crane", StructureManager.crane, -13);
            spawnWeight = StructureConfig.craneSpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("broadcaster_tower") {{
            canSpawn = biome -> isFlatBiome(biome);
            structure = new JigsawPiece("broadcaster_tower", StructureManager.broadcasting_tower, -9);
            spawnWeight = StructureConfig.broadcastingTowerSpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("plane1") {{
            canSpawn = biome -> biome.heightVariation <= 0.3F && !isWaterBiome(biome);
            structure = new JigsawPiece("crashed_plane_1", StructureManager.plane1, -5);
            spawnWeight = StructureConfig.plane1SpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("plane2") {{
            canSpawn = biome -> biome.heightVariation <= 0.3F && !isWaterBiome(biome);
            structure = new JigsawPiece("crashed_plane_2", StructureManager.plane2, -8);
            spawnWeight = StructureConfig.plane2SpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("desert_shack_1") {{
            canSpawn = biome -> biome.isOfType(Type.SANDY);
            structure = new JigsawPiece("desert_shack_1", StructureManager.desert_shack_1, -7);
            spawnWeight = StructureConfig.desertShack1SpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("desert_shack_2") {{
            canSpawn = biome -> biome.isOfType(Type.SANDY);
            structure = new JigsawPiece("desert_shack_2", StructureManager.desert_shack_2, -7);
            spawnWeight = StructureConfig.desertShack2SpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("desert_shack_3") {{
            canSpawn = biome -> biome.isOfType(Type.SANDY);
            structure = new JigsawPiece("desert_shack_3", StructureManager.desert_shack_3, -5);
            spawnWeight = StructureConfig.desertShack3SpawnWeight;
        }});

        NBTStructure.registerStructure(0, new SpawnCondition("dead_dish_small") {{
            canSpawn = biome -> biome.isOfType(Type.SANDY);
            structure = new JigsawPiece("dead_dish_small", StructureManager.dead_dish_small, -5);
            spawnWeight = StructureConfig.deadDishSmallSpawnWeight;
        }});

        ruin("ruinA", "NTMRuinsA", StructureManager.ntmruinsA, StructureConfig.ruinsASpawnWeight);
        ruin("ruinB", "NTMRuinsB", StructureManager.ntmruinsB, StructureConfig.ruinsBSpawnWeight);
        ruin("ruinC", "NTMRuinsC", StructureManager.ntmruinsC, StructureConfig.ruinsCSpawnWeight);
        ruin("ruinD", "NTMRuinsD", StructureManager.ntmruinsD, StructureConfig.ruinsDSpawnWeight);
        ruin("ruinE", "NTMRuinsE", StructureManager.ntmruinsE, StructureConfig.ruinsESpawnWeight);
        ruin("ruinF", "NTMRuinsF", StructureManager.ntmruinsF, StructureConfig.ruinsFSpawnWeight);
        ruin("ruinG", "NTMRuinsG", StructureManager.ntmruinsG, StructureConfig.ruinsGSpawnWeight);
        ruin("ruinH", "NTMRuinsH", StructureManager.ntmruinsH, StructureConfig.ruinsHSpawnWeight);
        ruin("ruinI", "NTMRuinsI", StructureManager.ntmruinsI, StructureConfig.ruinsISpawnWeight);
        ruin("ruinJ", "NTMRuinsJ", StructureManager.ntmruinsJ, StructureConfig.ruinsJSpawnWeight);

        NBTStructure.registerStructure(0, new SpawnCondition("tower_base") {{
            canSpawn = biome -> biome.heightVariation <= 0.3F && !isWaterBiome(biome) && !biome.isOfType(Type.SANDY);
            structure = new JigsawPiece("tower_base", StructureManager.tower_base, -6);
            spawnWeight = StructureConfig.towerBaseSpawnWeight;
        }});

        NBTStructure.registerNullWeight(0, StructureConfig.plainsNullWeight, biome -> biome.is("plains"));
        NBTStructure.registerNullWeight(0, StructureConfig.oceanNullWeight, biome -> biome.isOfType(Type.OCEAN));

        Map<Block, BlockSelector> bricks = new HashMap<>();
        bricks.put(ModBlocks.METEOR_BRICK.get(), new MeteorBricks());

        Map<Block, BlockSelector> crates = new HashMap<>();
        crates.put(ModBlocks.METEOR_BRICK.get(), new MeteorBricks());
        crates.put(ModBlocks.CRATE.get(), new SupplyCrates());
        crates.put(ModBlocks.METEOR_SPAWNER.get(), new CrabSpawners());

        // concrete_colored (jede Farbe) -> GreenOoze
        Map<Block, BlockSelector> ooze = new HashMap<>();
        ooze.put(ModBlocks.METEOR_BRICK.get(), new MeteorBricks());
        GreenOoze greenOoze = new GreenOoze();
        for (Block concrete : new Block[] {
                ModBlocks.CONCRETE_WHITE.get(), ModBlocks.CONCRETE_ORANGE.get(), ModBlocks.CONCRETE_MAGENTA.get(), ModBlocks.CONCRETE_LIGHT_BLUE.get(),
                ModBlocks.CONCRETE_YELLOW.get(), ModBlocks.CONCRETE_LIME.get(), ModBlocks.CONCRETE_PINK.get(), ModBlocks.CONCRETE_GRAY.get(),
                ModBlocks.CONCRETE_SILVER.get(), ModBlocks.CONCRETE_CYAN.get(), ModBlocks.CONCRETE_PURPLE.get(), ModBlocks.CONCRETE_BLUE.get(),
                ModBlocks.CONCRETE_BROWN.get(), ModBlocks.CONCRETE_GREEN.get(), ModBlocks.CONCRETE_RED.get(), ModBlocks.CONCRETE_BLACK.get() }) {
            ooze.put(concrete, greenOoze);
        }

        NBTStructure.registerStructure(0, new SpawnCondition("meteor_dungeon") {{
            minHeight = 32;
            maxHeight = 32;
            sizeLimit = 128;
            canSpawn = biome -> biome.rootHeight >= 0;
            startPool = "start";
            pools = new HashMap<String, JigsawPool>() {{
                put("start", new JigsawPool() {{
                    add(new JigsawPiece("meteor_core", StructureManager.meteor_core) {{ blockTable = bricks; }}, 1);
                }});
                put("spike", new JigsawPool() {{
                    add(new JigsawPiece("meteor_spike", StructureManager.meteor_spike) {{ heightOffset = -3; conformToTerrain = true; }}, 1);
                }});
                put("default", new JigsawPool() {{
                    add(new JigsawPiece("meteor_corner", StructureManager.meteor_corner) {{ blockTable = bricks; }}, 2);
                    add(new JigsawPiece("meteor_t", StructureManager.meteor_t) {{ blockTable = bricks; }}, 3);
                    add(new JigsawPiece("meteor_stairs", StructureManager.meteor_stairs) {{ blockTable = bricks; }}, 1);
                    add(new JigsawPiece("meteor_room_base_thru", StructureManager.meteor_room_base_thru) {{ blockTable = bricks; }}, 3);
                    add(new JigsawPiece("meteor_room_base_end", StructureManager.meteor_room_base_end) {{ blockTable = bricks; }}, 4);
                    fallback = "fallback";
                }});
                put("10room", new JigsawPool() {{
                    add(new JigsawPiece("meteor_room_basic", StructureManager.meteor_room_basic) {{ blockTable = bricks; }}, 1);
                    add(new JigsawPiece("meteor_room_balcony", StructureManager.meteor_room_balcony) {{ blockTable = bricks; }}, 1);
                    add(new JigsawPiece("meteor_room_dragon", StructureManager.meteor_room_dragon) {{ blockTable = bricks; }}, 1);
                    add(new JigsawPiece("meteor_room_ladder", StructureManager.meteor_room_ladder) {{ blockTable = bricks; }}, 1);
                    add(new JigsawPiece("meteor_room_ooze", StructureManager.meteor_room_ooze) {{ blockTable = ooze; }}, 1);
                    add(new JigsawPiece("meteor_room_split", StructureManager.meteor_room_split) {{ blockTable = bricks; }}, 1);
                    add(new JigsawPiece("meteor_room_stairs", StructureManager.meteor_room_stairs) {{ blockTable = bricks; }}, 1);
                    add(new JigsawPiece("meteor_room_triple", StructureManager.meteor_room_triple) {{ blockTable = bricks; }}, 1);
                    fallback = "roomback";
                }});
                put("3x3loot", new JigsawPool() {{
                    add(new JigsawPiece("meteor_3_bale", StructureManager.meteor_3_bale), 1);
                    add(new JigsawPiece("meteor_3_blank", StructureManager.meteor_3_blank), 1);
                    add(new JigsawPiece("meteor_3_block", StructureManager.meteor_3_block), 1);
                    add(new JigsawPiece("meteor_3_crab", StructureManager.meteor_3_crab), 1);
                    add(new JigsawPiece("meteor_3_crab_tesla", StructureManager.meteor_3_crab_tesla), 1);
                    add(new JigsawPiece("meteor_3_crate", StructureManager.meteor_3_crate), 1);
                    add(new JigsawPiece("meteor_3_dirt", StructureManager.meteor_3_dirt), 1);
                    add(new JigsawPiece("meteor_3_lead", StructureManager.meteor_3_lead), 1);
                    add(new JigsawPiece("meteor_3_ooze", StructureManager.meteor_3_ooze), 1);
                    add(new JigsawPiece("meteor_3_pillar", StructureManager.meteor_3_pillar), 1);
                    add(new JigsawPiece("meteor_3_star", StructureManager.meteor_3_star), 1);
                    add(new JigsawPiece("meteor_3_tesla", StructureManager.meteor_3_tesla), 1);
                    add(new JigsawPiece("meteor_3_book", StructureManager.meteor_3_book), 1);
                    add(new JigsawPiece("meteor_3_mku", StructureManager.meteor_3_mku), 1);
                    add(new JigsawPiece("meteor_3_statue", StructureManager.meteor_3_statue), 1);
                    add(new JigsawPiece("meteor_3_glow", StructureManager.meteor_3_glow), 1);
                    fallback = "3x3loot"; // Beute auch an der Groessengrenze
                }});
                put("headloot", new JigsawPool() {{
                    add(new JigsawPiece("meteor_dragon_chest", StructureManager.meteor_dragon_chest) {{ blockTable = crates; }}, 1);
                    add(new JigsawPiece("meteor_dragon_tesla", StructureManager.meteor_dragon_tesla) {{ blockTable = crates; }}, 1);
                    add(new JigsawPiece("meteor_dragon_trap", StructureManager.meteor_dragon_trap) {{ blockTable = crates; }}, 1);
                    add(new JigsawPiece("meteor_dragon_crate_crab", StructureManager.meteor_dragon_crate_crab) {{ blockTable = crates; }}, 1);
                    fallback = "headback";
                }});
                put("fallback", new JigsawPool() {{
                    add(new JigsawPiece("meteor_fallback", StructureManager.meteor_fallback) {{ blockTable = bricks; }}, 1);
                }});
                put("roomback", new JigsawPool() {{
                    add(new JigsawPiece("meteor_room_fallback", StructureManager.meteor_room_fallback) {{ blockTable = bricks; }}, 1);
                }});
                put("headback", new JigsawPool() {{
                    add(new JigsawPiece("meteor_loot_fallback", StructureManager.meteor_dragon_fallback) {{ blockTable = crates; }}, 1);
                }});
            }};
        }});
    }

    private static void ruin(String name, String piece, NBTStructure nbt, int weight) {
        NBTStructure.registerStructure(0, new SpawnCondition(name) {{
            canSpawn = biome -> !isWaterBiome(biome) && biome.canSpawnLightningBolt();
            structure = new JigsawPiece(piece, nbt, -1) {{ conformToTerrain = true; }};
            spawnWeight = StructureConfig.enableRuins ? weight : 0;
        }});
    }
}
