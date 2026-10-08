package com.hbm_m.world.generator;

import java.util.List;
import java.util.Random;

import com.hbm_m.util.Vec3NT;
import com.hbm_m.world.gen.legacy.L;
import com.hbm_m.world.gen.legacy.LB;
import com.hbm_m.world.gen.legacy.MetaBlock;
import com.hbm_m.world.gen.legacy.VB;
import com.hbm_m.world.gen.legacy.WorldGenFlowers;
import com.hbm_m.world.gen.legacy.WorldGenMinable;

import net.minecraft.world.level.LevelAccessor;

/** 1:1 {@code com.hbm.world.generator.DungeonToolbox}: Kisten, Zufallsauswahl, Erzadern und Blumen fuer die Altklassen. */
public class DungeonToolbox {

    public static void generateBox(LevelAccessor world, int x, int y, int z, int sx, int sy, int sz, List<MetaBlock> blocks) {

        if (blocks.isEmpty())
            return;

        Random rand = L.rand(world);
        for (int i = x; i < x + sx; i++) {
            for (int j = y; j < y + sy; j++) {
                for (int k = z; k < z + sz; k++) {
                    MetaBlock b = getRandom(blocks, rand);
                    L.setBlock(world, i, j, k, b.block, b.meta, 2);
                }
            }
        }
    }

    public static void generateBox(LevelAccessor world, int x, int y, int z, int sx, int sy, int sz, LB block) {
        generateBox(world, x, y, z, sx, sy, sz, new MetaBlock(block));
    }

    // Kopie statt Huelle, damit keine Einzellisten entstehen
    public static void generateBox(LevelAccessor world, int x, int y, int z, int sx, int sy, int sz, MetaBlock block) {

        for (int i = x; i < x + sx; i++) {
            for (int j = y; j < y + sy; j++) {
                for (int k = z; k < z + sz; k++) {
                    L.setBlock(world, i, j, k, block.block, block.meta, 2);
                }
            }
        }
    }

    // mit Vektoren fuer bequeme Drehungen
    public static void generateBox(LevelAccessor world, int x, int y, int z, Vec3NT size, List<MetaBlock> blocks) {
        generateBox(world, x, y, z, (int) size.xCoord, (int) size.yCoord, (int) size.zCoord, blocks);
    }

    public static <T> T getRandom(List<T> list, Random rand) {

        if (list.isEmpty())
            return null;

        return list.get(rand.nextInt(list.size()));
    }

    public static void generateOre(LevelAccessor world, Random rand, int chunkX, int chunkZ, int veinCount, int amount, int minHeight, int variance, LB ore) {
        generateOre(world, rand, chunkX, chunkZ, veinCount, amount, minHeight, variance, ore, 0, VB.stone);
    }

    public static void generateOre(LevelAccessor world, Random rand, int chunkX, int chunkZ, int veinCount, int amount, int minHeight, int variance, LB ore, int meta) {
        generateOre(world, rand, chunkX, chunkZ, veinCount, amount, minHeight, variance, ore, meta, VB.stone);
    }

    public static void generateOre(LevelAccessor world, Random rand, int chunkX, int chunkZ, int veinCount, int amount, int minHeight, int variance, LB ore, LB target) {
        generateOre(world, rand, chunkX, chunkZ, veinCount, amount, minHeight, variance, ore, 0, target);
    }

    public static void generateOre(LevelAccessor world, Random rand, int chunkX, int chunkZ, int veinCount, int amount, int minHeight, int variance, LB ore, int meta, LB target) {

        for (int i = 0; i < veinCount; i++) {

            int x = chunkX + rand.nextInt(16);
            int y = minHeight + (variance > 0 ? rand.nextInt(variance) : 0);
            int z = chunkZ + rand.nextInt(16);

            (new WorldGenMinable(ore, meta, amount, target)).generate(world, rand, x, y, z);
        }
    }

    private static final WorldGenFlowers genFlowers = new WorldGenFlowers(null);

    public static synchronized void generateFlowers(LevelAccessor world, Random rand, int chunkX, int chunkZ, LB flower, int meta) {
        int x = chunkX + rand.nextInt(16) + 8;
        int z = chunkZ + rand.nextInt(16) + 8;
        int y = L.getHeightValue(world, x, z);
        genFlowers.setFlower(flower, meta);
        genFlowers.generate(world, rand, x, y, z);
    }
}
