package com.hbm_m.entity.logic;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.EntityDamageUtil;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ka-52 Kampfhubschrauber.
 *
 * <p><b>Ablauf:</b> Steht nach dem Platzieren mit stehenden Rotoren am Boden. Rechtsklick laesst ihn
 * anlaufen und starten, er zieht das Fahrwerk ein und kreist um seinen Startpunkt. Feindliche Mobs
 * greift er mit der Kanone und Raketen an. Ein weiterer Rechtsklick schickt ihn zurueck, er landet am
 * Startpunkt und faehrt die Rotoren herunter.
 *
 * <p><b>Absturz:</b> Bei 0 Lebenspunkten verliert er den Auftrieb, dreht sich um die Hochachse,
 * kippt ab und brennt. Die Rotoren laufen langsam aus, beim Aufschlag explodiert er.
 *
 * <p>Winkel folgen der Minecraft-Konvention: vorn ist {@code (-sin yaw, 0, cos yaw)}, positiver Pitch
 * neigt die Nase nach unten.
 */
public class EntityKa52 extends EntityPlaneBase {

    public static final byte PARKED = 0;
    public static final byte TAKEOFF = 1;
    public static final byte PATROL = 2;
    public static final byte ATTACK = 3;
    public static final byte LANDING = 4;
    public static final byte CRASHING = 5;

    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(EntityKa52.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> ROTOR = SynchedEntityData.defineId(EntityKa52.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GEAR = SynchedEntityData.defineId(EntityKa52.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROLL = SynchedEntityData.defineId(EntityKa52.class, EntityDataSerializers.FLOAT);

    private static final float MAX_HEALTH = 200.0F;
    private static final double CRUISE_HEIGHT = 25.0D;
    private static final double PATROL_RADIUS = 60.0D;
    private static final double CRUISE_SPEED = 0.8D;
    private static final double TARGET_RANGE = 64.0D;
    private static final double ATTACK_STANDOFF = 30.0D;
    private static final double ATTACK_HEIGHT = 12.0D;
    private static final float TURN_RATE = 4.0F;
    private static final float ROTOR_SPOOL = 0.01F;
    private static final float GEAR_SPEED = 0.03F;
    private static final double GEAR_UP_HEIGHT = 6.0D;
    private static final double RESPONSE = 0.08D;
    private static final float GUN_DAMAGE = 3.0F;
    private static final int GUN_INTERVAL = 3;
    private static final int ROCKET_INTERVAL = 60;
    private static final float ROCKET_DAMAGE = 14.0F;
    private static final int SOUND_INTERVAL = 40;

    private double homeX;
    private double homeZ;
    private boolean homeSet;
    private double patrolAngle;
    private boolean returnRequested;
    private float crashSpin;
    private int gunCooldown;
    private int rocketCooldown;
    @Nullable
    private Mob target;

    private float rotorAngle;
    private float lastRotorAngle;

    public EntityKa52(EntityType<? extends EntityKa52> type, Level level) {
        super(type, level);
    }

    @Override public float getMaxHealth() { return MAX_HEALTH; }
    @Override public int getLifetime() { return Integer.MAX_VALUE; }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, PARKED);
        this.entityData.define(ROTOR, 0.0F);
        this.entityData.define(GEAR, 0.0F);
        this.entityData.define(ROLL, 0.0F);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, PARKED);
        builder.define(ROTOR, 0.0F);
        builder.define(GEAR, 0.0F);
        builder.define(ROLL, 0.0F);
    }
    *///?}

    public byte getState() { return this.entityData.get(STATE); }
    /** 0 = stehend, 1 = volle Drehzahl. */
    public float getRotorSpeed() { return this.entityData.get(ROTOR); }
    /** 0 = ausgefahren, 1 = eingezogen. */
    public float getGearRetraction() { return this.entityData.get(GEAR); }
    public float getRoll() { return this.entityData.get(ROLL); }

    public float getRotorAngle(float partialTick) {
        return Mth.lerp(partialTick, this.lastRotorAngle, this.rotorAngle);
    }

    private void setState(byte state) { this.entityData.set(STATE, state); }

    @Override
    public void tick() {
        if (level().isClientSide) {
            clientTick();
        } else if (level() instanceof ServerLevel server) {
            this.entityData.set(HEALTH, this.health);
            serverTick(server);
        }
    }

    private void clientTick() {
        this.health = this.entityData.get(HEALTH);
        this.lastRotorAngle = this.rotorAngle;
        this.rotorAngle += getRotorSpeed() * 55.0F;
        if (this.turnProgress > 0) {
            double x = getX() + (this.syncPosX - getX()) / this.turnProgress;
            double y = getY() + (this.syncPosY - getY()) / this.turnProgress;
            double z = getZ() + (this.syncPosZ - getZ()) / this.turnProgress;
            setYRot((float) (getYRot() + Mth.wrapDegrees(this.syncYaw - getYRot()) / this.turnProgress));
            setXRot((float) (getXRot() + (this.syncPitch - getXRot()) / this.turnProgress));
            this.turnProgress--;
            setPos(x, y, z);
        }
    }

    private void serverTick(ServerLevel server) {
        if (!this.homeSet) {
            this.homeX = getX();
            this.homeZ = getZ();
            this.homeSet = true;
        }
        if (this.gunCooldown > 0) this.gunCooldown--;
        if (this.rocketCooldown > 0) this.rocketCooldown--;

        if (this.health <= 0.0F && getState() != CRASHING) {
            setState(CRASHING);
        }

        switch (getState()) {
            case PARKED -> tickParked();
            case TAKEOFF -> tickTakeoff(server);
            case PATROL -> tickPatrol(server);
            case ATTACK -> tickAttack(server);
            case LANDING -> tickLanding(server);
            default -> {
                tickCrash(server);
                if (isRemoved()) return;
            }
        }

        updateGear(server);
        if (getRotorSpeed() > 0.3F && this.tickCount % SOUND_INTERVAL == 0 && getState() != CRASHING) {
            playSound(server, "hbm:entity.chopperFlyingLoop", 5.0F, 0.6F + getRotorSpeed() * 0.4F);
        }
        if (getState() != PARKED) {
            loadNeighboringChunks(Mth.floor(getX() / 16.0D), Mth.floor(getZ() / 16.0D));
        }
    }

    // -- Flugphasen ------------------------------------------------------------

    private void tickParked() {
        setRotor(Math.max(0.0F, getRotorSpeed() - ROTOR_SPOOL));
        setDeltaMovement(Vec3.ZERO);
        setXRot(0.0F);
        this.entityData.set(ROLL, 0.0F);
    }

    private void tickTakeoff(ServerLevel server) {
        setRotor(Math.min(1.0F, getRotorSpeed() + ROTOR_SPOOL));
        if (getRotorSpeed() < 0.9F) {
            return;
        }
        fly(server, new Vec3(0.0D, 0.25D, 0.0D), getYRot());
        if (heightAboveGround(server) >= CRUISE_HEIGHT) {
            setState(PATROL);
        }
    }

    private void tickPatrol(ServerLevel server) {
        if (this.returnRequested) {
            setState(LANDING);
            return;
        }
        if (this.tickCount % 20 == 0) {
            this.target = findTarget(server);
            if (this.target != null) {
                setState(ATTACK);
                playSound(server, "hbm:entity.chopperCharge", 4.0F, 1.0F);
                return;
            }
        }
        this.patrolAngle += CRUISE_SPEED / PATROL_RADIUS;
        Vec3 waypoint = new Vec3(this.homeX + Math.cos(this.patrolAngle) * PATROL_RADIUS, 0.0D,
                this.homeZ + Math.sin(this.patrolAngle) * PATROL_RADIUS);
        Vec3 flat = new Vec3(waypoint.x - getX(), 0.0D, waypoint.z - getZ());
        Vec3 wanted = flat.lengthSqr() > 1.0D ? flat.normalize().scale(CRUISE_SPEED) : Vec3.ZERO;
        fly(server, holdHeight(server, wanted, CRUISE_HEIGHT), yawOf(flat));
    }

    private void tickAttack(ServerLevel server) {
        if (this.returnRequested || this.target == null || !this.target.isAlive()
                || this.target.distanceToSqr(this) > TARGET_RANGE * TARGET_RANGE * 4.0D) {
            this.target = null;
            setState(PATROL);
            return;
        }
        Mob mob = this.target;
        Vec3 flat = new Vec3(mob.getX() - getX(), 0.0D, mob.getZ() - getZ());
        double distance = flat.length();
        Vec3 wanted = Vec3.ZERO;
        if (distance > ATTACK_STANDOFF + 4.0D) {
            wanted = flat.normalize().scale(CRUISE_SPEED);
        } else if (distance < ATTACK_STANDOFF - 4.0D) {
            wanted = flat.normalize().scale(-0.3D);
        }
        double hover = mob.getY() + ATTACK_HEIGHT - groundAt(server);
        fly(server, holdHeight(server, wanted, Math.max(ATTACK_HEIGHT, hover)), yawOf(flat));

        float offNose = Math.abs(Mth.degreesDifference(getYRot(), yawOf(flat)));
        if (offNose < 15.0F && canSee(server, mob)) {
            fireCannon(server, mob);
            fireRockets(server, mob);
        }
    }

    private void tickLanding(ServerLevel server) {
        Vec3 flat = new Vec3(this.homeX - getX(), 0.0D, this.homeZ - getZ());
        double distance = flat.length();
        if (distance > 3.0D) {
            Vec3 wanted = flat.normalize().scale(Math.min(CRUISE_SPEED, distance * 0.05D + 0.1D));
            fly(server, holdHeight(server, wanted, CRUISE_HEIGHT), yawOf(flat));
            return;
        }
        fly(server, new Vec3(0.0D, -0.15D, 0.0D), getYRot());
        double height = heightAboveGround(server);
        if (height <= 0.05D) {
            setPos(getX(), groundAt(server), getZ());
            setDeltaMovement(Vec3.ZERO);
            this.returnRequested = false;
            setState(PARKED);
        }
    }

    private void tickCrash(ServerLevel server) {
        setRotor(Math.max(0.15F, getRotorSpeed() - 0.004F));
        this.crashSpin = Math.min(14.0F, this.crashSpin + 0.25F);
        setYRot(getYRot() + this.crashSpin);
        setXRot(Mth.approach(getXRot(), 18.0F, 0.4F));
        this.entityData.set(ROLL, Mth.approach(getRoll(), 22.0F, 0.5F));

        Vec3 v = getDeltaMovement();
        Vec3 next = new Vec3(v.x * 0.985D, Math.max(-1.2D, v.y - 0.035D), v.z * 0.985D);
        setDeltaMovement(next);
        setPos(getX() + next.x, getY() + next.y, getZ() + next.z);

        for (int i = 0; i < 6; i++) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX() + this.random.nextGaussian(), getY() + 2.5D,
                    getZ() + this.random.nextGaussian(), 1, 0.2D, 0.2D, 0.2D, 0.02D);
        }
        server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 2.0D, getZ(), 8, 0.8D, 0.5D, 0.8D, 0.02D);
        if (this.tickCount % SOUND_INTERVAL == 0) {
            playSound(server, "hbm:entity.chopperCrashingLoop", 6.0F, 1.0F);
        }

        boolean hitGround = !level().getBlockState(blockPosition()).isAir()
                || heightAboveGround(server) <= 0.2D || getY() < level().getMinBuildHeight();
        if (hitGround) {
            discard();
            new ExplosionVNT(level(), getX(), getY(), getZ(), 15.0F).makeStandard().explode();
            playSound(server, "hbm:entity.planeCrash", 25.0F, 1.0F);
        }
    }

    // -- Bewegung --------------------------------------------------------------

    /** Glaettet die Geschwindigkeit und leitet Neigung und Schraeglage daraus ab, wie bei einem echten Helikopter. */
    private void fly(ServerLevel server, Vec3 wanted, float wantedYaw) {
        setRotor(Math.min(1.0F, getRotorSpeed() + ROTOR_SPOOL));
        Vec3 velocity = getDeltaMovement().lerp(wanted, RESPONSE);
        setDeltaMovement(velocity);
        setPos(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);

        float before = getYRot();
        setYRot(Mth.approachDegrees(before, wantedYaw, TURN_RATE));
        float turn = Mth.degreesDifference(before, getYRot());

        double forward = velocity.x * -Math.sin(Math.toRadians(getYRot())) + velocity.z * Math.cos(Math.toRadians(getYRot()));
        setXRot(Mth.approach(getXRot(), (float) (forward * 14.0D), 1.0F));
        this.entityData.set(ROLL, Mth.approach(getRoll(), Mth.clamp(turn * 5.0F, -20.0F, 20.0F), 1.5F));
    }

    private Vec3 holdHeight(ServerLevel server, Vec3 horizontal, double height) {
        double error = groundAt(server) + height - getY();
        return new Vec3(horizontal.x, Mth.clamp(error * 0.08D, -0.4D, 0.4D), horizontal.z);
    }

    private double groundAt(ServerLevel server) {
        return server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(getX()), Mth.floor(getZ()));
    }

    private double heightAboveGround(ServerLevel server) {
        return getY() - groundAt(server);
    }

    private static float yawOf(Vec3 direction) {
        return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
    }

    private void setRotor(float value) {
        this.entityData.set(ROTOR, value);
    }

    private void updateGear(ServerLevel server) {
        boolean up = getState() != PARKED && getState() != CRASHING
                && !(getState() == LANDING && heightAboveGround(server) < GEAR_UP_HEIGHT * 2.0D)
                && heightAboveGround(server) > GEAR_UP_HEIGHT;
        this.entityData.set(GEAR, Mth.approach(getGearRetraction(), up ? 1.0F : 0.0F, GEAR_SPEED));
    }

    // -- Waffen ----------------------------------------------------------------

    @Nullable
    private Mob findTarget(ServerLevel server) {
        AABB area = getBoundingBox().inflate(TARGET_RANGE);
        List<Mob> mobs = server.getEntitiesOfClass(Mob.class, area, mob -> mob instanceof Enemy && mob.isAlive());
        Mob best = null;
        double bestSq = TARGET_RANGE * TARGET_RANGE;
        for (Mob mob : mobs) {
            double distSq = mob.distanceToSqr(this);
            if (distSq < bestSq && canSee(server, mob)) {
                bestSq = distSq;
                best = mob;
            }
        }
        return best;
    }

    private boolean canSee(ServerLevel server, Entity entity) {
        return server.clip(new ClipContext(position().add(0.0D, 0.5D, 0.0D), entity.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private Vec3 nosePoint() {
        double yaw = Math.toRadians(getYRot());
        return position().add(-Math.sin(yaw) * 5.0D, 0.4D, Math.cos(yaw) * 5.0D);
    }

    private void fireCannon(ServerLevel server, Mob mob) {
        if (this.gunCooldown > 0) {
            return;
        }
        this.gunCooldown = GUN_INTERVAL;
        EntityDamageUtil.attackEntityFromIgnoreIFrame(mob, ModDamageSources.shrapnel(server), GUN_DAMAGE);
        Vec3 muzzle = nosePoint();
        Vec3 path = mob.getEyePosition().subtract(muzzle);
        for (double f = 0.1D; f < 1.0D; f += 0.15D) {
            Vec3 point = muzzle.add(path.scale(f));
            server.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        playSound(server, "hbm:turret.howard_fire", 3.0F, 1.2F + this.random.nextFloat() * 0.2F);
    }

    private void fireRockets(ServerLevel server, Mob mob) {
        if (this.rocketCooldown > 0) {
            return;
        }
        this.rocketCooldown = ROCKET_INTERVAL;
        mob.hurt(ModDamageSources.shrapnel(server), ROCKET_DAMAGE);
        server.explode(this, mob.getX(), mob.getY(), mob.getZ(), 2.5F, Level.ExplosionInteraction.NONE);
        Vec3 muzzle = position().add(0.0D, 0.4D, 0.0D);
        Vec3 path = mob.getEyePosition().subtract(muzzle);
        for (double f = 0.05D; f < 1.0D; f += 0.05D) {
            Vec3 point = muzzle.add(path.scale(f));
            server.sendParticles(ParticleTypes.SMOKE, point.x, point.y, point.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
        }
        playSound(server, "hbm:turret.richard_fire", 4.0F, 1.0F);
    }

    private void playSound(ServerLevel server, String sound, float volume, float pitch) {
        server.playSound(null, getX(), getY(), getZ(), HbmSoundsNT.get(sound), SoundSource.NEUTRAL, volume, pitch);
    }

    // -- Bedienung -------------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            byte state = getState();
            if (state == PARKED) {
                setState(TAKEOFF);
            } else if (state != CRASHING) {
                this.returnRequested = true;
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    // -- Speichern -------------------------------------------------------------

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.homeX = nbt.getDouble("homeX");
        this.homeZ = nbt.getDouble("homeZ");
        this.homeSet = nbt.getBoolean("homeSet");
        this.patrolAngle = nbt.getDouble("patrolAngle");
        this.returnRequested = nbt.getBoolean("returning");
        setState(nbt.getByte("flightState"));
        setRotor(nbt.getFloat("rotor"));
        this.entityData.set(GEAR, nbt.getFloat("gear"));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putDouble("homeX", this.homeX);
        nbt.putDouble("homeZ", this.homeZ);
        nbt.putBoolean("homeSet", this.homeSet);
        nbt.putDouble("patrolAngle", this.patrolAngle);
        nbt.putBoolean("returning", this.returnRequested);
        nbt.putByte("flightState", getState());
        nbt.putFloat("rotor", getRotorSpeed());
        nbt.putFloat("gear", getGearRetraction());
    }
}
