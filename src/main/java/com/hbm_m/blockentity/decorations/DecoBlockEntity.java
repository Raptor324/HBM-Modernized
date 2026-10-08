package com.hbm_m.blockentity.decorations;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** 1:1 {@code TileEntityDecoBlock}: traegt nur das Modell (Boxcar, Duchess Gambit). */
public class DecoBlockEntity extends BlockEntity implements com.hbm_m.api.render.RenderBoundsProvider {

    public DecoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECO_BLOCK.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(16);
    }
}
