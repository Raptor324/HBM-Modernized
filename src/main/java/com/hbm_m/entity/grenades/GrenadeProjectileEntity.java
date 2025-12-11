package com.hbm_m.entity.grenades;

import com.hbm_m.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class GrenadeProjectileEntity extends ThrowableItemProjectile {

    static final EntityDataAccessor<String> GRENADE_TYPE_ID =
            SynchedEntityData.defineId(GrenadeProjectileEntity.class, EntityDataSerializers.STRING);

    private int bounceCount = 0;
    private GrenadeType grenadeType;

    private static final Random RANDOM = new Random();

    public GrenadeProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public GrenadeProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level,
                                   LivingEntity livingEntity, GrenadeType type) {
        super(entityType, livingEntity, level);
        this.grenadeType = type;
        this.entityData.set(GRENADE_TYPE_ID, type.name());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(GRENADE_TYPE_ID, GrenadeType.STANDARD.name());
    }

    @Override
    protected Item getDefaultItem() {
        if (grenadeType == null) {
            try {
                grenadeType = GrenadeType.valueOf(this.entityData.get(GRENADE_TYPE_ID));
            } catch (Exception e) {
                grenadeType = GrenadeType.STANDARD;
            }
        }
        return grenadeType != null ? grenadeType.getItem() : Items.SNOWBALL;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.level().isClientSide) {
            if (grenadeType == null) {
                grenadeType = GrenadeType.valueOf(this.entityData.get(GRENADE_TYPE_ID));
            }

            BlockPos blockPos = result.getBlockPos();
            this.level().playSound(null, blockPos, ModSounds.BOUNCE_RANDOM.get(), SoundSource.NEUTRAL, 2.1F, 1.0F);

            this.bounceCount++;

            if (this.bounceCount < grenadeType.getMaxBounces()) {
                Vec3 currentVelocity = this.getDeltaMovement();
                Vec3 hitNormal = Vec3.atLowerCornerOf(result.getDirection().getNormal());
                Vec3 reflectedVelocity = currentVelocity.subtract(hitNormal.scale(2 * currentVelocity.dot(hitNormal)));
                this.setDeltaMovement(reflectedVelocity.scale(grenadeType.getBounceMultiplier()));
            } else {
                explode(blockPos);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide) {
            if (grenadeType == null) {
                grenadeType = GrenadeType.valueOf(this.entityData.get(GRENADE_TYPE_ID));
            }

            if (grenadeType.explodesOnEntity()) {
                explode(result.getEntity().blockPosition());
            }
        }
    }

    private void explode(BlockPos pos) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            // Увеличиваем силу взрыва (например, умножаем на 1.5 для более мощного разрушения)
            float explosionPower = grenadeType.getExplosionPower() * 1.5f;

            this.level().explode(
                    this, null, null,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    explosionPower,
                    grenadeType.causesFire(),
                    Level.ExplosionInteraction.TNT
            );

            // Вызов кастомного звука
            playDetonationSound(this.level(), pos);

            float damageRadius = explosionPower * 2.0f;
            List<LivingEntity> entities = this.level().getEntitiesOfClass(
                    LivingEntity.class,
                    this.getBoundingBox().inflate(damageRadius)
            );

            for (LivingEntity entity : entities) {
                double distSqr = entity.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                if (distSqr < damageRadius * damageRadius) {
                    double dist = Math.sqrt(distSqr);
                    float damage = grenadeType.getCustomDamage() * (float) (1.0 - (dist / damageRadius));
                    if (damage > 0) {
                        entity.invulnerableTime = 0;
                        entity.hurt(this.damageSources().explosion(this, null), damage);
                    }
                }
            }

            this.discard();
        }
    }

    private void playDetonationSound(Level level, BlockPos pos) {
        SoundEvent[] sounds = new SoundEvent[]{
                ModSounds.EXPLOSION_SMALL_NEAR1.orElse(null),
                ModSounds.EXPLOSION_SMALL_NEAR2.orElse(null),
                ModSounds.EXPLOSION_SMALL_NEAR3.orElse(null)
        };

        List<SoundEvent> availableSounds = Arrays.stream(sounds)
                .filter(Objects::nonNull)
                .toList();

        if (!availableSounds.isEmpty()) {
            SoundEvent soundToPlay = availableSounds.get(RANDOM.nextInt(availableSounds.size()));
            level.playSound(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    soundToPlay,
                    SoundSource.BLOCKS,
                    3.0F,
                    1.0F
            );
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BounceCount", this.bounceCount);
        tag.putString("GrenadeType", this.entityData.get(GRENADE_TYPE_ID));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.bounceCount = tag.getInt("BounceCount");
        if (tag.contains("GrenadeType")) {
            this.entityData.set(GRENADE_TYPE_ID, tag.getString("GrenadeType"));
            this.grenadeType = GrenadeType.valueOf(tag.getString("GrenadeType"));
        }
    }
}
