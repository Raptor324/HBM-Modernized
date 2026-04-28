package com.hbm_m.api.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class FluidCapabilityAccess {
    private FluidCapabilityAccess() {}

    public static boolean hasFluidHandler(LevelAccessor level, BlockPos pos, Direction sideFromPos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;

        //? if forge {
        /*return be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, sideFromPos).isPresent();
        *///?} else if fabric {
        return net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(level, pos, sideFromPos) != null;
        //?} else {
        /*return false;
        *///?}
    }
}

