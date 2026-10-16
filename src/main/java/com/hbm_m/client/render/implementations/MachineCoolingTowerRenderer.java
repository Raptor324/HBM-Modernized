package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineTowerLargeBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCoolingTowerBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderers;

/**
 * Градирня на фабрике {@link MachineRenderers}: единственная статическая часть
 * "Cube_Cube.001", без анимации. Дистанция прорисовки — конфиг статики
 * (modelStaticRenderDistance); fade по дистанции применяется движком.
 */
public final class MachineCoolingTowerRenderer {

    public static void register() {
        MachineRenderers.machine("coolingtower", ModBlockEntities.COOLING_TOWER_BE.get(),
                MachineCoolingTowerBlockEntity.class)
            .part("Cube_Cube.001")
            .facing(be -> be.getBlockState().getValue(MachineTowerLargeBlock.FACING))
            .register();
    }

    private MachineCoolingTowerRenderer() {}
}
