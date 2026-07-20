package com.hbm_m.block.entity.decorations;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Purely decorative - no game logic. Rendering is entirely handled by
 * {@link com.hbm_m.client.render.implementations.SoyuzRocketRenderer};
 * see {@link com.hbm_m.client.model.SoyuzRocketBakedModel} for why a static
 * baked block model can't be used (the rocket is ~52 blocks tall, far past
 * the 16-bit chunk-mesh vertex range).
 */
public class SoyuzRocketBlockEntity extends BlockEntity {

    public SoyuzRocketBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECO_SOYUZ_ROCKET_BE.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockPos p = this.getBlockPos();
        return new AABB(p.getX() - 6, p.getY(), p.getZ() - 6,
                         p.getX() + 7, p.getY() + 54, p.getZ() + 7);
    }
}
