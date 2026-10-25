package com.hbm_m.block.rail;

import com.hbm_m.util.ForgeDirection;
import com.hbm_m.util.Vec3NT;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/** 1:1 {@code RailStandardCurveBase} ({@code rail_large_curve}): Normalspur, 90-Grad-Kurve, Breite 4 (5 m). */
public class RailStandardCurveBase extends RailDummyableBlock implements IRailNTM {

    protected int width = 4;

    public RailStandardCurveBase(Properties properties) {
        super(properties);
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

        double turnRadius = width;
        double axisDist = width + 0.5D;

        Vec3NT vec = Vec3NT.createVectorHelper(trainX, trainY, trainZ);
        double axisX = cX + 0.5 + dir.offsetX * 0.5 + rot.offsetX * axisDist;
        double axisZ = cZ + 0.5 + dir.offsetZ * 0.5 + rot.offsetZ * axisDist;

        Vec3NT dist = Vec3NT.createVectorHelper(vec.xCoord - axisX, 0, vec.zCoord - axisZ);
        dist = dist.normalize();
        dist.xCoord *= turnRadius;
        dist.zCoord *= turnRadius;

        double moveAngle = Math.atan2(motionX, motionZ) * 180D / Math.PI + 90;

        if (speed == 0) {
            info.dist(0).pos(new BlockPos(x, y, z)).yaw((float) moveAngle);
            return Vec3NT.createVectorHelper(axisX + dist.xCoord, y, axisZ + dist.zCoord);
        }

        double angleDeg = Math.atan2(dist.xCoord, dist.zCoord) * 180D / Math.PI + 90;
        if (dir == ForgeDirection.WEST) angleDeg -= 90;
        if (dir == ForgeDirection.EAST) angleDeg += 90;
        if (dir == ForgeDirection.SOUTH) angleDeg += 180;
        angleDeg = Mth.wrapDegrees(angleDeg);
        double length90Deg = turnRadius * Math.PI / 2D;
        double angularChange = speed / length90Deg * 90D;

        ForgeDirection moveDir;

        if (Math.abs(motionX) > Math.abs(motionZ)) {
            moveDir = motionX > 0 ? ForgeDirection.EAST : ForgeDirection.WEST;
        } else {
            moveDir = motionZ > 0 ? ForgeDirection.SOUTH : ForgeDirection.NORTH;
        }

        if (moveDir == dir || moveDir == rot.getOpposite()) {
            angularChange *= -1;
        }

        double effAngle = angleDeg + angularChange;
        moveAngle += angularChange;

        if (effAngle > 90) {
            double angleOvershoot = effAngle - 90D;
            moveAngle -= angleOvershoot;
            double lengthOvershoot = angleOvershoot * length90Deg / 90D;
            info.dist(lengthOvershoot * Math.signum(speed * angularChange)).pos(new BlockPos(cX - dir.offsetX * width + rot.offsetX * (width + 1), y, cZ - dir.offsetZ * width + rot.offsetZ * (width + 1))).yaw((float) moveAngle);
            return Vec3NT.createVectorHelper(axisX - dir.offsetX * turnRadius, y + 0.1875, axisZ - dir.offsetZ * turnRadius);
        }

        if (effAngle < 0) {
            double angleOvershoot = -effAngle;
            moveAngle -= angleOvershoot;
            double lengthOvershoot = angleOvershoot * length90Deg / 90D;
            info.dist(-lengthOvershoot * Math.signum(speed * angularChange)).pos(new BlockPos(cX + dir.offsetX, y, cZ + dir.offsetZ)).yaw((float) moveAngle);
            return Vec3NT.createVectorHelper(axisX - rot.offsetX * turnRadius, y + 0.1875, axisZ - rot.offsetZ * turnRadius);
        }

        double radianChange = angularChange * Math.PI / 180D;
        dist.rotateAroundY((float) radianChange);

        return Vec3NT.createVectorHelper(axisX + dist.xCoord, y + 0.1875, axisZ + dist.zCoord);
    }

    @Override
    public TrackGauge getGauge(Level world, int x, int y, int z) {
        return TrackGauge.STANDARD;
    }

    @Override
    public int[] getDimensions() {
        return new int[] {0, 0, width, 0, width, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    protected boolean checkRequirement(Level world, int x, int y, int z, ForgeDirection dir, int o) {

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        dir = dir.getOpposite();

        int dX = dir.offsetX;
        int dZ = dir.offsetZ;
        int rX = rot.offsetX;
        int rZ = rot.offsetZ;

        return isReplaceable(world, x + dX, y, z + dZ) &&
                isReplaceable(world, x + rX, y, z + rZ) &&
                isReplaceable(world, x + dX + rX, y, z + dZ + rZ) &&
                isReplaceable(world, x + dX + rX * 2, y, z + dZ + rZ * 2) &&
                isReplaceable(world, x + dX * 2 + rX, y, z + dZ * 2 + rZ) &&
                isReplaceable(world, x + dX * 2 + rX * 2, y, z + dZ * 2 + rZ * 2) &&
                isReplaceable(world, x + dX * 3 + rX, y, z + dZ * 3 + rZ) &&
                isReplaceable(world, x + dX * 3 + rX * 2, y, z + dZ * 3 + rZ * 2) &&
                isReplaceable(world, x + dX * 2 + rX * 3, y, z + dZ * 2 + rZ * 3) &&
                isReplaceable(world, x + dX * 3 + rX * 3, y, z + dZ * 3 + rZ * 3) &&
                isReplaceable(world, x + dX * 4 + rX * 3, y, z + dZ * 4 + rZ * 3) &&
                isReplaceable(world, x + dX * 3 + rX * 4, y, z + dZ * 3 + rZ * 4) &&
                isReplaceable(world, x + dX * 4 + rX * 4, y, z + dZ * 4 + rZ * 4);
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {

        safeRem = true;

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        dir = dir.getOpposite();

        int dX = dir.offsetX;
        int dZ = dir.offsetZ;
        int rX = rot.offsetX;
        int rZ = rot.offsetZ;

        setBlock(world, x + dX, y, z + dZ, dir.ordinal());
        setBlock(world, x + rX, y, z + rZ, rot.ordinal());
        setBlock(world, x + dX + rX, y, z + dZ + rZ, rot.ordinal());
        setBlock(world, x + dX + rX * 2, y, z + dZ + rZ * 2, rot.ordinal());
        setBlock(world, x + dX * 2 + rX, y, z + dZ * 2 + rZ, dir.ordinal());
        setBlock(world, x + dX * 2 + rX * 2, y, z + dZ * 2 + rZ * 2, dir.ordinal());
        setBlock(world, x + dX * 3 + rX, y, z + dZ * 3 + rZ, dir.ordinal());
        setBlock(world, x + dX * 3 + rX * 2, y, z + dZ * 3 + rZ * 2, dir.ordinal());
        setBlock(world, x + dX * 2 + rX * 3, y, z + dZ * 2 + rZ * 3, rot.ordinal());
        setBlock(world, x + dX * 3 + rX * 3, y, z + dZ * 3 + rZ * 3, rot.ordinal());
        setBlock(world, x + dX * 4 + rX * 3, y, z + dZ * 4 + rZ * 3, dir.ordinal());
        setBlock(world, x + dX * 3 + rX * 4, y, z + dZ * 3 + rZ * 4, rot.ordinal());
        setBlock(world, x + dX * 4 + rX * 4, y, z + dZ * 4 + rZ * 4, rot.ordinal());

        safeRem = false;
    }
}
