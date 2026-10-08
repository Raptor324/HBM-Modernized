package com.hbm_m.worldgen;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.nature.OreBedrockBlockEntity;
import com.hbm_m.item.ModItems;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Порт незерской бедрок-руды ({@code BedrockOre.weightedOresNether} + generate из 1.7.10):
 * patch 3x3 блоков {@code ore_bedrock} прямо на бедроке Незера с ресурсом из весовой
 * таблицы (свечение 100 / фосфор 50 / кварц 100), залитый сверху {@code stone_depth_nether}.
 * У незерских залежей Tier 1 — кислота не требуется (как в оригинале).
 */
public class NetherBedrockOreFeature extends Feature<NoneFeatureConfiguration> {

    public NetherBedrockOreFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        // Original: nur innerhalb von if(WorldConfig.netherOre)
        if (!com.hbm_m.config.WorldConfig.netherOre) return false;
        BlockPos origin = context.origin();
        LevelAccessor level = context.level();
        var rand = context.random();

        ItemStack resource = pickResource(rand);
        BlockState oreState = ModBlocks.ORE_BEDROCK.get().defaultBlockState();
        int placed = 0;

        int baseY = level.getMinBuildHeight();
        int x = origin.getX();
        int z = origin.getZ();

        for (int ix = x - 1; ix <= x + 1; ix++) {
            for (int iz = z - 1; iz <= z + 1; iz++) {
                boolean isCenter = ix == x && iz == z;
                if (!isCenter && rand.nextBoolean()) continue;

                BlockPos pos = new BlockPos(ix, baseY, iz);
                BlockState existing = level.getBlockState(pos);
                if (!existing.is(Blocks.BEDROCK)) continue; // Original: isReplaceableOreGen(bedrock)

                level.setBlock(pos, oreState, 3);
                if (level.getBlockEntity(pos) instanceof OreBedrockBlockEntity ore) {
                    ore.resource = resource.copy();
                    ore.acidType = net.minecraft.world.level.material.Fluids.EMPTY;
                    ore.acidAmountMb = 0;
                    ore.tier = 1;
                }
                placed++;
            }
        }

        if (placed > 0) {
            Block depthRock = ModBlocks.STONE_DEPTH_NETHER.get();
            for (int ix = x - 3; ix <= x + 3; ix++) {
                for (int iz = z - 3; iz <= z + 3; iz++) {
                    for (int iy = baseY + 1; iy <= baseY + 6; iy++) {
                        BlockPos pos = new BlockPos(ix, iy, iz);
                        BlockState existing = level.getBlockState(pos);
                        if ((iy < baseY + 3 || existing.is(net.minecraft.world.level.block.Blocks.BEDROCK))
                                && (existing.is(net.minecraft.world.level.block.Blocks.BEDROCK)
                                || existing.is(net.minecraft.tags.BlockTags.STONE_ORE_REPLACEABLES)
                                || existing.is(net.minecraft.tags.BlockTags.DEEPSLATE_ORE_REPLACEABLES))) {
                            level.setBlock(pos, depthRock.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        return placed > 0;
    }

    /**
     * Весовая таблица оригинала (WeightedRandom): glowstone / phosphorus / quartz, стаков по 4.
     * Restport: Gewichte aus WorldConfig.bedrock{Glowstone,Phosphorus,Quartz}Spawn (Vorgabe 100/50/100).
     */
    private ItemStack pickResource(net.minecraft.util.RandomSource rand) {
        int glow = Math.max(0, com.hbm_m.config.WorldConfig.bedrockGlowstoneSpawn);
        int phos = Math.max(0, com.hbm_m.config.WorldConfig.bedrockPhosphorusSpawn);
        int quartz = Math.max(0, com.hbm_m.config.WorldConfig.bedrockQuartzSpawn);
        int total = glow + phos + quartz;
        if (total <= 0) return new ItemStack(Items.GLOWSTONE_DUST, 4);
        int roll = rand.nextInt(total);
        if (roll < glow) return new ItemStack(Items.GLOWSTONE_DUST, 4);
        if (roll < glow + phos) return new ItemStack(com.hbm_m.item.ModItems.FIRE_POWDER.get(), 4); // Original: powder_fire
        return new ItemStack(Items.QUARTZ, 4);
    }
}
