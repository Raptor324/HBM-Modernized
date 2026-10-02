package com.hbm_m.worldgen;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Порт колтановых месторождений из 1.7.10 (HbmWorldGen, блок enable528ColtanDeposit).
 *
 * В оригинале разбросанного спавна нет: руда генерируется вокруг центров,
 * детерминированно вычисленных из сида мира:
 * <pre>
 *   Random colRand = new Random(world.getSeed() + 5);
 *   int colX = (int)(colRand.nextGaussian() * 1500);
 *   int colZ = (int)(colRand.nextGaussian() * 1500);
 * </pre>
 * Далее на каждый чанк — 2 попытки, каждая проверяет 5 «сужающихся колец»
 * (range = 750 / r); если точка чанка попадает в квадрат колец, ставится
 * жила WorldGenMinable размером 4 (оригинал: y 15..39 → современный −49..−25).
 */
public class ColtanDepositFeature extends Feature<NoneFeatureConfiguration> {

    private static final int SEED_SALT = 5;
    private static final double SIGMA = 1500.0D;
    private static final int BASE_RANGE = 750;

    private static long cachedSeed = Long.MIN_VALUE;
    private static int centerX;
    private static int centerZ;

    public ColtanDepositFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    private static void computeCenters(long seed) {
        if (cachedSeed == seed) return;
        RandomSource rand = RandomSource.create(seed + SEED_SALT);
        centerX = (int) (rand.nextGaussian() * SIGMA);
        centerZ = (int) (rand.nextGaussian() * SIGMA);
        cachedSeed = seed;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource rand = context.random();

        computeCenters(level.getSeed());

        boolean placed = false;
        for (int attempt = 0; attempt < 2; attempt++) {
            for (int ring = 1; ring <= 5; ring++) {
                int x = origin.getX() + rand.nextInt(16);
                int y = rand.nextInt(25) - 49;
                int z = origin.getZ() + rand.nextInt(16);

                int range = BASE_RANGE / ring;
                if (x <= centerX + range && x >= centerX - range
                        && z <= centerZ + range && z >= centerZ - range) {
                    generateVein(level, rand, x, y, z, 4);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /** Аналог WorldGenMinable(ore, 4): блоб из n блоков со случайным смещением. */
    private void generateVein(WorldGenLevel level, RandomSource rand, int x, int y, int z, int size) {
        float dx = rand.nextFloat() * (float) Math.PI;
        double dX = x + Math.sin(dx) * size * 0.5F;
        double dY = y + 0.5D;
        double dZ = z + Math.cos(dx) * size * 0.5F;
        double stepX = -Math.sin(dx) / size;
        double stepZ = Math.cos(dx) / size;

        BlockState ore = ModBlocks.COLTAN_ORE.get().defaultBlockState();

        for (int i = 0; i < size; i++) {
            dX += stepX * rand.nextDouble();
            dZ += stepZ * rand.nextDouble();
            double offset = Math.sin(i * Math.PI / size) * (size * 0.5D);
            for (int bx = (int) (dX - offset); bx <= (int) (dX + offset); bx++) {
                for (int by = (int) (dY - offset); by <= (int) (dY + offset); by++) {
                    for (int bz = (int) (dZ - offset); bz <= (int) (dZ + offset); bz++) {
                        double tx = (bx + 0.5D - dX) / offset;
                        double ty = (by + 0.5D - dY) / offset;
                        double tz = (bz + 0.5D - dZ) / offset;
                        if (tx * tx + ty * ty + tz * tz >= 1.0D) continue;
                        BlockPos pos = new BlockPos(bx, by, bz);
                        BlockState state = level.getBlockState(pos);
                        if (state.is(BlockTags.STONE_ORE_REPLACEABLES)
                                || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
                            level.setBlock(pos, ore, 2);
                        }
                    }
                }
            }
        }
    }
}
