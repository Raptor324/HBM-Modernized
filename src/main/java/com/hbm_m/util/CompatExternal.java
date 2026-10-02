package com.hbm_m.util;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.UniversalMachinePartBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Teil von {@code com.hbm.util.CompatExternal}: Blockentity des Multiblock-Kerns zu einer Position. */
public final class CompatExternal {

    private CompatExternal() {}

    @Nullable
    public static BlockEntity getCoreFromPos(Level world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        // Original: BlockDummyable.findCore; im Port fuehren Teilbloecke die Kernposition selbst.
        if (te instanceof UniversalMachinePartBlockEntity part) {
            BlockPos core = part.getControllerPos();
            if (core != null) return world.getBlockEntity(core);
        }
        return te;
    }
}
