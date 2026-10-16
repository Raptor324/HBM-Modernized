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
                if (!existing.is(Blocks.BEDROCK) && !existing.canBeReplaced()) continue;

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
                        if (existing.is(Blocks.BEDROCK) || existing.canBeReplaced()) {
                            level.setBlock(pos, depthRock.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        return placed > 0;
    }

    /** Весовая таблица оригинала: glowstone 100, phosphorus 50, quartz 100 (стаков по 4). */
    private ItemStack pickResource(net.minecraft.util.RandomSource rand) {
        int roll = rand.nextInt(250);
        if (roll < 100) return new ItemStack(Items.GLOWSTONE_DUST, 4);
        if (roll < 150) return new ItemStack(ModItems.CRYSTAL_PHOSPHORUS.get(), 4);
        return new ItemStack(Items.QUARTZ, 4);
    }
}
