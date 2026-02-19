package com.hbm_m.block.entity.custom.machines;

import com.hbm_m.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachineHydraulicFrackiningTowerBlockEntity extends BlockEntity {

    public MachineHydraulicFrackiningTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYDRAULIC_FRACKINING_TOWER_BE.get(), pos, state);
    }
}
