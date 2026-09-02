package com.hbm_m.blockentity.network;

import com.hbm_m.block.network.RedConnectorBlock;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Порт TileEntityConnectorSuper (1.7.10): дальний коннектор ЛЭП, радиус 100 м.
 * Точка крепления смещена к стороне машины.
 */
public class RedConnectorSuperBlockEntity extends PylonBaseBlockEntity {

    public RedConnectorSuperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RED_CONNECTOR_SUPER_BE.get(), pos, state);
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return side == getBlockState().getValue(RedConnectorBlock.FACING);
    }

    @Override
    public Vec3[] getMountPos() {
        Direction dir = getBlockState().getValue(RedConnectorBlock.FACING);
        return new Vec3[] {new Vec3(0.5 + dir.getStepX() * 0.375, 0.5 + dir.getStepY() * 0.375, 0.5 + dir.getStepZ() * 0.375)};
    }

    @Override
    public double getMaxWireLength() {
        return 100;
    }
}
