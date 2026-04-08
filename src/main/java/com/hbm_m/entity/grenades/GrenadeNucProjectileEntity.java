package com.hbm_m.entity.grenades;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.command.ExplosionCommandOptions;
import com.hbm_m.explosion.command.NuclearScenarioLaunchers;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class GrenadeNucProjectileEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Boolean> TIMER_ACTIVATED = SynchedEntityData.defineId(GrenadeNucProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DETONATION_TIME = SynchedEntityData.defineId(GrenadeNucProjectileEntity.class, EntityDataSerializers.INT);

    // Параметры ядерной гранаты
    private static final int FUSE_SECONDS = 7;
    private static final float MIN_BOUNCE_SPEED = 0.1f;
    private static final float BOUNCE_MULTIPLIER = 0.4f;

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
            this.discard();
            NuclearScenarioLaunchers.launchGrenadeNuc(serverLevel, pos, ExplosionCommandOptions.DEFAULT, null);
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

}
