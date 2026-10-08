package com.hbm_m.world.feature;

import java.util.Random;

import com.hbm_m.util.Vec3NT;
import com.hbm_m.world.gen.legacy.L;
import com.hbm_m.world.gen.legacy.LB;
import com.hbm_m.world.gen.legacy.MB;
import com.hbm_m.world.gen.legacy.VB;

import net.minecraft.world.level.LevelAccessor;

/**
 * 1:1 {@code com.hbm.world.feature.DepthDeposit}: kugelige Tiefenerz-Lagerstaette mit Tiefengestein-Huelle in der
 * Grundgesteinsschicht (ersetzt auch Grundgestein).
 *
 * <p>Port: Die Oberwelt beginnt in 1.20 nicht bei y 0, sondern bei {@code getMinBuildHeight()}; die Hoehen der
 * Oberwelt werden deshalb relativ zum Weltboden gerechnet (Nether unveraendert 0-127).</p>
 */
public class DepthDeposit {

    public static void generateConditionOverworld(LevelAccessor world, int x, int yMin, int yDev, int z, int size, double fill, LB block, Random rand, int chance) {

        if (rand.nextInt(chance) == 0)
            generate(world, x + rand.nextInt(16) + 8, world.getMinBuildHeight() + yMin + rand.nextInt(yDev), z + rand.nextInt(16) + 8, size, fill, block, rand, VB.stone, MB.stone_depth, world.getMinBuildHeight());
    }

    public static void generateConditionNether(LevelAccessor world, int x, int yMin, int yDev, int z, int size, double fill, LB block, Random rand, int chance) {

        if (rand.nextInt(chance) == 0)
            generate(world, x + rand.nextInt(16) + 8, yMin + rand.nextInt(yDev), z + rand.nextInt(16) + 8, size, fill, block, rand, VB.netherrack, MB.stone_depth_nether, 0);
    }

    public static void generateCondition(LevelAccessor world, int x, int yMin, int yDev, int z, int size, double fill, LB block, Random rand, int chance, LB genTarget, LB filler) {

        if (rand.nextInt(chance) == 0)
            generate(world, x + rand.nextInt(16) + 8, yMin + rand.nextInt(yDev), z + rand.nextInt(16) + 8, size, fill, block, rand, genTarget, filler, 0);
    }

    public static void generate(LevelAccessor world, int x, int y, int z, int size, double fill, LB block, Random rand, LB genTarget, LB filler, int bottom) {

        for (int i = x - size; i <= x + size; i++) {
            for (int j = y - size; j <= y + size; j++) {
                for (int k = z - size; k <= z + size; k++) {

                    if (j < bottom + 1 || j > bottom + 126)
                        continue;

                    double len = Vec3NT.createVectorHelper(x - i, y - j, z - k).lengthVector();

                    if (L.isReplaceableOreGen(world, i, j, k, genTarget) || L.isReplaceableOreGen(world, i, j, k, VB.bedrock)) { // ja, richtig gehoert: Grundgestein

                        if (len + rand.nextInt(2) < size * fill) {
                            L.setBlock(world, i, j, k, block);

                        } else if (len + rand.nextInt(2) <= size) {
                            L.setBlock(world, i, j, k, filler);
                        }
                    }
                }
            }
        }
    }
}
