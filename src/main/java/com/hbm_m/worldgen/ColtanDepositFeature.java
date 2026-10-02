package com.hbm_m.worldgen;

import java.util.List;
import java.util.Optional;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.config.ModClothConfig;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

/**
 * 1:1 HbmWorldGen (Coltan): die Lagerstaette liegt um einen einzigen, aus dem Weltseed bestimmten Punkt
 * ({@code new Random(seed + 5)}, Gauss * 1500) - genau dorthin zeigt der Coltan-Kompass ({@code coltan_tool}).
 * Pro Chunk 2 x 5 Versuche, Reichweite 750 / r, Adern der Groesse 4 auf Hoehe 15-39.
 * Optional ({@code enable528ColtanSpawn}) zusaetzlich zufaellig verteilte Adern wie normale Erze.
 */
public class ColtanDepositFeature extends Feature<NoneFeatureConfiguration> {

    public ColtanDepositFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** Lage der Lagerstaette (colX, colZ) wie im Original und im Kompass. */
    public static int[] depositCenter(long seed) {
        java.util.Random colRand = new java.util.Random(seed + 5);
        int colX = (int) (colRand.nextGaussian() * 1500);
        int colZ = (int) (colRand.nextGaussian() * 1500);
        return new int[] { colX, colZ };
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        int i = context.origin().getX() & ~15;
        int j = context.origin().getZ() & ~15;
        ModClothConfig cfg = ModClothConfig.get();
        boolean placed = false;

        if (cfg.enable528ColtanSpawn) {
            for (int v = 0; v < cfg.coltanRate; v++) {
                placed |= vein(context, rand, new BlockPos(i + rand.nextInt(16), 15 + rand.nextInt(40), j + rand.nextInt(16)));
            }
        }

        int[] col = depositCenter(level.getSeed());
        int colX = col[0];
        int colZ = col[1];
        int colRange = 750;

        if (cfg.enable528ColtanDeposit) {
            for (int k = 0; k < 2; k++) {
                for (int r = 1; r <= 5; r++) {
                    int randPosX = i + rand.nextInt(16);
                    int randPosY = rand.nextInt(25) + 15;
                    int randPosZ = j + rand.nextInt(16);

                    int range = colRange / r;

                    if (randPosX <= colX + range && randPosX >= colX - range && randPosZ <= colZ + range && randPosZ >= colZ - range) {
                        placed |= vein(context, rand, new BlockPos(randPosX, randPosY, randPosZ));
                    }
                }
            }
        }
        return placed;
    }

    /** WorldGenMinable(ore_coltan, 4): Ader aus 4 Bloecken im Stein (Tiefenschiefer als Tiefenschiefer-Variante). */
    private static boolean vein(FeaturePlaceContext<NoneFeatureConfiguration> context, RandomSource rand, BlockPos pos) {
        OreConfiguration config = new OreConfiguration(List.of(
                OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), ModBlocks.COLTAN_ORE.get().defaultBlockState()),
                OreConfiguration.target(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES), ModBlocks.COLTAN_ORE_DEEPSLATE.get().defaultBlockState())), 4);
        return Feature.ORE.place(new FeaturePlaceContext<>(Optional.empty(), context.level(), context.chunkGenerator(), rand, pos, config));
    }
}
