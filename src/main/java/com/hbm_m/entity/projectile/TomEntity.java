package com.hbm_m.entity.projectile;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.logic.EntityExplosionChunkloading;
import com.hbm_m.particle.explosions.nuclear.medium.MediumNuclearMushroomCloud;
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
 * tektite-ring/lava crater) plus a mushroom cloud.
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
            spawnGlowBall();
            return;
        }

        boolean grounded = this.onGround()
                || !level().getBlockState(BlockPos.containing(getX(), getY() - 0.1, getZ())).isAir()
                || getY() < 10;
        if (grounded) {
            detonate();
        }
    }

    /**
     * Port of legacy {@code TomPronter.prontTom()}'s glow: instead of one 3D flame model
     * spun 20x around the bomb casing with additive blending, spawn a ring of blue glow
     * particles rotating around the meteor each tick - same "swirling ball" silhouette,
     * particle-based instead of a custom animated model.
     */
    private void spawnGlowBall() {
        final int count = 10;
        double baseAngle = (tickCount * 6.0D) % 360.0D;
        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(baseAngle + i * (360.0D / count));
            double radius = 0.5D + (i % 3) * 0.15D;
            double ox = Math.cos(angle) * radius;
            double oz = Math.sin(angle) * radius;
            double oy = (random.nextDouble() - 0.5D) * radius;
            level().addParticle(com.hbm_m.particle.ModParticleTypes.TOM_GLOW.get(),
                    getX() + ox, getY() + oy, getZ() + oz, 0.0D, 0.0D, 0.0D);
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
        spawnMushroomCloud(server, getX(), getY(), getZ());

        this.discard();
    }

    private static void spawnMushroomCloud(ServerLevel level, double x, double y, double z) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        MediumNuclearMushroomCloud.spawnBlackSphere(level, x, y, z, level.random);
        server.tell(new TickTask(server.getTickCount() + 2, () ->
                MediumNuclearMushroomCloud.spawnShockwaveRing(level, x, y, z, level.random)));
        for (int i = 0; i < 10; i++) {
            final int step = i;
            server.tell(new TickTask(server.getTickCount() + 5 + i, () ->
                    MediumNuclearMushroomCloud.spawnStemSegment(level, x, y + (step * 2.0), z, level.random)));
        }
        server.tell(new TickTask(server.getTickCount() + 8, () ->
                MediumNuclearMushroomCloud.spawnMushroomBase(level, x, y, z, level.random)));
        server.tell(new TickTask(server.getTickCount() + 18, () ->
                MediumNuclearMushroomCloud.spawnMushroomCap(level, x, y, z, level.random)));
        server.tell(new TickTask(server.getTickCount() + 22, () ->
                MediumNuclearMushroomCloud.spawnCondensationRing(level, x, y + 15, z, level.random)));
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
    public boolean fireImmune() {
        return true;
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
