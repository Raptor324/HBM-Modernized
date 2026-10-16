package com.hbm_m.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import com.hbm_m.block.ModBlocks;

/**
 * Порт {@code MapGenBedrockOil} из 1.7.10: октаэдр блоков {@code ore_bedrock_oil}
 * (|dx|<5, |dz|<5, |dx|+|dy|+|dz|<=6) вокруг точки спавна на бедроке, заменяя
 * бедрок и камень/сланец. Частота спавна (rarity в placed feature) — как в
 * оригинале: 1 месторождение на ~200 чанков.
 */
public class BedrockOilOreFeature extends Feature<NoneFeatureConfiguration> {

    public BedrockOilOreFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos origin = context.origin();
        LevelAccessor level = context.level();

        int baseY = level.getMinBuildHeight();
        BlockState oil = ModBlocks.ORE_BEDROCK_OIL.get().defaultBlockState();
        int placed = 0;

        // Октаэдр из MapGenBedrockOil.func_151538_a: |x|<5, y 0..4, |x|+|y|+|z| <= 6
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int dy = 0; dy < 5; dy++) {
                    if (Math.abs(dx) + dy + Math.abs(dz) > 6) continue;

                    BlockPos pos = new BlockPos(origin.getX() + dx, baseY + dy, origin.getZ() + dz);
                    if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) continue;

                    BlockState existing = level.getBlockState(pos);
                    boolean replaceable = existing.is(net.minecraft.world.level.block.Blocks.BEDROCK)
                            || existing.is(net.minecraft.tags.BlockTags.STONE_ORE_REPLACEABLES)
                            || existing.is(net.minecraft.tags.BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                            || existing.isAir();
                    if (!replaceable) continue;

                    level.setBlock(pos, oil, 3);
                    placed++;
                }
            }
        }

        return placed > 0;
    }
}
