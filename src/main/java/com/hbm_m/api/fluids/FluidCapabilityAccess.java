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

        //? if fabric {
        /*// Тот же overload, что и в FluidDuctBlockEntity / ForgeFluidHandlerAdapter: иначе 3-arg find даёт иной результат и труба «липнет» к контроллеру.
        if (level instanceof net.minecraft.world.level.Level lvl) {
            BlockState state = level.getBlockState(pos);
            result = net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(lvl, pos, state, be, sideFromPos) != null;
        }
        *///?}

        return result;
    }
}