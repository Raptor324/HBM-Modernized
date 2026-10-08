package com.hbm_m.entity.projectile;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.logic.EntityExplosionChunkloading;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;

/**
 * Gerald/Horizons meteor. Port of legacy {@code com.hbm.entity.projectile.EntityTom}: falls at
 * a fixed speed from orbit height, chimes periodically, and on hitting solid ground (or falling
 * below Y=10) detonates via {@link com.hbm_m.entity.logic.TomBlastEntity} (the authentic
 * tektite-ring/lava crater) plus the {@code EntityCloudTom} fire wall (500 ticks).
 */
public class TomEntity extends EntityExplosionChunkloading {

    private static final double DESCENT_SPEED = 0.5D;
    private static final int DESTRUCTION_RANGE = 600;

    public TomEntity(EntityType<? extends TomEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            updateChunkTicket();
        }

        setDeltaMovement(0.0D, -DESCENT_SPEED, 0.0D);
        move(MoverType.SELF, getDeltaMovement());

        if (tickCount % 100 == 0 && level() instanceof ServerLevel server) {
            server.playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOYUZ_CHIME.get(), SoundSource.HOSTILE, 10000.0F, 1.0F);
        }

        if (level().isClientSide) {
            return;
        }

        // Original: Block an (int)-Position nicht Luft oder unter Y 10
        boolean grounded = !level().getBlockState(new BlockPos((int) getX(), (int) getY(), (int) getZ())).isAir()
                || getY() < 10;
        if (grounded) {
            detonate();
        }
    }

    private void detonate() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        clearChunkTicket();

        com.hbm_m.entity.logic.TomBlastEntity blast =
                com.hbm_m.entity.logic.TomBlastEntity.create(server, getX(), getY(), getZ(), DESTRUCTION_RANGE);
        server.addFreshEntity(blast);

        com.hbm_m.entity.effect.EntityCloudTom cloud = new com.hbm_m.entity.effect.EntityCloudTom(com.hbm_m.entity.ModEntities.CLOUD_TOM.get(), server, 500);
        cloud.moveTo(getX(), getY(), getZ(), 0, 0);
        server.addFreshEntity(cloud);

        this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        clearChunkTicket();
        super.remove(reason);
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
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }


    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500_000.0D * 500_000.0D;
    }
}
