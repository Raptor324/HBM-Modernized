package com.hbm_m.entity.projectile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityThrowableNT}: "near-identical copy of EntityThrowable but deobfuscated &amp; untangled". Eigene
 * Bewegung mit Luft-/Wasserwiderstand und Schwerkraft, Treffer gegen Bloecke (Strahl) und Wesen (aufgeweitete
 * Hitbox 0,3, Werfer erst nach {@link #selfDamageDelay()} Ticks), Stecken im Block.
 */
public abstract class EntityThrowableNT extends Entity {

    private static final EntityDataAccessor<Byte> STUCK_IN = SynchedEntityData.defineId(EntityThrowableNT.class, EntityDataSerializers.BYTE);

    private BlockPos stuckPos = new BlockPos(0, -1, 0);
    @Nullable private Block stuckBlock;
    protected boolean inGround;
    public int throwableShake;
    @Nullable protected LivingEntity thrower;
    @Nullable private UUID throwerId;
    public int ticksInGround;
    public int ticksInAir;

    protected EntityThrowableNT(EntityType<? extends EntityThrowableNT> type, Level world) {
        super(type, world);
    }

    /** Original {@code EntityThrowableNT(World, EntityLivingBase)}: aus Augenhoehe in Blickrichtung. */
    protected EntityThrowableNT(EntityType<? extends EntityThrowableNT> type, Level world, LivingEntity thrower) {
        this(type, world);
        this.thrower = thrower;
        this.throwerId = thrower.getUUID();
        float yaw = thrower.getYRot();
        float pitch = thrower.getXRot();
        double x = thrower.getX() - Mth.cos(yaw / 180.0F * (float) Math.PI) * 0.16F;
        double y = thrower.getY() + thrower.getEyeHeight() - 0.1D;
        double z = thrower.getZ() - Mth.sin(yaw / 180.0F * (float) Math.PI) * 0.16F;
        this.moveTo(x, y, z, yaw, pitch);
        float velocity = 0.4F;
        double mx = -Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI) * velocity;
        double mz = Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI) * velocity;
        double my = -Mth.sin((pitch + this.throwAngle()) / 180.0F * (float) Math.PI) * velocity;
        this.setThrowableHeading(mx, my, mz, this.throwForce(), 1.0F);
    }

    protected EntityThrowableNT(EntityType<? extends EntityThrowableNT> type, Level world, double x, double y, double z) {
        this(type, world);
        this.ticksInGround = 0;
        this.setPos(x, y, z);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(STUCK_IN, (byte) 0);
        defineExtraData();
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STUCK_IN, (byte) 0);
        defineExtraData(builder);
    }
    *///?}

    //? if < 1.21.1 {
    /** Zusaetzliche synchronisierte Werte der Unterklassen. */
    protected void defineExtraData() { }
    //?} else {
    /*protected void defineExtraData(SynchedEntityData.Builder builder) { }
    *///?}

    public void setStuckIn(int side) { this.entityData.set(STUCK_IN, (byte) side); }
    public int getStuckIn() { return this.entityData.get(STUCK_IN); }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        double perimeter = this.getBoundingBox().getSize() * 4.0D;
        perimeter *= 64.0D;
        return dist < perimeter * perimeter;
    }

    protected float throwForce() { return 1.5F; }
    protected double headingForceMult() { return 0.0075D; }
    protected float throwAngle() { return 0.0F; }
    protected double motionMult() { return 1.0D; }

    public void setThrowableHeading(double motionX, double motionY, double motionZ, float velocity, float inaccuracy) {
        float throwLen = (float) Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX /= throwLen;
        motionY /= throwLen;
        motionZ /= throwLen;
        motionX += this.random.nextGaussian() * headingForceMult() * inaccuracy;
        motionY += this.random.nextGaussian() * headingForceMult() * inaccuracy;
        motionZ += this.random.nextGaussian() * headingForceMult() * inaccuracy;
        motionX *= velocity;
        motionY *= velocity;
        motionZ *= velocity;
        this.setDeltaMovement(motionX, motionY, motionZ);
        float hyp = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        this.setYRot((float) (Math.atan2(motionX, motionZ) * 180.0D / Math.PI));
        this.setXRot((float) (Math.atan2(motionY, hyp) * 180.0D / Math.PI));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.ticksInGround = 0;
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        this.setDeltaMovement(x, y, z);
        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            float hyp = (float) Math.sqrt(x * x + z * z);
            this.setYRot((float) (Math.atan2(x, z) * 180.0D / Math.PI));
            this.setXRot((float) (Math.atan2(y, hyp) * 180.0D / Math.PI));
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.throwableShake > 0) {
            --this.throwableShake;
        }

        Vec3 motion = this.getDeltaMovement();

        if (this.inGround) {
            if (this.level().getBlockState(this.stuckPos).getBlock() == this.stuckBlock) {
                ++this.ticksInGround;

                if (this.groundDespawn() > 0 && this.ticksInGround == this.groundDespawn()) {
                    this.discard();
                }
                return;

            } else {
                this.inGround = false;
                this.setDeltaMovement(motion.x * (this.random.nextFloat() * 0.2F), motion.y * (this.random.nextFloat() * 0.2F), motion.z * (this.random.nextFloat() * 0.2F));
                this.ticksInGround = 0;
                this.ticksInAir = 0;
            }

        } else {
            ++this.ticksInAir;

            motion = this.getDeltaMovement();
            Vec3 pos = this.position();
            Vec3 nextPos = pos.add(motion.x * motionMult(), motion.y * motionMult(), motion.z * motionMult());
            HitResult mop = null;
            if (!this.isSpectral()) {
                BlockHitResult bhr = this.level().clip(new ClipContext(pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (bhr.getType() != HitResult.Type.MISS) mop = bhr;
            }

            if (mop != null) {
                nextPos = mop.getLocation();
            }

            if (!this.level().isClientSide && this.doesImpactEntities()) {

                Entity hitEntity = null;
                List<Entity> list = this.level().getEntities(this, this.getBoundingBox().expandTowards(motion.x * motionMult(), motion.y * motionMult(), motion.z * motionMult()).inflate(1.0D));
                double nearest = 0.0D;
                LivingEntity thrower = this.getThrower();
                Vec3 nonPenImpact = null;

                for (Entity entity : list) {

                    if (entity.isPickable() && (entity != thrower || this.ticksInAir >= this.selfDamageDelay()) && entity.isAlive()) {
                        double hitbox = 0.3F;
                        AABB aabb = entity.getBoundingBox().inflate(hitbox);
                        Optional<Vec3> hitMop = aabb.clip(pos, nextPos);

                        if (hitMop.isPresent()) {

                            // if penetration is enabled, run impact for all intersecting entities
                            if (this.doesPenetrate()) {
                                this.onImpact(new EntityHitResult(entity, hitMop.get()));
                            } else {

                                double dist = pos.distanceTo(hitMop.get());

                                if (dist < nearest || nearest == 0.0D) {
                                    hitEntity = entity;
                                    nearest = dist;
                                    nonPenImpact = hitMop.get();
                                }
                            }
                        }
                    }
                }

                // if not, only run it for the closest MOP
                if (!this.doesPenetrate() && hitEntity != null) {
                    mop = new EntityHitResult(hitEntity, nonPenImpact);
                }
            }

            if (mop != null) {
                if (mop instanceof BlockHitResult bhr && this.level().getBlockState(bhr.getBlockPos()).is(Blocks.NETHER_PORTAL)) {
                    //? if < 1.21.1 {
                    this.handleInsidePortal(bhr.getBlockPos());
                    //?} else {
                    /*this.setAsInsidePortal((net.minecraft.world.level.block.Portal) Blocks.NETHER_PORTAL, bhr.getBlockPos());
                    *///?}
                } else {
                    this.onImpact(mop);
                }
            }

            if (this.isRemoved()) return;

            motion = this.getDeltaMovement();

            if (!this.onGround()) {
                float hyp = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
                float yaw = (float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);
                float pitch = (float) (Math.atan2(motion.y, hyp) * 180.0D / Math.PI);

                while (pitch - this.xRotO < -180.0F) this.xRotO -= 360.0F;
                while (pitch - this.xRotO >= 180.0F) this.xRotO += 360.0F;
                while (yaw - this.yRotO < -180.0F) this.yRotO -= 360.0F;
                while (yaw - this.yRotO >= 180.0F) this.yRotO += 360.0F;

                this.setXRot(this.xRotO + (pitch - this.xRotO) * 0.2F);
                this.setYRot(this.yRotO + (yaw - this.yRotO) * 0.2F);
            }

            float drag = this.getAirDrag();
            double gravity = this.getGravityVelocity();

            if (fullBlockCollisions()) {
                this.move(MoverType.SELF, new Vec3(motion.x * motionMult(), motion.y * motionMult(), motion.z * motionMult()));
            } else {
                this.setPos(this.getX() + motion.x * motionMult(), this.getY() + motion.y * motionMult(), this.getZ() + motion.z * motionMult());
            }

            if (this.isInWater()) {
                for (int i = 0; i < 4; ++i) {
                    float f = 0.25F;
                    this.level().addParticle(ParticleTypes.BUBBLE, this.getX() - motion.x * f, this.getY() - motion.y * f, this.getZ() - motion.z * f, motion.x, motion.y, motion.z);
                }

                drag = this.getWaterDrag();
            }

            this.setDeltaMovement(motion.x * drag, motion.y * drag - gravity, motion.z * drag);
        }
    }

    public boolean fullBlockCollisions() { return false; }
    public boolean doesImpactEntities() { return true; }
    public boolean doesPenetrate() { return false; }
    public boolean isSpectral() { return false; }
    public int selfDamageDelay() { return 5; }

    public void getStuck(BlockPos pos, int side) {
        this.stuckPos = pos;
        this.stuckBlock = this.level().getBlockState(pos).getBlock();
        this.inGround = true;
        this.setDeltaMovement(Vec3.ZERO);
        this.setStuckIn(side);
        if (this.level() instanceof ServerLevel) this.hasImpulse = true;
    }

    public double getGravityVelocity() { return 0.03D; }

    protected abstract void onImpact(HitResult mop);

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("xTile", this.stuckPos.getX());
        nbt.putInt("yTile", this.stuckPos.getY());
        nbt.putInt("zTile", this.stuckPos.getZ());
        nbt.putByte("shake", (byte) this.throwableShake);
        nbt.putByte("inGround", (byte) (this.inGround ? 1 : 0));
        nbt.putInt("ticksInGround", this.ticksInGround);
        nbt.putInt("ticksInAir", this.ticksInAir);
        if (this.throwerId != null) nbt.putUUID("owner", this.throwerId);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.stuckPos = new BlockPos(nbt.getInt("xTile"), nbt.getInt("yTile"), nbt.getInt("zTile"));
        this.throwableShake = nbt.getByte("shake") & 255;
        this.inGround = nbt.getByte("inGround") == 1;
        if (this.inGround) this.stuckBlock = this.level().getBlockState(this.stuckPos).getBlock();
        this.ticksInGround = nbt.getInt("ticksInGround");
        this.ticksInAir = nbt.getInt("ticksInAir");
        this.throwerId = nbt.hasUUID("owner") ? nbt.getUUID("owner") : null;
    }

    public void setThrower(@Nullable LivingEntity thrower) {
        this.thrower = thrower;
        this.throwerId = thrower != null ? thrower.getUUID() : null;
    }

    @Nullable
    public LivingEntity getThrower() {
        if (this.thrower == null && this.throwerId != null && this.level() instanceof ServerLevel sl
                && sl.getEntity(this.throwerId) instanceof LivingEntity living) {
            this.thrower = living;
        }
        return this.thrower;
    }

    /** Original {@code getDamage(name)}: mit Werfer {@code EntityDamageSourceIndirect(name, this, thrower)}, sonst ohne Verursacher. */
    protected net.minecraft.world.damagesource.DamageSource getDamage(net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> key) {
        var holder = this.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(key);
        LivingEntity t = this.getThrower();
        return t != null ? new net.minecraft.world.damagesource.DamageSource(holder, this, t) : new net.minecraft.world.damagesource.DamageSource(holder);
    }

    /* ================================== Additional Getters =====================================*/
    protected float getAirDrag() { return 0.99F; }
    protected float getWaterDrag() { return 0.8F; }
    protected int groundDespawn() { return 1200; }
}
