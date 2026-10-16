package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of the 1.7.10 TileEntityFoundryMold (extends TileEntityFoundryCastingBase - identical
 * casting behaviour to the basin, only the block's collision shape/rendering differs). Reuses
 * {@link MachineFoundryBasinBlockEntity}'s logic wholesale via its protected constructor.
 */
public class MachineFoundryMoldBlockEntity extends MachineFoundryBasinBlockEntity {

    public MachineFoundryMoldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_MOLD_BE.get(), pos, state);
    }
}
