package com.hbm_m.block.bomb;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.config.ModClothConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code CrystalVirus} (Dunkelkristall): frisst sich bei Zufallsticks in alle sechs Nachbarn (ausser Luft und
 * Kristall) und verhaertet dabei selbst; verhaertet auch, sobald er rundum nur noch Luft oder Kristall beruehrt.
 * Ausbreitung nur mit {@code enableVirus}.
 */
public class CrystalVirusBlock extends Block {

    public CrystalVirusBlock(Properties properties) {
        super(properties.randomTicks());
    }

    static boolean isCrystalOrAir(BlockState s) {
        return s.isAir() || s.is(ModBlocks.CRYSTAL_VIRUS.get()) || s.is(ModBlocks.CRYSTAL_HARDENED.get()) || s.is(ModBlocks.CRYSTAL_PULSAR.get());
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {

        if (ModClothConfig.get().enableVirus) {
            for (Direction dir : new Direction[] { Direction.EAST, Direction.UP, Direction.SOUTH, Direction.WEST, Direction.DOWN, Direction.NORTH }) {
                BlockPos p = pos.relative(dir);
                if (!isCrystalOrAir(world.getBlockState(p))) {
                    world.setBlockAndUpdate(p, ModBlocks.CRYSTAL_VIRUS.get().defaultBlockState());
                }
            }
            world.setBlockAndUpdate(pos, ModBlocks.CRYSTAL_HARDENED.get().defaultBlockState());
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {

        if (world.isClientSide) return;

        for (Direction dir : Direction.values()) {
            if (!isCrystalOrAir(world.getBlockState(pos.relative(dir)))) return;
        }
        world.setBlockAndUpdate(pos, ModBlocks.CRYSTAL_HARDENED.get().defaultBlockState());
    }
}
