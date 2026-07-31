package com.hbm_m.worldgen;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.nature.OreBedrockBlockEntity;
import com.hbm_m.item.ModItems;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 1:1-Aequivalent zu {@code BedrockOre.generateAuto}/{@code generate} aus dem 1.7.10-Original:
 * setzt ein paar {@code ore_bedrock}-Bloecke direkt auf der Bedrock-Schicht (deterministische
 * Dichte per {@link BedrockOreDensity}, siehe dort fuer Tier/Fluid-Tabelle) und fuellt darueber
 * mit {@code stone_depth} auf, damit der Mining Drill eine abbaubare "Ader" vorfindet.
 */
public class BedrockOreFeature extends Feature<NoneFeatureConfiguration> {

    public BedrockOreFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos origin = context.origin();
        LevelAccessor level = context.level();

        int baseY = level.getMinBuildHeight();
        int x = origin.getX();
        int z = origin.getZ();

        double density = BedrockOreDensity.getTotalDensity(x, z);
        int tier = BedrockOreDensity.getTier(density);
        var acid = BedrockOreDensity.getBoreFluid(density);
        int acidAmount = BedrockOreDensity.getBoreFluidAmountMb(density);

        BlockState oreState = ModBlocks.ORE_BEDROCK.get().defaultBlockState();
        int placed = 0;

        for (int ix = x - 1; ix <= x + 1; ix++) {
            for (int iz = z - 1; iz <= z + 1; iz++) {
                boolean isCenter = ix == x && iz == z;
                if (!isCenter && level.getRandom().nextBoolean()) continue;

                BlockPos pos = new BlockPos(ix, baseY, iz);
                BlockState existing = level.getBlockState(pos);
                if (!existing.is(net.minecraft.world.level.block.Blocks.BEDROCK) && !existing.canBeReplaced()) continue;

                level.setBlock(pos, oreState, 3);
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof OreBedrockBlockEntity ore) {
                    ore.resource = new net.minecraft.world.item.ItemStack(ModItems.BEDROCK_ORE_BASE.get());
                    ore.acidType = acid;
                    ore.acidAmountMb = acidAmount;
                    ore.tier = tier;
                }
                placed++;
            }
        }

        if (placed > 0) {
            Block depthRock = ModBlocks.STONE_DEPTH.get();
            for (int ix = x - 3; ix <= x + 3; ix++) {
                for (int iz = z - 3; iz <= z + 3; iz++) {
                    for (int iy = baseY + 1; iy <= baseY + 6; iy++) {
                        BlockPos pos = new BlockPos(ix, iy, iz);
                        BlockState existing = level.getBlockState(pos);
                        if (existing.is(net.minecraft.world.level.block.Blocks.BEDROCK) || existing.canBeReplaced()) {
                            level.setBlock(pos, depthRock.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        return placed > 0;
    }
}
