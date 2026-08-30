package com.hbm_m.blockentity.network;

import com.hbm_m.api.energy.Nodespace;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Порт TileEntityPylon (1.7.10): пилон ЛЭП, радиус 25 м, башня 5 блоков.
 * Узел дополнительно связан с четырьмя горизонтальными соседями.
 */
public class RedPylonBlockEntity extends PylonBaseBlockEntity {

    public RedPylonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RED_PYLON_BE.get(), pos, state);
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return side != Direction.DOWN;
    }

    @Override
    public Vec3[] getMountPos() {
        return new Vec3[] {new Vec3(0.5, 5.5, 0.5)};
    }

    @Override
    public double getMaxWireLength() {
        return 25;
    }

    @Override
    protected void addExtraConnections(Nodespace.PowerNode node, BlockPos pos) {
        node.addConnection(new NodeDirPos(pos.getX() + 1, pos.getY(), pos.getZ(), Direction.EAST));
        node.addConnection(new NodeDirPos(pos.getX() - 1, pos.getY(), pos.getZ(), Direction.WEST));
        node.addConnection(new NodeDirPos(pos.getX(), pos.getY(), pos.getZ() + 1, Direction.SOUTH));
        node.addConnection(new NodeDirPos(pos.getX(), pos.getY(), pos.getZ() - 1, Direction.NORTH));
    }
}
