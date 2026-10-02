package com.hbm_m.inventory.fluid;

import com.hbm_m.block.fluid.HbmFluidBlock;

import dev.architectury.core.fluid.ArchitecturyFlowingFluid;
import dev.architectury.core.fluid.ArchitecturyFluidAttributes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Welt-Fluessigkeit mit den Haken, die HBMs {@code BlockFluidClassic}-Unterklassen brauchen: der
 * Block erfaehrt jeden Fliess-Tick ({@code updateTick}), bekommt auf Wunsch Zufallsticks und
 * entscheidet ueber {@code canDisplace}, wohin die Fluessigkeit fliesst.
 */
public final class HbmFlowingFluid {

    private HbmFlowingFluid() {}

    private static HbmFluidBlock block(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof HbmFluidBlock b ? b : null;
    }

    private static Boolean displace(BlockGetter level, BlockState from, BlockPos to, BlockState toState) {
        return from.getBlock() instanceof HbmFluidBlock b ? b.canDisplace(level, to, toState) : null;
    }

    public static class Source extends ArchitecturyFlowingFluid.Source {
        public Source(ArchitecturyFluidAttributes attributes) { super(attributes); }

        @Override
        public void tick(Level level, BlockPos pos, FluidState state) {
            super.tick(level, pos, state);
            HbmFluidBlock b = block(level, pos);
            if (b != null) b.onFluidTick(level, pos, level.getBlockState(pos));
        }

        @Override
        protected boolean isRandomlyTicking() {
            return true;
        }

        @Override
        protected void randomTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
            HbmFluidBlock b = block(level, pos);
            if (b != null && b.ticksRandomly() && level instanceof ServerLevel sl) b.onRandomFluidTick(sl, pos, level.getBlockState(pos), random);
        }

        @Override
        protected boolean canSpreadTo(BlockGetter level, BlockPos fromPos, BlockState fromState, Direction dir, BlockPos toPos,
                                      BlockState toState, FluidState toFluid, Fluid fluid) {
            Boolean r = displace(level, fromState, toPos, toState);
            if (r != null && !r) return false;
            if (r != null && toFluid.isEmpty()) return true;
            return super.canSpreadTo(level, fromPos, fromState, dir, toPos, toState, toFluid, fluid);
        }
    }

    public static class Flowing extends ArchitecturyFlowingFluid.Flowing {
        public Flowing(ArchitecturyFluidAttributes attributes) { super(attributes); }

        @Override
        public void tick(Level level, BlockPos pos, FluidState state) {
            super.tick(level, pos, state);
            HbmFluidBlock b = block(level, pos);
            if (b != null) b.onFluidTick(level, pos, level.getBlockState(pos));
        }

        @Override
        protected boolean isRandomlyTicking() {
            return true;
        }

        @Override
        protected void randomTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
            HbmFluidBlock b = block(level, pos);
            if (b != null && b.ticksRandomly() && level instanceof ServerLevel sl) b.onRandomFluidTick(sl, pos, level.getBlockState(pos), random);
        }

        @Override
        protected boolean canSpreadTo(BlockGetter level, BlockPos fromPos, BlockState fromState, Direction dir, BlockPos toPos,
                                      BlockState toState, FluidState toFluid, Fluid fluid) {
            Boolean r = displace(level, fromState, toPos, toState);
            if (r != null && !r) return false;
            if (r != null && toFluid.isEmpty()) return true;
            return super.canSpreadTo(level, fromPos, fromState, dir, toPos, toState, toFluid, fluid);
        }
    }
}
