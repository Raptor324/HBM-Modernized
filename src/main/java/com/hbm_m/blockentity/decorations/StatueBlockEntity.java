package com.hbm_m.blockentity.decorations;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityDecoBlockAltF}: reiner Traeger fuer den Statuen-Renderer ({@code RenderDecoBlockAlt}),
 * ohne eigene Logik. Das Original kennt dazu noch die Varianten ohne Zubehoer, mit Uhr (W) und mit Gewehr (G);
 * registriert ist aber nur {@code statue_elb_f} mit Uhr und Gewehr.
 */
public class StatueBlockEntity extends BlockEntity {

    public StatueBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STATUE_ELB_F.get(), pos, state);
    }
}
