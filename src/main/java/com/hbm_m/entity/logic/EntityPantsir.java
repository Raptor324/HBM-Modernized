package com.hbm_m.entity.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.missile.MissileAntiBallisticEntity;
import com.hbm_m.entity.missile.MissileBaseEntity;
import com.hbm_m.entity.missile.MissilePantsirEntity;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.handler.EnumKeybind;
import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.interfaces.IOwnedMachine;
import com.hbm_m.item.ModItems;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.server.missile.MissileTrackBroadcaster;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.EntityDamageUtil;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Pantsir-S1: fahrbares Flugabwehrsystem auf KamAZ-Fahrgestell.
 *
 * <p><b>Fahren.</b> Rechtsklick setzt den Spieler ans Steuer. W beschleunigt, S bremst und faehrt
 * rueckwaerts, A/D lenken die beiden Vorderachsen. Der Server rechnet die Fahrt, das Fahrzeug folgt dem
 * Gelaende und steigt einzelne Bloecke hoch. Das Waffensystem arbeitet waehrend der Fahrt weiter, die
 * rechte Waffentaste schaltet es vom Fahrersitz aus um.
 *
 * <p><b>Sensorik.</b> Der Radarkopf dreht sich und sieht nur, was sein Strahl gerade ueberstreicht, bis
 * 20 km weit. Blind ist er in drei Faellen: steil ueber dem Fahrzeug, unter dem Horizont und hinter
 * Gelaende. Ein erfasstes Ziel wird verfolgt, bis es laenger als {@link #TRACK_GRACE} Ticks in einem
 * toten Winkel steckt, danach faengt die Suche von vorn an.
 *
 * <p><b>Bedrohung.</b> Als Ziel gilt jede sinkende Rakete und jede, deren Zielpunkt im Schutzbereich
 * von {@link #DEFENDED_RADIUS} liegt. Eigene Raketen, die woandershin steigen, bleiben unbehelligt.
 * Flugzeuge wie Bomber und C-130 sind ebenfalls Ziele.
 *
 * <p><b>Freund-Feind-Erkennung.</b> Raketen und Flugzeuge tragen den Spieler, der sie gestartet oder
 * gerufen hat. Gehoert er dem Besitzer dieses Fahrzeugs oder dessen Team, bleibt das Ziel unbehelligt.
 * Ziele ohne bekannte Herkunft gelten als feindlich. Schleichen und Rechtsklick schaltet das ab.
 *
 * <p><b>Bewaffnung.</b> 57E6-Flugkoerper bis 12 km, die 30-mm-Kanonen nur im Nahbereich gegen Raketen,
 * die durchkommen, und gegen feindliche Mobs. Beides braucht Munition: per Rechtsklick mit einer 57E6
 * oder einem Gurt ammo_dgk nachladen.
 *
 * <p>Turm und Werfer sind Winkel im Fahrzeugraum, 0 Grad zeigt zur Kabine. Der Server setzt nur die
 * Sollwinkel, beide Seiten drehen mit derselben Geschwindigkeit darauf zu.
 */
public class EntityPantsir extends Entity implements IOwnedMachine {

    /** Lage der Drehachsen im Fahrzeugraum, aus dem Modell gemessen. */
    public static final double TURRET_AXIS_Z = -2.35D;
    public static final double RADAR_AXIS_Z = -3.62D;
    public static final double LAUNCHER_AXIS_Y = 3.7D;

    public static final int MAX_AMMO = 12;
    /** Kanonenmunition: ein Gurt (ammo_dgk) sind 100 Schuss, wie beim Howard. */
    public static final int MAX_SHELLS = 1_000;
    public static final int SHELLS_PER_BELT = 100;

    private static final EntityDataAccessor<Float> TARGET_YAW = SynchedEntityData.defineId(EntityPantsir.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_PITCH = SynchedEntityData.defineId(EntityPantsir.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> LOCKED = SynchedEntityData.defineId(EntityPantsir.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(EntityPantsir.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> STEER = SynchedEntityData.defineId(EntityPantsir.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(EntityPantsir.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROLL = SynchedEntityData.defineId(EntityPantsir.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(EntityPantsir.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> AMMO = SynchedEntityData.defineId(EntityPantsir.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHELLS = SynchedEntityData.defineId(EntityPantsir.class, EntityDataSerializers.INT);

    private static final float MAX_HEALTH = 300.0F;

    /** 1 Block = 1 m: Radar 20 km, Flugkoerper 12 km, 30-mm-Kanonen nur fuer den Nahbereich. */
    private static final double RADAR_RANGE = 20_000.0D;
    private static final double MISSILE_RANGE = 12_000.0D;
    private static final double GUN_RANGE = 60.0D;
    private static final double DEFENDED_RADIUS = 3_000.0D;

    /** Halbe Breite des Radarstrahls in Grad. */
    private static final float BEAM_WIDTH = 25.0F;
    /** Toter Winkel: darueber sieht das Radar nichts mehr, die Werfer kommen auch nicht hoeher. */
    private static final float MAX_RADAR_ELEVATION = 72.0F;
    /** Ziele unter dieser Elevation verschwinden im Bodenecho. */
    private static final float MIN_RADAR_ELEVATION = -3.0F;
    private static final int TRACK_GRACE = 60;
    /** Gelaendeabschattung wird nur im Nahfeld und nur in geladenen Chunks geprueft. */
    private static final int MASK_CHECK_RANGE = 800;
    private static final int MASK_CHECK_STEP = 12;
    private static final double RADAR_HEIGHT = 2.0D;

    private static final float YAW_SPEED = 7.0F;
    private static final float PITCH_SPEED = 5.0F;
    private static final float MAX_PITCH = 72.0F;
    private static final float MIN_PITCH = -5.0F;
    private static final float AIM_TOLERANCE = 6.0F;

    /** Leerlauf: langsames Absuchen der Umgebung in zufaelligen Schwenks. */
    private static final float IDLE_YAW_SPEED = 2.0F;
    private static final float IDLE_PITCH_SPEED = 1.0F;
    private static final float IDLE_MAX_SWING = 120.0F;
    private static final float IDLE_MAX_PITCH = 30.0F;
    private static final int CHUNK_REFRESH_TICKS = 200;
    private static final int IDLE_MIN_TICKS = 60;
    private static final int IDLE_SPREAD_TICKS = 140;

    private static final float RADAR_SPEED_IDLE = 12.0F;
    private static final float RADAR_SPEED_LOCKED = 24.0F;

    private static final int SCAN_INTERVAL = 5;
    private static final int LAUNCH_COOLDOWN = 20;
    private static final int GUN_INTERVAL = 2;
    private static final float GUN_DAMAGE = 3.0F;
    private static final int MISSILES_PER_TARGET = 2;
    /** Lebt das Ziel danach noch, gilt die Salve als fehlgegangen. */
    private static final int SALVO_TIMEOUT = 120;

    /** Rohre im Werferraum: Seite (1 oder -1), Spalte und Reihe. */
    private static final double[] TUBE_COLUMNS = {1.12D, 1.46D};
    private static final double[] TUBE_ROWS = {-0.32D, 0.0D, 0.32D};
    private static final double MUZZLE_FORWARD = 3.3D;
    private static final double GUN_LATERAL = 0.95D;

    /** Fahrwerte: Hoechstfahrt rund 65 km/h vorwaerts, 18 km/h rueckwaerts. */
    private static final double MAX_SPEED = 0.9D;
    private static final double MAX_REVERSE = 0.25D;
    private static final double ACCELERATION = 0.012D;
    private static final double BRAKING = 0.03D;
    private static final double ROLLING_DRAG = 0.985D;
    private static final double WHEELBASE = 6.0D;
    private static final float MAX_STEER = 30.0F;
    private static final float STEER_RATE = 3.0F;
    private static final double GRAVITY = 0.08D;
    private static final double GROUND_PROBE_LENGTH = 4.0D;
    private static final double GROUND_PROBE_WIDTH = 1.3D;
    private static final float ATTITUDE_RATE = 1.2F;
    private static final double SEAT_X = 0.6D;
    private static final double SEAT_Y = 1.1D;
    private static final double SEAT_Z = 3.9D;

    private int nextTube;
    private float yaw;
    private float pitch;
    private float lastYaw;
    private float lastPitch;
    private float radar;
    private float lastRadar;
    private float wheelAngle;
    private float lastWheelAngle;

    private int launchCooldown;
    private int gunCooldown;

    @Nullable
    private Entity target;
    private final Map<UUID, Integer> shotsAtTarget = new HashMap<>();
    private final Map<UUID, Long> lastShotAtTarget = new HashMap<>();
    private final List<EntityPlaneBase> planes = new ArrayList<>();
    private int blindTicks;
    private int idleTimer;
    private boolean iffEnabled = true;
    private boolean toggleKeyHeld;
    private double speed;
    @Nullable
    private UUID ownerUuid;

    private boolean chunkHeld;
    private int heldChunkX;
    private int heldChunkZ;

    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private float lerpYaw;
    private float lerpPitch;

    public EntityPantsir(EntityType<? extends EntityPantsir> type, Level level) {
        super(type, level);
        //? if < 1.21.1 {
        setMaxUpStep(1.1F);
        //?}
    }

    //? if >= 1.21.1 {
    /*@Override
    public float maxUpStep() {
        return 1.1F;
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(TARGET_YAW, 0.0F);
        this.entityData.define(TARGET_PITCH, 0.0F);
        this.entityData.define(LOCKED, false);
        this.entityData.define(ACTIVE, true);
        this.entityData.define(STEER, 0.0F);
        this.entityData.define(SPEED, 0.0F);
        this.entityData.define(ROLL, 0.0F);
        this.entityData.define(HEALTH, MAX_HEALTH);
        this.entityData.define(AMMO, MAX_AMMO);
        this.entityData.define(SHELLS, MAX_SHELLS / 2);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TARGET_YAW, 0.0F);
        builder.define(TARGET_PITCH, 0.0F);
        builder.define(LOCKED, false);
        builder.define(ACTIVE, true);
        builder.define(STEER, 0.0F);
        builder.define(SPEED, 0.0F);
        builder.define(ROLL, 0.0F);
        builder.define(HEALTH, MAX_HEALTH);
        builder.define(AMMO, MAX_AMMO);
        builder.define(SHELLS, MAX_SHELLS / 2);
    }
    *///?}

    /** Uebernimmt den Stand aus der frueheren Block-Version. */
    public void restore(boolean active, boolean iff, int ammo, int shells, @Nullable UUID owner) {
        this.entityData.set(ACTIVE, active);
        this.iffEnabled = iff;
        this.entityData.set(AMMO, Mth.clamp(ammo, 0, MAX_AMMO));
        this.entityData.set(SHELLS, Mth.clamp(shells, 0, MAX_SHELLS));
        this.ownerUuid = owner;
    }

    public boolean isActive() { return this.entityData.get(ACTIVE); }
    public boolean isLocked() { return this.entityData.get(LOCKED); }
    public float getSteer() { return this.entityData.get(STEER); }
    /** Fahrt in Bloecken pro Tick, rueckwaerts negativ. */
    public float getDriveSpeed() { return this.entityData.get(SPEED); }
    public float getRoll() { return this.entityData.get(ROLL); }
    public float getHealthFraction() { return this.entityData.get(HEALTH) / MAX_HEALTH; }
    public int getAmmo() { return this.entityData.get(AMMO); }
    public int getShells() { return this.entityData.get(SHELLS); }

    public float getTurretYaw(float partialTick) { return Mth.rotLerp(partialTick, this.lastYaw, this.yaw); }
    public float getLauncherPitch(float partialTick) { return Mth.lerp(partialTick, this.lastPitch, this.pitch); }
    public float getRadarYaw(float partialTick) { return Mth.rotLerp(partialTick, this.lastRadar, this.radar); }
    public float getWheelAngle(float partialTick) { return Mth.lerp(partialTick, this.lastWheelAngle, this.wheelAngle); }

    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public boolean canBeCollidedWith() { return !isRemoved(); }
    @Override public boolean isPushable() { return false; }

    @Override
    public void tick() {
        super.tick();
        animate();
        if (level().isClientSide) {
            clientMove();
            exhaust();
            com.hbm_m.client.PantsirClient.onEntityTick(this);
        } else if (level() instanceof ServerLevel server) {
            drive(server);
            serverTick(server);
        }
    }

    private void animate() {
        boolean locked = isLocked();
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;
        this.lastRadar = this.radar;
        this.lastWheelAngle = this.wheelAngle;
        this.yaw = Mth.approachDegrees(this.yaw, this.entityData.get(TARGET_YAW), locked ? YAW_SPEED : IDLE_YAW_SPEED);
        this.pitch = Mth.approach(this.pitch, this.entityData.get(TARGET_PITCH), locked ? PITCH_SPEED : IDLE_PITCH_SPEED);
        if (isActive()) {
            this.radar = Mth.wrapDegrees(this.radar + (locked ? RADAR_SPEED_LOCKED : RADAR_SPEED_IDLE));
        }
        this.wheelAngle += (float) Math.toDegrees(getDriveSpeed() / 0.728D);
    }

    private void clientMove() {
        if (this.lerpSteps > 0) {
            double x = getX() + (this.lerpX - getX()) / this.lerpSteps;
            double y = getY() + (this.lerpY - getY()) / this.lerpSteps;
            double z = getZ() + (this.lerpZ - getZ()) / this.lerpSteps;
            setYRot(getYRot() + Mth.wrapDegrees(this.lerpYaw - getYRot()) / this.lerpSteps);
            setXRot(getXRot() + (this.lerpPitch - getXRot()) / this.lerpSteps);
            this.lerpSteps--;
            setPos(x, y, z);
        }
    }

    @Override
    //? if < 1.21.1 {
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
    //?} else {
    /*public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
    *///?}
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYaw = yRot;
        this.lerpPitch = xRot;
        this.lerpSteps = steps + 2;
    }

    /** Dieselqualm aus dem Auspuff hinter der Kabine, unter Last dichter, dazu Staub an den Raedern. */
    private void exhaust() {
        double speedAbs = Math.abs(getDriveSpeed());
        double heading = Math.toRadians(getYRot());
        double sin = -Math.sin(heading);
        double cos = Math.cos(heading);
        if (this.tickCount % (speedAbs > 0.1D ? 2 : 6) == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX() + sin * 2.8D + cos * 1.1D, getY() + 3.2D,
                    getZ() + cos * 2.8D - sin * 1.1D, 0.0D, 0.06D, 0.0D);
        }
        if (speedAbs > 0.25D && onGround()) {
            for (int side = -1; side <= 1; side += 2) {
                level().addParticle(ParticleTypes.POOF, getX() - sin * 3.8D + cos * side * 1.1D, getY() + 0.1D,
                        getZ() - cos * 3.8D - sin * side * 1.1D, 0.0D, 0.02D, 0.0D);
            }
        }
        if (getHealthFraction() < 0.5F) {
            level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY() + 3.0D, getZ(), 0.0D, 0.1D, 0.0D);
        }
    }

    // -- Fahren ----------------------------------------------------------------

    private void drive(ServerLevel server) {
        Player driver = getDriver();
        double throttle = 0.0D;
        double steering = 0.0D;
        if (driver != null) {
            HbmPlayerProps keys = HbmPlayerProps.get(driver);
            throttle = key(keys, EnumKeybind.HELI_FORWARD) - key(keys, EnumKeybind.HELI_BACK);
            steering = key(keys, EnumKeybind.HELI_LEFT) - key(keys, EnumKeybind.HELI_RIGHT);
            boolean toggle = keys.getKeyPressed(EnumKeybind.GUN_SECONDARY);
            if (toggle && !this.toggleKeyHeld) {
                toggle(server);
                reportStatus(driver);
            }
            this.toggleKeyHeld = toggle;
        }

        if (throttle > 0.0D) {
            this.speed = Math.min(MAX_SPEED, this.speed + (this.speed < 0.0D ? BRAKING : ACCELERATION));
        } else if (throttle < 0.0D) {
            this.speed = Math.max(-MAX_REVERSE, this.speed - (this.speed > 0.0D ? BRAKING : ACCELERATION));
        } else {
            this.speed *= ROLLING_DRAG;
            if (Math.abs(this.speed) < 0.004D) {
                this.speed = 0.0D;
            }
        }

        float steer = Mth.approach(getSteer(), (float) (steering * MAX_STEER), STEER_RATE);
        this.entityData.set(STEER, steer);
        if (Math.abs(this.speed) > 0.01D) {
            double turn = Math.toDegrees(this.speed * Math.tan(Math.toRadians(steer)) / WHEELBASE);
            setYRot(getYRot() - (float) turn);
        }

        double heading = Math.toRadians(getYRot());
        Vec3 velocity = getDeltaMovement();
        double fall = onGround() ? 0.0D : Math.max(-1.2D, velocity.y - GRAVITY);
        Vec3 step = new Vec3(-Math.sin(heading) * this.speed, onGround() ? -GRAVITY : fall, Math.cos(heading) * this.speed);
        move(MoverType.SELF, step);
        setDeltaMovement(step);
        if (this.horizontalCollision) {
            this.speed *= 0.4D;
        }
        this.entityData.set(SPEED, (float) this.speed);
        followGround(server, heading);
    }

    /** Neigt das Fahrzeug nach dem Gelaende unter Vorder- und Hinterachse sowie unter beiden Seiten. */
    private void followGround(ServerLevel server, double heading) {
        double fx = -Math.sin(heading);
        double fz = Math.cos(heading);
        double front = groundAt(server, getX() + fx * GROUND_PROBE_LENGTH, getZ() + fz * GROUND_PROBE_LENGTH);
        double rear = groundAt(server, getX() - fx * GROUND_PROBE_LENGTH, getZ() - fz * GROUND_PROBE_LENGTH);
        double left = groundAt(server, getX() + fz * GROUND_PROBE_WIDTH, getZ() - fx * GROUND_PROBE_WIDTH);
        double right = groundAt(server, getX() - fz * GROUND_PROBE_WIDTH, getZ() + fx * GROUND_PROBE_WIDTH);
        float slope = (float) Math.toDegrees(Math.atan2(Mth.clamp(rear - front, -3.0D, 3.0D), GROUND_PROBE_LENGTH * 2.0D));
        float bank = (float) Math.toDegrees(Math.atan2(Mth.clamp(right - left, -1.5D, 1.5D), GROUND_PROBE_WIDTH * 2.0D));
        setXRot(Mth.approach(getXRot(), slope, ATTITUDE_RATE));
        this.entityData.set(ROLL, Mth.approach(getRoll(), Mth.clamp(bank, -12.0F, 12.0F), ATTITUDE_RATE));
    }

    private static double groundAt(ServerLevel server, double x, double z) {
        return server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
    }

    private static double key(HbmPlayerProps keys, EnumKeybind key) {
        return keys.getKeyPressed(key) ? 1.0D : 0.0D;
    }

    @Nullable
    private Player getDriver() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction move) {
        double heading = Math.toRadians(getYRot());
        double sin = Math.sin(heading);
        double cos = Math.cos(heading);
        move.accept(passenger, getX() + SEAT_X * cos - SEAT_Z * sin, getY() + SEAT_Y, getZ() + SEAT_Z * cos + SEAT_X * sin);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        double heading = Math.toRadians(getYRot());
        double side = 2.2D;
        return new Vec3(getX() + side * Math.cos(heading) - SEAT_Z * Math.sin(heading), getY() + 0.2D,
                getZ() + SEAT_Z * Math.cos(heading) + side * Math.sin(heading));
    }

    // -- Waffensystem ----------------------------------------------------------

    private void serverTick(ServerLevel server) {
        keepChunkLoaded(server);
        if (this.launchCooldown > 0) {
            this.launchCooldown--;
        }
        if (this.gunCooldown > 0) {
            this.gunCooldown--;
        }

        boolean wasLocked = isLocked();
        if (!isActive()) {
            this.target = null;
        } else {
            updateTarget(server);
        }

        Entity current = this.target;
        this.entityData.set(LOCKED, current != null);
        if (current != null) {
            aimAt(current);
            engage(server, current);
        } else {
            if (wasLocked) {
                this.idleTimer = IDLE_MIN_TICKS;
            }
            this.shotsAtTarget.clear();
            this.lastShotAtTarget.clear();
            idleSweep(server);
        }
        if (current != null && !wasLocked) {
            playSound(server, "hbm:turret.sentry_lockon", 3.0F, 1.0F);
        }
    }

    /**
     * Ohne Ziel bleibt der Turm stehen, wo er zuletzt hingezeigt hat, und schwenkt von Zeit zu Zeit
     * gemaechlich in eine neue Richtung. In die Fahrstellung geht er nur, wenn die Anlage aus ist.
     */
    private void idleSweep(ServerLevel server) {
        if (!isActive()) {
            this.entityData.set(TARGET_YAW, 0.0F);
            this.entityData.set(TARGET_PITCH, 0.0F);
            return;
        }
        if (--this.idleTimer > 0) {
            return;
        }
        this.idleTimer = IDLE_MIN_TICKS + server.random.nextInt(IDLE_SPREAD_TICKS);
        float swing = (server.random.nextFloat() * 2.0F - 1.0F) * IDLE_MAX_SWING;
        this.entityData.set(TARGET_YAW, Mth.wrapDegrees(this.yaw + swing));
        this.entityData.set(TARGET_PITCH, server.random.nextFloat() * IDLE_MAX_PITCH);
    }

    /** Eine laufende Verfolgung halten, sonst das Radar und zuletzt die Kanonen nach Zielen suchen lassen. */
    private void updateTarget(ServerLevel server) {
        if (!isTargetValid(this.target) || isFriendly(server, this.target)) {
            this.target = null;
        }
        if (isAirTarget(this.target)) {
            this.blindTicks = canRadarSee(server, this.target) ? 0 : this.blindTicks + 1;
            if (this.blindTicks > TRACK_GRACE) {
                this.target = null;
                this.blindTicks = 0;
            }
            return;
        }
        Entity contact = sweepRadar(server);
        if (contact != null) {
            this.target = contact;
            this.blindTicks = 0;
        } else if (this.target == null && server.getGameTime() % SCAN_INTERVAL == 0) {
            this.target = findMob(server);
        }
    }

    /** Eigenes oder verbuendetes Luftziel. Offline-Spieler zaehlen nur ueber die eigene Kennung. */
    private boolean isFriendly(ServerLevel server, @Nullable Entity entity) {
        if (!this.iffEnabled || this.ownerUuid == null) {
            return false;
        }
        UUID sender = entity instanceof MissileBaseEntity missile ? missile.getLauncherOwner()
                : entity instanceof EntityPlaneBase plane ? plane.getCaller() : null;
        if (sender == null) {
            return false;
        }
        if (sender.equals(this.ownerUuid)) {
            return true;
        }
        Player owner = server.getPlayerByUUID(this.ownerUuid);
        Player other = server.getPlayerByUUID(sender);
        return owner != null && other != null && owner.isAlliedTo(other);
    }

    private static boolean isAirTarget(@Nullable Entity entity) {
        return entity instanceof MissileBaseEntity || entity instanceof EntityPlaneBase;
    }

    /** Naechste Bedrohung im aktuellen Radarstrahl. */
    @Nullable
    private Entity sweepRadar(ServerLevel server) {
        if (server.getGameTime() % SCAN_INTERVAL == 0) {
            this.planes.clear();
            for (Entity entity : server.getAllEntities()) {
                if (entity instanceof EntityPlaneBase plane && plane.isAlive()) {
                    this.planes.add(plane);
                }
            }
        }
        Vec3 origin = turretOrigin();
        float heading = bodyYaw() + this.yaw + this.radar;
        Entity best = null;
        double bestSq = RADAR_RANGE * RADAR_RANGE;
        for (MissileBaseEntity missile : MissileTrackBroadcaster.getActiveMissiles(server)) {
            double distSq = missile.position().distanceToSqr(origin);
            if (distSq < bestSq && isThreat(missile) && !isFriendly(server, missile)
                    && inBeam(server, missile, origin, heading)) {
                bestSq = distSq;
                best = missile;
            }
        }
        for (EntityPlaneBase plane : this.planes) {
            double distSq = plane.position().distanceToSqr(origin);
            if (distSq < bestSq && !plane.isRemoved() && !isFriendly(server, plane)
                    && inBeam(server, plane, origin, heading)) {
                bestSq = distSq;
                best = plane;
            }
        }
        return best;
    }

    private boolean inBeam(ServerLevel server, Entity entity, Vec3 origin, float heading) {
        Vec3 toTarget = entity.position().subtract(origin);
        float bearing = (float) Math.toDegrees(Math.atan2(toTarget.x, toTarget.z));
        return Math.abs(Mth.degreesDifference(heading, bearing)) <= BEAM_WIDTH && canRadarSee(server, entity);
    }

    private boolean isThreat(MissileBaseEntity missile) {
        if (missile.isRemoved() || missile instanceof MissileAntiBallisticEntity || !missile.canBeDetectedByRadar()) {
            return false;
        }
        if (missile.getDeltaMovement().y < 0.0D) {
            return true;
        }
        double dx = missile.getImpactX() - getX();
        double dz = missile.getImpactZ() - getZ();
        return dx * dx + dz * dz < DEFENDED_RADIUS * DEFENDED_RADIUS;
    }

    /** Falsch im toten Winkel: steil ueber dem Fahrzeug, unter dem Horizont oder hinter Gelaende. */
    private boolean canRadarSee(ServerLevel server, Entity entity) {
        Vec3 from = turretOrigin().add(0.0D, RADAR_HEIGHT, 0.0D);
        Vec3 delta = entity.position().subtract(from);
        double ground = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double elevation = Math.toDegrees(Math.atan2(delta.y, ground));
        if (elevation > MAX_RADAR_ELEVATION || elevation < MIN_RADAR_ELEVATION) {
            return false;
        }
        int steps = (int) (Math.min(ground, MASK_CHECK_RANGE) / MASK_CHECK_STEP);
        for (int i = 1; i <= steps; i++) {
            double t = i * MASK_CHECK_STEP / ground;
            int x = Mth.floor(from.x + delta.x * t);
            int z = Mth.floor(from.z + delta.z * t);
            if (server.hasChunk(x >> 4, z >> 4)
                    && server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) > from.y + delta.y * t) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private Entity findMob(ServerLevel server) {
        Vec3 origin = turretOrigin();
        Entity best = null;
        AABB area = getBoundingBox().inflate(GUN_RANGE);
        List<Mob> mobs = server.getEntitiesOfClass(Mob.class, area, mob -> mob instanceof Enemy && mob.isAlive());
        double bestSq = GUN_RANGE * GUN_RANGE;
        for (Mob mob : mobs) {
            double distSq = mob.getEyePosition().distanceToSqr(origin);
            if (distSq < bestSq && canSee(server, mob)) {
                bestSq = distSq;
                best = mob;
            }
        }
        return best;
    }

    private boolean isTargetValid(@Nullable Entity entity) {
        if (entity == null || entity.isRemoved() || !entity.isAlive() || entity.level() != level()) {
            return false;
        }
        double range = isAirTarget(entity) ? RADAR_RANGE : GUN_RANGE;
        return entity.position().distanceToSqr(turretOrigin()) < range * range;
    }

    private boolean canSee(ServerLevel server, LivingEntity entity) {
        Vec3 from = turretOrigin().add(0.0D, 1.0D, 0.0D);
        return server.clip(new ClipContext(from, entity.getEyePosition(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private void aimAt(Entity entity) {
        Vec3 aim = aimPoint(entity).subtract(turretOrigin());
        double world = Math.toDegrees(Math.atan2(aim.x, aim.z));
        this.entityData.set(TARGET_YAW, Mth.wrapDegrees((float) world - bodyYaw()));
        float elevation = (float) Math.toDegrees(Math.atan2(aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z)));
        this.entityData.set(TARGET_PITCH, Mth.clamp(elevation, MIN_PITCH, MAX_PITCH));
    }

    private Vec3 aimPoint(Entity entity) {
        if (!isAirTarget(entity)) {
            return entity instanceof LivingEntity living ? living.getEyePosition() : entity.position();
        }
        Vec3 step = new Vec3(entity.getX() - entity.xOld, entity.getY() - entity.yOld, entity.getZ() - entity.zOld);
        double ticks = entity.position().distanceTo(turretOrigin()) / MissilePantsirEntity.TOP_SPEED;
        return entity.position().add(step.scale(ticks));
    }

    private void engage(ServerLevel server, Entity entity) {
        if (!isAimed()) {
            return;
        }
        double distance = entity.position().distanceTo(turretOrigin());
        if (isAirTarget(entity) && distance > GUN_RANGE && distance < MISSILE_RANGE) {
            tryLaunch(server, entity);
        }
        if (distance < GUN_RANGE) {
            fireGuns(server, entity);
        }
    }

    private boolean isAimed() {
        return Math.abs(Mth.degreesDifference(this.yaw, this.entityData.get(TARGET_YAW))) < AIM_TOLERANCE
                && Math.abs(this.pitch - this.entityData.get(TARGET_PITCH)) < AIM_TOLERANCE;
    }

    private void tryLaunch(ServerLevel server, Entity entity) {
        long now = server.getGameTime();
        if (now - this.lastShotAtTarget.getOrDefault(entity.getUUID(), now) > SALVO_TIMEOUT) {
            this.shotsAtTarget.remove(entity.getUUID());
        }
        int fired = this.shotsAtTarget.getOrDefault(entity.getUUID(), 0);
        if (getAmmo() <= 0 || this.launchCooldown > 0 || fired >= MISSILES_PER_TARGET) {
            return;
        }
        int tube = this.nextTube;
        double side = tube % 2 == 0 ? 1.0D : -1.0D;
        double column = TUBE_COLUMNS[(tube / 2) % TUBE_COLUMNS.length];
        double row = TUBE_ROWS[(tube / 4) % TUBE_ROWS.length];
        Vec3 muzzle = turretPoint(side * column, row, MUZZLE_FORWARD);
        Vec3 heading = turretPoint(side * column, row, MUZZLE_FORWARD + 1.0D).subtract(muzzle);

        MissilePantsirEntity missile = new MissilePantsirEntity(ModEntities.MISSILE_PANTSIR.get(), server);
        missile.launch(muzzle, heading, entity);
        missile.setLaunchFacing(getDirection());
        server.addFreshEntity(missile);

        this.entityData.set(AMMO, getAmmo() - 1);
        this.nextTube = (tube + 1) % MAX_AMMO;
        this.launchCooldown = LAUNCH_COOLDOWN;
        this.shotsAtTarget.put(entity.getUUID(), fired + 1);
        this.lastShotAtTarget.put(entity.getUUID(), now);

        sendLargeExplode(server, muzzle, 3.0F, (byte) 6);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 30, 0.5D, 0.5D, 0.5D, 0.05D);
        playSound(server, "hbm:turret.richard_fire", 4.0F, 0.8F);
        server.playSound(null, muzzle.x, muzzle.y, muzzle.z, com.hbm_m.sound.ModSounds.MISSILE_TAKEOFF.get(),
                SoundSource.BLOCKS, 6.0F, 1.5F);
    }

    private void fireGuns(ServerLevel server, Entity entity) {
        if (this.gunCooldown > 0 || getShells() <= 0) {
            return;
        }
        this.gunCooldown = GUN_INTERVAL;
        this.entityData.set(SHELLS, getShells() - 1);
        EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, ModDamageSources.shrapnel(server), GUN_DAMAGE);

        for (int side = -1; side <= 1; side += 2) {
            Vec3 muzzle = turretPoint(side * GUN_LATERAL, 0.3D, 3.0D);
            sendLargeExplode(server, muzzle, 1.5F, (byte) 1);
            Vec3 path = entity.position().subtract(muzzle);
            for (double f = 0.1D; f < 1.0D; f += 0.15D) {
                Vec3 point = muzzle.add(path.scale(f));
                server.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        playSound(server, "hbm:turret.howard_fire", 4.0F, 0.9F + server.random.nextFloat() * 0.3F);
        playSound(server, "hbm:turret.howard_fire", 4.0F, 1.0F + server.random.nextFloat() * 0.3F);
    }

    /** Dieselbe Muendungswolke wie bei den Geschuetztuermen ({@code TurretBaseBlockEntity}). */
    private static void sendLargeExplode(ServerLevel server, Vec3 pos, float size, byte count) {
        CompoundTag data = new CompoundTag();
        data.putString("type", "vanillaExt");
        data.putString("mode", "largeexplode");
        data.putFloat("size", size);
        data.putByte("count", count);
        IParticleCreator.sendPacket(server, pos.x, pos.y, pos.z, 50, data);
    }

    private void playSound(ServerLevel server, String sound, float volume, float pitchValue) {
        Vec3 origin = turretOrigin();
        server.playSound(null, origin.x, origin.y, origin.z, HbmSoundsNT.get(sound), SoundSource.BLOCKS, volume, pitchValue);
    }

    // -- Chunks ----------------------------------------------------------------

    /**
     * Haelt den Chunk unter dem Fahrzeug geladen, solange die Anlage an ist: sonst tickt sie nicht, und
     * Raketen fliegen ungehindert durch, wenn kein Spieler in der Naehe ist. Die Sperre wandert mit und
     * ueberlebt einen Neustart. Regelmaessig erneuert, weil ein zweiter Pantsir im selben Chunk sie beim
     * Abschalten mit aufhebt.
     */
    private void keepChunkLoaded(ServerLevel server) {
        if (!isActive()) {
            return;
        }
        int x = getBlockX() >> 4;
        int z = getBlockZ() >> 4;
        boolean moved = this.chunkHeld && (x != this.heldChunkX || z != this.heldChunkZ);
        if (moved) {
            server.setChunkForced(this.heldChunkX, this.heldChunkZ, false);
        }
        if (!this.chunkHeld || moved || server.getGameTime() % CHUNK_REFRESH_TICKS == 0) {
            server.setChunkForced(x, z, true);
            this.chunkHeld = true;
            this.heldChunkX = x;
            this.heldChunkZ = z;
        }
    }

    private void releaseChunk(ServerLevel server) {
        if (this.chunkHeld) {
            server.setChunkForced(this.heldChunkX, this.heldChunkZ, false);
            this.chunkHeld = false;
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (reason.shouldDestroy() && level() instanceof ServerLevel server) {
            releaseChunk(server);
        }
        super.remove(reason);
    }

    // -- Bedienung -------------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level() instanceof ServerLevel server) {
            ItemStack held = player.getMainHandItem();
            if (player.isSecondaryUseActive()) {
                toggleIff(player);
            } else if (held.is(ModItems.MISSILE_57E6.get())) {
                if (getAmmo() < MAX_AMMO) {
                    this.entityData.set(AMMO, getAmmo() + 1);
                    consume(player, held);
                }
                reportLoaded(player);
            } else if (held.is(ModItems.AMMO_DGK.get())) {
                if (getShells() + SHELLS_PER_BELT <= MAX_SHELLS) {
                    this.entityData.set(SHELLS, getShells() + SHELLS_PER_BELT);
                    playSound(server, "hbm:turret.howard_reload", 2.0F, 1.0F);
                    consume(player, held);
                }
                reportLoaded(player);
            } else if (getPassengers().isEmpty() && player.startRiding(this)) {
                player.displayClientMessage(Component.translatable("entity.hbm_m.pantsir_s1.controls"), true);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private static void consume(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private void toggle(ServerLevel server) {
        boolean active = !isActive();
        this.entityData.set(ACTIVE, active);
        if (!active) {
            releaseChunk(server);
        }
    }

    private void toggleIff(Player player) {
        this.iffEnabled = !this.iffEnabled;
        player.displayClientMessage(Component.translatable(
                this.iffEnabled ? "message.hbm_m.pantsir.iff_on" : "message.hbm_m.pantsir.iff_off"), true);
    }

    private void reportLoaded(Player player) {
        player.displayClientMessage(Component.translatable("message.hbm_m.pantsir.loaded",
                getAmmo(), MAX_AMMO, getShells(), MAX_SHELLS), true);
    }

    private void reportStatus(Player player) {
        player.displayClientMessage(Component.translatable(
                isActive() ? "message.hbm_m.pantsir.on" : "message.hbm_m.pantsir.off",
                getAmmo(), MAX_AMMO, getShells(), MAX_SHELLS), true);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (isInvulnerableTo(source) || level().isClientSide || isRemoved()) {
            return false;
        }
        float health = this.entityData.get(HEALTH) - amount;
        this.entityData.set(HEALTH, health);
        if (health <= 0.0F) {
            destroy();
        }
        return true;
    }

    private void destroy() {
        double x = getX();
        double y = getY();
        double z = getZ();
        ejectPassengers();
        if (getAmmo() > 0) {
            spawnAtLocation(new ItemStack(ModItems.MISSILE_57E6.get(), getAmmo()));
        }
        discard();
        new ExplosionVNT(level(), x, y + 1.5D, z, 12.0F).makeStandard().explode();
    }

    @Override
    public void setOwner(UUID owner) {
        this.ownerUuid = owner;
    }

    @Nullable
    @Override
    public UUID getOwner() {
        return this.ownerUuid;
    }

    // -- Geometrie -------------------------------------------------------------

    /** Drehung des Rumpfs im Sinn der Turmwinkel: bei 0 Grad schaut die Kabine nach Sueden (+Z). */
    private float bodyYaw() {
        return -getYRot();
    }

    private Vec3 turretOrigin() {
        double angle = Math.toRadians(bodyYaw());
        return new Vec3(getX() + Math.sin(angle) * TURRET_AXIS_Z, getY() + LAUNCHER_AXIS_Y,
                getZ() + Math.cos(angle) * TURRET_AXIS_Z);
    }

    /** Punkt im Werferraum in Weltkoordinaten: seitlich, hoch, voraus. */
    private Vec3 turretPoint(double lateral, double up, double forward) {
        double elevation = Math.toRadians(this.pitch);
        double y = up * Math.cos(elevation) + forward * Math.sin(elevation);
        double z = forward * Math.cos(elevation) - up * Math.sin(elevation);
        double angle = Math.toRadians(bodyYaw() + this.yaw);
        double worldX = lateral * Math.cos(angle) + z * Math.sin(angle);
        double worldZ = z * Math.cos(angle) - lateral * Math.sin(angle);
        return turretOrigin().add(worldX, y, worldZ);
    }

    // -- Speichern -------------------------------------------------------------

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        this.entityData.set(ACTIVE, !tag.contains("active") || tag.getBoolean("active"));
        this.iffEnabled = !tag.contains("iff") || tag.getBoolean("iff");
        this.ownerUuid = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        this.entityData.set(AMMO, tag.contains("ammo") ? tag.getInt("ammo") : MAX_AMMO);
        this.entityData.set(SHELLS, tag.contains("shells") ? tag.getInt("shells") : MAX_SHELLS / 2);
        this.entityData.set(HEALTH, tag.contains("health") ? tag.getFloat("health") : MAX_HEALTH);
        this.nextTube = tag.getInt("tube");
        this.yaw = tag.getFloat("turretYaw");
        this.pitch = tag.getFloat("launcherPitch");
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;
        this.entityData.set(TARGET_YAW, tag.getFloat("targetYaw"));
        this.entityData.set(TARGET_PITCH, tag.getFloat("targetPitch"));
        this.chunkHeld = tag.getBoolean("chunkHeld");
        this.heldChunkX = tag.getInt("chunkX");
        this.heldChunkZ = tag.getInt("chunkZ");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putBoolean("active", isActive());
        tag.putBoolean("iff", this.iffEnabled);
        if (this.ownerUuid != null) {
            tag.putUUID("owner", this.ownerUuid);
        }
        tag.putInt("ammo", getAmmo());
        tag.putInt("shells", getShells());
        tag.putFloat("health", this.entityData.get(HEALTH));
        tag.putInt("tube", this.nextTube);
        tag.putFloat("turretYaw", this.yaw);
        tag.putFloat("launcherPitch", this.pitch);
        tag.putFloat("targetYaw", this.entityData.get(TARGET_YAW));
        tag.putFloat("targetPitch", this.entityData.get(TARGET_PITCH));
        tag.putBoolean("chunkHeld", this.chunkHeld);
        tag.putInt("chunkX", this.heldChunkX);
        tag.putInt("chunkZ", this.heldChunkZ);
    }
}
