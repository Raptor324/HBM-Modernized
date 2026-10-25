package com.hbm_m.block.rail;

import com.hbm_m.util.ForgeDirection;
import com.hbm_m.util.Vec3NT;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/** 1:1 {@code RailStandardStraightShort} ({@code rail_large_straight_short}): Normalspur, 1 m gerade. */
public class RailStandardStraightShort extends RailDummyableBlock implements IRailNTM {

    public RailStandardStraightShort(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[] {0, 0, 0, 0, 1, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public Vec3NT getSnappingPos(Level world, int x, int y, int z, double trainX, double trainY, double trainZ) {
        return snapAndMove(world, x, y, z, trainX, trainY, trainZ, 0, 0, 0, 0, new RailContext());
    }

    @Override
    public Vec3NT getTravelLocation(Level world, int x, int y, int z, double trainX, double trainY, double trainZ, double motionX, double motionY, double motionZ, double speed, RailContext info, MoveContext context) {
        return snapAndMove(world, x, y, z, trainX, trainY, trainZ, motionX, motionY, motionZ, speed, info);
    }

    /* Einrastposition bestimmen und bei Bedarf die Bewegung aufaddieren. */
    public Vec3NT snapAndMove(Level world, int x, int y, int z, double trainX, double trainY, double trainZ, double motionX, double motionY, double motionZ, double speed, RailContext info) {
        int[] pos = this.findCore(world, x, y, z);
        if (pos == null) return Vec3NT.createVectorHelper(trainX, trainY, trainZ);
        int cX = pos[0];
        int cY = pos[1];
        int cZ = pos[2];
        int meta = getMeta(world, cX, cY, cZ) - offset;
        ForgeDirection dir = ForgeDirection.getOrientation(meta);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

        Vec3NT vec = Vec3NT.createVectorHelper(trainX, trainY, trainZ);

        if (dir == ForgeDirection.EAST || dir == ForgeDirection.WEST) {
            double targetX = trainX;
            if (motionX > 0) {
                targetX += speed;
                info.yaw(-90F);
            } else {
                targetX -= speed;
                info.yaw(90F);
            }
            vec.xCoord = Mth.clamp(targetX, cX, cX + 1);
            vec.yCoord = y + 0.1875;
            vec.zCoord = cZ + 0.5 + rot.offsetZ * 0.5;
            info.dist(Math.abs(targetX - vec.xCoord) * Math.signum(speed));
            info.pos(new BlockPos(cX + (motionX * speed > 0 ? 1 : -1), y, cZ));
        } else {
            double targetZ = trainZ;
            if (motionZ > 0) {
                targetZ += speed;
                info.yaw(0F);
            } else {
                targetZ -= speed;
                info.yaw(180F);
            }
            vec.xCoord = cX + 0.5 + rot.offsetX * 0.5;
            vec.yCoord = y + 0.1875;
            vec.zCoord = Mth.clamp(targetZ, cZ, cZ + 1);
            info.dist(Math.abs(targetZ - vec.zCoord) * Math.signum(speed));
            info.pos(new BlockPos(cX, y, cZ + (motionZ * speed > 0 ? 1 : -1)));
        }

        return vec;
    }

    @Override
    public TrackGauge getGauge(Level world, int x, int y, int z) {
        return TrackGauge.STANDARD;
    }
}
