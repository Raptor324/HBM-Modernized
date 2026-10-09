package com.hbm_m.blockentity.decorations;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** 1:1 {@code BlockVendingMachine.TileEntityVendingMachine}: nur Renderbereich (1x2x1). */
public class VendingMachineBlockEntity extends BlockEntity implements com.hbm_m.api.render.RenderBoundsProvider {

    public VendingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VENDING_MACHINE.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return com.hbm_m.platform.BlockHooks.aabb(worldPosition, worldPosition.offset(1, 2, 1));
    }
}
