package com.hbm_m.block.rail;

import com.hbm_m.util.ForgeDirection;
import com.hbm_m.util.Vec3NT;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** 1:1 {@code RailStandardRamp} ({@code rail_large_ramp}): Normalspur, 5 m Rampe um einen Block nach oben. */
public class RailStandardRamp extends RailDummyableBlock implements IRailNTM {

    public RailStandardRamp(Properties properties) {
        super(properties);
        this.bounding.add(new AABB(-2.5, 0.0, -1.5, -1.5, 0.1, 0.5));
        this.bounding.add(new AABB(-1.5, 0.0, -1.5, -0.5, 0.3, 0.5));
        this.bounding.add(new AABB(-0.5, 0.0, -1.5, 0.5, 0.5, 0.5));
        this.bounding.add(new AABB(0.5, 0.0, -1.5, 1.5, 0.7, 0.5));
        this.bounding.add(new AABB(1.5, 0.0, -1.5, 2.5, 0.9, 0.5));
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
            double dist = (cX + 0.5 - targetX + 2.5) / 5;
            vec.xCoord = Mth.clamp(targetX, cX - 2, cX + 3);
            vec.yCoord = Mth.clamp(dir == ForgeDirection.EAST ? cY + dist : cY + 1 - dist, cY, cY + 1) + 0.1875;
            vec.zCoord = cZ + 0.5 + rot.offsetZ * 0.5;
            info.dist(Math.abs(targetX - vec.xCoord) * Math.signum(speed));
            info.pos(new BlockPos(cX + (motionX * speed > 0 ? 3 : -3), cY + (motionX * speed > 0 ^ dir == ForgeDirection.EAST ? 1 : 0), cZ));
        } else {
            double targetZ = trainZ;
            if (motionZ > 0) {
                targetZ += speed;
                info.yaw(0F);
            } else {
                targetZ -= speed;
                info.yaw(180F);
            }
            double dist = (cZ + 0.5 - targetZ + 2.5) / 5;
            vec.xCoord = cX + 0.5 + rot.offsetX * 0.5;
            vec.yCoord = Mth.clamp(dir == ForgeDirection.SOUTH ? cY + dist : cY + 1 - dist, cY, cY + 1) + 0.1875;
            vec.zCoord = Mth.clamp(targetZ, cZ - 2, cZ + 3);
            info.dist(Math.abs(targetZ - vec.zCoord) * Math.signum(speed));
            info.pos(new BlockPos(cX, cY + (motionZ * speed > 0 ^ dir == ForgeDirection.SOUTH ? 1 : 0), cZ + (motionZ * speed > 0 ? 3 : -3)));
        }

        return vec;
    }

    @Override
    public TrackGauge getGauge(Level world, int x, int y, int z) {
        return TrackGauge.STANDARD;
    }

    @Override
    protected boolean checkRequirement(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        return checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, getDimensions(), x, y, z, dir) &&
                checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[] {1, -1, 2, 2, 1, 0}, x, y, z, dir);
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, getDimensions(), this, dir);
        fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[] {1, -1, 2, 2, 1, 0}, this, dir);
    }
}
