package com.hbm_m.block.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;

/** Port of {@code com.hbm.blocks.network.BlockConveyorTriple} (1.7.10 Original) - three parallel lanes. */
public class ConveyorTripleBlock extends ConveyorBendableBlock {

    public ConveyorTripleBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level level, BlockPos pos, Vec3 itemPos) {
        Direction dir = getTravelDirection(level, pos, itemPos);

        double clampedX = Math.max(pos.getX(), Math.min(pos.getX() + 1, itemPos.x));
        double clampedZ = Math.max(pos.getZ(), Math.min(pos.getZ() + 1, itemPos.z));

        double posX = pos.getX() + 0.5;
        double posZ = pos.getZ() + 0.5;

        if (dir.getStepX() != 0) {
            posX = clampedX;
            posZ += itemPos.z > posZ + 0.15 ? 0.3125 : (itemPos.z < posZ - 0.15 ? -0.3125 : 0);
        }
        if (dir.getStepZ() != 0) {
            posZ = clampedZ;
            posX += itemPos.x > posX + 0.15 ? 0.3125 : (itemPos.x < posX - 0.15 ? -0.3125 : 0);
        }

        return new Vec3(posX, pos.getY() + 0.25, posZ);
    }
}
