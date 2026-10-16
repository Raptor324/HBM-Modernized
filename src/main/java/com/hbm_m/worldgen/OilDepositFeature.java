package com.hbm_m.worldgen;

import java.util.function.Predicate;

import com.hbm_m.block.ModBlocks;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Порт {@code MapGenBubble} из 1.7.10 (нефтяные месторождения): сплюснутая сфера
 * (коэффициент 3 по вертикали) блоков нефти в камне/сланце, радиус задаётся
 * параметрами фичи. Опционально размазывает границы (fuzzy — песчаные месторождения)
 * и оставляет на поверхности пятна масляной земли над месторождением
 * ({@code addSurfaceSpot} оригинала, упрощённо — без ямы с разливом).
 */
public class OilDepositFeature extends Feature<NoneFeatureConfiguration> {

    private final Block block;
    private final int minSize;
    private final int maxSize;
    private final boolean fuzzy;
    private final Predicate<BlockState> replaceable;
    private final boolean surfaceSpot;

    /** Каменное месторождение нефти (MapGenBubble oilBubble: r 8..16, y 15..40). */
    public static OilDepositFeature stone(Codec<NoneFeatureConfiguration> codec) {
        return new OilDepositFeature(codec, ModBlocks.ORE_OIL.get(), 8, 16, false,
                s -> s.is(BlockTags.STONE_ORE_REPLACEABLES) || s.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES), true);
    }

    /** Песчаное месторождение (sandOilBubble: r 16..48, fuzzy, только пустынные биомы). */
    public static OilDepositFeature sand(Codec<NoneFeatureConfiguration> codec) {
        return new OilDepositFeature(codec, ModBlocks.ORE_OIL_SAND.get(), 16, 48, true,
                s -> s.is(Blocks.SAND) || s.is(Blocks.RED_SAND), false);
    }

    public OilDepositFeature(Codec<NoneFeatureConfiguration> codec, Block block, int minSize, int maxSize,
            boolean fuzzy, Predicate<BlockState> replaceable, boolean surfaceSpot) {
        super(codec);
        this.block = block;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.fuzzy = fuzzy;
        this.replaceable = replaceable;
        this.surfaceSpot = surfaceSpot;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos origin = context.origin();
        LevelAccessor level = context.level();
        var rand = context.random();

        int radius = minSize + rand.nextInt(Math.max(1, maxSize - minSize));
        double radiusSqr = (radius * (double) radius) / 2.0; // как в оригинале
        int placed = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    double rSqr = dx * dx + dz * dz + (double) dy * dy * 3;
                    if (fuzzy) rSqr -= rand.nextDouble() * radiusSqr / 3;
                    if (rSqr >= radiusSqr) continue;

                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) continue;
                    if (!replaceable.test(level.getBlockState(pos))) continue;

                    level.setBlock(pos, block.defaultBlockState(), 3);
                    placed++;
                }
            }
        }

        if (surfaceSpot && placed > 0) {
            addSurfaceSpot(level, origin, rand);
        }

        return placed > 0;
    }

    /** Упрощённый {@code MapGenBubble.addSurfaceSpot}: пятна грязи/песка над месторождением. */
    private void addSurfaceSpot(LevelAccessor level, BlockPos origin, net.minecraft.util.RandomSource rand) {
        int spotWidth = 7;
        for (int i = 0; i < 60; i++) {
            int offX = (int) (rand.nextGaussian() * spotWidth);
            int offZ = (int) (rand.nextGaussian() * spotWidth);
            int x = origin.getX() + offX;
            int z = origin.getZ() + offZ;

            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);

            boolean inner = offX * offX + offZ * offZ < (spotWidth / 2) * (spotWidth / 2);

            Block replacement = null;
            if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)) {
                replacement = inner ? ModBlocks.DIRT_OILY.get() : ModBlocks.DEAD_DIRT.get();
            } else if (state.is(Blocks.SAND)) {
                replacement = ModBlocks.SAND_DIRTY.get();
            } else if (state.is(Blocks.RED_SAND)) {
                replacement = ModBlocks.SAND_DIRTY_RED.get();
            } else if (state.is(Blocks.STONE)) {
                replacement = ModBlocks.STONE_CRACKED.get();
            }

            if (replacement != null) {
                level.setBlock(pos, replacement.defaultBlockState(), 3);
            }
        }
    }
}
