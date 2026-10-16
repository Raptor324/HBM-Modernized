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

    /** Малая форма — порт TileEntityFoundryMold.getMoldSize (только слитки-формы размера 0). */
    @Override
    public int getMoldSize() { return 0; }

    // Рендер-геометрия 1:1 с TileEntityFoundryMold (IRenderFoundry): мелкая чаша.
    @Override public float getSurfaceRange() { return 0.25f; }
    @Override public float getOutputHeight() { return 0.25f; }
}
