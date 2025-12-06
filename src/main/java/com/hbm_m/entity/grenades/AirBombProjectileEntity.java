package com.hbm_m.entity.grenades;

import com.hbm_m.block.IDetonatable;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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

import java.util.List;
import java.util.Random;

public class AirBombProjectileEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Float> SYNCHED_YAW =
            SynchedEntityData.defineId(AirBombProjectileEntity.class, EntityDataSerializers.FLOAT);

    // ✅ Параметры взрыва
    private static final float EXPLOSION_POWER = 12.0f;
    private static final float DAMAGE_RADIUS = 28.0f;
    private static final float DAMAGE_AMOUNT = 250.0f;
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
                return;
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

            // ✅ ВСЕ КАСТОМНЫЕ ЭФФЕКТЫ
            triggerNearbyDetonations(serverLevel, pos, null);
            dealExplosionDamage(serverLevel, x, y, z);
            scheduleExplosionEffects(serverLevel, x, y, z);
            playDetonationSound(serverLevel, pos);

            // ✅ Второй взрыв через 1 секунду
            if (serverLevel.getServer() != null) {
                serverLevel.getServer().tell(new net.minecraft.server.TickTask(20, () -> {
                    serverLevel.explode(null, x, y, z, EXPLOSION_POWER * 0.8F, Level.ExplosionInteraction.TNT);
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

    }

    private void playDetonationSound(Level level, BlockPos pos) {
        if (ModSounds.EXPLOSION_LARGE_NEAR.isPresent()) {
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    ModSounds.EXPLOSION_LARGE_NEAR.get(), net.minecraft.sounds.SoundSource.BLOCKS, 3.0F, 0.8F + RANDOM.nextFloat() * 0.2F);
        }
    }

    // ✅ КАСТОМНЫЕ ЭФФЕКТЫ
    private void scheduleExplosionEffects(ServerLevel level, double x, double y, double z) {
        spawnFlash(level, x, y, z);
        spawnSparks(level, x, y, z);
        level.getServer().tell(new net.minecraft.server.TickTask(3, () -> spawnShockwave(level, x, y, z)));
        level.getServer().tell(new net.minecraft.server.TickTask(8, () -> spawnMushroomCloud(level, x, y, z)));
    }

    private void spawnFlash(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ModExplosionParticles.FLASH.get(), x, y, z, 1, 0, 0, 0, 0);
    }

    private void spawnSparks(ServerLevel level, double x, double y, double z) {
        for (int i = 0; i < 400; i++) {
            double xSpeed = (level.random.nextDouble() - 0.5) * 6.0;
            double ySpeed = level.random.nextDouble() * 5.0;
            double zSpeed = (level.random.nextDouble() - 0.5) * 6.0;
            level.sendParticles(ModExplosionParticles.EXPLOSION_SPARK.get(), x, y, z, 1, xSpeed, ySpeed, zSpeed, 1.5);
        }
    }

    private void spawnShockwave(ServerLevel level, double x, double y, double z) {
        for (int ring = 0; ring < 6; ring++) {
            double ringY = y + (ring * 0.3);
            level.sendParticles(ModExplosionParticles.SHOCKWAVE.get(), x, ringY, z, 1, 0, 0, 0, 0);
        }
    }

    private void spawnMushroomCloud(ServerLevel level, double x, double y, double z) {
        // Стебель
        for (int i = 0; i < 150; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 6.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 6.0;
            double ySpeed = 0.8 + level.random.nextDouble() * 0.4;
            level.sendParticles(ModExplosionParticles.MUSHROOM_SMOKE.get(),
                    x + offsetX, y, z + offsetZ, 1, offsetX * 0.08, ySpeed, offsetZ * 0.08, 1.5);
        }
        // Шапка
        for (int i = 0; i < 250; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = 8.0 + level.random.nextDouble() * 12.0;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double capY = y + 20 + level.random.nextDouble() * 10;
            double xSpeed = Math.cos(angle) * 0.5;
            double ySpeed = -0.1 + level.random.nextDouble() * 0.2;
            double zSpeed = Math.sin(angle) * 0.5;
            level.sendParticles(ModExplosionParticles.MUSHROOM_SMOKE.get(),
                    x + offsetX, capY, z + offsetZ, 1, xSpeed, ySpeed, zSpeed, 1.5);
        }
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
                            int delay = (int)(dist * 2.0);
                            serverLevel.getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
                                detonatable.onDetonate(serverLevel, checkPos, checkState, player);
                            }));
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
