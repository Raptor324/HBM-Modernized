package com.hbm_m.worldgen;

import com.hbm_m.block.ModBlocks;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 1:1 {@code BiomeDecoratorNoMansLand} (Sellafit-Teil): je Chunk zweimal {@code sellafiteGen} und zweimal
 * {@code gravelGen} - beide im Original {@code WorldGenSurfaceSpot(sellafield_slaked, 6, 0.15F)}. Ein Fleck
 * ersetzt Erde und Gras in einem Kreis (Radius 2-5, +-2 Bloecke um die Oberflaeche) mit 15 % Chance.
 */
public class NoMansLandSpotsFeature extends Feature<NoneFeatureConfiguration> {

    public static final int SELLAFITE_PER_CHUNK = 2;
    public static final int GRAVEL_PER_CHUNK = 2;

    public NoMansLandSpotsFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel world = ctx.level();
        RandomSource rand = ctx.random();
        int chunkX = ctx.origin().getX();
        int chunkZ = ctx.origin().getZ();

        // SELLAFIT, danach "GRAVEL" (im Original ebenfalls Sellafit)
        for (int i = 0; i < SELLAFITE_PER_CHUNK + GRAVEL_PER_CHUNK; ++i) {
            int x = chunkX + rand.nextInt(16) + 8;
            int z = chunkZ + rand.nextInt(16) + 8;
            surfaceSpot(world, rand, x, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z,
                    ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState(), 6, 0.15F);
        }

        return true;
    }

    /** 1:1 {@code WorldGenSurfaceSpot.generate}. */
    public static void surfaceSpot(WorldGenLevel world, RandomSource rand, int x, int y, int z, BlockState spot, int radius, float chance) {
        int r = rand.nextInt(radius - 2) + 2;
        int depth = 2;

        for (int iX = x - r; iX <= x + r; ++iX) {
            for (int iZ = z - r; iZ <= z + r; ++iZ) {
                int k1 = iX - x;
                int l1 = iZ - z;

                if (k1 * k1 + l1 * l1 <= r * r) {
                    for (int iY = y - depth; iY <= y + depth; ++iY) {
                        BlockPos pos = new BlockPos(iX, iY, iZ);
                        BlockState block = world.getBlockState(pos);

                        if (block.is(Blocks.DIRT) || block.is(Blocks.GRASS_BLOCK)) {
                            if (rand.nextFloat() < chance) world.setBlock(pos, spot, 2);
                        }
                    }
                }
            }
        }
    }
}
