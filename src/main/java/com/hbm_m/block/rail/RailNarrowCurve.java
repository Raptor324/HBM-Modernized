package com.hbm_m.block.rail;

import com.hbm_m.util.ForgeDirection;
import com.hbm_m.util.Vec3NT;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/** 1:1 {@code RailNarrowCurve} ({@code rail_narrow_curve}): Schmalspur-Kurve, Radius 4,5 (5x5-Flaeche). */
public class RailNarrowCurve extends RailDummyableBlock implements IRailNTM {

    public RailNarrowCurve(Properties properties) {
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

        double turnRadius = 4.5D;

        Vec3NT vec = Vec3NT.createVectorHelper(trainX, trainY, trainZ);
        double axisX = cX + 0.5 + dir.offsetX * 0.5 + rot.offsetX * turnRadius;
        double axisZ = cZ + 0.5 + dir.offsetZ * 0.5 + rot.offsetZ * turnRadius;

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
            info.dist(lengthOvershoot * Math.signum(speed * angularChange)).pos(new BlockPos(cX - dir.offsetX * 4 + rot.offsetX * 5, y, cZ - dir.offsetZ * 4 + rot.offsetZ * 5)).yaw((float) moveAngle);
            return Vec3NT.createVectorHelper(axisX - dir.offsetX * turnRadius, y, axisZ - dir.offsetZ * turnRadius);
        }

        if (effAngle < 0) {
            double angleOvershoot = -effAngle;
            moveAngle -= angleOvershoot;
            double lengthOvershoot = angleOvershoot * length90Deg / 90D;
            info.dist(-lengthOvershoot * Math.signum(speed * angularChange)).pos(new BlockPos(cX + dir.offsetX, y, cZ + dir.offsetZ)).yaw((float) moveAngle);
            return Vec3NT.createVectorHelper(axisX - rot.offsetX * turnRadius, y, axisZ - rot.offsetZ * turnRadius);
        }

        double radianChange = angularChange * Math.PI / 180D;
        dist.rotateAroundY((float) radianChange);

        return Vec3NT.createVectorHelper(axisX + dist.xCoord, y, axisZ + dist.zCoord);
    }

    @Override
    public TrackGauge getGauge(Level world, int x, int y, int z) {
        return TrackGauge.NARROW;
    }

    @Override
    public int[] getDimensions() {
        return new int[] {0, 0, 4, 0, 4, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }
}
