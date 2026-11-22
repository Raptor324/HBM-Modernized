package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineTowerLargeBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCoolingTowerBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderers;

/**
 * Cooling tower on the {@link MachineRenderers} factory: a single static part
 * "Cube_Cube.001", no animation. Render distance comes from the static-render config
 * (modelStaticRenderDistance); distance fade is applied by the engine.
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
