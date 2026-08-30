package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineFrackingTowerBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineFrackingTowerBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderers;

/**
 * Фракинг-башня на фабрике {@link MachineRenderers}: единственная статическая часть
 * "Cube_Cube.001", без анимации. Дистанция прорисовки — конфиг статики.
 */
public final class MachineHydraulicFrackiningTowerRenderer {

    public static void register() {
        MachineRenderers.machine("frackingtower", ModBlockEntities.HYDRAULIC_FRACKINING_TOWER_BE.get(),
                MachineFrackingTowerBlockEntity.class)
            .part("Cube_Cube.001")
            .facing(be -> be.getBlockState().getValue(MachineFrackingTowerBlock.FACING))
            .register();
    }

    private MachineHydraulicFrackiningTowerRenderer() {}
}
