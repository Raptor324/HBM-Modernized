package com.hbm_m.block.rail;

import com.hbm_m.util.ForgeDirection;
import com.hbm_m.util.Vec3NT;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/** 1:1 {@code RailStandardBuffer} ({@code rail_large_buffer}): Normalspur, 5 m mit Prellbock. */
public class RailStandardBuffer extends RailDummyableBlock implements IRailNTM {

    public RailStandardBuffer(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[] {0, 0, 2, 2, 1, 0};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    public Vec3NT getSnappingPos(Level world, int x, int y, int z, double trainX, double trainY, double trainZ) {
        return snapAndMove(world, x, y, z, trainX, trainY, trainZ, 0, 0, 0, 0, new RailContext(), new MoveContext(RailCheckType.OTHER, 0));
    }

    @Override
    public Vec3NT getTravelLocation(Level world, int x, int y, int z, double trainX, double trainY, double trainZ, double motionX, double motionY, double motionZ, double speed, RailContext info, MoveContext context) {
        return snapAndMove(world, x, y, z, trainX, trainY, trainZ, motionX, motionY, motionZ, speed, info, context);
    }

    /* Einrastposition bestimmen und bei Bedarf die Bewegung aufaddieren. */
    public Vec3NT snapAndMove(Level world, int x, int y, int z, double trainX, double trainY, double trainZ, double motionX, double motionY, double motionZ, double speed, RailContext info, MoveContext context) {
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
            vec.xCoord = Mth.clamp(targetX, cX - 2, cX + 3);
            vec.yCoord = y + 0.1875;
            vec.zCoord = cZ + 0.5 + rot.offsetZ * 0.5;

            double nX = (dir == ForgeDirection.EAST ? -1 - context.collisionBogieDistance : 2);
            double pX = (dir == ForgeDirection.WEST ? 0 - context.collisionBogieDistance : 3);
            double buffer = Mth.clamp(targetX, cX - nX, cX + pX);

            if (buffer != vec.xCoord) {
                context.collision = true;
                context.overshoot = Math.abs(buffer - vec.xCoord);
                vec.xCoord = buffer;
                return vec;
            }

            info.dist(Math.abs(targetX - vec.xCoord) * Math.signum(speed));
            info.pos(new BlockPos(cX + (motionX * speed > 0 ? 3 : -3), y, cZ));
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
            vec.zCoord = Mth.clamp(targetZ, cZ - 2, cZ + 3);

            double nZ = (dir == ForgeDirection.SOUTH ? -1 - context.collisionBogieDistance : 2);
            double pZ = (dir == ForgeDirection.NORTH ? 0 - context.collisionBogieDistance : 3);
            double buffer = Mth.clamp(targetZ, cZ - nZ, cZ + pZ);

            // Original vergleicht hier mit xCoord (Tippfehler im Original, 1:1 uebernommen)
            if (buffer != vec.xCoord) {
                context.collision = true;
                context.overshoot = Math.abs(buffer - vec.zCoord);
                vec.zCoord = buffer;
                return vec;
            }

            info.dist(Math.abs(targetZ - vec.zCoord) * Math.signum(speed));
            info.pos(new BlockPos(cX, y, cZ + (motionZ * speed > 0 ? 3 : -3)));
        }

        return vec;
    }

    @Override
    public TrackGauge getGauge(Level world, int x, int y, int z) {
        return TrackGauge.STANDARD;
    }
}
