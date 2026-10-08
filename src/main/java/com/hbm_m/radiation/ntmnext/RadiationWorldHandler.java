// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (com.hbm.handler.radiation.RadiationWorldHandler), Commit 3f9a261a.

package com.hbm_m.radiation.ntmnext;

import com.hbm_m.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Weltzerstoerung durch hohe Strahlung: Pflanzen sterben, Gras wird zu Oedland. */
final class RadiationWorldHandler {

    private RadiationWorldHandler() {}

    static void decayBlock(ServerLevel world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof DoublePlantBlock) {
            BlockPos base = pos.immutable();
            DoubleBlockHalf half = state.getValue(DoublePlantBlock.HALF);
            BlockPos lower = half == DoubleBlockHalf.LOWER ? base : base.below();
            BlockPos upper = half == DoubleBlockHalf.LOWER ? base.above() : base;
            world.setBlock(upper, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            world.setBlock(lower, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            return;
        }
        if (block == Blocks.GRASS_BLOCK) {
            world.setBlock(
                    pos, ModBlocks.WASTE_EARTH.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            return;
        }

        if (block instanceof TallGrassBlock) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            return;
        }
        if (block instanceof LeavesBlock && block != ModBlocks.WASTE_LEAVES.get()) {
            if (world.getRandom().nextInt(7) <= 5) {
                world.setBlock(
                        pos,
                        ModBlocks.WASTE_LEAVES.get().defaultBlockState(),
                        Block.UPDATE_CLIENTS);
            } else {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }
}
