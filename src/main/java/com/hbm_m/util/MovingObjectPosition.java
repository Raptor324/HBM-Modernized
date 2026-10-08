package com.hbm_m.util;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Felder und Namen von {@code net.minecraft.util.MovingObjectPosition} (1.7.10) als Huelle um ein {@link HitResult}:
 * {@code typeOfHit}, {@code blockX/Y/Z}, {@code sideHit} (Forge-Zahl 0-5), {@code hitVec}, {@code entityHit}.
 * Damit lassen sich Treffer-Lambdas der Waffen- und Geschosskonfigurationen zeilengetreu portieren.
 */
public class MovingObjectPosition {

    public enum MovingObjectType { MISS, BLOCK, ENTITY }

    public MovingObjectType typeOfHit;
    public int blockX;
    public int blockY;
    public int blockZ;
    /** 0 unten, 1 oben, 2 Nord, 3 Sued, 4 West, 5 Ost; -1 ohne Blocktreffer */
    public int sideHit = -1;
    public Vec3NT hitVec;
    @Nullable
    public Entity entityHit;

    public MovingObjectPosition(int x, int y, int z, int side, Vec3NT hitVec) {
        this.typeOfHit = MovingObjectType.BLOCK;
        this.blockX = x;
        this.blockY = y;
        this.blockZ = z;
        this.sideHit = side;
        this.hitVec = new Vec3NT(hitVec.xCoord, hitVec.yCoord, hitVec.zCoord);
    }

    public MovingObjectPosition(Entity entity) {
        this(entity, new Vec3NT(entity.getX(), entity.getY(), entity.getZ()));
    }

    public MovingObjectPosition(Entity entity, Vec3NT hitVec) {
        this.typeOfHit = MovingObjectType.ENTITY;
        this.entityHit = entity;
        this.hitVec = hitVec;
    }

    private MovingObjectPosition() { }

    @Nullable
    public static MovingObjectPosition of(@Nullable HitResult hit) {
        if (hit == null || hit.getType() == HitResult.Type.MISS) return null;
        if (hit instanceof BlockHitResult bhr) {
            BlockPos p = bhr.getBlockPos();
            return new MovingObjectPosition(p.getX(), p.getY(), p.getZ(), bhr.getDirection().get3DDataValue(), new Vec3NT(bhr.getLocation()));
        }
        if (hit instanceof EntityHitResult ehr) {
            return new MovingObjectPosition(ehr.getEntity(), new Vec3NT(ehr.getLocation()));
        }
        return null;
    }

    public BlockPos getBlockPos() {
        return new BlockPos(blockX, blockY, blockZ);
    }

    public Vec3 hitVec3() {
        return hitVec.toVec3();
    }
}
