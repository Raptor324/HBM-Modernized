package com.hbm_m.worldgen;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Порт {@code OreLayer3D} из 1.7.10 — объёмные (3D-перлин) пластовые поля
 * stone_resource: HEMATITE (scaleH 0.04, scaleV 0.25), BAUXITE (0.03/0.15),
 * MALACHITE (0.1/0.15).
 *
 * Математика оригинала: три независимых шума (4 октавы):
 * <pre>
 *   nx = noiseX(y * scaleV, z * scaleH);
 *   nz = noiseZ(x * scaleH, y * scaleV);
 *   ny = noiseY(x * scaleH, z * scaleH);
 *   if (nx * ny * nz > threshold) заменить камень;
 * </pre>
 * Диапазон y в оригинале 6..64 (тут: −58..0 по конвенции «y → y − 64»).
 *
 * Отличия от оригинала:
 *  - Оригинальный перлин 1.7.10 выдавал значения, сильно выходящие за [-1, 1],
 *    отсюда пороги 230/300/275. Здесь шум нормирован, поэтому пороги
 *    откалиброваны с сохранением исходного соотношения редкости:
 *    гематит 0.10 (порог 230), малахит 0.14 (275), боксит 0.18 (300).
 *  - В оригинале значение nz бралось из noiseX (кэш cacheZ/noiseZ вычислялся,
 *    но не использовался — очевидный баг); здесь noiseZ используется по назначению.
 */
public class OreLayer3DFeature extends Feature<NoneFeatureConfiguration> {

    /** Современный диапазон полосы: оригинальные y 6..64 → −58..0. */
    private static final int Y_MIN = -58;
    private static final int Y_MAX = 0;

    private final Supplier<Block> block;
    private final double scaleH;
    private final double scaleV;
    private final double threshold;
    private final int noiseId;

    public OreLayer3DFeature(Supplier<Block> block, double scaleH, double scaleV,
                             double threshold, int noiseId) {
        super(NoneFeatureConfiguration.CODEC);
        this.block = block;
        this.scaleH = scaleH;
        this.scaleV = scaleV;
        this.threshold = threshold;
        this.noiseId = noiseId;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        // Соли как в оригинале: worldSeed + 101/102/103 + id
        HbmPerlinNoise noiseX = new HbmPerlinNoise(level.getSeed() + 101 + noiseId, 4);
        HbmPerlinNoise noiseY = new HbmPerlinNoise(level.getSeed() + 102 + noiseId, 4);
        HbmPerlinNoise noiseZ = new HbmPerlinNoise(level.getSeed() + 103 + noiseId, 4);

        Block ore = block.get();
        boolean placed = false;

        for (int ox = 0; ox < 16; ox++) {
            for (int oz = 0; oz < 16; oz++) {
                int x = origin.getX() + ox;
                int z = origin.getZ() + oz;

                double ny = noiseY.getValue(x * scaleH, z * scaleH);

                for (int y = Y_MIN; y <= Y_MAX; y++) {
                    double nx = noiseX.getValue(y * scaleV, z * scaleH);
                    double nz = noiseZ.getValue(x * scaleH, y * scaleV);

                    if (nx * ny * nz > threshold) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (state.is(BlockTags.STONE_ORE_REPLACEABLES)
                                || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
                            level.setBlock(pos, ore.defaultBlockState(), 2);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }
}
