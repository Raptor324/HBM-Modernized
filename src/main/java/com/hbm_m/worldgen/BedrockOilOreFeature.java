package com.hbm_m.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import com.hbm_m.block.ModBlocks;

/**
 * Feature für Bedrock Oil Ore Generation
 * - Ersetzt Bedrock-Blöcke mit Ore_Bedrock_Oil
 * - Generiert einen kompletten Chunk (16x16x16 = 256 Blöcke pro Y-Level)
 * - Nur auf Bedrock-Höhe (Y=0 bis -64)
 */
public class BedrockOilOreFeature extends Feature<NoneFeatureConfiguration> {

    public BedrockOilOreFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos origin = context.origin();
        LevelAccessor level = context.level();

        // Erzeuge auf den unteren Bedrock-Schichten.
        final int minTargetY = -64;
        final int maxTargetY = -63;

        // Auf Chunk-Grenzen ausrichten, damit es wirklich ein kompletter 16x16-Chunk wird.
        int startX = origin.getX() & ~15;
        int startZ = origin.getZ() & ~15;

        int blockCount = 0;
        for (int x = startX; x < startX + 16; x++) {
            for (int z = startZ; z < startZ + 16; z++) {
                for (int y = minTargetY; y <= maxTargetY; y++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    level.setBlock(checkPos, ModBlocks.ORE_BEDROCK_OIL.get().defaultBlockState(), 3);
                    blockCount++;
                }
            }
        }

        return blockCount > 0;
    }
}
