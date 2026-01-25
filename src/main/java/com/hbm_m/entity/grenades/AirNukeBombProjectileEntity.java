package com.hbm_m.entity.grenades;

import com.hbm_m.block.custom.explosives.IDetonatable;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.particle.explosions.basic.ExplosionParticleUtils;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.util.explosions.nuclear.CraterGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.TickTask;
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

/**
 * ✅ АВИАБОМБА v4
 *
 * Улучшения:
 * ✅ Полностью динамический размер кратера
 * ✅ Радиус определяется ТОЛЬКО силой пробития лучей
 * ✅ Синхронизация лучей исправлена
 */
public class AirNukeBombProjectileEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Float> SYNCHED_YAW =
            SynchedEntityData.defineId(AirNukeBombProjectileEntity.class, EntityDataSerializers.FLOAT);

    private static final float EXPLOSION_POWER = 25.0f;
    private static final float DAMAGE_RADIUS = 60.0f;
    private static final int DETONATION_RADIUS = 20;
    private static final int CRATER_GENERATION_DELAY = 30;
    private static final Random RANDOM = new Random();

    private boolean instantDetonation = false;

    public AirNukeBombProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public AirNukeBombProjectileEntity(Level level, LivingEntity thrower) {
        super(ModEntities.AIRNUKEBOMB_PROJECTILE.get(), thrower, level);
    }

    public AirNukeBombProjectileEntity(Level level, LivingEntity thrower, float planeYaw) {
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

            if (this.tickCount == 1 && !instantDetonation) {
                playBombWhistle();
            }
        }
    }

    private void playBombWhistle() {
        if (ModSounds.BOMBWHISTLE.isPresent()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.BOMBWHISTLE.get(), SoundSource.HOSTILE, 10.0F, 0.5F + RANDOM.nextFloat() * 0.2F);
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

            // ✅ ЯДЕРНЫЙ ВЗРЫВ: сначала эффекты, потом кратер
            triggerNearbyDetonations(serverLevel, pos, null);
            dealExplosionDamage(serverLevel, x, y, z);
            scheduleExplosionEffects(serverLevel, x, y, z);
            playDetonationSound(serverLevel, pos);

            // ✅ ОСНОВНОЙ ЯДЕРНЫЙ ВЗРЫВ (без разрушения блоков)
            serverLevel.explode(null, x, y, z, EXPLOSION_POWER, Level.ExplosionInteraction.NONE);

            // ✅ ГЕНЕРАТОР КРАТЕРА (радиус определяется лучами!)
            if (serverLevel.getServer() != null) {
                serverLevel.getServer().tell(new TickTask(CRATER_GENERATION_DELAY, () -> {
                    CraterGenerator.generateCrater(
                            serverLevel,
                            pos,
                            ModBlocks.SELLAFIELD_SLAKED.get(),
                            ModBlocks.SELLAFIELD_SLAKED1.get(),
                            ModBlocks.SELLAFIELD_SLAKED2.get(),
                            ModBlocks.SELLAFIELD_SLAKED3.get(),
                            ModBlocks.WASTE_LOG.get(),
                            ModBlocks.WASTE_PLANKS.get(),
                            ModBlocks.BURNED_GRASS.get(),
                            ModBlocks.DEAD_DIRT.get()
                    );
                }));
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

        for (LivingEntity entity : entitiesNearby) {
            entity.hurt(serverLevel.damageSources().generic(), 50.0F);
        }
    }

    private void scheduleExplosionEffects(ServerLevel level, double x, double y, double z) {
        // ✅ Flash
        level.sendParticles(
                (SimpleParticleType) ModExplosionParticles.FLASH.get(),
                x, y, z, 1, 0, 0, 0, 0
        );

        // ✅ Sparks
        ExplosionParticleUtils.spawnAirBombSparks(level, x, y, z);

        // ✅ Shockwave через 3 тика
        level.getServer().tell(new TickTask(3, () ->
                ExplosionParticleUtils.spawnAirBombShockwave(level, x, y, z)));

        // ✅ Mushroom Cloud через 8 тиков
        level.getServer().tell(new TickTask(8, () ->
                ExplosionParticleUtils.spawnAirBombMushroomCloud(level, x, y, z)));
    }

    private void playDetonationSound(ServerLevel level, BlockPos pos) {
        List<SoundEvent> candidates = new ArrayList<>();
        if (ModSounds.EXPLOSION_LARGE_NEAR.isPresent()) candidates.add(ModSounds.BOMBDET1.get());
        if (ModSounds.EXPLOSION_LARGE_NEAR.isPresent()) candidates.add(ModSounds.BOMBDET2.get());
        if (ModSounds.EXPLOSION_LARGE_NEAR.isPresent()) candidates.add(ModSounds.BOMBDET3.get());

        SoundEvent soundEvent = candidates.get(RANDOM.nextInt(candidates.size()));
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                soundEvent, SoundSource.BLOCKS, 8.0F, 0.8F + RANDOM.nextFloat() * 0.2F);
    }

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
                            serverLevel.getServer().tell(new TickTask(delay, () ->
                                    detonatable.onDetonate(serverLevel, checkPos, checkState, player)));
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