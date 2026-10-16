package com.hbm_m.blockentity.network;

import com.hbm_m.api.energy.Nodespace;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.block.network.RedPylonMediumBlock;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Порт TileEntityPylonMedium (1.7.10): средний пилон, TRIPLE, радиус 45 м, башня 7 блоков.
 * Три крепления уходят лесенкой по FACING; с трансформером узел связан с блоком позади.
 */
public class RedPylonMediumBlockEntity extends PylonBaseBlockEntity {

    public RedPylonMediumBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RED_PYLON_MEDIUM_BE.get(), pos, state);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.TRIPLE;
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return hasTransformer() && side == getBlockState().getValue(RedPylonMediumBlock.FACING).getOpposite();
    }

    @Override
    public Vec3[] getMountPos() {
        Direction dir = getBlockState().getValue(RedPylonMediumBlock.FACING);
        double h = 7.5;
        return new Vec3[] {
                new Vec3(0.5, h, 0.5),
                new Vec3(0.5 + dir.getStepX(), h, 0.5 + dir.getStepZ()),
                new Vec3(0.5 + dir.getStepX() * 2, h, 0.5 + dir.getStepZ() * 2)
        };
    }

    @Override
    public double getMaxWireLength() {
        return 45;
    }

    public boolean hasTransformer() {
        return getBlockState().getBlock() instanceof RedPylonMediumBlock
                && ((RedPylonMediumBlock) getBlockState().getBlock()).isTransformer();
    }

    @Override
    protected void addExtraConnections(Nodespace.PowerNode node, BlockPos pos) {
        if (hasTransformer()) {
            Direction dir = getBlockState().getValue(RedPylonMediumBlock.FACING).getOpposite();
            node.addConnection(new NodeDirPos(pos.getX() + dir.getStepX(), pos.getY(), pos.getZ() + dir.getStepZ(), dir));
        }
    }
}
