package com.hbm_m.lib;

import java.util.Random;

import com.hbm_m.blockentity.bomb.LandMineBlockEntity;
import com.hbm_m.blockentity.crates.SafeBlockEntity;
import com.hbm_m.config.GeneralConfig;
import com.hbm_m.config.WorldConfig;
import com.hbm_m.itempool.ItemPool;
import com.hbm_m.itempool.ItemPoolsSingle;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.world.dungeon.Antenna;
import com.hbm_m.world.dungeon.ArcticVault;
import com.hbm_m.world.dungeon.Barrel;
import com.hbm_m.world.dungeon.LibraryDungeon;
import com.hbm_m.world.feature.DepthDeposit;
import com.hbm_m.world.feature.Dud;
import com.hbm_m.world.feature.Geyser;
import com.hbm_m.world.gen.LegacyBiome;
import com.hbm_m.world.gen.legacy.L;
import com.hbm_m.world.gen.legacy.LB;
import com.hbm_m.world.gen.legacy.MB;
import com.hbm_m.world.gen.legacy.VB;
import com.hbm_m.world.gen.legacy.WorldGenMinable;
import com.hbm_m.world.generator.DungeonToolbox;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code com.hbm.lib.HbmWorldGen} ({@code IWorldGenerator}, nach der Chunk-Dekoration): Blumen, Gasblasen,
 * Erze, Tiefenerze, Bauwerke und Kleinkram je Dimension. Aufgerufen pro Chunk von
 * {@link com.hbm_m.worldgen.HbmWorldGenFeature}.
 *
 * <p>Teile, die der Port bereits als eigene Merkmale/Strukturen fuehrt, sind hier nur vermerkt:
 * Standard-Erze (ModWorldGenProvider, Hoehen an 1.18+ angepasst), Glyphidennester, Landekapsel, Signallaterne,
 * Coltan-Lagerstaette, Grundgesteinserz, Schluesselloch-Stein. Die grossen Bauwerke (Wuesten-Kraftwerk, Raumschiff,
 * Dschungelverlies, Pyramide) sind groesser als das 3x3-Chunkfenster der 1.20-Merkmale und laufen deshalb als
 * Strukturen ({@link com.hbm_m.world.gen.LegacyDungeonStructure}).</p>
 */
public class HbmWorldGen {

    public void generate(Random rand, int chunkX, int chunkZ, WorldGenLevel world, int dimensionId) {
        // TODO(port): MapGenChainloader.repairBadGeneration (Metadaten-Reparatur fremder Generatoren) entfaellt in 1.20

        switch (dimensionId) {
            case -1:
                generateNether(world, rand, chunkX * 16, chunkZ * 16); break;
            case 0:
                generateSurface(world, rand, chunkX * 16, chunkZ * 16); break;
            case 1:
                generateEnd(world, rand, chunkX * 16, chunkZ * 16); break;
            default:
                if (GeneralConfig.enableMDOres)
                    generateSurface(world, rand, chunkX * 16, chunkZ * 16); break;
        }
    }

    /** Original {@code generateSurface}: Weltschalter "Strukturen", von {@code GeneralConfig.enableDungeons} 0/1 uebersteuert. */
    public static boolean dungeonsEnabled(WorldGenLevel world) {
        boolean enableDungeons = world.getLevel().getServer().getWorldData().worldGenOptions().generateStructures();
        if (GeneralConfig.enableDungeons == 1) enableDungeons = true;
        if (GeneralConfig.enableDungeons == 0) enableDungeons = false;
        return enableDungeons;
    }

    private static LegacyBiome biomeAt(WorldGenLevel world, int x, int z) {
        int y = L.getHeightValue(world, x, z);
        return LegacyBiome.of(world.getBiome(new BlockPos(x, y, z)));
    }

    private void generateSurface(WorldGenLevel world, Random rand, int i, int j) {

        LegacyBiome biome = biomeAt(world, i, j);

        com.hbm_m.saveddata.TomSaveData tom = com.hbm_m.saveddata.TomSaveData.forWorld(world);
        if (tom == null || !tom.impact) {

            if (biome.kind == LegacyBiome.Kind.FOREST && rand.nextInt(16) == 0) {
                DungeonToolbox.generateFlowers(world, rand, i, j, MB.plant_flower, 0); // FOXGLOVE
            }
            if (biome.is("roofedForest") && rand.nextInt(8) == 0) {
                DungeonToolbox.generateFlowers(world, rand, i, j, MB.plant_flower, 2); // NIGHTSHADE
            }
            if (biome.kind == LegacyBiome.Kind.JUNGLE && rand.nextInt(8) == 0) {
                DungeonToolbox.generateFlowers(world, rand, i, j, MB.plant_flower, 1); // TOBACCO
            }
            if (rand.nextInt(64) == 0) {
                DungeonToolbox.generateFlowers(world, rand, i, j, MB.plant_flower, 3); // WEED
            }
            if (biome.kind == LegacyBiome.Kind.RIVER && rand.nextInt(4) == 0) {
                DungeonToolbox.generateFlowers(world, rand, i, j, MB.reeds, 0);
            }
            if (biome.kind == LegacyBiome.Kind.BEACH && rand.nextInt(8) == 0) {
                DungeonToolbox.generateFlowers(world, rand, i, j, MB.reeds, 0);
            }
        }

        if (WorldConfig.gasbubbleSpawn > 0 && rand.nextInt(WorldConfig.gasbubbleSpawn) == 0)
            DungeonToolbox.generateOre(world, rand, i, j, 1, 32, 30, 10, MB.gas_flammable, 1);

        if (WorldConfig.explosivebubbleSpawn > 0 && rand.nextInt(WorldConfig.explosivebubbleSpawn) == 0)
            DungeonToolbox.generateOre(world, rand, i, j, 1, 32, 30, 10, MB.gas_explosive, 1);

        if (WorldConfig.alexandriteSpawn > 0 && rand.nextInt(WorldConfig.alexandriteSpawn) == 0)
            DungeonToolbox.generateOre(world, rand, i, j, 1, 3, 10, 5, MB.ore_alexandrite);

        if (WorldConfig.overworldOre) {

            DepthDeposit.generateConditionOverworld(world, i, 0, 3, j, 5, 0.6D, MB.cluster_depth_iron, rand, 24);
            DepthDeposit.generateConditionOverworld(world, i, 0, 3, j, 5, 0.6D, MB.cluster_depth_titanium, rand, 32);
            DepthDeposit.generateConditionOverworld(world, i, 0, 3, j, 5, 0.6D, MB.cluster_depth_tungsten, rand, 32);
            DepthDeposit.generateConditionOverworld(world, i, 0, 3, j, 5, 0.8D, MB.ore_depth_cinnebar, rand, 16);
            DepthDeposit.generateConditionOverworld(world, i, 0, 3, j, 5, 0.8D, MB.ore_depth_zirconium, rand, 16);
            DepthDeposit.generateConditionOverworld(world, i, 0, 3, j, 5, 0.8D, MB.ore_depth_borax, rand, 16);

            DungeonToolbox.generateOre(world, rand, i, j, 25, 6, 30, 10, MB.ore_gneiss_iron, MB.stone_gneiss);
            DungeonToolbox.generateOre(world, rand, i, j, 10, 6, 30, 10, MB.ore_gneiss_gold, MB.stone_gneiss);
            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.uraniumSpawn * 3, 6, 30, 10, MB.ore_gneiss_uranium, MB.stone_gneiss);
            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.copperSpawn * 3, 6, 30, 10, MB.ore_gneiss_copper, MB.stone_gneiss);
            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.asbestosSpawn * 3, 6, 30, 10, MB.ore_gneiss_asbestos, MB.stone_gneiss);
            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.lithiumSpawn, 6, 30, 10, MB.ore_gneiss_lithium, MB.stone_gneiss);
            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.rareSpawn, 6, 30, 10, MB.ore_gneiss_rare, MB.stone_gneiss);
            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.gassshaleSpawn * 3, 10, 30, 10, MB.ore_gneiss_gas, MB.stone_gneiss);

            // Standard-Erze (ore_uranium ... ore_cobalt): im Port ueber ModWorldGenProvider (Hoehen an 1.18+ angepasst)

            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.ironClusterSpawn, 6, 15, 45, MB.cluster_iron);
            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.titaniumClusterSpawn, 6, 15, 30, MB.cluster_titanium);
            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.aluminiumClusterSpawn, 6, 15, 35, MB.cluster_aluminium);
            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.copperClusterSpawn, 6, 15, 20, MB.cluster_copper);

            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.limestoneSpawn, 16, 25, 30, MB.stone_resource, 4); // LIMESTONE

            // BedrockOre.generateAuto: im Port ore_bedrock_mineral (BedrockOreFeature)
            // Coltan (Lagerstaette + 528-Zufallsadern): im Port ColtanDepositFeature

            for (int k = 0; k < rand.nextInt(4); k++) {
                int randPosX = i + rand.nextInt(16);
                int randPosY = rand.nextInt(15) + 15;
                int randPosZ = j + rand.nextInt(16);

                if (randPosX <= -350 && randPosX >= -450 && randPosZ <= -350 && randPosZ >= -450)
                    (new WorldGenMinable(MB.ore_australium, 50)).generate(world, rand, randPosX, randPosY, randPosZ);
            }
        }

        boolean enableDungeons = dungeonsEnabled(world);

        if (enableDungeons &&world.getLevel().dimension() == net.minecraft.world.level.Level.OVERWORLD) {

            // Glyphidennester: im Port GlyphidHiveFeature

            if (biome.temperature >= 0.4F && biome.rainfall <= 0.6F) {
                if (WorldConfig.antennaStructure > 0 && rand.nextInt(WorldConfig.antennaStructure) == 0) {
                    for (int a = 0; a < 1; a++) {
                        int x = i + rand.nextInt(16);
                        int z = j + rand.nextInt(16);
                        int y = L.getHeightValue(world, x, z);

                        new Antenna().generate(world, rand, x, y, z);
                    }
                }
            }

            // DesertAtom001 (atomStructure): Port als Struktur hbm_m:desert_atom (LegacyDungeonStructure)

            if (WorldConfig.dungeonStructure > 0 && rand.nextInt(WorldConfig.dungeonStructure) == 0) {
                int x = i + rand.nextInt(16);
                // Original: y aus 0-255 der 1.7.10-Welt
                int y = rand.nextInt(256);
                int z = j + rand.nextInt(16);
                new LibraryDungeon().generate(world, rand, x, y, z);
            }

            if (WorldConfig.dudStructure > 0 && rand.nextInt(WorldConfig.dudStructure) == 0) {
                int x = i + 8 + rand.nextInt(16);
                int z = j + 8 + rand.nextInt(16);
                int y = L.getHeightValue(world, x, z);

                new Dud().generate(world, rand, x, y, z);
            }

            // Spaceship (spaceshipStructure): Port als Struktur hbm_m:spaceship (LegacyDungeonStructure)

            if (WorldConfig.barrelStructure > 0 && biome.temperature >= 1.5F && !biome.canSpawnLightningBolt() && rand.nextInt(WorldConfig.barrelStructure) == 0) {
                int x = i + rand.nextInt(16);
                int z = j + rand.nextInt(16);
                int y = L.getHeightValue(world, x, z);

                new Barrel().generate(world, rand, x, y, z);
            }

            if (WorldConfig.broadcaster > 0 && rand.nextInt(WorldConfig.broadcaster) == 0) {
                int x = i + rand.nextInt(16);
                int z = j + rand.nextInt(16);
                int y = L.getHeightValue(world, x, z);

                if (L.getBlock(world, x, y - 1, z).canPlaceTorchOnTop(world, x, y - 1, z)) {
                    L.setBlock(world, x, y, z, MB.broadcaster_pc, rand.nextInt(4) + 2, 2);

                    if (GeneralConfig.enableDebugMode)
                        MainRegistry.LOGGER.info("[Debug] Successfully spawned corrupted broadcaster at " + x + " " + (y) + " " + z);
                }
            }

            if (WorldConfig.minefreq > 0 && GeneralConfig.enableMines && rand.nextInt(WorldConfig.minefreq) == 0) {
                int x = i + rand.nextInt(16) + 8;
                int z = j + rand.nextInt(16) + 8;
                int y = L.getHeightValue(world, x, z);

                for (int g = y + 2; g >= y; g--) {

                    if (L.getBlock(world, x, g - 1, z).canPlaceTorchOnTop(world, x, g - 1, z)) {
                        L.setBlock(world, x, g, z, MB.mine_ap);
                        BlockEntity tile = L.getTileEntity(world, x, g, z);
                        if (tile instanceof LandMineBlockEntity landmine) {
                            landmine.waitingForPlayer = true;
                            landmine.setChanged();
                        }
                        if (GeneralConfig.enableDebugMode) MainRegistry.LOGGER.info("[Debug] Successfully spawned landmine at " + x + " " + g + " " + z);
                        break;
                    }
                }
            }

            // Signallaterne (1/2000): im Port LanternBehemothFeature

            if (GeneralConfig.enable528BosniaSimulator() && rand.nextInt(16) == 0) {
                int x = i + rand.nextInt(16);
                int z = j + rand.nextInt(16);
                int y = L.getHeightValue(world, x, z);
                if (L.getBlock(world, x, y - 1, z).canPlaceTorchOnTop(world, x, y - 1, z)) {
                    L.setBlock(world, x, y, z, MB.mine_he);
                    BlockEntity tile = L.getTileEntity(world, x, y, z);
                    if (tile instanceof LandMineBlockEntity landmine) {
                        landmine.waitingForPlayer = true;
                        landmine.setChanged();
                    }
                }
            }

            if (WorldConfig.geyserChlorine > 0 && biome.is("plains") && rand.nextInt(WorldConfig.geyserChlorine) == 0) {
                int x = i + rand.nextInt(16);
                int z = j + rand.nextInt(16);
                int y = L.getHeightValue(world, x, z);

                if (L.getBlock(world, x, y - 1, z) == VB.grass)
                    new Geyser().generate(world, rand, x, y, z);
            }

            // Landekapsel (capsuleStructure, Strand): im Port SoyuzCapsuleFeature

            if (rand.nextInt(1000) == 0) {
                int x = i + rand.nextInt(16);
                int z = j + rand.nextInt(16);

                boolean done = false;

                for (int k = world.getMinBuildHeight(); k < world.getMaxBuildHeight(); k++) {
                    // Original: Blocks.log mit Meta 0 = Eichenstamm (aufrecht)
                    if (L.getState(world, x, k, z) == VB.log.state(0)) {
                        L.setBlock(world, x, k, z, MB.pink_log);
                        done = true;
                    }
                }

                if (GeneralConfig.enableDebugMode && done)
                    MainRegistry.LOGGER.info("[Debug] Successfully spawned pink tree at " + x + " " + z);
            }

            if (WorldConfig.vaultfreq > 0 && GeneralConfig.enableVaults && rand.nextInt(WorldConfig.vaultfreq) == 0) {
                int x = i + rand.nextInt(16);
                int z = j + rand.nextInt(16);
                int y = L.getHeightValue(world, x, z);

                if (L.getBlock(world, x, y - 1, z).canPlaceTorchOnTop(world, x, y - 1, z)) {
                    L.setBlock(world, x, y, z, MB.safe, rand.nextInt(4) + 2, 2);

                    BlockEntity tile = L.getTileEntity(world, x, y, z);
                    if (tile instanceof SafeBlockEntity safe) {

                        switch (rand.nextInt(10)) {
                            case 0: case 1: case 2: case 3:
                                safe.setMod(1);
                                ItemPool.generateChestContents(rand, ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_RUSTY), safe, rand.nextInt(4) + 3);
                                break;
                            case 4: case 5: case 6:
                                safe.setMod(0.1);
                                ItemPool.generateChestContents(rand, ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_STANDARD), safe, rand.nextInt(3) + 2);
                                break;
                            case 7: case 8:
                                safe.setMod(0.02);
                                ItemPool.generateChestContents(rand, ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_REINFORCED), safe, rand.nextInt(3) + 1);
                                break;
                            case 9:
                                safe.setMod(0.0);
                                ItemPool.generateChestContents(rand, ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_UNBREAKABLE), safe, rand.nextInt(2) + 1);
                                break;
                        }

                        safe.setPins(rand.nextInt(999) + 1);
                        safe.lock();

                        if (rand.nextInt(10) < 3) safe.fillWithSpiders(); // 30 %: die Tresore stehen schon ewig herum, da wohnen Spinnen drin
                    }

                    if (GeneralConfig.enableDebugMode)
                        MainRegistry.LOGGER.info("[Debug] Successfully spawned safe at " + x + " " + (y + 1) + " " + z);
                }
            }

            // Dschungelverlies (jungleStructure): Port als Struktur hbm_m:jungle_dungeon (LegacyDungeonStructure)

            if (WorldConfig.arcticStructure > 0 && rand.nextInt(WorldConfig.arcticStructure) == 0) {
                int x = i + rand.nextInt(16);
                int z = j + rand.nextInt(16);
                int y = 16 + rand.nextInt(32);
                new ArcticVault().trySpawn(world, x, y, z);
            }

            // Pyramide (pyramidStructure): Port als Struktur hbm_m:pyramid (LegacyDungeonStructure)
        }

        if (WorldConfig.meteoriteSpawn > 0 && rand.nextInt(WorldConfig.meteoriteSpawn) == 0) {
            int x = i + rand.nextInt(16) + 8;
            int z = j + rand.nextInt(16) + 8;
            int y = L.getHeightValue(world, x, z) - rand.nextInt(10);
            LB b = L.getBlock(world, x, y - 2, z);
            if (!b.isAir(world, x, y, z) && !b.getMaterial().isLiquid() && y > world.getMinBuildHeight() + 1)
                (new com.hbm_m.worldgen.Meteorite()).generateWorldgen(world, net.minecraft.util.RandomSource.create(rand.nextLong()), x, y, z);
        }

        // Schluesselloch-Stein (1/4, y 6-18): im Port stone_keyhole (ModWorldGenProvider)

        genBlueprintChest(world, rand, i, j, 5000, 5000);
    }

    private static void genBlueprintChest(WorldGenLevel world, Random rand, int i, int j, int boundsX, int boundsZ) {
        if (Math.abs(i) < 100 && Math.abs(j) < 100) return;
        if (rand.nextInt(20) < 10) return; // nextBoolean haette eine merkwuerdige Periodizitaet

        int cX = Math.abs(i) % boundsX;
        int cZ = Math.abs(j) % boundsZ;

        if (cX >= 0 && cX < 16 && cZ >= 0 && cZ < 16) {
            int x = i + 8;
            int z = j + 8;
            int y = L.getHeightValue(world, x, z) - rand.nextInt(2);

            L.setBlock(world, x, y, z, VB.chest);

            for (int a = x - 1; a <= x + 1; a++) for (int b = y - 1; b <= y + 1; b++) for (int c = z - 1; c <= z + 1; c++) {
                if (a != x || b != y || c != z) L.setBlock(world, a, b, c, VB.obsidian);
            }

            BlockEntity tile = L.getTileEntity(world, x, y, z);

            if (tile != null) ItemPool.generateChestContents(rand, ItemPool.getPool(ItemPoolsSingle.POOL_BLUEPRINTS), tile, 50);
        }
    }

    private void generateNether(WorldGenLevel world, Random rand, int i, int j) {

        if (WorldConfig.netherOre) {
            // Audit 11: Netherhoehe in 1.18+ unveraendert -> Erze 1:1 hier statt ueber ModWorldGenProvider
            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.netherUraniumuSpawn, 6, 0, 127, MB.ore_nether_uranium, VB.netherrack);
            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.netherTungstenSpawn, 10, 0, 127, MB.ore_nether_tungsten, VB.netherrack);
            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.netherSulfurSpawn, 12, 0, 127, MB.ore_nether_sulfur, VB.netherrack);
            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.netherPhosphorusSpawn, 6, 0, 127, MB.ore_nether_fire, VB.netherrack);
            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.netherCoalSpawn, 32, 16, 96, MB.ore_nether_coal, VB.netherrack);
            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.netherCobaltSpawn, 6, 100, 26, MB.ore_nether_cobalt, VB.netherrack);

            if (GeneralConfig.enablePlutoniumOre)
                DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.netherPlutoniumSpawn, 4, 0, 127, MB.ore_nether_plutonium, VB.netherrack);

            // Grundgesteinserz des Nethers: im Port NetherBedrockOreFeature

            DepthDeposit.generateConditionNether(world, i, 0, 3, j, 7, 0.6D, MB.ore_depth_nether_neodymium, rand, 16);
            DepthDeposit.generateConditionNether(world, i, 125, 3, j, 7, 0.6D, MB.ore_depth_nether_neodymium, rand, 16);
        }

        for (int k = 0; k < 30; k++) {
            int x = i + rand.nextInt(16);
            int z = j + rand.nextInt(16);
            int d = 16 + rand.nextInt(96);

            for (int y = d - 5; y <= d; y++)
                if (L.getBlock(world, x, y + 1, z) == VB.air && L.getBlock(world, x, y, z) == VB.netherrack)
                    L.setBlock(world, x, y, z, MB.ore_nether_smoldering);
        }

        for (int k = 0; k < 1; k++) {
            int x = i + rand.nextInt(16);
            int z = j + rand.nextInt(16);
            int d = 16 + rand.nextInt(96);

            for (int y = d - 5; y <= d; y++)
                if (L.getBlock(world, x, y + 1, z) == VB.air && L.getBlock(world, x, y, z) == VB.netherrack)
                    L.setBlock(world, x, y, z, MB.geysir_nether);
        }
    }

    private void generateEnd(WorldGenLevel world, Random rand, int i, int j) {

        if (WorldConfig.endOre) {
            DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.endTikiteSpawn, 6, 0, 127, MB.ore_tikite, VB.end_stone);
        }
    }
}
