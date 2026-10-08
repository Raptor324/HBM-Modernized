package com.hbm_m.entity.projectile;

import com.hbm_m.entity.projectile.rocketbehavior.IRocketSteeringBehavior;
import com.hbm_m.entity.projectile.rocketbehavior.IRocketTargetingBehavior;
import com.hbm_m.entity.projectile.rocketbehavior.RocketSteeringBallisticArc;
import com.hbm_m.entity.projectile.rocketbehavior.RocketTargetingPredictive;
import com.hbm_m.item.weapon.ItemAmmoHIMARS;
import com.hbm_m.item.weapon.ItemAmmoHIMARS.HIMARSRocket;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.Vec3NT;

import api.hbm_m.entity.IRadarDetectable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityArtilleryRocket} (entity_himars): gelenkte Rakete der Himars (Typ = Meta von {@code ammo_himars},
 * siehe {@link ItemAmmoHIMARS}). Vorhersagende Zielverfolgung + ballistischer Bogen, kurz vor dem Ziel geradeaus.
 * Der Client zeichnet die Kerosin-Abgasspur. Haelt ihren Chunk geladen.
 */
public class EntityArtilleryRocket extends EntityThrowableInterp implements IRadarDetectable {

    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(EntityArtilleryRocket.class, EntityDataSerializers.INT);

    //TODO: find satisfying solution for when an entity is unloaded and reloaded, possibly a custom entity lookup using persistent UUIDs
    public Entity targetEntity = null;
    public Vec3 lastTargetPos;

    public IRocketTargetingBehavior targeting;
    public IRocketSteeringBehavior steering;

    public EntityArtilleryRocket(EntityType<? extends EntityArtilleryRocket> type, Level world) {
        super(type, world);
        this.noCulling = true;

        this.targeting = new RocketTargetingPredictive();
        this.steering = new RocketSteeringBallisticArc();
    }

    //? if < 1.21.1 {
    @Override
    protected void defineExtraData() {
        this.entityData.define(TYPE, 0);
    }
    //?} else {
    /*@Override
    protected void defineExtraData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, 0);
    }
    *///?}

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    public EntityArtilleryRocket setType(int type) {
        this.entityData.set(TYPE, type);
        return this;
    }

    public int getTypeId() {
        return this.entityData.get(TYPE);
    }

    public HIMARSRocket getRocketType() {
        try {
            return ItemAmmoHIMARS.itemTypes[this.entityData.get(TYPE)];
        } catch (Exception ex) {
            return ItemAmmoHIMARS.itemTypes[0];
        }
    }

    public EntityArtilleryRocket setTarget(Entity target) {
        this.targetEntity = target;
        setTarget(target.getX(), target.getY() + target.getBbHeight() / 2D, target.getZ());
        return this;
    }

    public EntityArtilleryRocket setTarget(double x, double y, double z) {
        this.lastTargetPos = new Vec3(x, y, z);
        return this;
    }

    public Vec3 getLastTarget() {
        return this.lastTargetPos;
    }

    @Override
    public void tick() {

        if (level().isClientSide) {
            this.xOld = this.getX();
            this.yOld = this.getY();
            this.zOld = this.getZ();
        }

        super.tick();

        if (!level().isClientSide) {
            if (this.isRemoved()) return;
            if (this.lastTargetPos == null) this.lastTargetPos = this.position();

            Vec3 m = this.getDeltaMovement();
            Vec3NT delta = new Vec3NT(this.lastTargetPos.x - this.getX(), this.lastTargetPos.y - this.getY(), this.lastTargetPos.z - this.getZ());
            double momentum = Math.sqrt(m.x * m.x + m.y * m.y + m.z * m.z) * motionMult();
            if (delta.lengthVector() <= momentum * 1.5) {
                if (this.targetEntity == null || !this.targetEntity.isAlive()) {
                    this.targeting = null;
                    this.steering = null;
                }
                delta.normalizeSelf();
                this.setDeltaMovement(delta.xCoord * momentum / motionMult(), delta.yCoord * momentum / motionMult(), delta.zCoord * momentum / motionMult());
            } else {
                if (this.targeting != null && this.targetEntity != null) this.targeting.recalculateTargetPosition(this, this.targetEntity);
                if (this.steering != null) this.steering.adjustCourse(this, 25D, 15D);
            }

            ArtilleryChunkLoader.load(this, (int) Math.floor(getX() / 16D), (int) Math.floor(getZ() / 16D));
            this.getRocketType().onUpdate(this);
        } else {

            Vec3NT v = new Vec3NT(xOld - getX(), yOld - getY(), zOld - getZ());
            double velocity = v.lengthVector();
            v = v.normalize();

            int offset = 6;
            if (velocity > 1) {
                for (int i = offset; i < velocity + offset; i++) {
                    CompoundTag data = new CompoundTag();
                    data.putDouble("posX", getX() + v.xCoord * i);
                    data.putDouble("posY", getY() + v.yCoord * i);
                    data.putDouble("posZ", getZ() + v.zCoord * i);
                    data.putString("type", "exKerosene");
                    com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
                }
            }
        }
    }

    @Override
    protected void onImpact(HitResult hit) {

        if (!level().isClientSide) {
            MovingObjectPosition mop = MovingObjectPosition.of(hit);
            if (mop != null) this.getRocketType().onImpact(this, mop);
        }
    }

    public void killAndClear() {
        this.discard();
        this.clearChunkLoader();
    }

    public void clearChunkLoader() {
        if (!level().isClientSide) ArtilleryChunkLoader.release(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        // ForgeChunkManager gab das Ticket mit der gebundenen Entity frei
        clearChunkLoader();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);

        if (this.lastTargetPos == null) {
            this.lastTargetPos = this.position();
        }

        nbt.putDouble("targetX", this.lastTargetPos.x);
        nbt.putDouble("targetY", this.lastTargetPos.y);
        nbt.putDouble("targetZ", this.lastTargetPos.z);

        nbt.putInt("type", this.entityData.get(TYPE));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        this.lastTargetPos = new Vec3(nbt.getDouble("targetX"), nbt.getDouble("targetY"), nbt.getDouble("targetZ"));

        this.entityData.set(TYPE, nbt.getInt("type"));
    }

    @Override
    protected float getAirDrag() {
        return 1.0F;
    }

    @Override
    public double getGravityVelocity() {
        return this.steering != null ? 0D : 0.01D;
    }

    @Override
    public RadarTargetType getTargetType() {
        return RadarTargetType.ARTILLERY;
    }

    @Override
    public int approachNum() {
        return 0;
    }
}
