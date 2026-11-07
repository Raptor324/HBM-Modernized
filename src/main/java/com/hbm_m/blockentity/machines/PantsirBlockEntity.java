package com.hbm_m.blockentity.machines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.logic.EntityPlaneBase;
import com.hbm_m.entity.missile.MissileAntiBallisticEntity;
import com.hbm_m.entity.missile.MissileBaseEntity;
import com.hbm_m.entity.missile.MissilePantsirEntity;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.server.missile.MissileTrackBroadcaster;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.EntityDamageUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Pantsir-S1: Flugabwehrsystem auf KamAZ-Fahrgestell.
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
 * gerufen hat. Gehoert er dem Besitzer dieser Anlage oder dessen Team, bleibt das Ziel unbehelligt.
 * Ziele ohne bekannte Herkunft gelten als feindlich. Schleichen und Rechtsklick schaltet das ab.
 *
 * <p><b>Bewaffnung.</b> 57E6-Flugkoerper bis 12 km, die 30-mm-Kanonen nur im Nahbereich gegen Raketen,
 * die durchkommen, und gegen feindliche Mobs. Beides braucht Munition: per Rechtsklick mit einer 57E6
 * oder einem Gurt ammo_dgk nachladen.
 *
 * <p>Ausrichtung: Turm und Werfer sind Winkel im Fahrzeugraum, 0 Grad zeigt zur Kabine. Der Server
 * setzt nur die Sollwinkel, beide Seiten drehen mit derselben Geschwindigkeit darauf zu. Dadurch
 * reichen wenige Zustandspakete und die Bewegung bleibt trotzdem fluessig.
 */
public class PantsirBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.interfaces.IOwnedMachine {

    /** Lage der Drehachsen im Fahrzeugraum, aus dem Modell gemessen. */
    public static final double TURRET_AXIS_Z = -2.35D;
    public static final double RADAR_AXIS_Z = -3.62D;
    public static final double LAUNCHER_AXIS_Y = 3.7D;

    public static final int MAX_AMMO = 12;
    /** Kanonenmunition: ein Gurt (ammo_dgk) sind 100 Schuss, wie beim Howard. */
    public static final int MAX_SHELLS = 1_000;
    public static final int SHELLS_PER_BELT = 100;

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

    private boolean active = true;
    private int ammo = MAX_AMMO;
    private int shells = MAX_SHELLS / 2;
    private int nextTube;

    private float yaw;
    private float pitch;
    private float lastYaw;
    private float lastPitch;
    private float targetYaw;
    private float targetPitch;
    private float radar;
    private float lastRadar;
    private boolean locked;

    private int launchCooldown;
    private int gunCooldown;

    @Nullable
    private Entity target;
    private final Map<UUID, Integer> shotsAtTarget = new HashMap<>();
    private final Map<UUID, Long> lastShotAtTarget = new HashMap<>();
    private int blindTicks;
    private final List<EntityPlaneBase> planes = new ArrayList<>();
    private int idleTimer;
    private boolean chunkHeld;
    private boolean iffEnabled = true;

    public PantsirBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.PANTSIR_S1_BE.get(), pos, state);
    }

    public PantsirBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PantsirBlockEntity be) {
        be.animate();
        if (level instanceof ServerLevel server) {
            be.serverTick(server);
        }
    }

    private void animate() {
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;
        this.lastRadar = this.radar;
        this.yaw = Mth.approachDegrees(this.yaw, this.targetYaw, this.locked ? YAW_SPEED : IDLE_YAW_SPEED);
        this.pitch = Mth.approach(this.pitch, this.targetPitch, this.locked ? PITCH_SPEED : IDLE_PITCH_SPEED);
        if (this.active) {
            this.radar = Mth.wrapDegrees(this.radar + (this.locked ? RADAR_SPEED_LOCKED : RADAR_SPEED_IDLE));
        }
    }

    // -- Server ----------------------------------------------------------------

    private void serverTick(ServerLevel server) {
        keepChunkLoaded(server);

        if (this.launchCooldown > 0) {
            this.launchCooldown--;
        }
        if (this.gunCooldown > 0) {
            this.gunCooldown--;
        }

        boolean wasLocked = this.locked;
        float previousYaw = this.targetYaw;
        float previousPitch = this.targetPitch;

        if (!this.active) {
            this.target = null;
        } else {
            updateTarget(server);
        }

        Entity current = this.target;
        this.locked = current != null;
        if (current != null) {
            aimAt(current);
            engage(server, current);
        } else {
            if (wasLocked) {
                this.idleTimer = IDLE_MIN_TICKS;
            }
            forgetTargets();
            idleSweep(server);
        }

        if (this.locked && !wasLocked) {
            playSound(server, "hbm:turret.sentry_lockon", 3.0F, 1.0F);
        }
        boolean moved = Math.abs(Mth.degreesDifference(previousYaw, this.targetYaw)) > 0.5F
                || Math.abs(previousPitch - this.targetPitch) > 0.5F;
        if (this.locked != wasLocked || (moved && (!this.locked || server.getGameTime() % 2 == 0))) {
            setChanged();
            sendUpdateToClient();
        }
    }

    /**
     * Ohne Ziel bleibt der Turm stehen, wo er zuletzt hingezeigt hat, und schwenkt von Zeit zu Zeit
     * gemaechlich in eine neue Richtung. In die Fahrstellung geht er nur, wenn die Anlage aus ist.
     */
    private void idleSweep(ServerLevel server) {
        if (!this.active) {
            this.targetYaw = 0.0F;
            this.targetPitch = 0.0F;
            return;
        }
        if (--this.idleTimer > 0) {
            return;
        }
        this.idleTimer = IDLE_MIN_TICKS + server.random.nextInt(IDLE_SPREAD_TICKS);
        float swing = (server.random.nextFloat() * 2.0F - 1.0F) * IDLE_MAX_SWING;
        this.targetYaw = Mth.wrapDegrees(this.yaw + swing);
        this.targetPitch = server.random.nextFloat() * IDLE_MAX_PITCH;
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
        float heading = blockYaw() + this.yaw + this.radar;
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
        double dx = missile.getImpactX() - worldPosition.getX();
        double dz = missile.getImpactZ() - worldPosition.getZ();
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
        AABB area = new AABB(worldPosition).inflate(GUN_RANGE);
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
        if (entity == null || entity.isRemoved() || !entity.isAlive() || entity.level() != this.level) {
            return false;
        }
        double range = isAirTarget(entity) ? RADAR_RANGE : GUN_RANGE;
        return entity.position().distanceToSqr(turretOrigin()) < range * range;
    }

    private boolean canSee(ServerLevel server, LivingEntity entity) {
        Vec3 from = turretOrigin().add(0.0D, 1.0D, 0.0D);
        return server.clip(new ClipContext(from, entity.getEyePosition(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, entity)).getType() == HitResult.Type.MISS;
    }

    private void aimAt(Entity entity) {
        Vec3 aim = aimPoint(entity).subtract(turretOrigin());
        double world = Math.toDegrees(Math.atan2(aim.x, aim.z));
        this.targetYaw = Mth.wrapDegrees((float) world - blockYaw());
        float elevation = (float) Math.toDegrees(Math.atan2(aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z)));
        this.targetPitch = Mth.clamp(elevation, MIN_PITCH, MAX_PITCH);
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
        return Math.abs(Mth.degreesDifference(this.yaw, this.targetYaw)) < AIM_TOLERANCE
                && Math.abs(this.pitch - this.targetPitch) < AIM_TOLERANCE;
    }

    private void tryLaunch(ServerLevel server, Entity entity) {
        long now = server.getGameTime();
        if (now - this.lastShotAtTarget.getOrDefault(entity.getUUID(), now) > SALVO_TIMEOUT) {
            this.shotsAtTarget.remove(entity.getUUID());
        }
        int fired = this.shotsAtTarget.getOrDefault(entity.getUUID(), 0);
        if (this.ammo <= 0 || this.launchCooldown > 0 || fired >= MISSILES_PER_TARGET) {
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
        missile.setLaunchFacing(facing());
        server.addFreshEntity(missile);

        this.ammo--;
        this.nextTube = (tube + 1) % MAX_AMMO;
        this.launchCooldown = LAUNCH_COOLDOWN;
        this.shotsAtTarget.put(entity.getUUID(), fired + 1);
        this.lastShotAtTarget.put(entity.getUUID(), now);

        sendLargeExplode(server, muzzle, 3.0F, (byte) 6);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 30, 0.5D, 0.5D, 0.5D, 0.05D);
        playSound(server, "hbm:turret.richard_fire", 4.0F, 0.8F);
        server.playSound(null, muzzle.x, muzzle.y, muzzle.z, com.hbm_m.sound.ModSounds.MISSILE_TAKEOFF.get(),
                SoundSource.BLOCKS, 6.0F, 1.5F);
        setChanged();
    }

    private void fireGuns(ServerLevel server, Entity entity) {
        if (this.gunCooldown > 0 || this.shells <= 0) {
            return;
        }
        this.gunCooldown = GUN_INTERVAL;
        this.shells--;
        setChanged();
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

    private void forgetTargets() {
        this.shotsAtTarget.clear();
        this.lastShotAtTarget.clear();
    }

    private void playSound(ServerLevel server, String sound, float volume, float pitchValue) {
        Vec3 origin = turretOrigin();
        server.playSound(null, origin.x, origin.y, origin.z, HbmSoundsNT.get(sound), SoundSource.BLOCKS, volume, pitchValue);
    }

    /** Wer die Anlage aufgestellt hat, fuer die Freund-Feind-Erkennung. */
    @org.jetbrains.annotations.Nullable
    private java.util.UUID ownerUuid;

    @Override
    public void setOwner(java.util.UUID owner) {
        this.ownerUuid = owner;
        setChanged();
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public java.util.UUID getOwner() {
        return this.ownerUuid;
    }

    /** Schaltet die Freund-Feind-Erkennung um, zum Beispiel fuer Tests mit eigenen Raketen. */
    public void toggleIff(Player player) {
        this.iffEnabled = !this.iffEnabled;
        setChanged();
        player.displayClientMessage(Component.translatable(
                this.iffEnabled ? "message.hbm_m.pantsir.iff_on" : "message.hbm_m.pantsir.iff_off"), true);
    }

    /** Laedt einen Flugkoerper nach. Falsch, wenn alle Rohre voll sind. */
    public boolean loadMissile() {
        if (this.ammo >= MAX_AMMO) {
            return false;
        }
        this.ammo++;
        setChanged();
        return true;
    }

    /** Laedt einen Munitionsgurt. Falsch, wenn er nicht mehr ganz hineinpasst. */
    public boolean loadBelt(ServerLevel server) {
        if (this.shells + SHELLS_PER_BELT > MAX_SHELLS) {
            return false;
        }
        this.shells += SHELLS_PER_BELT;
        playSound(server, "hbm:turret.howard_reload", 2.0F, 1.0F);
        setChanged();
        return true;
    }

    public int getAmmo() {
        return this.ammo;
    }

    public int getShells() {
        return this.shells;
    }

    /**
     * Haelt den eigenen Chunk geladen, solange die Anlage an ist: sonst tickt sie nicht, und Raketen
     * fliegen ungehindert durch, wenn kein Spieler in der Naehe ist. Forced Chunks ueberleben einen
     * Neustart. Regelmaessig erneuert, weil ein zweiter Pantsir im selben Chunk die Sperre beim
     * Abschalten mit aufhebt.
     */
    private void keepChunkLoaded(ServerLevel server) {
        if (this.active && (!this.chunkHeld || server.getGameTime() % CHUNK_REFRESH_TICKS == 0)) {
            setChunkHeld(server, true);
        }
    }

    private void setChunkHeld(ServerLevel server, boolean held) {
        server.setChunkForced(worldPosition.getX() >> 4, worldPosition.getZ() >> 4, held);
        this.chunkHeld = held;
    }

    /** Beim Abbau aufrufen, sonst bliebe der Chunk fuer immer geladen. */
    public void releaseChunk(ServerLevel server) {
        setChunkHeld(server, false);
    }

    /** Schaltet die Anlage um und gibt den neuen Zustand zurueck. */
    public boolean toggle() {
        this.active = !this.active;
        if (this.level instanceof ServerLevel server) {
            setChunkHeld(server, this.active);
        }
        setChanged();
        sendUpdateToClient();
        return this.active;
    }

    public void reportLoaded(Player player) {
        player.displayClientMessage(Component.translatable("message.hbm_m.pantsir.loaded", this.ammo, MAX_AMMO, this.shells, MAX_SHELLS), true);
    }

    public void reportStatus(Player player) {
        player.displayClientMessage(Component.translatable(
                this.active ? "message.hbm_m.pantsir.on" : "message.hbm_m.pantsir.off", this.ammo, MAX_AMMO, this.shells, MAX_SHELLS), true);
    }

    // -- Geometrie -------------------------------------------------------------

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
    }

    /** Drehung des Fahrzeugs: bei 0 Grad schaut die Kabine nach Sueden (+Z). */
    public static float blockYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case WEST -> 270.0F;
            case EAST -> 90.0F;
            default -> 0.0F;
        };
    }

    private float blockYaw() {
        return blockYaw(facing());
    }

    private Vec3 turretOrigin() {
        double angle = Math.toRadians(blockYaw());
        return new Vec3(
                worldPosition.getX() + 0.5D + Math.sin(angle) * TURRET_AXIS_Z,
                worldPosition.getY() + LAUNCHER_AXIS_Y,
                worldPosition.getZ() + 0.5D + Math.cos(angle) * TURRET_AXIS_Z);
    }

    /** Punkt im Werferraum in Weltkoordinaten: seitlich, hoch, voraus. */
    private Vec3 turretPoint(double lateral, double up, double forward) {
        double elevation = Math.toRadians(this.pitch);
        double y = up * Math.cos(elevation) + forward * Math.sin(elevation);
        double z = forward * Math.cos(elevation) - up * Math.sin(elevation);
        double angle = Math.toRadians(blockYaw() + this.yaw);
        double worldX = lateral * Math.cos(angle) + z * Math.sin(angle);
        double worldZ = z * Math.cos(angle) - lateral * Math.sin(angle);
        return turretOrigin().add(worldX, y, worldZ);
    }

    // -- Zugriff fuer den Renderer ---------------------------------------------

    public float getTurretYaw(float partialTicks) {
        return Mth.rotLerp(partialTicks, this.lastYaw, this.yaw);
    }

    public float getLauncherPitch(float partialTicks) {
        return Mth.lerp(partialTicks, this.lastPitch, this.pitch);
    }

    public float getRadarYaw(float partialTicks) {
        return Mth.rotLerp(partialTicks, this.lastRadar, this.radar);
    }

    // -- Rahmenkram ------------------------------------------------------------

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.pantsir_s1");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return null;
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("pantsir_active", this.active);
        tag.putBoolean("pantsir_iff", this.iffEnabled);
        if (this.ownerUuid != null) {
            tag.putUUID("owner_uuid", this.ownerUuid);
        }
        tag.putBoolean("pantsir_locked", this.locked);
        tag.putInt("pantsir_ammo", this.ammo);
        tag.putInt("pantsir_shells", this.shells);
        tag.putInt("pantsir_tube", this.nextTube);
        tag.putFloat("pantsir_yaw", this.yaw);
        tag.putFloat("pantsir_pitch", this.pitch);
        tag.putFloat("pantsir_target_yaw", this.targetYaw);
        tag.putFloat("pantsir_target_pitch", this.targetPitch);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.active = !tag.contains("pantsir_active") || tag.getBoolean("pantsir_active");
        this.locked = tag.getBoolean("pantsir_locked");
        this.iffEnabled = !tag.contains("pantsir_iff") || tag.getBoolean("pantsir_iff");
        this.ownerUuid = tag.hasUUID("owner_uuid") ? tag.getUUID("owner_uuid") : null;
        this.ammo = tag.contains("pantsir_ammo") ? tag.getInt("pantsir_ammo") : MAX_AMMO;
        this.shells = tag.contains("pantsir_shells") ? tag.getInt("pantsir_shells") : MAX_SHELLS / 2;
        this.nextTube = tag.getInt("pantsir_tube");
        this.targetYaw = tag.getFloat("pantsir_target_yaw");
        this.targetPitch = tag.getFloat("pantsir_target_pitch");
        // Beim Sync laeuft die Animation lokal weiter, ein Sprung auf den Serverstand wuerde ruckeln.
        if (this.level == null || !this.level.isClientSide) {
            this.yaw = tag.getFloat("pantsir_yaw");
            this.pitch = tag.getFloat("pantsir_pitch");
            this.lastYaw = this.yaw;
            this.lastPitch = this.pitch;
        }
    }

    /** 13 Bloecke lang, bis knapp 6 hoch. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(9.0D, 0.0D, 9.0D).expandTowards(0.0D, 8.0D, 0.0D);
    }
}
