package com.hbm_m.worldgen;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Порт полосовых (stratum) генераторов 1.7.10 на базе 2D-перлина:
 * {@code SchistStratum} (полоса каменного гнейса) и {@code OreCave}
 * (кожа пещер из stone_resource SULFUR/ASBESTOS).
 *
 * Математика оригинала (одна для обоих типов):
 * <pre>
 *   double n = noise.func_151601_a(x * 0.01, z * 0.01);
 *   if (n > threshold) {
 *       int range = (int)((n - threshold) * rangeMult);
 *       if (range > maxRange) range = (maxRange * 2) - range;
 *       if (range < 0) skip;
 *       fill y in [yLevel - range, yLevel + range];
 *   }
 * </pre>
 * Оригинальный перлин 1.7.10 выдавал значения, сильно выходящие за [-1, 1]
 * (пороги: 5 у гнейса, 1.5/1.75 у пещер). Здесь шум нормирован в [-1, 1], а
 * диапазон восстанавливается множителем {@code noiseSpan}: v = n * noiseSpan,
 * к которому применяется исходный порог/разложение. Значения noiseSpan
 * подобраны так, чтобы (v - threshold) * rangeMult упиралось в тот же потолок
 * maxRange, что и в оригинале.
 *
 * Y-координаты пересчитаны по конвенции порта «оригинальный y → y − 64».
 */
public class PerlinBandFeature extends Feature<NoneFeatureConfiguration> {

    /** Сплошная полоса (SchistStratum): заменяет камень во всей полосе. */
    public static final int MODE_SOLID = 0;
    /** Кожа пещер (OreCave): только камень рядом с воздухом + декор. */
    public static final int MODE_SKIN = 1;

    private final Supplier<Block> block;
    private final int mode;
    private final double noiseSpan;
    private final double threshold;
    private final int rangeMult;
    private final int maxRange;
    private final int yLevel;
    private final int octaves;
    private final long seedSalt;
    private final Supplier<Block> stalactite;
    private final Supplier<Block> stalagmite;

    /**
     * @param seedSalt  соль сида; оригинал: SchistStratum — 0,
     *                  OreCave — oreMeta * 31 + yLevel (оригинальные значения!)
     * @param yLevel    центр полосы (уже пересчитанный, современный y)
     */
    public PerlinBandFeature(Supplier<Block> block, int mode, double noiseSpan,
                             double threshold, int rangeMult, int maxRange,
                             int yLevel, int octaves, long seedSalt,
                             Supplier<Block> stalactite, Supplier<Block> stalagmite) {
        super(NoneFeatureConfiguration.CODEC);
        this.block = block;
        this.mode = mode;
        this.noiseSpan = noiseSpan;
        this.threshold = threshold;
        this.rangeMult = rangeMult;
        this.maxRange = maxRange;
        this.yLevel = yLevel;
        this.octaves = octaves;
        this.seedSalt = seedSalt;
        this.stalactite = stalactite;
        this.stalagmite = stalagmite;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource rand = context.random();

        HbmPerlinNoise noise = new HbmPerlinNoise(level.getSeed() + seedSalt, octaves);

        Block ore = block.get();
        boolean placed = false;

        for (int ox = 0; ox < 16; ox++) {
            for (int oz = 0; oz < 16; oz++) {
                int x = origin.getX() + ox;
                int z = origin.getZ() + oz;

                double v = noise.getValue(x * 0.01D, z * 0.01D) * noiseSpan;
                if (v <= threshold) continue;

                int range = (int) ((v - threshold) * rangeMult);
                if (range > maxRange) range = (maxRange * 2) - range;
                if (range < 0) continue;

                for (int y = yLevel - range; y <= yLevel + range; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (mode == MODE_SOLID) {
                        if (isReplaceable(state)) {
                            level.setBlock(pos, ore.defaultBlockState(), 2);
                            placed = true;
                        }
                    } else {
                        // MODE_SKIN: камень, соседствующий с воздухом (или декором)
                        if (isReplaceable(state) && hasAirNeighbor(level, pos)) {
                            level.setBlock(pos, ore.defaultBlockState(), 2);
                            placed = true;
                        } else if (state.isAir() && rand.nextInt(5) == 0) {
                            // Декор: сталактит, иначе сталагмит (как в OreCave)
                            Block deco = rand.nextBoolean() ? stalactite.get() : stalagmite.get();
                            level.setBlock(pos, deco.defaultBlockState(), 2);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }

    private static boolean isReplaceable(BlockState state) {
        return state.is(BlockTags.STONE_ORE_REPLACEABLES)
                || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
    }

    private static boolean hasAirNeighbor(WorldGenLevel level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).isAir()) return true;
        }
        return false;
    }
}
