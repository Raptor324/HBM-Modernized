package com.hbm_m.blockentity.network;

import com.hbm_m.api.energy.Nodespace;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.block.network.RedConnectorBlock;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Порт TileEntityConnector (1.7.10): коннектор ЛЭП, радиус 10 м.
 * FACING = сторона блока, которой он прижат к машине (направление машины).
 */
public class RedConnectorBlockEntity extends PylonBaseBlockEntity {

    public RedConnectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RED_CONNECTOR_BE.get(), pos, state);
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return side == getBlockState().getValue(RedConnectorBlock.FACING);
    }

    @Override
    public Vec3[] getMountPos() {
        return new Vec3[] {new Vec3(0.5, 0.5, 0.5)};
    }

    @Override
    public double getMaxWireLength() {
        return 10;
    }

    @Override
    protected void addExtraConnections(Nodespace.PowerNode node, BlockPos pos) {
        Direction dir = getBlockState().getValue(RedConnectorBlock.FACING);
        node.addConnection(new NodeDirPos(pos.getX() + dir.getStepX(), pos.getY() + dir.getStepY(), pos.getZ() + dir.getStepZ(), dir));
    }
}
