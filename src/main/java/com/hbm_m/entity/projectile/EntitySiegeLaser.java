package com.hbm_m.entity.projectile;

import com.hbm_m.platform.PlatformHooks;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.entity.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code com.hbm.entity.projectile.EntitySiegeLaser} ("entity_ntm_siege_laser"): schwerelose Laserbolzen des
 * Belagerungsschiffs. Lebt 60 Ticks; Treffer verursachen "laser"-Schaden, optional Feuer, Explosion (ohne
 * Blockschaden) und eine Chance, den getroffenen Block abzubauen. Die Farbe liegt im Datenwaechter (Original-Slot 12).
 */
public class EntitySiegeLaser extends ThrowableProjectile {

    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(EntitySiegeLaser.class, EntityDataSerializers.INT);

    private float damage = 2;
    private float explosive = 0F;
    private float breakChance = 0F;
    private boolean incendiary = false;

    public EntitySiegeLaser(EntityType<? extends EntitySiegeLaser> type, Level world) {
        super(type, world);
    }

    public EntitySiegeLaser(Level world) {
        this(ModEntities.SIEGE_LASER.get(), world);
    }

    public EntitySiegeLaser(Level world, LivingEntity entity) {
        super(ModEntities.SIEGE_LASER.get(), entity, world);
        // EntityThrowable(world, living): Blickrichtung, 1.5 Geschwindigkeit, 1.0 Streuung
        this.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), 0.0F, 1.5F, 1.0F);
    }

    public EntitySiegeLaser(Level world, double x, double y, double z) {
        super(ModEntities.SIEGE_LASER.get(), x, y, z, world);
    }

    @Override
    //? if < 1.21.1 {
    protected void defineSynchedData() {
        this.entityData.define(COLOR, 0xffffff);
    }
    //?} else {
    /*protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0xffffff);
    }
    *///?}

    public EntitySiegeLaser setDamage(float f) {
        this.damage = f;
        return this;
    }

    public EntitySiegeLaser setExplosive(float f) {
        this.explosive = f;
        return this;
    }

    public EntitySiegeLaser setBreakChance(float f) {
        this.breakChance = f;
        return this;
    }

    public EntitySiegeLaser setIncendiary() {
        this.incendiary = true;
        return this;
    }

    public EntitySiegeLaser setColor(int color) {
        this.entityData.set(COLOR, color);
        return this;
    }

    public int getColor() {
        return this.entityData.get(COLOR);
    }

    /** {@code EntityThrowable.getThrower()}. */
    public LivingEntity getThrower() {
        return getOwner() instanceof LivingEntity living ? living : null;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > 60)
            this.discard();
    }

    @Override
    protected void onHit(HitResult mop) {
        super.onHit(mop);

        if (mop.getType() == HitResult.Type.ENTITY) {
            Entity entityHit = ((EntityHitResult) mop).getEntity();
            Vec3 hitVec = mop.getLocation();
            DamageSource dmg;

            if (this.getThrower() != null)
                dmg = ModDamageSources.laser(this, this.getThrower());
            else
                dmg = ModDamageSources.create(level(), ModDamageTypes.LASER);

            if (entityHit.hurt(dmg, this.damage)) {
                this.discard();

                if (this.incendiary)
                    PlatformHooks.setSecondsOnFire(entityHit, 3);

                // newExplosion(..., smoking = false): keine Blockzerstoerung
                if (this.explosive > 0 && !level().isClientSide)
                    this.level().explode(this, hitVec.x, hitVec.y, hitVec.z, this.explosive, this.incendiary, Level.ExplosionInteraction.NONE);
            }

        } else if (mop.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) mop;
            Vec3 hitVec = mop.getLocation();

            if (this.explosive > 0) {
                if (!level().isClientSide)
                    this.level().explode(this, hitVec.x, hitVec.y, hitVec.z, this.explosive, this.incendiary, Level.ExplosionInteraction.NONE);

            } else if (this.incendiary) {
                BlockPos pos = bhr.getBlockPos().relative(bhr.getDirection());

                if (this.level().getBlockState(pos).canBeReplaced()) {
                    this.level().setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                }
            }

            if (this.random.nextFloat() < this.breakChance) {
                this.level().destroyBlock(bhr.getBlockPos(), false);
            }

            this.discard();
        }
    }

    //? if < 1.21.1 {
    @Override
    protected float getGravity() {
        return 0.0F;
    }
    //?} else {
    /*@Override
    protected double getDefaultGravity() {
        return 0.0F;
    }
    *///?}

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putFloat("damage", this.damage);
        nbt.putFloat("explosive", this.explosive);
        nbt.putFloat("breakChance", this.breakChance);
        nbt.putBoolean("incendiary", this.incendiary);
        nbt.putInt("color", this.getColor());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.damage = nbt.getFloat("damage");
        this.explosive = nbt.getFloat("explosive");
        this.breakChance = nbt.getFloat("breakChance");
        this.incendiary = nbt.getBoolean("incendiary");
        this.setColor(nbt.getInt("color"));
    }
}
