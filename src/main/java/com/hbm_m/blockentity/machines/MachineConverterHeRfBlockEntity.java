package com.hbm_m.blockentity.machines;

import com.hbm_m.api.energy.ConverterBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityConverterHeRf} (1.7.10 Original) - a fixed HE-to-RF (export-only)
 * energy bridge. Reuses {@link ConverterBlockEntity}'s HE/FE bridging logic wholesale, locked to
 * export mode (no screwdriver mode-cycling, matching the original's fixed-direction device).
 * <p>
 * SCOPE-Vereinfachung: Das Original nutzt ein festes 5:1-HE:RF-Verhaeltnis. Der Port verwendet
 * durchgehend ein 1:1-Verhaeltnis (wie beim bereits vorhandenen generischen {@code converter_block}),
 * da dieser Port HE- und FE-Einheiten bereits ueberall 1:1 gleichsetzt.
 */
public class MachineConverterHeRfBlockEntity extends ConverterBlockEntity {

    public static final int MODE_EXPORT = 1;

    public MachineConverterHeRfBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CONVERTER_HE_RF_BE.get(), pos, state, MODE_EXPORT);
    }
}
