package com.hbm_m.entity.projectile;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Суббоеприпас кластерной боеголовки (аналог {@code EntityBulletBaseMK4} + {@code cluster_submunition}).
 */
public class ClusterRocketEntity extends Projectile {

    private static final double GRAVITY = 0.025D;
    private static final float EXPLOSION_POWER = 7.5F;
    private static final int MAX_LIFE = 1_200;

    public ClusterRocketEntity(EntityType<? extends ClusterRocketEntity> type, Level level) {
        super(type, level);
    }

    //? if < 1.21.1 {

    @Override
    protected void defineSynchedData() {
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {

    
    }
    *///?}

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        syncInterpolationState();
    }

    /** Сбрасывает интерполяцию клиента — иначе от суббоеприпаса тянутся «хвосты» к (0,0,0) или к соседним. */
    private void syncInterpolationState() {
        this.xOld = this.getX();
        this.yOld = this.getY();
        this.zOld = this.getZ();
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        this.setPos(x, y, z);
        this.setYRot(yRot);
        this.setXRot(xRot);
        syncInterpolationState();
    }

    @Override
    public void tick() {
        this.xOld = this.getX();
        this.yOld = this.getY();
        this.zOld = this.getZ();
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();

        if (this.level().isClientSide) {
            clientTickMotion();
            return;
        }

        this.baseTick();

        if (this.tickCount > MAX_LIFE) {
            this.discard();
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x, motion.y - GRAVITY, motion.z);

        updateRotationFromMotion();

        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            this.onHit(hit);
            return;
        }

        motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
    }

    /** Клиентская симуляция по {@code getDeltaMovement()} из пакета спавна (как {@code EntityThrowableInterp}). */
    private void clientTickMotion() {
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() <= 1.0E-12D) {
            return;
        }
        this.setDeltaMovement(motion.x, motion.y - GRAVITY, motion.z);
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
        updateRotationFromMotion();
    }

    private void updateRotationFromMotion() {
        Vec3 motion = this.getDeltaMovement();
        double hyp = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (hyp <= 1.0E-6D && Math.abs(motion.y) <= 1.0E-6D) {
            return;
        }
        float yaw = (float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(motion.y, hyp) * 180.0D / Math.PI);
        this.setYRot(yaw);
        this.setXRot(pitch);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide && result.getType() == HitResult.Type.BLOCK) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                    EXPLOSION_POWER, Level.ExplosionInteraction.BLOCK);
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity entity) {
        return false;
    }
}
