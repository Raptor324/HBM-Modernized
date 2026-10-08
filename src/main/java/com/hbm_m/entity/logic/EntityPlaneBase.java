package com.hbm_m.entity.logic;

import java.util.Comparator;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.particle.helper.ExplosionSmallCreator;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.ParticleUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityPlaneBase}: geradeaus fliegendes Flugzeug mit 50 Lebenspunkten und 200 Ticks Lebensdauer. Wird es
 * abgeschossen, stuerzt es brennend ab und explodiert beim Aufschlag (Staerke 15). Haelt den eigenen Chunk geladen
 * (Original: ForgeChunkManager-Ticket, hier ein mitwanderndes Regionsticket).
 */
public abstract class EntityPlaneBase extends Entity {

    private static final TicketType<UUID> CHUNK_TICKET = TicketType.create("hbm_m_plane", Comparator.comparing(UUID::toString));

    protected static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(EntityPlaneBase.class, EntityDataSerializers.FLOAT);

    protected int turnProgress;
    protected double syncPosX;
    protected double syncPosY;
    protected double syncPosZ;
    protected double syncYaw;
    protected double syncPitch;

    private ChunkPos loadedChunk;

    public float health = getMaxHealth();
    public int timer = getLifetime();

    public EntityPlaneBase(EntityType<? extends EntityPlaneBase> type, Level world) {
        super(type, world);
        this.noPhysics = true;
    }

    public float getMaxHealth() { return 50F; }
    public int getLifetime() { return 200; }

    @Override public boolean isPickable() { return this.health > 0; }
    @Override public boolean isNoGravity() { return true; }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (source.is(ModDamageTypes.NUCLEAR_BLAST)) return false;
        if (this.isInvulnerableTo(source)) return false;
        if (!this.isRemoved() && !this.level().isClientSide && this.health > 0) {
            health -= amount;
            if (this.health <= 0) this.killPlane();
        }
        return true;
    }

    protected void killPlane() {
        if (level() instanceof ServerLevel server) ExplosionSmallCreator.composeEffect(server, getX(), getY(), getZ(), 25, 3.5F, 2F);
        level().playSound(null, getX(), getY(), getZ(), HbmSoundsNT.get("hbm:entity.planeShotDown"), SoundSource.NEUTRAL, 25.0F, 1.0F);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(HEALTH, 50F);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HEALTH, 50F);
    }
    *///?}

    public float getSyncedHealth() { return this.entityData.get(HEALTH); }

    @Override
    public void tick() {

        if (!level().isClientSide) {
            this.entityData.set(HEALTH, health);
        } else {
            health = this.entityData.get(HEALTH);
        }

        if (level().isClientSide) {

            if (this.turnProgress > 0) {
                double interpX = this.getX() + (this.syncPosX - this.getX()) / (double) this.turnProgress;
                double interpY = this.getY() + (this.syncPosY - this.getY()) / (double) this.turnProgress;
                double interpZ = this.getZ() + (this.syncPosZ - this.getZ()) / (double) this.turnProgress;
                double d = Mth.wrapDegrees(this.syncYaw - (double) this.getYRot());
                this.setYRot((float) ((double) this.getYRot() + d / (double) this.turnProgress));
                this.setXRot((float) ((double) this.getXRot() + (this.syncPitch - (double) this.getXRot()) / (double) this.turnProgress));
                --this.turnProgress;
                this.setPos(interpX, interpY, interpZ);
            } else {
                this.setPos(this.getX(), this.getY(), this.getZ());
            }

        } else {
            Vec3 m = getDeltaMovement();
            this.setPos(getX() + m.x, getY() + m.y, getZ() + m.z);

            this.rotation();

            if (this.health <= 0) {
                setDeltaMovement(m.x, m.y - 0.025, m.z);
                m = getDeltaMovement();

                for (int i = 0; i < 10; i++) ParticleUtil.spawnGasFlame(level(), getX() + random.nextGaussian() * 0.5 - m.x * 2, getY() + random.nextGaussian() * 0.5 - m.y * 2, getZ() + random.nextGaussian() * 0.5 - m.z * 2, 0.0, 0.1, 0.0);

                if (!level().getBlockState(blockPosition()).isAir() || getY() < level().getMinBuildHeight()) {
                    this.discard();
                    new ExplosionVNT(level(), getX(), getY(), getZ(), 15F).makeStandard().explode();
                    level().playSound(null, getX(), getY(), getZ(), HbmSoundsNT.get("hbm:entity.planeCrash"), SoundSource.NEUTRAL, 25.0F, 1.0F);
                    return;
                }
            } else {
                setDeltaMovement(m.x, 0, m.z);
            }

            if (this.tickCount > timer) this.discard();
            loadNeighboringChunks(Mth.floor(getX() / 16D), Mth.floor(getZ() / 16D));
        }
    }

    protected void rotation() {
        Vec3 m = getDeltaMovement();
        float motionHorizontal = (float) Math.sqrt(m.x * m.x + m.z * m.z);
        this.setYRot((float) (Math.atan2(m.x, m.z) * 180.0D / Math.PI));
        this.setXRot((float) (Math.atan2(m.y, motionHorizontal) * 180.0D / Math.PI) - 90);
        while (this.getXRot() - this.xRotO < -180.0F) this.xRotO -= 360.0F;
        while (this.getXRot() - this.xRotO >= 180.0F) this.xRotO += 360.0F;
        while (this.getYRot() - this.yRotO < -180.0F) this.yRotO -= 360.0F;
        while (this.getYRot() - this.yRotO >= 180.0F) this.yRotO += 360.0F;
    }

    /** Original {@code setPositionAndRotation2}: Serverpositionen werden ueber {@code steps} Ticks angenaehert. */
    @Override
    //? if < 1.21.1 {
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
    //?} else {
    /*public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
    *///?}
        this.syncPosX = x;
        this.syncPosY = y;
        this.syncPosZ = z;
        this.syncYaw = yaw;
        this.syncPitch = pitch;
        this.turnProgress = steps;
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);
        this.clearChunkLoader();
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        tickCount = nbt.getInt("ticksExisted");
        this.health = nbt.getFloat("health");
        this.entityData.set(HEALTH, this.health);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        nbt.putInt("ticksExisted", tickCount);
        nbt.putFloat("health", this.entityData.get(HEALTH));
    }

    public void clearChunkLoader() {
        if (loadedChunk != null && level() instanceof ServerLevel server) {
            server.getChunkSource().removeRegionTicket(CHUNK_TICKET, loadedChunk, 2, getUUID());
            loadedChunk = null;
        }
    }

    public void loadNeighboringChunks(int newChunkX, int newChunkZ) {
        if (level() instanceof ServerLevel server) {
            ChunkPos pos = new ChunkPos(newChunkX, newChunkZ);
            if (pos.equals(loadedChunk)) return;
            clearChunkLoader();
            server.getChunkSource().addRegionTicket(CHUNK_TICKET, pos, 2, getUUID());
            loadedChunk = pos;
        }
    }

    @Override public boolean shouldRenderAtSqrDistance(double distance) { return true; }
}
