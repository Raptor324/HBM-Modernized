package com.hbm_m.api.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class FluidCapabilityAccess {
    private FluidCapabilityAccess() {}

    public static boolean hasFluidHandler(LevelAccessor level, BlockPos pos, Direction sideFromPos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;

        boolean result = false;

        //? if forge {
        result = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, sideFromPos).isPresent();
        //?}


        //? if neoforge {
        /*// NeoForge: BlockCapability запрашивается через Level (LevelAccessor его не предоставляет).
        if (level instanceof net.minecraft.world.level.Level lvl) {
            result = lvl.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                    pos, sideFromPos) != null;
        }
        *///?}

        return result;
    }
}