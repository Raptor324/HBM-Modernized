package com.hbm_m.blockentity.machines;

import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.ModEntities;
<<<<<<< HEAD
import com.hbm_m.entity.logic.EntityPantsir;
=======
import com.hbm_m.entity.logic.EntityPlaneBase;
import com.hbm_m.entity.missile.MissileAntiBallisticEntity;
import com.hbm_m.entity.missile.MissileBaseEntity;
import com.hbm_m.entity.missile.MissilePantsirEntity;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.server.missile.MissileTrackBroadcaster;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.EntityDamageUtil;
>>>>>>> be20ad67d07abcf84297fe9a13f3b1a494637d54

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Der fruehere feste Pantsir-S1. Der Pantsir ist jetzt ein Fahrzeug ({@link EntityPantsir}); dieser Block
 * existiert nur noch in alten Welten und wandelt sich beim ersten Tick samt Munition und Besitzer um.
 */
public class PantsirBlockEntity extends BaseMachineBlockEntity {

    private boolean active = true;
    private boolean iff = true;
    private int ammo = EntityPantsir.MAX_AMMO;
    private int shells = EntityPantsir.MAX_SHELLS / 2;
    @Nullable
    private UUID owner;

    public PantsirBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PANTSIR_S1_BE.get(), pos, state, 1, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PantsirBlockEntity be) {
        if (level instanceof ServerLevel server) {
            be.convert(server, pos, state);
        }
    }

    private void convert(ServerLevel server, BlockPos pos, BlockState state) {
        EntityPantsir vehicle = ModEntities.PANTSIR.get().create(server);
        if (vehicle == null) {
            return;
        }
<<<<<<< HEAD
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        vehicle.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, facing.toYRot(), 0.0F);
        vehicle.restore(this.active, this.iff, this.ammo, this.shells, this.owner);
        server.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
        server.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        server.addFreshEntity(vehicle);
=======
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
>>>>>>> be20ad67d07abcf84297fe9a13f3b1a494637d54
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.pantsir_s1");
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
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.active = !tag.contains("pantsir_active") || tag.getBoolean("pantsir_active");
        this.iff = !tag.contains("pantsir_iff") || tag.getBoolean("pantsir_iff");
        this.owner = tag.hasUUID("owner_uuid") ? tag.getUUID("owner_uuid") : null;
        this.ammo = tag.contains("pantsir_ammo") ? tag.getInt("pantsir_ammo") : EntityPantsir.MAX_AMMO;
        this.shells = tag.contains("pantsir_shells") ? tag.getInt("pantsir_shells") : EntityPantsir.MAX_SHELLS / 2;
    }
}
