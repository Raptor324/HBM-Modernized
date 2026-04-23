package com.hbm_m.entity.missile;

import com.hbm_m.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Базовая сущность баллистической ракеты.
 *
 * Основана на старом EntityMissileBaseNT, но использует API 1.20.1:
 * - Управляет баллистикой (ускорение/замедление по дуге)
 * - Сохраняет стартовую и целевую координату
 * - Делегирует конкретный взрыв в подкласс (onMissileImpact)
 *
 * Chunkloading и радар пока опущены/станут заглушками.
 */
public abstract class MissileBaseEntity extends ThrowableItemProjectile {

    protected int startX;
    protected int startZ;
    protected int targetX;
    protected int targetZ;
    protected double velocity;
    protected double decelY;
    protected double accelXZ;
    protected boolean isCluster = false;
    protected int health = 50;
    protected boolean exploded = false;

    protected MissileBaseEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.startX = (int) this.getX();
        this.startZ = (int) this.getZ();
        this.targetX = this.startX;
        this.targetZ = this.startZ;
    }

    /**
     * Инициализация полёта при старте с пусковой площадки.
     */
    public void initLaunch(double x, double y, double z, int targetX, int targetZ) {
        this.setPos(x, y, z);
        this.startX = (int) Math.floor(x);
        this.startZ = (int) Math.floor(z);
        this.targetX = targetX;
        this.targetZ = targetZ;

        Vec3 vec = new Vec3(this.targetX - this.startX, 0, this.targetZ - this.startZ);
        double len = vec.length();
        if (len == 0) {
            len = 1.0D;
        }
        this.accelXZ = this.decelY = 1.0D / len;
        this.decelY *= 2.0D;
        this.velocity = 0.0D;

        this.setDeltaMovement(this.getDeltaMovement().x(), 2.0D, this.getDeltaMovement().z());

        float yaw = (float) (Math.atan2(this.targetX - this.getX(), this.targetZ - this.getZ()) * 180.0D / Math.PI);
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setXRot(0.0F);
        this.xRotO = 0.0F;
    }

    @Override
    protected Item getDefaultItem() {
        // Для прототипа возвращаем единственный тип ракеты – missile_test
        return ModItems.MISSILE_TEST.get();
    }

    @Override
    public void tick() {
        this.xOld = this.getX();
        this.yOld = this.getY();
        this.zOld = this.getZ();

        if (!this.level().isClientSide) {
            serverTickLogic();
        } else {
            spawnContrail();
        }

        super.tick();
        updateRotationFromMotion();
    }

    private void serverTickLogic() {
        if (this.velocity < 4.0D) {
            this.velocity += Mth.clamp((double) this.tickCount / 60.0D * 0.05D, 0.0D, 0.05D);
        }

        Vec3 motion = this.getDeltaMovement();

        if (hasPropulsion()) {
            double motionY = motion.y - this.decelY * this.velocity;

            Vec3 vector = new Vec3(this.targetX - this.startX, 0, this.targetZ - this.startZ).normalize();
            vector = new Vec3(vector.x * this.accelXZ, 0, vector.z * this.accelXZ);

            double motionX = motion.x;
            double motionZ = motion.z;

            if (motionY > 0) {
                motionX += vector.x * this.velocity;
                motionZ += vector.z * this.velocity;
            }

            if (motionY < 0) {
                motionX -= vector.x * this.velocity;
                motionZ -= vector.z * this.velocity;
            }

            motion = new Vec3(motionX, motionY, motionZ);
        } else {
            motion = motion.multiply(0.99D, 1.0D, 0.99D);
            if (motion.y > -1.5D) {
                motion = motion.add(0.0D, -0.05D, 0.0D);
            }
        }

        if (motion.y < -this.velocity && this.isCluster) {
            cluster();
            this.discard();
            return;
        }

        this.setDeltaMovement(motion);
    }

    protected boolean hasPropulsion() {
        return true;
    }

    protected void spawnContrail() {
        if (!(this.level() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel)) {
            return;
        }

        Vec3 motion = new Vec3(this.xOld - this.getX(), this.yOld - this.getY(), this.zOld - this.getZ());
        double len = motion.length();
        if (len <= 0.0D) {
            return;
        }
        motion = motion.normalize();

        for (int i = 0; i < Math.max(Math.min(len * 4.0D, 10.0D), 1.0D); i++) {
            double t = i / 4.0D;
            double px = this.getX() + motion.x * t;
            double py = this.getY() + motion.y * t;
            double pz = this.getZ() + motion.z * t;

            clientLevel.addParticle(
                    com.hbm_m.particle.ModParticleTypes.MISSILE_CONTRAIL.get(),
                    px, py, pz,
                    -motion.x * 0.1D,
                    -motion.y * 0.1D,
                    -motion.z * 0.1D);
        }
    }

    private void updateRotationFromMotion() {
        Vec3 motion = this.getDeltaMovement();
        double f2 = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float pitch = (float) (Math.atan2(motion.y, f2) * 180.0D / Math.PI) - 90.0F;
        float yaw = (float) (Math.atan2(this.targetX - this.getX(), this.targetZ - this.getZ()) * 180.0D / Math.PI);

        this.setXRot(pitch);
        this.setYRot(yaw);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (this.level().isClientSide || this.exploded) {
            return;
        }

        this.exploded = true;
        BlockPos pos;
        if (result.getType() == HitResult.Type.BLOCK) {
            pos = ((BlockHitResult) result).getBlockPos();
        } else {
            pos = this.blockPosition();
        }

        onMissileImpact(pos);
        this.discard();
    }

    protected abstract void onMissileImpact(BlockPos pos);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("StartX", this.startX);
        tag.putInt("StartZ", this.startZ);
        tag.putInt("TargetX", this.targetX);
        tag.putInt("TargetZ", this.targetZ);
        tag.putDouble("Velocity", this.velocity);
        tag.putDouble("DecelY", this.decelY);
        tag.putDouble("AccelXZ", this.accelXZ);
        tag.putInt("Health", this.health);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.startX = tag.getInt("StartX");
        this.startZ = tag.getInt("StartZ");
        this.targetX = tag.getInt("TargetX");
        this.targetZ = tag.getInt("TargetZ");
        this.velocity = tag.getDouble("Velocity");
        this.decelY = tag.getDouble("DecelY");
        this.accelXZ = tag.getDouble("AccelXZ");
        this.health = tag.getInt("Health");
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.isInvulnerableTo(source) && !this.level().isClientSide) {
            if (this.health > 0) {
                this.health -= (int) amount;
                if (this.health <= 0) {
                    killMissile();
                }
            }
            return true;
        }
        return false;
    }

    protected void killMissile() {
        if (!this.isRemoved()) {
            this.discard();
            // TODO: добавить некритичный взрыв/обломки при уничтожении ракеты в полёте
        }
    }

    protected void cluster() {
        // Заглушка для кластерных ракет
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this, MobCategory.MISC.ordinal());
    }
}

