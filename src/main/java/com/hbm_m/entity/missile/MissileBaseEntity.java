package com.hbm_m.entity.missile;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.hbm_m.explosion.MissileWarheadEffects;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import api.hbm_m.entity.IRadarDetectable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Базовая сущность баллистической ракеты.
 *
 * Управляет собственной баллистикой без гравитации и сопротивления среды,
 * как в оригинале EntityMissileBaseNT: вертикальная подача с torsion-замедлением
 * и горизонтальный разгон к точке цели.
 *
 * Дополнительно держит чанк‑тикет на текущем чанке, чтобы ракета не выгружалась
 * на больших дистанциях полёта.
 */
public abstract class MissileBaseEntity extends Projectile implements IRadarDetectable {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Тикет на загрузку чанка под ракетой. */
    private static final TicketType<UUID> CHUNK_TICKET =
            TicketType.create("hbm_m_missile", Comparator.comparing(UUID::toString));

    private static final int CHUNK_TICKET_RADIUS = 3;

    /**
     * Radius der vorausgeladenen Tickets. {@code addRegionTicket} setzt die Ticketstufe auf
     * {@code 33 - radius}, und Entitaeten ticken erst ab Stufe 31 - Radius 2 laedt den
     * Zielchunk also gerade so weit, dass die Rakete dort weiterlaeuft.
     */
    private static final int CHUNK_LOOKAHEAD_RADIUS = 2;

    /**
     * Wie viele Ticks Flugweg im Voraus geladen werden. Jedes Ticket zieht ein 5x5-Feld an
     * Chunks nach sich, bei Mach 6 liegen die Punkte rund sechs Chunks auseinander - sechs Ticks
     * sind der Kompromiss zwischen Vorlaufzeit fuer den Chunkladen und Last auf dem Server.
     */
    private static final int CHUNK_LOOKAHEAD_TICKS = 6;

    /** Obergrenze, damit ein absurdes Tempo nicht die halbe Welt laedt. */
    private static final int CHUNK_LOOKAHEAD_MAX = 8;

    /** Макс. длина сегмента raycast за тик (как в 1.7.10 — один луч, но без туннелирования). */
    private static final double MAX_COLLISION_SEGMENT = 1.0D;
    /** Obergrenze fuer Teilschritte pro Tick - Schutz vor Endlosschleifen bei absurden Werten. */
    private static final int MAX_SUB_STEPS = 256;

    protected int startX;
    protected int startZ;
    protected int targetX;
    protected int targetZ;
    protected double velocity;
    protected double decelY;
    protected double accelXZ;
    protected boolean isCluster = false;
    protected int health = 50;
    protected boolean exploded = false;

    private static final EntityDataAccessor<Direction> DATA_LAUNCH_FACING =
            SynchedEntityData.defineId(MissileBaseEntity.class, EntityDataSerializers.DIRECTION);

    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;

    private ChunkPos loadedChunk;

    /** Zusaetzlich gehaltene Tickets entlang des Flugwegs; siehe {@link #updateChunkTicket()}. */
    private final java.util.Set<ChunkPos> lookaheadChunks = new java.util.HashSet<>();

    protected MissileBaseEntity(EntityType<? extends MissileBaseEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.startX = (int) this.getX();
        this.startZ = (int) this.getZ();
        this.targetX = this.startX;
        this.targetZ = this.startZ;
    }

    /**
     * Инициализация полёта при старте с пусковой площадки.
     */
    public void initLaunch(double x, double y, double z, int targetX, int targetZ) {
        this.setPos(x, y, z);
        this.startX = (int) this.getX();
        this.startZ = (int) this.getZ();
        this.targetX = targetX;
        this.targetZ = targetZ;

        Vec3 vec = new Vec3(this.targetX - this.startX, 0, this.targetZ - this.startZ);
        double len = vec.length();
        if (len == 0) {
            len = 1.0D;
        }
        // Ballistische Bahn, aufgeloest fuer eine frei waehlbare Gipfelhoehe.
        // Mit senkrechtem Startimpuls 2 erreicht die Bahn ihren Scheitel bei `apogee` und legt
        // dabei genau `len` horizontal zurueck. apogeeFor() liefert per Default `len` zurueck und
        // reproduziert damit exakt die Originalwerte (decelY = 2/len, accelXZ = 1/len) - im
        // Original ist die Gipfelhoehe also immer gleich der Schussweite, was bei weiten Schuessen
        // bedeutet, dass die Rakete den Grossteil der Strecke weit oberhalb der Bauhoehe fliegt.
        double apogee = Math.max(1.0D, this.apogeeFor(len));
        this.decelY = 2.0D / apogee;
        this.accelXZ = len / (apogee * apogee);
        this.velocity = 0.0D;
        this.onTrajectorySolved(len, apogee);

        this.setDeltaMovement(this.getDeltaMovement().x(), 2.0D, this.getDeltaMovement().z());

        float yaw = (float) (Math.atan2(this.targetX - this.getX(), this.targetZ - this.getZ()) * 180.0D / Math.PI);
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setXRot(0.0F);
        this.xRotO = 0.0F;
    }

    public void setLaunchFacing(Direction facing) {
        Direction dir = facing == null ? Direction.NORTH : facing;
        this.entityData.set(DATA_LAUNCH_FACING, dir);
    }

    public Direction getLaunchFacing() {
        return this.entityData.get(DATA_LAUNCH_FACING);
    }

    /** Множитель смещения за тик (1.7.10 {@code motionMult()}). */
    public double getMotionMultiplier() {
        return this.velocity;
    }

    protected List<ItemStack> getDebris() {
        return MissileWarheadEffects.defaultDebrisForTier(debrisTierIndex());
    }

    @javax.annotation.Nullable
    protected ItemStack getDebrisRareDrop() {
        return ItemStack.EMPTY;
    }

    protected int debrisTierIndex() {
        return switch (getTargetType()) {
            case MISSILE_TIER4 -> 4;
            case MISSILE_TIER3 -> 3;
            case MISSILE_TIER2 -> 2;
            case MISSILE_TIER1 -> 1;
            default -> 0;
        };
    }

    @Override
    public RadarTargetType getTargetType() {
        return RadarTargetType.MISSILE_TIER0;
    }

    //? if < 1.21.1 {
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = steps + 1;
    }
    //?} else {
    /*@Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = steps + 1;
    }
    *///?}

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        // Projectile.recreateFromPacket ломает xRot/yRot, вычисляя их по-своему из motion.
        // Восстанавливаем оригинальные углы, пришедшие с сервера, чтобы ракета не ложилась на бок.
        this.setXRot((packet.getXRot() * 360.0F) / 256.0F);
        this.setYRot((packet.getYRot() * 360.0F) / 256.0F);
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        this.setDeltaMovement(x, y, z);
        // Базовый класс Projectile в ванилле при получении первого пакета движения
        // принудительно пересчитывает XRot/YRot из вектора скорости, если они равны строго 0.0F.
        // Именно это заставляло ракеты при вертикальном старте с базы мгновенно ложиться на бок.
        // Блокируем это поведение.
    }

    private void clientLerpStep() {
        if (this.lerpSteps > 0) {
            float d = 1.0F / this.lerpSteps;
            this.setPos(
                    Mth.lerp(d, this.getX(), this.lerpX),
                    Mth.lerp(d, this.getY(), this.lerpY),
                    Mth.lerp(d, this.getZ(), this.lerpZ));
            this.setYRot((float) Mth.rotLerp(d, this.getYRot(), (float) this.lerpYRot));
            this.setXRot((float) Mth.rotLerp(d, this.getXRot(), (float) this.lerpXRot));
            --this.lerpSteps;
        } else {
            this.reapplyPosition();
        }
    }

    /**
     * Полностью кастомный тик: мы не наследуем поведение от ThrowableProjectile,
     * поэтому здесь нет ни гравитации, ни сопротивления воздуха.
     */
    @Override
    public void tick() {
        // Обязательно обновляем значения предыдущего кадра, иначе рендер будет дёргаться или lerp'иться от нулей.
        this.xOld = this.getX();
        this.yOld = this.getY();
        this.zOld = this.getZ();
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();

        if (!this.level().isClientSide) {
            if (this.exploded) {
                releaseChunkTicket();
                this.discard();
                return;
            }

            this.baseTick();
            updateChunkTicket();

            if (this.isRemoved()) {
                return;
            }

            double stepLimit = maxStepDistance();
            if (!(stepLimit > 0.0D) || Double.isInfinite(stepLimit)) {
                // Unveraenderter Pfad: ein Schritt pro Tick, wie im Original.
                if (!advance(this.velocity)) {
                    return;
                }
                serverTickLogic();
            } else {
                // Teilschritte. updateVelocity() bleibt bei einem Aufruf pro Tick, nur die
                // Integration wird aufgeteilt - sonst laeuft der Euler-Schritt bei hohen
                // Geschwindigkeiten weg und die Rakete landet vor dem Ziel.
                updateVelocity();
                int subSteps = subStepsFor(stepLimit);
                double stepVelocity = this.velocity / subSteps;
                for (int i = 0; i < subSteps; i++) {
                    if (!advance(stepVelocity)) {
                        return;
                    }
                    integrateMotion(stepVelocity);
                }
            }
            com.hbm_m.server.missile.MissileTrackBroadcaster.broadcastPoseForMissile(this);
        } else {
            clientLerpStep();
            if (hasPropulsion()) {
                spawnContrail();
                spawnNozzleFlare();
            }
        }
    }

    /**
     * Ein Integrationsschritt: Kollisionspruefung, Positionsvorschub um {@code motion * stepVelocity}
     * und Ausrichtung. Liefert false, wenn die Rakete dabei verschwunden oder detoniert ist.
     */
    private boolean advance(double stepVelocity) {
        Vec3 motion = this.getDeltaMovement();
        Vec3 step = motion.scale(stepVelocity);

        BlockHitResult blockHit = raycastAlongStep(this.position(), step);
        if (blockHit != null) {
            Vec3 hitPos = blockHit.getLocation();
            this.setPos(hitPos.x, hitPos.y, hitPos.z);
            this.reapplyPosition();
            this.onHit(blockHit);
        }

        if (this.isRemoved() || this.exploded) {
            return false;
        }

        this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
        updateRotationFromMotion();
        normalizeRotationDeltas();
        return true;
    }

    /**
     * Wie viele Integrationsschritte dieser Tick braucht, damit pro Schritt hoechstens
     * {@code limit} Bloecke zurueckgelegt werden.
     */
    private int subStepsFor(double limit) {
        double stepLength = this.getDeltaMovement().length() * this.velocity;
        if (!(stepLength > limit)) {
            return 1;
        }
        return (int) Math.min(MAX_SUB_STEPS, Math.ceil(stepLength / limit));
    }

    /**
     * Groesste Strecke in Bloecken, die ein einzelner Integrationsschritt zuruecklegen darf.
     * Unbegrenzt bedeutet: ein Schritt pro Tick. Schnelle Raketen deckeln das, weil der
     * Euler-Schritt sonst die Bahn verfaelscht - bei einem 3.000-Bloecke-Schuss mit Mach 6
     * sind das rund 16 Bloecke Fehler zum Ziel, bei 10.000 Bloecken ueber 170.
     */
    protected double maxStepDistance() {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Сегментированный raycast вдоль шага движения (motion * velocity).
     * Предотвращает пролёт сквозь блоки при больших шагах на финальном снижении.
     */
    @javax.annotation.Nullable
    private BlockHitResult raycastAlongStep(Vec3 from, Vec3 step) {
        if (this.exploded || step.lengthSqr() <= 0.0D) {
            return null;
        }

        double len = step.length();
        int segments = Math.max(1, (int) Math.ceil(len / MAX_COLLISION_SEGMENT));
        Vec3 seg = step.scale(1.0D / segments);
        Vec3 cursor = from;

        for (int i = 0; i < segments; i++) {
            Vec3 to = (i == segments - 1) ? from.add(step) : cursor.add(seg);
            BlockHitResult hit = this.level().clip(
                    new ClipContext(cursor, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() != HitResult.Type.MISS) {
                return hit;
            }
            cursor = to;
        }
        return null;
    }

    private void normalizeRotationDeltas() {
        while (this.getXRot() - this.xRotO < -180.0F) {
            this.xRotO -= 360.0F;
        }
        while (this.getXRot() - this.xRotO >= 180.0F) {
            this.xRotO += 360.0F;
        }
        while (this.getYRot() - this.yRotO < -180.0F) {
            this.yRotO -= 360.0F;
        }
        while (this.getYRot() - this.yRotO >= 180.0F) {
            this.yRotO += 360.0F;
        }
    }

    /**
     * Gipfelhoehe der Bahn in Bloecken. Default = Schussweite, also 1:1 das Originalverhalten.
     * Unterklassen druecken die Bahn flacher, indem sie hier einen Deckel zurueckgeben - der
     * Einschlagpunkt bleibt dabei exakt erhalten, weil {@code accelXZ} mitgeloest wird.
     */
    protected double apogeeFor(double len) {
        return len;
    }

    /** Haken fuer Unterklassen, um aus Schussweite und Gipfelhoehe ihr Tempoprofil abzuleiten. */
    protected void onTrajectorySolved(double len, double apogee) {
    }

    /** Obergrenze des Zeitfaktors {@code velocity}. NICHT die Geschwindigkeit in Bloecken/Tick. */
    protected double maxVelocity() {
        return 4.0D;
    }

    /**
     * Fortschreiben von {@code velocity}. Das ist ein reiner Zeitfaktor: die Position rueckt pro
     * Tick um {@code motion * velocity} vor, und {@code motion} selbst wird ebenfalls mit
     * {@code velocity} integriert. Dadurch skaliert velocity nur das Tempo entlang der Bahn,
     * nicht die Bahn selbst - jedes Tempoprofil trifft denselben Punkt.
     */
    protected void updateVelocity() {
        double cap = maxVelocity();
        if (this.velocity < cap) {
            this.velocity = Math.min(cap,
                    this.velocity + Mth.clamp((double) this.tickCount / 60.0D * 0.05D, 0.0D, 0.05D));
        }
    }

    protected void serverTickLogic() {
        this.updateVelocity();
        this.integrateMotion(this.velocity);
    }

    /**
     * Integration von Schub und Schwerkraft ueber einen Zeitfaktor. Das ist der Rumpf des alten
     * {@code serverTickLogic()}; als Parameter, damit ein Tick in mehrere Schritte zerfallen kann.
     */
    protected void integrateMotion(double stepVelocity) {
        Vec3 motion = this.getDeltaMovement();

        if (hasPropulsion()) {
            double motionY = motion.y - this.decelY * stepVelocity;

            Vec3 vector = new Vec3(this.targetX - this.startX, 0, this.targetZ - this.startZ).normalize();
            vector = new Vec3(vector.x * this.accelXZ, 0, vector.z * this.accelXZ);

            double motionX = motion.x;
            double motionZ = motion.z;

            if (motionY > 0) {
                motionX += vector.x * stepVelocity;
                motionZ += vector.z * stepVelocity;
            }

            if (motionY < 0) {
                motionX -= vector.x * stepVelocity;
                motionZ -= vector.z * stepVelocity;
            }

            motion = new Vec3(motionX, motionY, motionZ);
        } else {
            motion = motion.multiply(0.99D, 1.0D, 0.99D);
            if (motion.y > -1.5D) {
                motion = motion.add(0.0D, -0.05D, 0.0D);
            }
        }

        if (motion.y < -1.5D && this.isCluster) {
            cluster();
            releaseChunkTicket();
            this.discard();
            return;
        }

        this.setDeltaMovement(motion);
    }

    protected boolean hasPropulsion() {
        return true;
    }

    protected float getContrailScale() {
        return 1.0F;
    }

    public float getContrailScaleForSync() {
        return getContrailScale();
    }

    protected void spawnContrail() {
        spawnContrailWithOffset(0.0D, 0.0D, 0.0D);
    }

    protected void spawnNozzleFlare() {
        if (!(this.level() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel)) {
            return;
        }
        if (com.hbm_m.client.missile.track.MissileTrackClient.shouldUseTrackWorldRender(this.getId())) {
            return;
        }
        Vec3 step = new Vec3(this.getX() - this.xOld, this.getY() - this.yOld, this.getZ() - this.zOld);
        com.hbm_m.client.missile.track.MissileNozzleFlare.spawn(
                clientLevel,
                this.getX(), this.getY(), this.getZ(),
                this.getXRot(), this.getYRot(),
                step,
                getContrailScale());
    }

    protected void spawnContrailWithOffset(double offsetX, double offsetY, double offsetZ) {
        if (!(this.level() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel)) {
            return;
        }
        if (com.hbm_m.client.missile.track.MissileTrackClient.shouldUseTrackWorldRender(this.getId())) {
            return;
        }
        Vec3 motion = new Vec3(this.xOld - this.getX(), this.yOld - this.getY(), this.zOld - this.getZ());
        if (motion.lengthSqr() <= 1.0E-8D) {
            motion = this.getDeltaMovement().reverse();
        }
        double len = motion.length();
        if (len <= 1.0E-8D) {
            return;
        }
        motion = motion.normalize();

        Vec3 thrust = new Vec3(0.0D, 1.0D, 0.0D);
        thrust = thrust.xRot(-this.getXRot() * ((float) Math.PI / 180.0F));
        thrust = thrust.yRot(-(this.getYRot() + 90.0F) * ((float) Math.PI / 180.0F));

        float contrailScale = getContrailScale();
        Vec3 exhaust = thrust.scale(-1.0D);
        com.hbm_m.client.missile.track.MissileTrackContrail.spawnSegments(
                clientLevel,
                this.getX(), this.getY(), this.getZ(),
                motion, len,
                exhaust,
                contrailScale,
                offsetX, offsetY, offsetZ);
    }

    protected void updateRotationFromMotion() {
        Vec3 motion = this.getDeltaMovement();

        // Защита от поворота на бок (pitch = -90) и случайного рыскания при нулевой скорости.
        // Именно из-за этого при призыве ракеты командой без движения она падала на бок в воздухе.
        if (motion.lengthSqr() < 1.0E-5D) {
            return;
        }
        
        double f2 = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float pitch = (float) (Math.atan2(motion.y, f2) * 180.0D / Math.PI) - 90.0F;
        float yaw = (float) (Math.atan2(this.targetX - this.getX(), this.targetZ - this.getZ()) * 180.0D / Math.PI);

        this.setXRot(pitch);
        this.setYRot(yaw);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (this.level().isClientSide || this.exploded) {
            return;
        }

        this.exploded = true;
        BlockPos pos;
        if (result.getType() == HitResult.Type.BLOCK) {
            pos = ((BlockHitResult) result).getBlockPos();
        } else {
            pos = this.blockPosition();
        }

        onMissileImpact(pos);
        releaseChunkTicket();
        this.discard();
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity entity) {
        return false;
    }

    protected abstract void onMissileImpact(BlockPos pos);

    public boolean canBeDetectedByRadar() {
        return true;
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LAUNCH_FACING, Direction.NORTH);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this, MobCategory.MISC.ordinal());
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(DATA_LAUNCH_FACING, Direction.NORTH);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }
    *///?}

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("StartX", this.startX);
        tag.putInt("StartZ", this.startZ);
        tag.putInt("TargetX", this.targetX);
        tag.putInt("TargetZ", this.targetZ);
        tag.putDouble("Velocity", this.velocity);
        tag.putDouble("DecelY", this.decelY);
        tag.putDouble("AccelXZ", this.accelXZ);
        tag.putInt("Health", this.health);
        tag.putBoolean("Exploded", this.exploded);
        tag.putString("LaunchFacing", this.getLaunchFacing().getName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.startX = tag.getInt("StartX");
        this.startZ = tag.getInt("StartZ");
        this.targetX = tag.getInt("TargetX");
        this.targetZ = tag.getInt("TargetZ");
        this.velocity = tag.getDouble("Velocity");
        this.decelY = tag.getDouble("DecelY");
        this.accelXZ = tag.getDouble("AccelXZ");
        this.health = tag.getInt("Health");
        this.exploded = tag.getBoolean("Exploded");
        if (tag.contains("LaunchFacing")) {
            Direction facing = Direction.byName(tag.getString("LaunchFacing"));
            this.setLaunchFacing(facing == null ? Direction.NORTH : facing);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.isInvulnerableTo(source) && !this.level().isClientSide) {
            if (this.health > 0) {
                this.health -= (int) amount;
                if (this.health <= 0) {
                    killMissile();
                }
            }
            return true;
        }
        return false;
    }

    protected void killMissile() {
        if (this.isRemoved()) {
            return;
        }
        releaseChunkTicket();
        if (!this.level().isClientSide && this.level() instanceof ServerLevel server) {
            // 1.7.10 EntityMissileBaseNT.killMissile: ОДИН createExplosion(5F) + shrapnel + debris.
            // Лишний server.explode(NONE) убран — он давал второй «бум» поверх explosion из
            // missileDestroyed (TNT), который и выполняет разрушение блоков + звук.
            Vec3 motion = this.getDeltaMovement();
            MissileWarheadEffects.missileDestroyed(server, this,
                    this.getX(), this.getY(), this.getZ(),
                    motion, getDebris(), getDebrisRareDrop());
        }
        this.discard();
    }

    protected void cluster() {
        if (this.level().isClientSide || this.exploded) {
            return;
        }
        this.exploded = true;
        onMissileImpact(BlockPos.containing(getX(), getY(), getZ()));
    }

    private void updateChunkTicket() {
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        ChunkPos newPos = new ChunkPos(this.blockPosition());
        if (!newPos.equals(this.loadedChunk)) {
            if (this.loadedChunk != null) {
                server.getChunkSource().removeRegionTicket(CHUNK_TICKET, this.loadedChunk, CHUNK_TICKET_RADIUS, this.getUUID());
            }
            this.loadedChunk = newPos;
            server.getChunkSource().addRegionTicket(CHUNK_TICKET, this.loadedChunk, CHUNK_TICKET_RADIUS, this.getUUID());
        }
        updateLookaheadTickets(server);
    }

    /**
     * Laedt den Flugweg voraus.
     *
     * <p>Das Ticket um die eigene Position herum haelt nur die unmittelbaren Nachbarchunks auf
     * Entitaetenstufe, also ein Fenster von 48 Bloecken. Eine Rakete mit vier Bloecken pro Tick
     * braucht ein Dutzend Ticks, um da herauszufliegen - genug Zeit, damit das naechste Ticket
     * traegt. Eine Rakete mit hundert Bloecken pro Tick ist nach einem einzigen Tick draussen,
     * landet in einem ungeladenen Chunk und hoert dort auf zu ticken: sie haengt in der Luft, der
     * Einschlag passiert erst, wenn ein Spieler die Gegend laedt.
     *
     * <p>Deshalb halten schnelle Raketen zusaetzlich Tickets auf den Chunks, in denen sie in den
     * naechsten Ticks sein werden. Fuer alles, was seinen aktuellen Chunk nicht pro Tick verlaesst,
     * aendert sich dadurch nichts - die Vorhersage landet dann im selben Chunk und wird verworfen.
     */
    private void updateLookaheadTickets(ServerLevel server) {
        java.util.Set<ChunkPos> wanted = null;

        Vec3 perTick = this.getDeltaMovement().scale(this.velocity);
        if (perTick.lengthSqr() > 1.0D) {
            Vec3 pos = this.position();
            for (int i = 1; i <= CHUNK_LOOKAHEAD_TICKS; i++) {
                Vec3 predicted = pos.add(perTick.scale(i));
                ChunkPos chunk = new ChunkPos(Mth.floor(predicted.x) >> 4, Mth.floor(predicted.z) >> 4);
                if (chunk.equals(this.loadedChunk)) {
                    continue;
                }
                if (wanted == null) {
                    wanted = new java.util.HashSet<>();
                }
                wanted.add(chunk);
                if (wanted.size() >= CHUNK_LOOKAHEAD_MAX) {
                    break;
                }
            }
        }

        if (wanted == null) {
            releaseLookaheadTickets(server);
            return;
        }

        for (java.util.Iterator<ChunkPos> it = this.lookaheadChunks.iterator(); it.hasNext(); ) {
            ChunkPos held = it.next();
            if (!wanted.contains(held)) {
                server.getChunkSource().removeRegionTicket(CHUNK_TICKET, held, CHUNK_LOOKAHEAD_RADIUS, this.getUUID());
                it.remove();
            }
        }
        for (ChunkPos chunk : wanted) {
            if (this.lookaheadChunks.add(chunk)) {
                server.getChunkSource().addRegionTicket(CHUNK_TICKET, chunk, CHUNK_LOOKAHEAD_RADIUS, this.getUUID());
            }
        }
    }

    private void releaseLookaheadTickets(ServerLevel server) {
        if (this.lookaheadChunks.isEmpty()) {
            return;
        }
        for (ChunkPos held : this.lookaheadChunks) {
            server.getChunkSource().removeRegionTicket(CHUNK_TICKET, held, CHUNK_LOOKAHEAD_RADIUS, this.getUUID());
        }
        this.lookaheadChunks.clear();
    }

    protected void releaseChunkTicket() {
        if (this.level() instanceof ServerLevel server) {
            releaseLookaheadTickets(server);
        }
        if (this.loadedChunk == null) {
            return;
        }
        if (this.level() instanceof ServerLevel server) {
            server.getChunkSource().removeRegionTicket(CHUNK_TICKET, this.loadedChunk, CHUNK_TICKET_RADIUS, this.getUUID());
        }
        this.loadedChunk = null;
    }

    //? if < 1.21.1 {
    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        onAddedToLevelHook();
    }
    //?} else {
    /*@Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        onAddedToLevelHook();
    }
    *///?}

    private void onAddedToLevelHook() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel server) {
            // Ракета сама держит region‑тикет на свой чанк, чтобы летать сквозь выгруженные
            // чанки. Вдали от игрока PersistentEntitySectionManager изредка ре‑десериализует
            // её из устаревшего entity‑chunk — копия наследует UUID оригинала. Если живая
            // ракета с таким UUID уже есть в этом уровне — это клон: выкидываем его до того,
            // как он получит тикет/контрейл, иначе в воздухе появится вторая (и третья) ракета
            // и второй контакт на радаре.
            if (com.hbm_m.server.missile.MissileTrackBroadcaster.isDuplicateSpawn(server, this)) {
                LOGGER.warn("Discarding duplicate missile {} (uuid={}) — a live missile with the same UUID already exists",
                        this.getId(), this.getUUID());
                this.discard();
                return;
            }
            if (this.loadedChunk == null) {
                this.loadedChunk = new ChunkPos(this.blockPosition());
                server.getChunkSource().addRegionTicket(CHUNK_TICKET, this.loadedChunk, CHUNK_TICKET_RADIUS, this.getUUID());
            }
            com.hbm_m.server.missile.MissileTrackBroadcaster.onMissileSpawned(this);
        }
    }

    //? if < 1.21.1 {
    @Override
    public void onRemovedFromWorld() {
        onRemovedFromLevelHook();
        super.onRemovedFromWorld();
    }
    //?} else {
    /*@Override
    public void onRemovedFromLevel() {
        onRemovedFromLevelHook();
        super.onRemovedFromLevel();
    }
    *///?}

    private void onRemovedFromLevelHook() {
        if (!this.level().isClientSide) {
            com.hbm_m.server.missile.MissileTrackBroadcaster.onMissileRemoved(this);
        }
        releaseChunkTicket();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }
}