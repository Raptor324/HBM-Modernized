package com.hbm_m.entity.grenades;

import com.hbm_m.block.custom.explosives.IDetonatable;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.particle.explosions.basic.ExplosionParticleUtils;
import com.hbm_m.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AirBombProjectileEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Float> SYNCHED_YAW =
            SynchedEntityData.defineId(AirBombProjectileEntity.class, EntityDataSerializers.FLOAT);

    // ✅ Параметры взрыва
    private static final float EXPLOSION_POWER = 12.0f;
    private static final float EXPLOSION_POWER2 = 20.0f;
    private static final float DAMAGE_RADIUS = 28.0f;
    private static final int DETONATION_RADIUS = 10;
    private static final Random RANDOM = new Random();

    private boolean instantDetonation = false;

    public AirBombProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public AirBombProjectileEntity(Level level, LivingEntity thrower) {
        super(ModEntities.AIRBOMB_PROJECTILE.get(), thrower, level);
    }

    public AirBombProjectileEntity(Level level, LivingEntity thrower, float planeYaw) {
        this(level, thrower);
        syncYawWithPlane(planeYaw);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SYNCHED_YAW, 0.0F);
    }

    public void syncYawWithPlane(float planeYaw) {
        this.entityData.set(SYNCHED_YAW, planeYaw);
        this.setYRot(planeYaw);
        this.yRotO = planeYaw;
    }

    public float getSynchedYaw() {
        return this.entityData.get(SYNCHED_YAW);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.AIRBOMB_A.get();
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            float synchedYaw = this.getSynchedYaw();
            if (synchedYaw != this.getYRot()) {
                this.setYRot(synchedYaw);
                this.yRotO = synchedYaw;
            }
            // 🆕 [translate:СВИСТ БОМБЫ]: только 1 раз при спавне
            if (this.tickCount == 1 && !instantDetonation) {
                playBombWhistle();
            }
        }
    }

    // 🆕 [translate:СВИСТ БОМБЫ]: один раз при спавне
    private void playBombWhistle() {
        if (ModSounds.BOMBWHISTLE.isPresent()) {
            this.level().playSound(
                    null,
                    this.getX(), this.getY(), this.getZ(),
                    ModSounds.BOMBWHISTLE.get(),
                    SoundSource.HOSTILE,
                    6.0F,
                    0.9F + RANDOM.nextFloat() * 0.2F
            );
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (!this.level().isClientSide && !instantDetonation) {
            instantDetonation = true;

            if (result.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockResult = (BlockHitResult) result;
                explode(blockResult.getBlockPos());
                return;
            }

            if (result.getType() == HitResult.Type.ENTITY) {
                explode(this.blockPosition());
            }
        }
    }

    private void explode(BlockPos pos) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            this.discard();

            // ✅ Все кастомные эффекты
            triggerNearbyDetonations(serverLevel, pos, null);
            dealExplosionDamage(serverLevel, x, y, z);
            scheduleExplosionEffects(serverLevel, x, y, z);
            playDetonationSound(serverLevel, pos);

            if (serverLevel.getServer() != null) {
                serverLevel.getServer().tell(new net.minecraft.server.TickTask(20, () ->
                        serverLevel.explode(null, x, y, z, EXPLOSION_POWER * 0.8F, Level.ExplosionInteraction.TNT)
                ));
            }
            if (serverLevel.getServer() != null) {
                serverLevel.getServer().tell(new net.minecraft.server.TickTask(20, () ->
                        serverLevel.explode(null, x, y, z, EXPLOSION_POWER2 * 0.8F, Level.ExplosionInteraction.NONE)
                ));
            }
        }
    }

    private void dealExplosionDamage(ServerLevel serverLevel, double x, double y, double z) {
        List<LivingEntity> entitiesNearby = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                new net.minecraft.world.phys.AABB(
                        x - DAMAGE_RADIUS, y - DAMAGE_RADIUS, z - DAMAGE_RADIUS,
                        x + DAMAGE_RADIUS, y + DAMAGE_RADIUS, z + DAMAGE_RADIUS
                )
        );
        // здесь можно добавить урон по [translate:entitiesNearby]
    }

    /**
     * Рандомный выбор одного из трёх звуков [translate:бомб].
     */
    private void playDetonationSound(Level level, BlockPos pos) {
        List<SoundEvent> candidates = new ArrayList<>();

        if (ModSounds.BOMBDET1.isPresent()) {
            candidates.add(ModSounds.BOMBDET1.get());
        }
        if (ModSounds.BOMBDET2.isPresent()) {
            candidates.add(ModSounds.BOMBDET2.get());
        }
        if (ModSounds.BOMBDET3.isPresent()) {
            candidates.add(ModSounds.BOMBDET3.get());
        }

        // Если ни один из трёх звуков не зарегистрирован — fallback на старый
        if (candidates.isEmpty()) {
            if (ModSounds.EXPLOSION_LARGE_NEAR.isPresent()) {
                level.playSound(
                        null,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        ModSounds.EXPLOSION_LARGE_NEAR.get(),
                        SoundSource.BLOCKS,
                        3.0F,
                        0.8F + RANDOM.nextFloat() * 0.2F
                );
            }
            return;
        }

        SoundEvent soundEvent = candidates.get(RANDOM.nextInt(candidates.size()));

        level.playSound(
                null,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                soundEvent,
                SoundSource.BLOCKS,
                8.0F,
                0.8F + RANDOM.nextFloat() * 0.2F
        );
    }


    private void scheduleExplosionEffects(ServerLevel level, double x, double y, double z) {
        level.sendParticles((SimpleParticleType) ModExplosionParticles.FLASH.get(),
                x, y, z, 1, 0, 0, 0, 0);
        ExplosionParticleUtils.spawnAirBombSparks(level, x, y, z);
        level.getServer().tell(new net.minecraft.server.TickTask(3, () ->
                ExplosionParticleUtils.spawnAirBombShockwave(level, x, y, z)));
        level.getServer().tell(new net.minecraft.server.TickTask(8, () ->
                ExplosionParticleUtils.spawnAirBombMushroomCloud(level, x, y, z)));}


    private void triggerNearbyDetonations(ServerLevel serverLevel, BlockPos pos, Player player) {
        for (int x = -DETONATION_RADIUS; x <= DETONATION_RADIUS; x++) {
            for (int y = -DETONATION_RADIUS; y <= DETONATION_RADIUS; y++) {
                for (int z = -DETONATION_RADIUS; z <= DETONATION_RADIUS; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist <= DETONATION_RADIUS && dist > 0) {
                        BlockPos checkPos = pos.offset(x, y, z);
                        BlockState checkState = serverLevel.getBlockState(checkPos);
                        Block block = checkState.getBlock();
                        if (block instanceof IDetonatable detonatable) {
                            int delay = (int) (dist * 2.0);
                            serverLevel.getServer().tell(new net.minecraft.server.TickTask(delay, () ->
                                    detonatable.onDetonate(serverLevel, checkPos, checkState, player)
                            ));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("InstantDetonation", instantDetonation);
        tag.putFloat("SynchedYaw", this.entityData.get(SYNCHED_YAW));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.instantDetonation = tag.getBoolean("InstantDetonation");
        this.entityData.set(SYNCHED_YAW, tag.getFloat("SynchedYaw"));
    }
}
