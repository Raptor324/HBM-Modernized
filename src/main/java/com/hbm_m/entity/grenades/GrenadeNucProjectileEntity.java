package com.hbm_m.entity.grenades;

import com.hbm_m.block.custom.explosives.IDetonatable;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.particle.explosions.basic.ExplosionParticleUtils;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.util.explosions.general.ShockwaveGenerator;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class GrenadeNucProjectileEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Boolean> TIMER_ACTIVATED = SynchedEntityData.defineId(GrenadeNucProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DETONATION_TIME = SynchedEntityData.defineId(GrenadeNucProjectileEntity.class, EntityDataSerializers.INT);

    // Параметры ядерной гранаты
    private static final int FUSE_SECONDS = 7;
    private static final float EXPLOSION_POWER = 10.0f;
    private static final float RADIATION_RADIUS = 25.0f;
    private static final float MIN_BOUNCE_SPEED = 0.1f;
    private static final float BOUNCE_MULTIPLIER = 0.4f;

    // Новые параметры для урона
    private static final float DAMAGE_RADIUS = 25.0f;
    private static final float DAMAGE_AMOUNT = 200.0f;
    private static final float MAX_DAMAGE_DISTANCE = 25.0f;
    private static final Random RANDOM = new Random();

    public GrenadeNucProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public GrenadeNucProjectileEntity(Level level, LivingEntity thrower) {
        super(ModEntities.GRENADE_NUC_PROJECTILE.get(), thrower, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TIMER_ACTIVATED, false);
        this.entityData.define(DETONATION_TIME, 0);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.GRENADE_NUC.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (this.entityData.get(TIMER_ACTIVATED)) {
                int detonationTime = this.entityData.get(DETONATION_TIME);
                if (this.tickCount >= detonationTime) {
                    explode(this.blockPosition());
                }
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            if (!this.entityData.get(TIMER_ACTIVATED)) {
                this.entityData.set(TIMER_ACTIVATED, true);
                this.entityData.set(DETONATION_TIME, this.tickCount + (FUSE_SECONDS * 20));
            }

            if (result.getType() == HitResult.Type.BLOCK) {
                handleBounce((BlockHitResult) result);
            }
        }
    }

    private void handleBounce(BlockHitResult result) {
        Vec3 velocity = this.getDeltaMovement();
        float speed = (float) velocity.length();

        if (speed < MIN_BOUNCE_SPEED) {
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoGravity(true);
            return;
        }

        BlockPos blockPos = result.getBlockPos();
        this.level().playSound(null, blockPos, ModSounds.BOUNCE_RANDOM.get(), SoundSource.NEUTRAL, 2.5F, 0.6F);

        Vec3 currentVelocity = this.getDeltaMovement();
        Vec3 hitNormal = Vec3.atLowerCornerOf(result.getDirection().getNormal());
        Vec3 reflectedVelocity = currentVelocity.subtract(hitNormal.scale(2 * currentVelocity.dot(hitNormal)));
        this.setDeltaMovement(reflectedVelocity.scale(BOUNCE_MULTIPLIER));
    }

    private void explode(BlockPos pos) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            this.discard();
            serverLevel.explode(this, x, y, z, 9.0F, true, Level.ExplosionInteraction.NONE);
            triggerNearbyDetonations(serverLevel, pos, null);
            dealExplosionDamage(serverLevel, x, y, z);

            // ЗАМЕНА ЭФФЕКТА
            scheduleExplosionEffects(serverLevel, x, y, z);

            playRandomDetonationSound(level(), pos);

            if (serverLevel.getServer() != null) {
                serverLevel.getServer().tell(new net.minecraft.server.TickTask(30, () -> {
                    serverLevel.explode(null, x, y, z, 9.0F, Level.ExplosionInteraction.NONE);
                    ShockwaveGenerator.generateCrater(
                            serverLevel,
                            pos,
                            25,
                            10,
                            ModBlocks.WASTE_LOG.get(),
                            ModBlocks.WASTE_PLANKS.get(),
                            ModBlocks.BURNED_GRASS.get()
                    );
                }));
            }
        }
    }

    private void dealExplosionDamage(ServerLevel serverLevel, double x, double y, double z) {
        List<LivingEntity> entitiesNearby = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                new net.minecraft.world.phys.AABB(x - DAMAGE_RADIUS, y - DAMAGE_RADIUS, z - DAMAGE_RADIUS,
                        x + DAMAGE_RADIUS, y + DAMAGE_RADIUS, z + DAMAGE_RADIUS)
        );

        for (LivingEntity entity : entitiesNearby) {
            double distanceToEntity = Math.sqrt(
                    Math.pow(entity.getX() - x, 2) +
                            Math.pow(entity.getY() - y, 2) +
                            Math.pow(entity.getZ() - z, 2)
            );

            if (distanceToEntity <= DAMAGE_RADIUS) {
                float damage = DAMAGE_AMOUNT;
                if (distanceToEntity > MAX_DAMAGE_DISTANCE) {
                    float remainingDistance = DAMAGE_RADIUS - MAX_DAMAGE_DISTANCE;
                    float damageDistance = (float) distanceToEntity - MAX_DAMAGE_DISTANCE;
                    damage = DAMAGE_AMOUNT * (1.0f - (damageDistance / remainingDistance)) * 0.5f;
                }
                entity.hurt(entity.damageSources().explosion(null), damage);
            }
        }
    }

    private void playRandomDetonationSound(Level level, BlockPos pos) {
        List<SoundEvent> sounds = Arrays.asList(
                ModSounds.MUKE_EXPLOSION.orElse(null),
                ModSounds.MUKE_EXPLOSION.orElse(null),
                ModSounds.MUKE_EXPLOSION.orElse(null)
        );
        sounds.removeIf(Objects::isNull);
        if (!sounds.isEmpty()) {
            SoundEvent sound = sounds.get(RANDOM.nextInt(sounds.size()));
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, SoundSource.BLOCKS, 4.0F, 1.0F);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("TimerActivated", this.entityData.get(TIMER_ACTIVATED));
        tag.putInt("DetonationTime", this.entityData.get(DETONATION_TIME));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(TIMER_ACTIVATED, tag.getBoolean("TimerActivated"));
        this.entityData.set(DETONATION_TIME, tag.getInt("DetonationTime"));
    }

    // === ЗАМЕНА ЭФФЕКТА ===
    private void scheduleExplosionEffects(ServerLevel level, double x, double y, double z) {

        // ✅ ИСПОЛЬЗУЕМ НОВЫЙ КОМПЛЕКСНЫЙ ЭФФЕКТ
        ExplosionParticleUtils.spawnFullNuclearExplosion(level, x, y, z);
    }

    private void triggerNearbyDetonations(ServerLevel serverLevel, BlockPos pos, Player player) {
        int DETONATION_RADIUS = 8;
        for (int x = -DETONATION_RADIUS; x <= DETONATION_RADIUS; x++) {
            for (int y = -DETONATION_RADIUS; y <= DETONATION_RADIUS; y++) {
                for (int z = -DETONATION_RADIUS; z <= DETONATION_RADIUS; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist <= DETONATION_RADIUS && dist > 0) {
                        BlockPos checkPos = pos.offset(x, y, z);
                        BlockState checkState = serverLevel.getBlockState(checkPos);
                        Block block = checkState.getBlock();
                        if (block instanceof IDetonatable detonatable) {
                            int delay = (int)(dist * 1.5);
                            serverLevel.getServer().tell(new net.minecraft.server.TickTask(delay, () -> {
                                detonatable.onDetonate(serverLevel, checkPos, checkState, player);
                            }));
                        }
                    }
                }
            }
        }
    }
}
