package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineLPW2}: ohne Logik, nur die grosse Renderbox fuer den animierten {@code Lpw2Renderer}.
 */
public class MachineLpw2BlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity {

    public MachineLpw2BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LPW2_BE.get(), pos, state);
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 10, worldPosition.getY(), worldPosition.getZ() - 10,
                worldPosition.getX() + 11, worldPosition.getY() + 7, worldPosition.getZ() + 11);
        return bb;
    }
}
