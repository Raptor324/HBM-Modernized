package com.hbm_m.client.render.implementations;

import com.hbm_m.block.decorations.SoyuzRocketBlock;
import com.hbm_m.block.entity.decorations.SoyuzRocketBlockEntity;
import com.hbm_m.client.model.SoyuzRocketBakedModel;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.client.render.machine.MachineRenderers;

/**
 * Декоративная ракета Союз на фабрике {@link MachineRenderers}: единственная часть
 * ROCKET (~52 блока высотой, multi-material mesh).
 */
public final class SoyuzRocketRenderer {

    public static void register() {
        MachineRenderers.machine("soyuzrocket", ModBlockEntities.DECO_SOYUZ_ROCKET_BE.get(),
                SoyuzRocketBlockEntity.class)
            .part(SoyuzRocketBakedModel.ROCKET)
            .facing(be -> be.getBlockState().getValue(SoyuzRocketBlock.FACING))
            .register();
    }

    private SoyuzRocketRenderer() {}
}
