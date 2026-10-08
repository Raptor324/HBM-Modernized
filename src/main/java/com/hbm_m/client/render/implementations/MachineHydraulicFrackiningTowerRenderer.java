package com.hbm_m.client.render.implementations;

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
            // w16d: Original RenderFrackingTower dreht fest um 180 Grad (weltfest, Ausleger nach Sueden wie die Zellen
            // bei z+2..3). Wurzeltransform -90 + Block-Grundwinkel 90 + legacy(SOUTH) 180 = 180.
            .facing(be -> net.minecraft.core.Direction.SOUTH)
            .register();
    }

    private MachineHydraulicFrackiningTowerRenderer() {}
}
