package com.hbm_m.blockentity.machines;

import com.hbm_m.api.energy.ConverterBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityConverterRfHe} (1.7.10 Original) - a fixed RF-to-HE (import-only)
 * energy bridge. See {@link MachineConverterHeRfBlockEntity} for the shared reuse/simplification
 * rationale.
 */
public class MachineConverterRfHeBlockEntity extends ConverterBlockEntity {

    public static final int MODE_IMPORT = 2;

    public MachineConverterRfHeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CONVERTER_RF_HE_BE.get(), pos, state, MODE_IMPORT);
    }
}
