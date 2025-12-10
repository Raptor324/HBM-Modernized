package com.hbm_m.entity.grenades;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.grenades.GrenadeType;
import com.hbm_m.item.ModItems;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.hbm_m.entity.grenades.GrenadeProjectileEntity.GRENADE_TYPE_ID;


public class GrenadeIfProjectileEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Boolean> TIMER_ACTIVATED = SynchedEntityData.defineId(GrenadeIfProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DETONATION_TIME = SynchedEntityData.defineId(GrenadeIfProjectileEntity.class, EntityDataSerializers.INT);
    private static final int FUSE_SECONDS = 4;

    private static final float MIN_BOUNCE_SPEED = 0.15f;

    private GrenadeIfType grenadeType;

    private static final Random RANDOM = new Random();

    public GrenadeIfProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public GrenadeIfProjectileEntity(Level level, LivingEntity thrower, GrenadeIfType type) {
        super(ModEntities.GRENADE_IF_PROJECTILE.get(), thrower, level);
        this.grenadeType = type;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TIMER_ACTIVATED, false);
        this.entityData.define(DETONATION_TIME, 0);
    }

    @Override
    protected Item getDefaultItem() {
        if (grenadeType == null) {
            try {
                grenadeType = GrenadeIfType.valueOf(this.entityData.get(GRENADE_TYPE_ID));
            } catch (Exception e) {
                grenadeType = GrenadeIfType.GRENADE_IF;
            }
        }
        return grenadeType != null ? grenadeType.getItem() : Items.SNOWBALL;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (this.entityData.get(TIMER_ACTIVATED)) {
                if (this.tickCount >= this.entityData.get(DETONATION_TIME)) {
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
            this.level().playSound(null, this.blockPosition(), ModSounds.BOUNCE_RANDOM.get(), SoundSource.NEUTRAL, 0.5F, 0.8F);
            return;
        }

        BlockPos blockPos = result.getBlockPos();
        this.level().playSound(null, blockPos, ModSounds.BOUNCE_RANDOM.get(), SoundSource.NEUTRAL, 2.1F, 1.0F);

        Vec3 currentVelocity = this.getDeltaMovement();
        Vec3 hitNormal = Vec3.atLowerCornerOf(result.getDirection().getNormal());
        Vec3 reflectedVelocity = currentVelocity.subtract(hitNormal.scale(2 * currentVelocity.dot(hitNormal)));
        this.setDeltaMovement(reflectedVelocity.scale(grenadeType.getBounceMultiplier()));
    }

    private void explode(BlockPos pos) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            this.level().explode(
                    this, null, null,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    grenadeType.getExplosionPower(),
                    grenadeType.causesFire(),
                    Level.ExplosionInteraction.BLOCK
            );

            // Вызов пользовательского звука
            playDetonationSound(this.level(), pos);

            float damageRadius = grenadeType.getExplosionPower() * 2.0f;
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
        tag.putBoolean("TimerActivated", this.entityData.get(TIMER_ACTIVATED));
        tag.putInt("DetonationTime", this.entityData.get(DETONATION_TIME));
        if (grenadeType != null) {
            tag.putString("GrenadeType", grenadeType.name());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(TIMER_ACTIVATED, tag.getBoolean("TimerActivated"));
        this.entityData.set(DETONATION_TIME, tag.getInt("DetonationTime"));
        if (tag.contains("GrenadeType")) {
            this.grenadeType = GrenadeIfType.valueOf(tag.getString("GrenadeType"));
        }
    }
}
