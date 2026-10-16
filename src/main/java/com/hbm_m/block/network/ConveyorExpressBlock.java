package com.hbm_m.block.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;

/** Port of {@code com.hbm.blocks.network.BlockConveyorExpress} (1.7.10 Original) - 3x belt speed. */
public class ConveyorExpressBlock extends ConveyorBendableBlock {

    public ConveyorExpressBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        return super.getTravelLocation(level, pos, itemPos, speed * 3);
    }
}
