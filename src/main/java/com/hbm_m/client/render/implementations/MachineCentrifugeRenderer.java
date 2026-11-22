package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineGasCentBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderers;

/**
 * Modern Nucleus machine renderer for the Gas Centrifuge:
 * - High-speed internal gas centrifuge rotor / drum spinning around the vertical Y axis
 * - Declarative GPU rotation evaluated via AnimParams attribute 15
 * - Static frame and connector flags
 */
public final class MachineCentrifugeRenderer {

    public static void register() {
        MachineRenderers.machine("gascent", ModBlockEntities.MACHINE_GASCENT_BE.get(),
                MachineGasCentBlockEntity.class)
            .part("Flag")
            .rotateY("Centrifuge", 0.0f, 0.0f, (be, pt) -> Math.toDegrees(be.getAnim(pt)))
            .blockTransform((be, animator) -> {
                animator.translate(0.5, 0.0, 0.5);
                switch (ObjBerHelper.facing(be)) {
                    case NORTH -> animator.rotate(270, 0, 1, 0);
                    case SOUTH -> animator.rotate(90, 0, 1, 0);
                    case WEST -> {}
                    case EAST -> animator.rotate(180, 0, 1, 0);
                    default -> animator.rotate(270, 0, 1, 0);
                }
            })
            .itemParts("Centrifuge", "Flag")
            .register();
    }

    private MachineCentrifugeRenderer() {}
}
