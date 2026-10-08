package com.hbm_m.blockentity.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.projectile.EntityBulletBaseMK4;
import com.hbm_m.inventory.ComparableStack;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.menu.TurretMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm_m.item.weapon.sedna.factory.XFactory50;
import com.hbm_m.item.weapon.sedna.factory.XFactory556mm;
import com.hbm_m.item.weapon.sedna.factory.XFactory9mm;
import com.hbm_m.item.weapon.sedna.factory.XFactoryAccelerator;
import com.hbm_m.item.weapon.sedna.factory.XFactoryFlamer;
import com.hbm_m.item.weapon.sedna.factory.XFactoryRocket;
import com.hbm_m.item.weapon.sedna.factory.XFactoryTurret;
import com.hbm_m.particle.SpentCasing;
import com.hbm_m.particle.SpentCasing.CasingType;
import com.hbm_m.particle.helper.CasingCreator;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.EntityDamageUtil;
import com.hbm_m.util.Vec3NT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Port aller Turret-Varianten aus der 1.7.10-Originalmod (TileEntityTurretBaseNT-Familie).
 * Eine gemeinsame Klasse fuer alle 11 Varianten, parametrisiert per {@link TurretStats}.
 *
 * <p>Die Geschuetztuerme Sentry, Chekhov, Friendly, Jeremy, Tauon, Richard, Howard und Fritz laufen
 * wie im Original ueber das SEDNA-Waffensystem: Munition = {@link BulletConfig#ammo} der Configs aus
 * {@code getAmmoList()}, Projektile = {@link EntityBulletBaseMK4} (Original {@code spawnBullet}),
 * Feuertakt/Schaden/Sounds/Partikel aus den jeweiligen {@code TileEntityTurretX#updateFiringTick}.
 * Arty/Himars (eigene Artilleriegranaten/Raketen) und Maxwell (Upgrades) sind davon unberuehrt.</p>
 */
public class TurretBaseBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2, com.hbm_m.blockentity.IRadarCommandReceiver,
        com.hbm_m.api.redstoneoverradio.IRORInteractive {

    private static final int AMMO_SLOT_COUNT = 9;
    private static final int BATTERY_SLOT = 9;
    /** Original Slot 0 (KI-Chip {@code turret_chip}); im Port hinten angehaengt, damit bestehende Slots gleich bleiben. */
    public static final int CHIP_SLOT = 10;
    private static final int SLOT_COUNT = 11;

    private static final float AIM_TOLERANCE_DEG = 3.0F;

    // Original WeaponConfig.ciwsHitrate: com.hbm_m.config.WeaponConfig.ciwsHitrate (server.json)

    /** Original {@code GunDGKFactory.CASINGDGK}. */
    public static final SpentCasing CASINGDGK = new SpentCasing(CasingType.STRAIGHT).setScale(1.5F).setBounceMotion(1F, 0.5F).setColor(SpentCasing.COLOR_CASE_BRASS).register("DGK").setupSmoke(0.02F, 0.5D, 60, 20).setMaxAge(60); //3 instead of 12 seconds

    private final TurretStats stats;
    private int cooldown = 0;
    private boolean hasTarget = false;

    /** On/Off-Schalter + Ziel-Kategorien (Original: {@code TileEntityTurretBaseNT}), per GUI-Button umschaltbar. */
    private boolean isOn = true;
    private boolean targetPlayers = false;
    private boolean targetAnimals = false;
    private boolean targetMobs = true;
    private boolean targetMachines = true;

    /** Yaw/Pitch der Zielverfolgung in Grad, synchronisiert fuer die clientseitige Animation. */
    public float yaw, prevYaw;
    public float pitch, prevPitch;
    private float desiredYaw;
    private float desiredPitch;
    private boolean wasActiveLastTick = false;
    private boolean hadTargetLastTick = false;
    @Nullable private LivingEntity lastTarget = null;

    /** Original {@code target}/{@code tPos}/{@code searchTimer}/{@code aligned}/{@code stattrak} (Strichliste der Abschuesse). */
    @Nullable private Entity target = null;
    @Nullable private Vec3 tPos = null;
    private int searchTimer;
    private boolean aligned = false;
    private int stattrak;

    /** Rueckstoss-Animation der beiden Sentry-Laeufe (abwechselnd, Original: {@code RenderTurretSentry}). */
    public float barrelLeftOffset, prevBarrelLeftOffset;
    public float barrelRightOffset, prevBarrelRightOffset;

    /** Gatling-Spinup/Spindown (Chekhov/Friendly, Original: {@code TileEntityTurretChekhov#spin}). */
    private float spin = 0.0F;
    public float barrelSpinAngle, prevBarrelSpinAngle;

    // --- SEDNA-Feuerlogik (Original: Felder der einzelnen TileEntityTurretX) ---
    /** {@code timer} aller Geschuetztuerme. */
    private int timer = 0;
    /** {@code TileEntityTurretSentry#shotSide}. */
    private boolean shotSide = false;
    /** {@code TileEntityTurretJeremy#reload} bzw. {@code TileEntityTurretRichard#reload}. */
    private int reload = 0;
    /** {@code TileEntityTurretHoward#loaded} bzw. {@code TileEntityTurretRichard#loaded}. */
    private int loaded = 0;
    /** {@code TileEntityTurretBaseNT#casingDelay} + {@code cachedCasingConfig}. */
    private int casingDelay = 0;
    @Nullable private SpentCasing cachedCasingConfig = null;
    /** {@code TileEntityTurretFritz#tank}: Diesel, 16000 mB. */
    private final FluidTank tank;

    /** Kran-Ladeanimation (Himars, Original: {@code crane}-Fortschritt vor jedem Schuss). */
    public float himarsCraneProgress, prevHimarsCraneProgress;
    private static final float HIMARS_FIXED_PITCH = 45.0F;

    /** Feuermodus (aktuell nur Arty: 0=Artillerie/indirekt, 1=Kanone/direkt+LOS, 2=Manuell/vereinfacht=Artillerie). */
    public static final int ARTY_MODE_ARTILLERY = 0;
    public static final int ARTY_MODE_CANNON = 1;
    public static final int ARTY_MODE_MANUAL = 2;
    private int fireMode = ARTY_MODE_ARTILLERY;
    public float barrelRecoilOffset, prevBarrelRecoilOffset;

    public TurretBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, TurretStats stats) {
        // Original getMaxPower() je Geschuetz; IEnergyReceiverMK2 nimmt bis zum Fuellstand auf
        super(type, pos, state, SLOT_COUNT, stats.getMaxPower(), stats.getMaxPower(), 0L);
        this.stats = stats;
        this.tank = new FluidTank(ModFluids.DIESEL.getSource(), 16000);
    }

    // --- Statische Fabrikmethoden fuer die BlockEntityType-Registrierung (siehe ModBlockEntities) ---
    public static TurretBaseBlockEntity createSentry(BlockPos pos, BlockState state)   { return new TurretBaseBlockEntity(ModBlockEntities.TURRET_SENTRY_BE.get(), pos, state, TurretStats.SENTRY); }
    public static TurretBaseBlockEntity createChekhov(BlockPos pos, BlockState state)  { return new TurretBaseBlockEntity(ModBlockEntities.TURRET_CHEKHOV_BE.get(), pos, state, TurretStats.CHEKHOV); }
    public static TurretBaseBlockEntity createFriendly(BlockPos pos, BlockState state) { return new TurretBaseBlockEntity(ModBlockEntities.TURRET_FRIENDLY_BE.get(), pos, state, TurretStats.FRIENDLY); }
    public static TurretBaseBlockEntity createJeremy(BlockPos pos, BlockState state)   { return new TurretBaseBlockEntity(ModBlockEntities.TURRET_JEREMY_BE.get(), pos, state, TurretStats.JEREMY); }
    public static TurretBaseBlockEntity createTauon(BlockPos pos, BlockState state)    { return new TurretBaseBlockEntity(ModBlockEntities.TURRET_TAUON_BE.get(), pos, state, TurretStats.TAUON); }
    public static TurretBaseBlockEntity createRichard(BlockPos pos, BlockState state)  { return new TurretBaseBlockEntity(ModBlockEntities.TURRET_RICHARD_BE.get(), pos, state, TurretStats.RICHARD); }
    public static TurretBaseBlockEntity createHoward(BlockPos pos, BlockState state)   { return new TurretBaseBlockEntity(ModBlockEntities.TURRET_HOWARD_BE.get(), pos, state, TurretStats.HOWARD); }
    public static TurretBaseBlockEntity createMaxwell(BlockPos pos, BlockState state)  { return new TurretBaseBlockEntity(ModBlockEntities.TURRET_MAXWELL_BE.get(), pos, state, TurretStats.MAXWELL); }
    public static TurretBaseBlockEntity createFritz(BlockPos pos, BlockState state)    { return new TurretBaseBlockEntity(ModBlockEntities.TURRET_FRITZ_BE.get(), pos, state, TurretStats.FRITZ); }
    public static TurretBaseBlockEntity createArty(BlockPos pos, BlockState state)     { return new TurretBaseBlockEntity(ModBlockEntities.TURRET_ARTY_BE.get(), pos, state, TurretStats.ARTY); }
    public static TurretBaseBlockEntity createHimars(BlockPos pos, BlockState state)   { return new TurretBaseBlockEntity(ModBlockEntities.TURRET_HIMARS_BE.get(), pos, state, TurretStats.HIMARS); }
    public static TurretBaseBlockEntity createSentryDamaged(BlockPos pos, BlockState state) { return new TurretBaseBlockEntity(ModBlockEntities.TURRET_SENTRY_DAMAGED_BE.get(), pos, state, TurretStats.SENTRY_DAMAGED); }
    public static TurretBaseBlockEntity createHowardDamaged(BlockPos pos, BlockState state) { return new TurretBaseBlockEntity(ModBlockEntities.TURRET_HOWARD_DAMAGED_BE.get(), pos, state, TurretStats.HOWARD_DAMAGED); }

    public static void tick(Level level, BlockPos pos, BlockState state, TurretBaseBlockEntity be) {
        if (level.isClientSide()) {
            be.clientTick();
            return;
        }
        be.serverTick(level, pos);
    }

    /** Die Geschuetztuerme, die im Original ueber SEDNA-BulletConfigs bzw. {@code EntityBulletBaseMK4} feuern. */
    private boolean isSednaTurret() {
        return stats == TurretStats.SENTRY || stats == TurretStats.CHEKHOV || stats == TurretStats.FRIENDLY
                || stats == TurretStats.JEREMY || stats == TurretStats.TAUON || stats == TurretStats.RICHARD
                || stats == TurretStats.HOWARD || stats == TurretStats.FRITZ
                || stats == TurretStats.SENTRY_DAMAGED || stats == TurretStats.HOWARD_DAMAGED;
    }

    private boolean isGatling() {
        return stats == TurretStats.CHEKHOV || stats == TurretStats.FRIENDLY;
    }

    private void serverTick(Level level, BlockPos pos) {
        ensureNetworkInitialized();
        tickLegacyMigration(level, pos);
        chargeFromBatterySlot(BATTERY_SLOT);

        // SEDNA-Tuerme: Original-Ablauf (Energie pro Tick, Zielsuche, Sichtlinie) - siehe serverTickSedna
        if (isSednaTurret()) {
            serverTickSedna(level, pos);
            return;
        }

        if (cooldown > 0) cooldown--;

        prevBarrelLeftOffset = barrelLeftOffset;
        prevBarrelRightOffset = barrelRightOffset;
        prevBarrelSpinAngle = barrelSpinAngle;
        prevBarrelRecoilOffset = barrelRecoilOffset;
        barrelLeftOffset *= 0.6F;
        barrelRightOffset *= 0.6F;
        barrelRecoilOffset *= 0.7F;

        // Arty/Himars: eigene updateEntity() (TileEntityTurretArty / TileEntityTurretHIMARS)
        if (stats == TurretStats.ARTY || stats == TurretStats.HIMARS) {
            tickArtillery(level, pos);
            return;
        }

        if (!isOn) {
            hasTarget = false;
            lastTarget = null;
            if (isGatling()) tickGatlingSpin(false);
            if (wasActiveLastTick) {
                setChanged();
                sendUpdateToClient();
                wasActiveLastTick = false;
            }
            return;
        }

        LivingEntity target = findTarget(level, pos);
        hasTarget = target != null;

        prevYaw = yaw;
        prevPitch = pitch;

        if (hasTarget && !hadTargetLastTick && !isSednaTurret()) {
            level.playSound(null, pos, com.hbm_m.sound.ModSounds.TOOL_TECH_BLEEP.get(), SoundSource.BLOCKS, 1.0F, 1.4F);
        }
        lastTarget = target;
        hadTargetLastTick = hasTarget;

        if (isGatling()) {
            // Original TileEntityTurretChekhov#updateEntity (Client): accel +-2, max 45.
            tickGatlingSpin(target != null);
        }

        if (target == null) {
            if (hasTarget != wasActiveLastTick) {
                setChanged();
                sendUpdateToClient();
            }
            wasActiveLastTick = hasTarget;
            return;
        }

        updateAim(pos, target);

        if (stats == TurretStats.MAXWELL) {
            tickMaxwell(level, pos, target);
        } else {
            boolean readyToFire = isAimed() && cooldown <= 0 && getEnergyStored() >= stats.energyPerShot;
            if (readyToFire) {
                int ammoSlot = findAmmoSlot();
                if (ammoSlot >= 0) {
                    fire(level, pos, target, ammoSlot);
                }
            }
        }

        setChanged();
        sendUpdateToClient();
        wasActiveLastTick = true;
    }

    /**
     * The point the gun actually sits at, and the single origin used for aiming, line of sight and
     * firing alike - the original's {@code getTurretPos()}.
     */
    private Vec3 turretOrigin(BlockPos pos) {
        // Original getTurretPos(): xCoord + getHorizontalOffset() - die Mitte der Grundplatte, nicht des Kerns
        Vec3 offset = getHorizontalOffset();
        return new Vec3(pos.getX() + offset.x, pos.getY() + 0.5D + stats.pivotY, pos.getZ() + offset.z);
    }

    // --- Mehrblockstruktur (Original BlockDummyable) ---

    /**
     * Turm aus der Einzelblock-Zeit des Ports, dessen 2x2/4x4-Struktur (noch) nicht steht. Er zielt und
     * zeichnet weiter um die Blockmitte, bis die Auto-Reparatur die Dummy-Zellen setzen konnte.
     */
    private boolean legacySingle = false;

    public boolean isLegacySingle() {
        return legacySingle;
    }

    /**
     * Original {@code TileEntityTurretBaseNT#getHorizontalOffset()}: XZ-Versatz vom Kern zur Mitte der
     * 2x2-Grundplatte, je nach Ausrichtung ({@code meta - offset}: 2=N, 4=W, 5=O, sonst 0). Arty/HIMARS
     * erben das unveraendert; Sentry ({@code TileEntityTurretSentry}) liefert die Blockmitte.
     * Port-FACING entspricht dem Original-{@code dir} (siehe DummyableStructureBuilder).
     */
    public Vec3 getHorizontalOffset() {
        BlockState state = getBlockState();
        if (legacySingle || !(state.getBlock() instanceof com.hbm_m.block.machines.TurretMultiblockBlock)) {
            return new Vec3(0.5D, 0.0D, 0.5D);
        }
        Direction facing = state.getValue(com.hbm_m.block.machines.DummyableMachineBlock.FACING);
        if (facing == Direction.NORTH) return new Vec3(1, 0, 1);
        if (facing == Direction.WEST) return new Vec3(1, 0, 0);
        if (facing == Direction.EAST) return new Vec3(0, 0, 1);
        return new Vec3(0, 0, 0);
    }

    /** Struktur steht vollstaendig - dann entfaellt die Pruefung bis zum naechsten Laden. */
    private boolean structureVerified = false;

    /**
     * Migration alter Welten und Weltgen-Kerne (Silo: {@code placeCore} setzt nur den Kern, {@code fillSpace} ist
     * im Port leer): solange die Struktur unvollstaendig ist, wird alle 100 Ticks die Auto-Reparatur versucht
     * (setzt die Zellen nur auf freiem Platz). Ein Einzelblock-Turm wird dabei zum Kern.
     */
    private void tickLegacyMigration(Level level, BlockPos pos) {
        if (structureVerified || level.getGameTime() % 100L != 0L) return;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof com.hbm_m.block.machines.TurretMultiblockBlock block)) {
            legacySingle = false;
            structureVerified = true;
            return;
        }
        if (!isStructureComplete(level, pos, state, block)) {
            block.getStructureHelper().attemptAutoRepair(level, pos, state, block);
        }
        if (!isRemoved() && isStructureComplete(level, pos, state, block)) {
            structureVerified = true;
            if (legacySingle) {
                legacySingle = false;
                setChanged();
                sendUpdateToClient();
            }
        }
    }

    private static boolean isStructureComplete(Level level, BlockPos pos, BlockState state, com.hbm_m.block.machines.TurretMultiblockBlock block) {
        Direction facing = state.getValue(com.hbm_m.block.machines.DummyableMachineBlock.FACING);
        for (BlockPos p : block.getStructureHelper().getAllPartPositions(pos, facing)) {
            if (p.equals(pos)) continue;
            if (!(level.getBlockEntity(p) instanceof com.hbm_m.interfaces.IMultiblockPart part) || !pos.equals(part.getControllerPos())) {
                return false;
            }
        }
        return true;
    }

    /** Original {@code getRenderBoundingBox() -> INFINITE_EXTENT_AABB}: Laeufe ragen weit ueber den Kern hinaus. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /** {@code getEntityPos}: the original aims at the target's centre of mass, not its eyes. */
    private static Vec3 targetPoint(LivingEntity target) {
        return new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
    }

    private void updateAim(BlockPos pos, LivingEntity target) {
        Vec3 center = turretOrigin(pos);
        Vec3 to = targetPoint(target).subtract(center);
        double horizontalDist = Math.sqrt(to.x * to.x + to.z * to.z);

        desiredYaw = (float) Math.toDegrees(Math.atan2(to.z, to.x)) + (float) stats.yawExtraOffsetDeg;
        desiredPitch = stats == TurretStats.HIMARS ? HIMARS_FIXED_PITCH
                : (float) Math.toDegrees(Math.atan2(to.y, horizontalDist));

        yaw = turnToward(yaw, desiredYaw, (float) stats.yawSpeed);
        pitch = turnToward(pitch, desiredPitch, (float) stats.pitchSpeed);
    }

    private static float turnToward(float current, float target, float maxDelta) {
        float delta = Mth.wrapDegrees(target - current);
        delta = Mth.clamp(delta, -maxDelta, maxDelta);
        return current + delta;
    }

    /** Original {@code getAcceptableInaccuracy()}: 15 Grad fuer Sentry/Chekhov/Friendly/Fritz, sonst 5. */
    private float aimTolerance() {
        if (stats == TurretStats.SENTRY || stats == TurretStats.CHEKHOV || stats == TurretStats.FRIENDLY || stats == TurretStats.FRITZ) return 15.0F;
        if (isSednaTurret()) return 5.0F;
        return AIM_TOLERANCE_DEG;
    }

    private boolean isAimed() {
        float tol = aimTolerance();
        return Math.abs(Mth.wrapDegrees(yaw - desiredYaw)) < tol
                && Math.abs(Mth.wrapDegrees(pitch - desiredPitch)) < tol;
    }

    /** Original {@code rotationYaw} (Bogenmass, MC-Konvention) aus der Port-Ausrichtung in Grad. */
    private double origRotationYaw() {
        return Math.toRadians(yaw - stats.yawExtraOffsetDeg - 90.0D);
    }

    /** Original {@code rotationPitch} (Bogenmass, positiv = nach oben). */
    private double origRotationPitch() {
        return Math.toRadians(pitch);
    }

    @Nullable
    private LivingEntity findTarget(Level level, BlockPos pos) {
        // Original seekNewTarget(): Suchbox und Abstand ab getTurretPos() zum getEntityPos() des Ziels
        Vec3 turretCenter = turretOrigin(pos);
        AABB area = new AABB(turretCenter, turretCenter).inflate(stats.range);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && isAcceptableTarget(e));

        LivingEntity closest = null;
        double closestDistSqr = Double.MAX_VALUE;

        boolean requireLos = !(stats == TurretStats.ARTY && fireMode != ARTY_MODE_CANNON);

        for (LivingEntity candidate : candidates) {
            double distSqr = targetPoint(candidate).distanceToSqr(turretCenter);
            if (distSqr < closestDistSqr && (!requireLos || hasLineOfSight(level, pos, candidate))) {
                closestDistSqr = distSqr;
                closest = candidate;
            }
        }
        return closest;
    }

    /**
     * Ziel-Kategorie-Filter fuer Arty/Himars/Maxwell: Original {@code TileEntityTurretBaseNT#entityAcceptableTarget}
     * (siehe {@link #entityAcceptableTarget}).
     */
    private boolean isAcceptableTarget(LivingEntity e) {
        return entityAcceptableTarget(e);
    }

    private boolean hasLineOfSight(BlockGetter level, BlockPos pos, LivingEntity target) {
        Vec3 from = turretOrigin(pos);
        Vec3 to = targetPoint(target);
        ClipContext ctx = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target);
        BlockHitResult hit = ((Level) level).clip(ctx);
        return hit.getType() == HitResult.Type.MISS;
    }

    // ------------------------------------------------------------------------------------------
    // SEDNA (Original: TileEntityTurretBaseNT#getAmmoList / getFirstConfigLoaded / spawnBullet / conusmeAmmo)
    // ------------------------------------------------------------------------------------------

    /** Original {@code getAmmoList()} der einzelnen Tuerme ("Yes, new turrets fire BulletNTs."). */
    public List<BulletConfig> getAmmoList() {
        List<BulletConfig> configs = new ArrayList<>();
        switch (kind()) {
            case SENTRY -> addAll(configs, XFactory9mm.p9_sp, XFactory9mm.p9_fmj, XFactory9mm.p9_jhp, XFactory9mm.p9_ap);
            case CHEKHOV -> addAll(configs, XFactory50.bmg50_sp, XFactory50.bmg50_fmj, XFactory50.bmg50_jhp, XFactory50.bmg50_ap, XFactory50.bmg50_du);
            case FRIENDLY -> addAll(configs, XFactory556mm.r556_sp, XFactory556mm.r556_fmj, XFactory556mm.r556_jhp, XFactory556mm.r556_ap);
            case JEREMY -> addAll(configs, XFactoryTurret.shell_normal, XFactoryTurret.shell_explosive, XFactoryTurret.shell_ap, XFactoryTurret.shell_du, XFactoryTurret.shell_w9);
            case TAUON -> addAll(configs, XFactoryAccelerator.tau_uranium);
            case RICHARD -> { if (XFactoryRocket.rocket_ml != null) addAll(configs, XFactoryRocket.rocket_ml); }
            case HOWARD -> addAll(configs, XFactoryTurret.dgk_normal);
            default -> { }
        }
        return configs;
    }

    private static void addAll(List<BulletConfig> list, BulletConfig... configs) {
        for (BulletConfig c : configs) if (c != null) list.add(c);
    }

    /** Original {@code getAmmoTypesForDisplay()}. */
    public List<ItemStack> getAmmoTypesForDisplay() {
        List<ItemStack> stacks = new ArrayList<>();
        if (stats == TurretStats.FRITZ) {
            stacks.add(new ItemStack(WeaponItems.ammo(EnumAmmo.FLAME_DIESEL)));
            // TileEntityTurretFritz: alle brennbaren Fluessigkeiten (FT_Combustible + FT_Liquid) als Fluessigkeitssymbol
            for (com.hbm_m.inventory.fluid.ModFluids.FluidEntry entry : com.hbm_m.api.fluids.HbmFluidRegistry.getOrderedFluids()) {
                Fluid type = entry.getSource();
                if (FluidType.hasTrait(type, com.hbm_m.inventory.fluid.trait.FT_Combustible.class) && FluidType.hasTrait(type, com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Liquid.class)) {
                    ItemStack icon = com.hbm_m.item.weapon.sedna.mags.MagazineFluid.fluidIcon(type);
                    if (!icon.isEmpty()) stacks.add(icon);
                }
            }
            return stacks;
        }
        for (BulletConfig config : getAmmoList()) {
            if (config.ammo != null) stacks.add(config.ammo.toStack());
        }
        return stacks;
    }

    @Nullable
    private BulletConfig getFirstConfigLoaded() {
        List<BulletConfig> list = getAmmoList();
        if (list.isEmpty()) return null;

        //doing it like this will fire slots in the right order, not in the order of the configs
        for (int i = 0; i < AMMO_SLOT_COUNT; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                for (BulletConfig conf : list) {
                    if (conf.ammo != null && conf.ammo.matchesRecipe(stack, true)) return conf;
                }
            }
        }
        return null;
    }

    private void conusmeAmmo(ComparableStack ammo) {
        for (int i = 0; i < AMMO_SLOT_COUNT; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && ammo.matchesRecipe(stack, true)) {
                stack.shrink(1);
                inventory.setStackInSlot(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
                setChanged();
                return;
            }
        }
        setChanged();
    }

    /** Original {@code getBarrelLength()}. */
    private double getBarrelLength() {
        return switch (kind()) {
            case SENTRY, RICHARD -> 1.25D;
            case CHEKHOV, FRIENDLY -> 3.5D;
            case JEREMY -> 4.25D;
            case TAUON -> 2.0D - 0.0625D;
            case HOWARD -> 3.25D;
            case FRITZ -> 2.25D;
            default -> 1.0D;
        };
    }

    private boolean usesCasings() {
        TurretStats stats = kind();
        return stats == TurretStats.SENTRY || stats == TurretStats.CHEKHOV || stats == TurretStats.FRIENDLY
                || stats == TurretStats.JEREMY || stats == TurretStats.HOWARD;
    }

    private int casingDelayFor() {
        return stats == TurretStats.JEREMY ? 22 : 0;
    }

    /** Laufmuendung: {@code getTurretPos() + (barrelLength,0,0)} gedreht wie im Original. */
    private Vec3NT muzzleOffset(double length) {
        Vec3NT vec = Vec3NT.createVectorHelper(length, 0, 0);
        vec.rotateAroundZ((float) -origRotationPitch());
        vec.rotateAroundY((float) -(origRotationYaw() + Math.PI * 0.5));
        return vec;
    }

    /** Original {@code spawnBullet(BulletConfig, float)}. Richard setzt zusaetzlich {@code lockonTarget}. */
    private void spawnBullet(Level level, BlockPos pos, BulletConfig bullet, float baseDamage, @Nullable Entity lockon) {
        Vec3 tp = turretOrigin(pos);
        Vec3NT vec = muzzleOffset(getBarrelLength());

        EntityBulletBaseMK4 proj = new EntityBulletBaseMK4(level, bullet, baseDamage, bullet.spread, (float) origRotationYaw(), (float) origRotationPitch());
        proj.setPos(tp.x + vec.xCoord, tp.y + vec.yCoord, tp.z + vec.zCoord);
        proj.xo = proj.getX(); proj.yo = proj.getY(); proj.zo = proj.getZ();
        if (lockon != null) proj.lockonTarget = lockon;
        level.addFreshEntity(proj);

        if (stats != TurretStats.RICHARD && usesCasings()) {
            if (casingDelayFor() == 0) {
                spawnCasing(level, pos);
            } else {
                casingDelay = casingDelayFor();
            }
        }
    }

    private void playSoundNT(Level level, BlockPos pos, String sound, float volume, float pitch) {
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), HbmSoundsNT.get(sound), SoundSource.BLOCKS, volume, pitch);
    }

    /** Original {@code AuxParticlePacketNT} mit {@code type=vanillaExt, mode=largeexplode} (Radius 50). */
    private void sendLargeExplode(Level level, double x, double y, double z, float size, byte count) {
        if (!(level instanceof ServerLevel server)) return;
        CompoundTag data = new CompoundTag();
        data.putString("type", "vanillaExt");
        data.putString("mode", "largeexplode");
        data.putFloat("size", size);
        data.putByte("count", count);
        IParticleCreator.sendPacket(server, x, y, z, 50, data);
    }

    /** Laeuft jeden Server-Tick (Original: die {@code updateEntity()}-Overrides der einzelnen Tuerme). */
    private void preTickSedna(Level level, BlockPos pos) {
        if (!isSednaTurret()) return;

        // TileEntityTurretBaseNT#updateEntity: verzoegerter Huelsenauswurf
        if (usesCasings() && casingDelayFor() > 0) {
            if (casingDelay > 0) {
                casingDelay--;
            } else {
                spawnCasing(level, pos);
            }
        }

        if (stats == TurretStats.JEREMY) {
            if (reload > 0) reload--;
            if (reload == 1) playSoundNT(level, pos, "hbm:turret.jeremy_reload", 2.0F, 1.0F);
        }

        if (isGatling()) {
            // TileEntityTurretChekhov#updateEntity (Server): ohne Ziel laeuft der Timer zurueck
            if (tPos == null) {
                timer--;
                if (timer > 20) timer = 20;
                if (timer < 0) timer = 0;
            }
        }

        if (stats == TurretStats.HOWARD) {
            if (loaded <= 0) {
                BulletConfig conf = getFirstConfigLoaded();
                if (conf != null) {
                    conusmeAmmo(conf.ammo);
                    playSoundNT(level, pos, "hbm:turret.howard_reload", 4.0F, 1F);
                    loaded = 200;
                }
            }
        }

        if (stats == TurretStats.RICHARD) {
            if (reload > 0) {
                reload--;
                if (reload == 0) loaded = 17;
            }
            if (loaded <= 0 && reload <= 0 && getFirstConfigLoaded() != null) {
                reload = 100;
            }
            if (getFirstConfigLoaded() == null) {
                loaded = 0;
            }
        }

        if (stats == TurretStats.FRITZ) {
            tickFritzTank();
        }
    }

    /** Original {@code updateFiringTick()} der einzelnen Tuerme (nur wenn ausgerichtet). */
    private void updateFiringTickSedna(Level level, BlockPos pos, Entity target) {
        // Energie: Original verbraucht getConsumption() pro Tick in serverTickSedna, nicht pro Schuss.
        Vec3 tp = turretOrigin(pos);

        switch (stats) {
            case SENTRY -> {
                timer++;
                if (timer % 10 == 0) {
                    BulletConfig conf = getFirstConfigLoaded();
                    if (conf != null) {
                        this.cachedCasingConfig = conf.casing;
                        spawnBullet(level, pos, conf, 5F, null);
                        conusmeAmmo(conf.ammo);
                        playSoundNT(level, pos, "hbm:turret.sentry_fire", 2.0F, 1.0F);

                        Vec3NT vec = muzzleOffset(getBarrelLength());
                        Vec3NT side = Vec3NT.createVectorHelper(0.125 * (shotSide ? 1 : -1), 0, 0);
                        side.rotateAroundY((float) -(origRotationYaw()));
                        sendLargeExplode(level, tp.x + vec.xCoord + side.xCoord, tp.y + vec.yCoord, tp.z + vec.zCoord + side.zCoord, 1F, (byte) 1);

                        if (shotSide) {
                            barrelLeftOffset = -0.35F;
                        } else {
                            barrelRightOffset = -0.35F;
                        }
                        shotSide = !shotSide;
                    }
                }
            }
            case CHEKHOV, FRIENDLY -> {
                timer++;
                int delay = stats == TurretStats.FRIENDLY ? 5 : 2;
                if (timer > 20 && timer % delay == 0) {
                    BulletConfig conf = getFirstConfigLoaded();
                    if (conf != null) {
                        this.cachedCasingConfig = conf.casing;
                        spawnBullet(level, pos, conf, 10F, null);
                        conusmeAmmo(conf.ammo);
                        playSoundNT(level, pos, "hbm:turret.chekhov_fire", 2.0F, 1.0F);

                        Vec3NT vec = muzzleOffset(getBarrelLength());
                        sendLargeExplode(level, tp.x + vec.xCoord, tp.y + vec.yCoord, tp.z + vec.zCoord, 1.5F, (byte) 1);
                    }
                }
            }
            case JEREMY -> {
                timer++;
                if (timer % 40 == 0) {
                    BulletConfig conf = getFirstConfigLoaded();
                    if (conf != null) {
                        this.cachedCasingConfig = conf.casing;
                        spawnBullet(level, pos, conf, 50F, null);
                        conusmeAmmo(conf.ammo);
                        playSoundNT(level, pos, "hbm:turret.jeremy_fire", 4.0F, 1.0F);
                        prevBarrelRecoilOffset = barrelRecoilOffset;
                        barrelRecoilOffset = -0.4F;
                        Vec3NT vec = muzzleOffset(getBarrelLength());
                        reload = 20;
                        sendLargeExplode(level, tp.x + vec.xCoord, tp.y + vec.yCoord, tp.z + vec.zCoord, 0F, (byte) 5);
                    }
                }
            }
            case TAUON -> {
                timer++;
                if (timer % 5 == 0) {
                    BulletConfig conf = getFirstConfigLoaded();
                    if (conf != null) {
                        target.hurt(ModDamageSources.electricity(level), 30F + level.random.nextInt(11));
                        conusmeAmmo(conf.ammo);
                        playSoundNT(level, pos, "hbm:weapon.tauShoot", 4.0F, 0.9F + level.random.nextFloat() * 0.3F);

                        Vec3NT vec = muzzleOffset(getBarrelLength());
                        if (level instanceof ServerLevel server) {
                            CompoundTag dPart = new CompoundTag();
                            dPart.putString("type", "tau");
                            dPart.putByte("count", (byte) 5);
                            IParticleCreator.sendPacket(server, tp.x + vec.xCoord, tp.y + vec.yCoord, tp.z + vec.zCoord, 50, dPart);
                        }
                        // Original: this.shot = true; networkPackNT -> Client setzt beam = 3 (RenderTurretTauon)
                        tauShots++;
                    }
                }
            }
            case RICHARD -> {
                if (reload > 0) return;
                timer++;
                if (timer > 0 && timer % 10 == 0) {
                    BulletConfig conf = getFirstConfigLoaded();
                    if (conf != null) {
                        spawnBullet(level, pos, conf, 30F, target);
                        conusmeAmmo(conf.ammo);
                        playSoundNT(level, pos, "hbm:turret.richard_fire", 2.0F, 1.0F);
                        this.loaded--;
                    } else {
                        this.loaded = 0;
                    }
                }
            }
            case HOWARD -> {
                timer++;
                if (loaded > 0) {
                    playSoundNT(level, pos, "hbm:turret.howard_fire", 4.0F, 0.9F + level.random.nextFloat() * 0.3F);
                    playSoundNT(level, pos, "hbm:turret.howard_fire", 4.0F, 1F + level.random.nextFloat() * 0.3F);

                    for (int i = 0; i < 2; i++) {
                        this.cachedCasingConfig = CASINGDGK;
                        spawnCasing(level, pos);
                    }

                    if (timer % 2 == 0) {
                        loaded--;

                        if (level.random.nextInt(100) + 1 <= com.hbm_m.config.WeaponConfig.ciwsHitrate)
                            EntityDamageUtil.attackEntityFromIgnoreIFrame(target, ModDamageSources.shrapnel(level), 2F + level.random.nextInt(2));

                        Vec3NT vec = muzzleOffset(getBarrelLength());
                        Vec3NT hOff = Vec3NT.createVectorHelper(0, 0.25, 0);
                        hOff.rotateAroundZ((float) -origRotationPitch());
                        hOff.rotateAroundY((float) -(origRotationYaw() + Math.PI * 0.5));

                        for (int i = 0; i < 2; i++) {
                            if (i == 1) {
                                hOff.xCoord *= -1;
                                hOff.yCoord *= -1;
                                hOff.zCoord *= -1;
                            }
                            sendLargeExplode(level, tp.x + vec.xCoord + hOff.xCoord, tp.y + vec.yCoord + hOff.yCoord, tp.z + vec.zCoord + hOff.zCoord, 1.5F, (byte) 1);
                        }
                    }
                }
            }
            case FRITZ -> {
                Fluid type = tank.getTankType();
                FT_Flammable trait = FluidType.getTrait(type, FT_Flammable.class);
                if (trait != null && FluidType.hasTrait(type, com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Liquid.class) && tank.getFill() >= 2) {
                    tank.setFill(tank.getFill() - 2);

                    Vec3NT vec = muzzleOffset(getBarrelLength());
                    float damage = Math.min((float) (trait.getHeatEnergy() / 500_000F), 20F);
                    BulletConfig cfg = type == ModFluids.BALEFIRE.getSource() ? XFactoryFlamer.flame_nograv_bf : XFactoryFlamer.flame_nograv;
                    if (cfg != null) {
                        EntityBulletBaseMK4 proj = new EntityBulletBaseMK4(level, cfg, damage, 0.05F, (float) origRotationYaw(), (float) origRotationPitch());
                        proj.setPos(tp.x + vec.xCoord, tp.y + vec.yCoord, tp.z + vec.zCoord);
                        proj.xo = proj.getX(); proj.yo = proj.getY(); proj.zo = proj.getZ();
                        level.addFreshEntity(proj);
                    }
                    playSoundNT(level, pos, "hbm:weapon.flamethrowerShoot", 2F, 1F + level.random.nextFloat() * 0.5F);
                }
            }
            case SENTRY_DAMAGED -> {
                // TileEntityTurretSentryDamaged#updateFiringTick: ohne Munition, nur jeder zweite Schuss ist echt
                timer++;
                if (timer % 10 == 0) {
                    BulletConfig conf = XFactory9mm.p9_fmj;
                    if (conf != null) {
                        Vec3NT vec = Vec3NT.createVectorHelper(0, 0, 0);
                        Vec3NT side = Vec3NT.createVectorHelper(0, 0, 0);

                        this.cachedCasingConfig = conf.casing;

                        if (shotSide) {
                            playSoundNT(level, pos, "hbm:turret.sentry_fire", 2.0F, 1.0F);
                            spawnBullet(level, pos, conf, 5F, null);

                            vec = muzzleOffset(getBarrelLength());
                            side = Vec3NT.createVectorHelper(0.125 * (shotSide ? 1 : -1), 0, 0);
                            side.rotateAroundY((float) -(origRotationYaw()));
                        } else {
                            playSoundNT(level, pos, "hbm:turret.sentry_fire", 2.0F, 0.75F);
                            if (usesCasings()) {
                                if (casingDelayFor() == 0) {
                                    spawnCasing(level, pos);
                                } else {
                                    casingDelay = casingDelayFor();
                                }
                            }
                        }

                        sendLargeExplode(level, tp.x + vec.xCoord + side.xCoord, tp.y + vec.yCoord, tp.z + vec.zCoord + side.zCoord, 1F, (byte) 1);

                        if (shotSide) {
                            barrelLeftOffset = -0.35F;
                        } else {
                            barrelRightOffset = -0.35F;
                        }
                        shotSide = !shotSide;
                    }
                }
            }
            case HOWARD_DAMAGED -> {
                // TileEntityTurretHowardDamaged#updateFiringTick: halbe Trefferquote, ohne Munition
                timer++;
                if (this.tPos != null) {
                    if (timer % 4 == 0) {
                        playSoundNT(level, pos, "hbm:turret.howard_fire", 4.0F, 0.7F + level.random.nextFloat() * 0.3F);

                        this.cachedCasingConfig = CASINGDGK;
                        spawnCasing(level, pos);

                        if (level.random.nextInt(100) + 1 <= com.hbm_m.config.WeaponConfig.ciwsHitrate * 0.5)
                            EntityDamageUtil.attackEntityFromIgnoreIFrame(target, ModDamageSources.shrapnel(level), 2F + level.random.nextInt(2));

                        Vec3NT vec = muzzleOffset(getBarrelLength());
                        Vec3NT hOff = Vec3NT.createVectorHelper(0, 0.25, 0);
                        hOff.rotateAroundZ((float) -origRotationPitch());
                        hOff.rotateAroundY((float) -(origRotationYaw() + Math.PI * 0.5));

                        sendLargeExplode(level, tp.x + vec.xCoord + hOff.xCoord, tp.y + vec.yCoord + hOff.yCoord, tp.z + vec.zCoord + hOff.zCoord, 1.5F, (byte) 1);
                    }
                }
            }
            default -> { }
        }
    }

    /** Original {@code TileEntityTurretFritz#updateEntity}: Tank aus Slots befuellen, Diesel-Munition = 1000 mB. */
    private void tickFritzTank() {
        // Original: Slot 9 = Typ-Identifikator, Slots 1..8 = Behaelter -> Port: Slot 8 bzw. 0..7
        ItemStack[] slots = new ItemStack[AMMO_SLOT_COUNT];
        for (int i = 0; i < AMMO_SLOT_COUNT; i++) slots[i] = inventory.getStackInSlot(i).copy();
        boolean changed = tank.setType(8, 8, slots);
        for (int i = 0; i < 8; i++) {
            changed |= tank.loadTank(i, 8, slots);
        }
        if (changed) {
            for (int i = 0; i < AMMO_SLOT_COUNT; i++) {
                inventory.setStackInSlot(i, slots[i] == null ? ItemStack.EMPTY : slots[i]);
            }
            setChanged();
        }

        Item diesel = WeaponItems.ammo(EnumAmmo.FLAME_DIESEL);
        for (int i = 0; i < AMMO_SLOT_COUNT; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(diesel)) {
                if (tank.getTankType() == ModFluids.DIESEL.getSource() && tank.getFill() + 1000 <= tank.getMaxFill()) {
                    tank.setFill(tank.getFill() + 1000);
                    stack.shrink(1);
                    inventory.setStackInSlot(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
                    setChanged();
                }
            }
        }

    }

    // --- Huelsenauswurf (Original: TileEntityTurretBaseNT#spawnCasing + CasingEjector) ---

    /** Original {@code getCasingSpawnPos()}. */
    private Vec3 getCasingSpawnPos(BlockPos pos) {
        Vec3 tp = turretOrigin(pos);
        Vec3NT vec = switch (kind()) {
            case SENTRY -> Vec3NT.createVectorHelper(0, 0.25, -0.125);
            case CHEKHOV, FRIENDLY -> Vec3NT.createVectorHelper(-1.125, 0.125, 0.25);
            case JEREMY -> Vec3NT.createVectorHelper(-2, 0, 0);
            case HOWARD -> Vec3NT.createVectorHelper(-0.875, 0.2, -0.125);
            default -> Vec3NT.createVectorHelper(0, 0, 0);
        };
        vec.rotateAroundZ((float) -origRotationPitch());
        vec.rotateAroundY((float) -(origRotationYaw() + Math.PI * 0.5));
        return new Vec3(tp.x + vec.xCoord, tp.y + vec.yCoord, tp.z + vec.zCoord);
    }

    private void spawnCasing(Level level, BlockPos pos) {
        if (cachedCasingConfig == null) return;
        Vec3 spawn = getCasingSpawnPos(pos);

        if (stats == TurretStats.JEREMY) {
            // TileEntityTurretJeremy#spawnCasing
            float yawDeg = (float) Math.toDegrees(origRotationYaw());
            float pitchDeg = (float) -Math.toDegrees(origRotationPitch());
            CasingCreator.composeEffect(level, spawn.x, spawn.y, spawn.z, yawDeg, pitchDeg,
                    -0.2, -0.2, 0, 0.01, -5, 0, cachedCasingConfig.getName(), true, 100, 0.5, 20);
            cachedCasingConfig = null;
            return;
        }

        // CasingEjector der Tuerme: {motionX, motionY, motionZ, angleRange}
        double mx, my, mz;
        float factor;
        switch (kind()) {
            case SENTRY -> { mx = 0.2; my = 0.2; mz = 0; factor = 0.01F; }
            case CHEKHOV -> { mx = -0.8; my = 0.8; mz = 0; factor = 0.1F; }
            case FRIENDLY -> { mx = -0.3; my = 0.6; mz = 0; factor = 0.05F; }
            case HOWARD -> { mx = 0.4; my = 0; mz = 0; factor = 0.03F; }
            default -> { cachedCasingConfig = null; return; }
        }

        // CasingEjector#spawnCasing/rotateVector: erst Pitch um X, dann -Yaw um Y (rechtshaendige Matrizen)
        var rand = level.random;
        float pitchE = (float) -origRotationPitch();
        float yawE = (float) origRotationYaw();
        float pitchA = pitchE + (float) rand.nextGaussian() * factor;
        float yawA = yawE + (float) rand.nextGaussian() * factor;
        double vx = mx + rand.nextGaussian() * factor;
        double vy = my + rand.nextGaussian() * factor;
        double vz = mz + rand.nextGaussian() * factor;
        double cp = Math.cos(pitchA), sp = Math.sin(pitchA);
        double y1 = vy * cp - vz * sp;
        double z1 = vy * sp + vz * cp;
        double x1 = vx;
        double cy = Math.cos(-yawA), sy = Math.sin(-yawA);
        double x2 = x1 * cy + z1 * sy;
        double z2 = -x1 * sy + z1 * cy;

        if (level instanceof ServerLevel server) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "casingNT");
            data.putDouble("mX", x2);
            data.putDouble("mY", y1);
            data.putDouble("mZ", z2);
            data.putFloat("yaw", (float) Math.toDegrees(yawE));
            data.putFloat("pitch", (float) Math.toDegrees(pitchE));
            data.putFloat("mPitch", (float) (rand.nextGaussian() * 5F));
            data.putFloat("mYaw", (float) (rand.nextGaussian() * 10F));
            data.putString("name", cachedCasingConfig.getName());
            data.putBoolean("smoking", false);
            data.putInt("smokeLife", 0);
            data.putDouble("smokeLift", 0);
            data.putInt("nodeLife", 0);
            IParticleCreator.sendPacket(server, spawn.x, spawn.y, spawn.z, 50, data);
        }

        cachedCasingConfig = null;
    }

    // ------------------------------------------------------------------------------------------
    // Original TileEntityTurretBaseNT#updateEntity (Serverteil) fuer die SEDNA-Tuerme:
    // Energiemodell (getConsumption pro Tick), Zielsuche alle getDecetorInterval Ticks, Sichtlinie mit
    // Mindestabstand und Hoehen-/Neigungsgrenzen, KI-Chip-Freundesliste und Kompatibilitaetslisten.
    // ------------------------------------------------------------------------------------------

    /** Beschaedigte Varianten erben Sentry/Howard (Ammo, Laufaenge, Huelsen). */
    private TurretStats kind() {
        if (stats == TurretStats.SENTRY_DAMAGED) return TurretStats.SENTRY;
        if (stats == TurretStats.HOWARD_DAMAGED) return TurretStats.HOWARD;
        return stats;
    }

    /** Original {@code hasPower()}: beschaedigte Tuerme brauchen keinen Strom. */
    public boolean hasPower() {
        return stats.isDamaged() || getEnergyStored() >= stats.getConsumption();
    }

    /** Original {@code isOn()}: beschaedigte Tuerme sind immer an. */
    private boolean isOnNT() {
        return stats.isDamaged() || isOn;
    }

    private void serverTickSedna(Level level, BlockPos pos) {
        prevBarrelLeftOffset = barrelLeftOffset;
        prevBarrelRightOffset = barrelRightOffset;
        prevBarrelSpinAngle = barrelSpinAngle;
        prevBarrelRecoilOffset = barrelRecoilOffset;
        barrelLeftOffset *= 0.6F;
        barrelRightOffset *= 0.6F;
        barrelRecoilOffset *= 0.7F;
        prevYaw = yaw;
        prevPitch = pitch;

        this.aligned = false;
        // updateConnections: Fritz haengt sich zusaetzlich mit seinem Tank ans Rohrnetz
        if (stats == TurretStats.FRITZ) updateFluidConnections(level);

        if (this.target != null && !this.target.isAlive()) {
            this.target = null;
            this.stattrak++;
        }

        if (this.target != null) {
            if (!this.entityInLOS(this.target)) {
                this.target = null;
            }
        }

        if (this.target != null) {
            this.tPos = this.getEntityPos(this.target);
        } else {
            this.tPos = null;
        }

        if (isOnNT() && hasPower()) {
            if (this.tPos != null) this.alignTurret(pos);
        } else {
            this.target = null;
            this.tPos = null;
        }

        if (this.target != null && !this.target.isAlive()) {
            this.target = null;
            this.tPos = null;
            this.stattrak++;
        }

        if (isOnNT() && hasPower()) {
            searchTimer--;

            if (!stats.isDamaged()) this.setEnergyStored(this.getEnergyStored() - stats.getConsumption());

            if (searchTimer <= 0) {
                searchTimer = stats.getDecetorInterval();

                if (this.target == null)
                    this.seekNewTarget(level, pos);
            }
        } else {
            searchTimer = 0;
        }

        hasTarget = this.tPos != null;
        if (isGatling()) tickGatlingSpin(this.tPos != null);

        if (this.aligned && this.target != null) {
            updateFiringTickSedna(level, pos, this.target);
        }

        // updateEntity()-Teile der einzelnen Tuerme (Nachladen, Gatling-Timer, Fritz-Tank, Huelsen)
        preTickSedna(level, pos);

        setChanged();
        sendUpdateToClient();
    }

    /** Original {@code turnTowards}/{@code turnTowardsAngle}: dreht mit Yaw-/Pitch-Geschwindigkeit, setzt {@code aligned}. */
    private void alignTurret(BlockPos pos) {
        Vec3 center = turretOrigin(pos);
        Vec3 to = this.tPos.subtract(center);
        double horizontalDist = Math.sqrt(to.x * to.x + to.z * to.z);

        desiredYaw = (float) Math.toDegrees(Math.atan2(to.z, to.x)) + (float) stats.yawExtraOffsetDeg;
        desiredPitch = (float) Math.toDegrees(Math.atan2(to.y, horizontalDist));

        yaw = turnToward(yaw, desiredYaw, (float) stats.yawSpeed);
        pitch = turnToward(pitch, desiredPitch, (float) stats.pitchSpeed);

        double deltaYaw = Mth.wrapDegrees(desiredYaw - yaw);
        double deltaPitch = desiredPitch - pitch;
        double deltaAngle = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);

        if (deltaAngle <= stats.getAcceptableInaccuracy()) {
            this.aligned = true;
        }
    }

    /** Original {@code getEntityPos}: Koerpermitte des Ziels. */
    private Vec3 getEntityPos(Entity e) {
        return new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.5D, e.getZ());
    }

    /** Original {@code seekNewTarget()}: naechstes erlaubtes Ziel in Reichweite und Sichtlinie. */
    private void seekNewTarget(Level level, BlockPos blockPos) {
        Vec3 pos = turretOrigin(blockPos);
        double range = stats.range;
        List<Entity> entities = level.getEntities((Entity) null, new AABB(pos, pos).inflate(range, range, range), e -> true);

        Entity lastTarget = this.target;
        Entity target = null;
        double closest = range;

        for (Entity entity : entities) {
            Vec3 ent = this.getEntityPos(entity);
            double dist = ent.subtract(pos).length();

            //check if it's in range
            if (dist > range) continue;
            //check if we should even fire at this entity
            if (!entityAcceptableTarget(entity)) continue;
            //check for visibility
            if (!entityInLOS(entity)) continue;

            //replace current target if this one is closer
            if (dist < closest) {
                closest = dist;
                target = entity;
            }
        }

        this.target = target;

        if (target != null)
            this.tPos = this.getEntityPos(this.target);

        // TileEntityTurretSentry#seekNewTarget: Aufschaltton am neuen Ziel
        if (kind() == TurretStats.SENTRY && lastTarget != this.target && this.target != null) {
            level.playSound(null, this.target.getX(), this.target.getY(), this.target.getZ(), HbmSoundsNT.get("hbm:turret.sentry_lockon"), SoundSource.BLOCKS, 2.0F, 1.5F);
        }
    }

    /** Original {@code entityInLOS}: Sichtlinie, Unsichtbarkeit, Mindest-/Hoechstabstand und Schwenkbereich. */
    public boolean entityInLOS(Entity e) {
        if (!e.isAlive()) return false;

        if (!stats.hasThermalVision() && e instanceof LivingEntity living && living.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY))
            return false;

        Vec3 pos = turretOrigin(worldPosition);
        Vec3 ent = this.getEntityPos(e);
        Vec3 delta = ent.subtract(pos);
        double length = delta.length();

        if (length < stats.getDecetorGrace() || length > stats.range * 1.1) //the latter statement is only relevant for entities that have already been detected
            return false;

        delta = delta.normalize();
        double pitchDeg = Math.toDegrees(Math.asin(delta.y / delta.length()));

        //check if the entity is within swivel range
        if (pitchDeg < -stats.getTurretDepression() || pitchDeg > stats.getTurretElevation())
            return false;

        // Library.isObstructedOpaque: Strahl vom Ziel zum Turm durch kollidierende Bloecke
        BlockHitResult hit = level.clip(new ClipContext(ent, pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e));
        return hit.getType() == HitResult.Type.MISS;
    }

    /** Original {@code entityAcceptableTarget}: Kompatibilitaetslisten, KI-Chip-Freundesliste, Zielkategorien. */
    public boolean entityAcceptableTarget(Entity e) {
        if (!e.isAlive()) return false;

        // TileEntityTurretSentryDamaged/HowardDamaged: will fire at any living entity
        if (stats.isDamaged()) {
            if (e instanceof Player player && player.getAbilities().instabuild)
                return false;
            return e instanceof LivingEntity;
        }

        for (Class<?> c : com.hbm_m.util.CompatExternal.turretTargetBlacklist) if (c.isAssignableFrom(e.getClass())) return false;

        for (java.util.Map.Entry<Class<?>, java.util.function.BiFunction<Entity, Object, Integer>> entry : com.hbm_m.util.CompatExternal.turretTargetCondition.entrySet()) {
            if (entry.getKey().isAssignableFrom(e.getClass())) {
                java.util.function.BiFunction<Entity, Object, Integer> lambda = entry.getValue();
                if (lambda != null) {
                    int result = lambda.apply(e, this);
                    if (result == -1) return false;
                    if (result == 1) return true;
                }
            }
        }

        List<String> wl = getWhitelist();

        if (wl != null) {
            if (e instanceof Player player) {
                if (wl.contains(player.getGameProfile().getName())) {
                    return false;
                }
            } else if (e instanceof net.minecraft.world.entity.Mob mob) {
                if (mob.getCustomName() != null && wl.contains(mob.getCustomName().getString())) {
                    return false;
                }
            }
        }

        if (targetAnimals) {
            // 1.7.10 IAnimals: alle Tiere, Wassertiere, Golems und (ueber IMob) auch Monster
            if (e instanceof net.minecraft.world.entity.Mob && !(e instanceof net.minecraft.world.entity.npc.Npc)) return true;
            if (e instanceof net.minecraft.world.entity.npc.Npc) return true;
            for (Class<?> c : com.hbm_m.util.CompatExternal.turretTargetFriendly) if (c.isAssignableFrom(e.getClass())) return true;
        }

        if (targetMobs) {
            //never target the ender dragon directly
            if (e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) return false;
            if (e instanceof net.minecraft.world.entity.boss.EnderDragonPart) return true;
            if (e instanceof Enemy) return true;
            for (Class<?> c : com.hbm_m.util.CompatExternal.turretTargetHostile) if (c.isAssignableFrom(e.getClass())) return true;
        }

        if (targetMachines) {
            if (e instanceof api.hbm_m.entity.IRadarDetectable d && !d.canBeSeenBy(this)) return false;
            if (e instanceof com.hbm_m.entity.missile.MissileBaseEntity) return e.getDeltaMovement().y < 0; // EntityMissileBaseNT + EntityMissileCustom
            if (e instanceof net.minecraft.world.entity.vehicle.AbstractMinecart) return true;
            if (e instanceof com.hbm_m.entity.train.EntityRailCarBase) return true;
            if (e instanceof com.hbm_m.entity.logic.EntityBomber) return true;
            for (Class<?> c : com.hbm_m.util.CompatExternal.turretTargetMachine) if (c.isAssignableFrom(e.getClass())) return true;
        }

        if (targetPlayers) {
            if (isFakePlayer(e)) return false;
            if (e instanceof Player) return true;
            for (Class<?> c : com.hbm_m.util.CompatExternal.turretTargetPlayer) if (c.isAssignableFrom(e.getClass())) return true;
        }

        return false;
    }

    private static boolean isFakePlayer(Entity e) {
        //? if forge {
        return e instanceof net.minecraftforge.common.util.FakePlayer;
        //?} else {
        /*return e.getClass().getSimpleName().contains("FakePlayer");
        *///?}
    }

    /**
     * Reads the namelist from the AI chip ({@code turret_chip}, Original Slot 0)
     * @return null if there is either no chip to be found or if the name list is empty
     */
    @Nullable
    public List<String> getWhitelist() {
        ItemStack chip = inventory.getStackInSlot(CHIP_SLOT);
        if (!chip.isEmpty() && chip.is(ModItems.TURRET_CHIP.get())) {
            String[] array = com.hbm_m.item.machine.ItemTurretBiometry.getNames(chip);
            if (array == null) return null;
            return java.util.Arrays.asList(array);
        }
        return null;
    }

    /** Appends a new name to the chip */
    public void addName(String name) {
        ItemStack chip = inventory.getStackInSlot(CHIP_SLOT);
        if (!chip.isEmpty() && chip.is(ModItems.TURRET_CHIP.get())) {
            com.hbm_m.item.machine.ItemTurretBiometry.addName(chip, name);
            inventory.setStackInSlot(CHIP_SLOT, chip);
        }
    }

    /** Removes the chip's entry at a given index */
    public void removeName(int index) {
        ItemStack chip = inventory.getStackInSlot(CHIP_SLOT);
        if (!chip.isEmpty() && chip.is(ModItems.TURRET_CHIP.get())) {
            String[] array = com.hbm_m.item.machine.ItemTurretBiometry.getNames(chip);
            if (array == null) return;

            List<String> names = new ArrayList<>(java.util.Arrays.asList(array));
            com.hbm_m.item.machine.ItemTurretBiometry.clearNames(chip);

            if (index >= 0 && index < names.size()) names.remove(index);

            for (String name : names)
                com.hbm_m.item.machine.ItemTurretBiometry.addName(chip, name);
            inventory.setStackInSlot(CHIP_SLOT, chip);
        }
    }

    /** Original {@code receiveControl}: Namen hinzufuegen ({@code name}) bzw. loeschen ({@code del}). */
    public void handleNameControl(int action, String name, int index) {
        if (action == com.hbm_m.network.TurretControlPacket.ACTION_DEL_NAME) {
            this.removeName(index);
        } else if (action == com.hbm_m.network.TurretControlPacket.ACTION_ADD_NAME && name != null && !name.isEmpty()) {
            this.addName(name.length() > 25 ? name.substring(0, 25) : name);
        }
        setChanged();
        sendUpdateToClient();
    }

    /**
     * Original {@code TileEntityTurretFritz#updateConnections()}: Tank an die acht Rohrpositionen rund um die
     * 2x2-Grundplatte ({@code dir = (meta - offset).getOpposite()}, {@code rot = dir.getRotation(UP)}).
     * Einzelblock-Altbestand: die vier Nachbarn des Blocks.
     */
    private void updateFluidConnections(Level level) {
        if (!(level instanceof ServerLevel server)) return;
        BlockState state = getBlockState();
        if (legacySingle || !(state.getBlock() instanceof com.hbm_m.block.machines.TurretMultiblockBlock)) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                trySubscribe(tank.getTankType(), server, worldPosition.relative(dir), dir);
            }
            return;
        }
        Direction dir = state.getValue(com.hbm_m.block.machines.DummyableMachineBlock.FACING).getOpposite();
        Direction rot = dir.getClockWise(); // ForgeDirection.getRotation(UP)
        Fluid type = tank.getTankType();
        trySubscribe(type, server, fritzPos(dir, rot, -1, 0), dir.getOpposite());
        trySubscribe(type, server, fritzPos(dir, rot, -1, -1), dir.getOpposite());
        trySubscribe(type, server, fritzPos(dir, rot, 0, -2), rot.getOpposite());
        trySubscribe(type, server, fritzPos(dir, rot, 1, -2), rot.getOpposite());
        trySubscribe(type, server, fritzPos(dir, rot, 0, 1), rot);
        trySubscribe(type, server, fritzPos(dir, rot, 1, 1), rot);
        trySubscribe(type, server, fritzPos(dir, rot, 2, 0), dir);
        trySubscribe(type, server, fritzPos(dir, rot, 2, -1), dir);
    }

    private BlockPos fritzPos(Direction dir, Direction rot, int d, int r) {
        return worldPosition.offset(dir.getStepX() * d + rot.getStepX() * r, 0, dir.getStepZ() * d + rot.getStepZ() * r);
    }

    // --- Client (Original: updateEntity mit worldObj.isRemote) ---

    /** Tauon: Strahl ({@code beam}), Rotor ({@code spin}) und Strahllaenge ({@code lastDist}). */
    public int beam;
    public float tauSpin, lastTauSpin;
    public double lastDist;
    /** Zaehlt die Tauon-Schuesse; aendert sich der Wert beim Client, wird der Strahl gezeigt (Original: {@code shot}-Paket). */
    private int tauShots;
    private boolean clientSynced;

    private void clientTick() {
        if (stats != TurretStats.TAUON && stats != TurretStats.MAXWELL) return;

        if (this.tPos != null) {
            Vec3 pos = turretOrigin(worldPosition);
            this.lastDist = this.tPos.subtract(pos).length();
        }

        if (beam > 0)
            beam--;

        // Original TileEntityTurretMaxwell: nur Strahllaenge und Abklingen, kein Rotor
        if (stats == TurretStats.MAXWELL) return;

        this.lastTauSpin = this.tauSpin;

        if (this.tPos != null) {
            this.tauSpin += 45;
        }

        if (this.tauSpin >= 360F) {
            this.tauSpin -= 360F;
            this.lastTauSpin -= 360F;
        }
    }

    public int getStattrak() { return stattrak; }
    @Nullable public Vec3 getTPos() { return tPos; }
    public long getMaxPower() { return stats.getMaxPower(); }

    // ------------------------------------------------------------------------------------------
    // Arty / Himars / Maxwell (nicht SEDNA)
    // ------------------------------------------------------------------------------------------

    /** Munitionsakzeptanz je Turret-Typ. */
    private boolean isAcceptedAmmo(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stats == TurretStats.FRITZ) {
            // Original: isItemValidForSlot -> true (Behaelter, Typ-Identifikator, Diesel-Munition);
            // Batterien gehen hier per Shift-Klick weiter in den Batterieslot.
            return !isEnergyProviderItem(stack);
        }
        if (isSednaTurret()) {
            for (BulletConfig conf : getAmmoList()) {
                if (conf.ammo != null && conf.ammo.matchesRecipe(stack, true)) return true;
            }
            return false;
        }
        if (stats == TurretStats.HIMARS) {
            // ItemAmmoHIMARS: alle acht Raketentypen
            return stack.getItem() instanceof com.hbm_m.item.weapon.ItemAmmoHIMARS;
        }
        if (stats == TurretStats.MAXWELL) {
            // The laser turret eats upgrades as ammunition; the original lists all seventeen
            // (speed / effect / power / afterburn / overdrive, each 1-3, plus 5G and screm).
            return stack.is(ModItems.UPGRADE_SPEED_1.get()) || stack.is(ModItems.UPGRADE_SPEED_2.get()) || stack.is(ModItems.UPGRADE_SPEED_3.get())
                    || stack.is(ModItems.UPGRADE_EFFECT_1.get()) || stack.is(ModItems.UPGRADE_EFFECT_2.get()) || stack.is(ModItems.UPGRADE_EFFECT_3.get())
                    || stack.is(ModItems.UPGRADE_POWER_1.get()) || stack.is(ModItems.UPGRADE_POWER_2.get()) || stack.is(ModItems.UPGRADE_POWER_3.get())
                    || stack.is(ModItems.UPGRADE_AFTERBURN_1.get()) || stack.is(ModItems.UPGRADE_AFTERBURN_2.get()) || stack.is(ModItems.UPGRADE_AFTERBURN_3.get())
                    || stack.is(ModItems.UPGRADE_OVERDRIVE_1.get()) || stack.is(ModItems.UPGRADE_OVERDRIVE_2.get()) || stack.is(ModItems.UPGRADE_OVERDRIVE_3.get())
                    || stack.is(ModItems.UPGRADE_5G.get()) || stack.is(ModItems.UPGRADE_SCREM.get());
        }
        if (stats == TurretStats.ARTY) {
            // ItemAmmoArty: alle zwoelf Granatentypen
            return stack.getItem() instanceof com.hbm_m.item.weapon.ItemAmmoArty;
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------
    // Arty / Himars (Original: TileEntityTurretBaseArtillery, TileEntityTurretArty, TileEntityTurretHIMARS)
    // ------------------------------------------------------------------------------------------

    /** Original {@code target}, {@code tPos}, {@code searchTimer}, {@code targetQueue}. */
    @Nullable private Entity artyTarget = null;
    @Nullable private Vec3 artyTPos = null;
    private int artySearchTimer = 0;
    private final List<Vec3> targetQueue = new ArrayList<>();

    /** Himars {@code typeLoaded} / {@code ammo}; {@code crane} ist {@link #himarsCraneProgress}. */
    public int himarsTypeLoaded = -1;
    public int himarsAmmo = 0;

    /** Arty {@code MODE_MANUAL} (2) bzw. Himars {@code MODE_MANUAL} (1). */
    private boolean artilleryManual() {
        return stats == TurretStats.ARTY ? fireMode == ARTY_MODE_MANUAL : fireMode == 1;
    }

    private boolean artilleryCannon() {
        return stats == TurretStats.ARTY && fireMode == ARTY_MODE_CANNON;
    }

    private double artilleryDetectorRange() {
        if (stats == TurretStats.ARTY) return artilleryCannon() ? 250D : 3000D;
        return 5000D;
    }

    private double artilleryDetectorGrace() {
        if (stats == TurretStats.ARTY) return artilleryCannon() ? 32D : 250D;
        return 250D;
    }

    private int artilleryDetectorInterval() {
        if (stats == TurretStats.ARTY) return artilleryCannon() ? 20 : 200;
        return 10;
    }

    private double artilleryBarrelLength() {
        return stats == TurretStats.ARTY ? 9D : 0.5D;
    }

    /** Original {@code getTurretPos()}: Hoehe {@code getHeightOffset()} (Arty 3, Himars 5) ueber dem Kern. */
    private Vec3 artilleryTurretPos(BlockPos pos) {
        Vec3 c = turretOrigin(pos);
        return new Vec3(c.x, pos.getY() + (stats == TurretStats.ARTY ? 3D : 5D), c.z);
    }

    /** Arty {@code getV0()}. */
    private double artyV0() {
        return artilleryCannon() ? 20D : 50D;
    }

    private static Vec3 artilleryEntityPos(Entity e) {
        return new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.5D, e.getZ());
    }

    /**
     * Original {@code turnTowardsAngle(targetPitch, targetYaw)} in Bogenmass und Original-Ausrichtung; schreibt das
     * Ergebnis in {@link #yaw}/{@link #pitch} zurueck. Rueckgabe = {@code aligned}.
     */
    private boolean turnTowardsAngle(double targetPitch, double targetYaw, double inaccuracyDeg) {

        double rotationPitch = Math.toRadians(pitch);
        double rotationYaw = origRotationYaw();
        double turnYaw = Math.toRadians(stats.yawSpeed);
        double turnPitch = Math.toRadians(stats.pitchSpeed);
        double pi2 = Math.PI * 2;

        //if we are about to overshoot the target by turning, just snap to the correct rotation
        if (Math.abs(rotationPitch - targetPitch) < turnPitch || Math.abs(rotationPitch - targetPitch) > pi2 - turnPitch) {
            rotationPitch = targetPitch;
        } else {
            if (targetPitch > rotationPitch) rotationPitch += turnPitch;
            else rotationPitch -= turnPitch;
        }

        double deltaYaw = (targetYaw - rotationYaw) % pi2;

        int dir = 0;
        if (deltaYaw < -Math.PI) dir = 1;
        else if (deltaYaw < 0) dir = -1;
        else if (deltaYaw > Math.PI) dir = -1;
        else if (deltaYaw > 0) dir = 1;

        if (Math.abs(rotationYaw - targetYaw) < turnYaw || Math.abs(rotationYaw - targetYaw) > pi2 - turnYaw) {
            rotationYaw = targetYaw;
        } else {
            rotationYaw += turnYaw * dir;
        }

        double deltaPitch = targetPitch - rotationPitch;
        deltaYaw = targetYaw - rotationYaw;

        double deltaAngle = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);

        rotationYaw = rotationYaw % pi2;
        rotationPitch = rotationPitch % pi2;

        this.yaw = (float) (Math.toDegrees(rotationYaw) + 90.0D + stats.yawExtraOffsetDeg);
        this.pitch = (float) Math.toDegrees(rotationPitch);
        this.desiredYaw = (float) (Math.toDegrees(targetYaw) + 90.0D + stats.yawExtraOffsetDeg);
        this.desiredPitch = (float) Math.toDegrees(targetPitch);

        return deltaAngle <= Math.toRadians(inaccuracyDeg);
    }

    /** {@code TileEntityTurretArty#alignTurret}: ballistische Loesung ab der Laufmuendung (hoher Bogen, Kanone flach). */
    private boolean alignArty(BlockPos pos) {

        Vec3 tp = artilleryTurretPos(pos);
        Vec3NT barrel = muzzleOffset(artilleryBarrelLength());
        double px = tp.x + barrel.xCoord;
        double py = tp.y + barrel.yCoord;
        double pz = tp.z + barrel.zCoord;

        double dx = artyTPos.x - px;
        double dy = artyTPos.y - py;
        double dz = artyTPos.z - pz;
        double targetYaw = -Math.atan2(dx, dz);

        double x = Math.sqrt(dx * dx + dz * dz);
        double y = dy;
        double v0 = artyV0();
        double v02 = v0 * v0;
        double g = 9.81 * 0.05;
        double upperLower = artilleryCannon() ? -1 : 1;
        double targetPitch = Math.atan((v02 + Math.sqrt(v02 * v02 - g * (g * x * x + 2 * y * v02)) * upperLower) / (g * x));

        if (Double.isNaN(targetPitch)) {
            // ausser Reichweite: Original laeuft mit NaN weiter und richtet sich nie aus
            turnTowardsAngle(origRotationPitch(), targetYaw, 0);
            return false;
        }

        return turnTowardsAngle(targetPitch, targetYaw, 0);
    }

    /** {@code TileEntityTurretHIMARS#alignTurret}: feste 45 Grad Erhoehung. */
    private boolean alignHimars(BlockPos pos) {
        Vec3 tp = artilleryTurretPos(pos);
        double targetYaw = -Math.atan2(artyTPos.x - tp.x, artyTPos.z - tp.z);
        return turnTowardsAngle(Math.PI / 4D, targetYaw, 5);
    }

    /** {@code TileEntityTurretBaseArtillery#entityInLOS} (Kanonenmodus: {@code TileEntityTurretBaseNT#entityInLOS}). */
    private boolean artilleryInLOS(Level level, BlockPos pos, Entity e) {

        if (artilleryCannon() && !e.isAlive()) return false;

        Vec3 tp = artilleryTurretPos(pos);
        Vec3 ent = artilleryEntityPos(e);
        Vec3 delta = ent.subtract(tp);
        double length = delta.length();

        if (length < artilleryDetectorGrace() || length > artilleryDetectorRange() * 1.1) return false;

        if (artilleryCannon()) {
            delta = delta.normalize();
            double pitchDeg = Math.toDegrees(Math.asin(delta.y / delta.length()));
            // Arty: getTurretDepression 30, getTurretElevation 90
            if (pitchDeg < -30D || pitchDeg > 90D) return false;
            BlockHitResult hit = level.clip(new ClipContext(ent, tp, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, e));
            return hit.getType() == HitResult.Type.MISS;
        }

        int height = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(e.getX()), (int) Math.floor(e.getZ()));
        return height < (e.getY() + e.getBbHeight());
    }

    /** {@code seekNewTarget}: naechstes passendes Ziel in Reichweite. */
    private void seekArtilleryTarget(Level level, BlockPos pos) {

        Vec3 tp = artilleryTurretPos(pos);
        double range = artilleryDetectorRange();
        List<Entity> entities = level.getEntities((Entity) null, new AABB(tp, tp).inflate(range), e -> true);

        Entity target = null;
        double closest = range;

        for (Entity entity : entities) {
            double dist = artilleryEntityPos(entity).subtract(tp).length();
            if (dist > range) continue;
            if (!entityAcceptableTarget(entity)) continue;
            if (!artilleryInLOS(level, pos, entity)) continue;
            if (dist < closest) {
                closest = dist;
                target = entity;
            }
        }

        this.artyTarget = target;
        if (target != null) this.artyTPos = artilleryEntityPos(target);
    }

    private boolean himarsHasAmmo() {
        return this.himarsTypeLoaded >= 0 && this.himarsAmmo > 0;
    }

    /** {@code updateEntity()} von Arty und Himars (Server). */
    private void tickArtillery(Level level, BlockPos pos) {

        prevYaw = yaw;
        prevPitch = pitch;
        prevHimarsCraneProgress = himarsCraneProgress;

        boolean manual = artilleryManual();

        if (manual) {
            if (!this.targetQueue.isEmpty()) this.artyTPos = this.targetQueue.get(0);
        } else {
            this.targetQueue.clear();
        }

        boolean aligned = false;

        if (this.artyTarget != null && !this.artyTarget.isAlive()) this.artyTarget = null;

        if (this.artyTarget != null && !manual) {
            if (!artilleryInLOS(level, pos, this.artyTarget)) this.artyTarget = null;
        }

        if (this.artyTarget != null) {
            this.artyTPos = artilleryEntityPos(this.artyTarget);
        } else if (!manual) {
            this.artyTPos = null;
        }

        boolean powered = getEnergyStored() >= stats.getConsumption();

        if (isOn && powered) {

            if (stats == TurretStats.HIMARS) {

                if (!himarsHasAmmo() || this.himarsCraneProgress > 0) {

                    aligned = turnTowardsAngle(0, origRotationYaw(), 5);

                    if (aligned) {
                        if (himarsHasAmmo()) {
                            this.himarsCraneProgress -= 0.0125F;
                        } else {
                            this.himarsCraneProgress += 0.0125F;

                            if (this.himarsCraneProgress >= 1F) {
                                int slot = findAmmoSlot();
                                if (slot >= 0) {
                                    ItemStack stack = inventory.getStackInSlot(slot);
                                    int available = com.hbm_m.item.weapon.ItemAmmoHIMARS.typeOf(stack);
                                    this.himarsTypeLoaded = available;
                                    this.himarsAmmo = com.hbm_m.item.weapon.ItemAmmoHIMARS.itemTypes[available].amount;
                                    stack.shrink(1);
                                    inventory.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
                                }
                            }
                        }
                    }

                    this.himarsCraneProgress = Mth.clamp(this.himarsCraneProgress, 0F, 1F);

                } else if (this.artyTPos != null) {
                    aligned = alignHimars(pos);
                }

            } else if (this.artyTPos != null) {
                aligned = alignArty(pos);
            }

        } else {
            this.artyTarget = null;
            this.artyTPos = null;
        }

        if (!isOn) this.targetQueue.clear();

        if (this.artyTarget != null && !this.artyTarget.isAlive()) {
            this.artyTarget = null;
            this.artyTPos = null;
        }

        if (isOn && powered) {
            artySearchTimer--;
            setEnergyStored(getEnergyStored() - stats.getConsumption());

            if (artySearchTimer <= 0) {
                artySearchTimer = artilleryDetectorInterval();
                if (this.artyTarget == null && !manual) seekArtilleryTarget(level, pos);
            }
        } else {
            artySearchTimer = 0;
        }

        if (aligned && (stats == TurretStats.ARTY || this.himarsCraneProgress <= 0)) {
            if (stats == TurretStats.ARTY) updateFiringTickArty(level, pos);
            else updateFiringTickHimars(level, pos);
        }

        if (stats == TurretStats.ARTY) {
            if (casingDelay > 0) {
                casingDelay--;
            } else {
                spawnArtyCasing(level, pos);
            }
        }

        hasTarget = this.artyTarget != null;
        setChanged();
        sendUpdateToClient();
    }

    /** {@code TileEntityTurretArty#updateFiringTick}: Artillerie alle 300 Ticks, Kanone/Manuell alle 40. */
    private void updateFiringTickArty(Level level, BlockPos pos) {

        timer++;

        int delay = fireMode == ARTY_MODE_ARTILLERY ? 300 : 40;

        if (timer % delay == 0) {

            int slot = findAmmoSlot();

            if (slot >= 0 && this.artyTPos != null) {
                ItemStack conf = inventory.getStackInSlot(slot);
                int type = com.hbm_m.item.weapon.ItemAmmoArty.typeOf(conf);
                cachedCasingConfig = com.hbm_m.item.weapon.ItemAmmoArty.itemTypes[type].casing;
                spawnArtyShell(level, pos, conf, type);
                conf.shrink(1);
                inventory.setStackInSlot(slot, conf.isEmpty() ? ItemStack.EMPTY : conf);
                playSoundNT(level, pos, "hbm:turret.jeremy_fire", 25.0F, 1.0F);
                Vec3 tp = artilleryTurretPos(pos);
                Vec3NT vec = muzzleOffset(artilleryBarrelLength());
                // didJustShoot: Ruecklauf des Rohrs
                prevBarrelRecoilOffset = barrelRecoilOffset;
                barrelRecoilOffset = -0.4F;

                if (level instanceof ServerLevel server) {
                    CompoundTag data = new CompoundTag();
                    data.putString("type", "vanillaExt");
                    data.putString("mode", "largeexplode");
                    data.putFloat("size", 0F);
                    data.putByte("count", (byte) 5);
                    IParticleCreator.sendPacket(server, tp.x + vec.xCoord, tp.y + vec.yCoord, tp.z + vec.zCoord, 150, data);
                }
            }

            if (artilleryManual() && !this.targetQueue.isEmpty()) {
                this.targetQueue.remove(0);
                this.artyTPos = null;
            }
        }
    }

    /** {@code TileEntityTurretArty#spawnShell}. */
    private void spawnArtyShell(Level level, BlockPos pos, ItemStack type, int meta) {

        Vec3 tp = artilleryTurretPos(pos);
        Vec3NT vec = muzzleOffset(artilleryBarrelLength());

        com.hbm_m.entity.projectile.EntityArtilleryShell proj = new com.hbm_m.entity.projectile.EntityArtilleryShell(com.hbm_m.entity.ModEntities.ARTILLERY_SHELL.get(), level);
        proj.moveTo(tp.x + vec.xCoord, tp.y + vec.yCoord, tp.z + vec.zCoord, 0.0F, 0.0F);
        proj.setThrowableHeading(vec.xCoord, vec.yCoord, vec.zCoord, (float) artyV0(), 0.0F);
        proj.setTarget((int) artyTPos.x, (int) artyTPos.y, (int) artyTPos.z);
        proj.setType(meta);

        if (meta == com.hbm_m.item.weapon.ItemAmmoArty.CARGO) {
            CompoundTag tag = com.hbm_m.platform.PlatformHooks.getItemTag(type);
            if (tag != null && tag.contains("cargo")) {
                proj.setCargo(com.hbm_m.platform.PlatformHooks.itemStackOf(tag.getCompound("cargo"), com.hbm_m.platform.PlatformHooks.bestEffortProvider()));
            }
        }

        if (!artilleryCannon()) proj.setWhistle(true);

        level.addFreshEntity(proj);

        casingDelay = 7;
    }

    /** {@code TileEntityTurretArty#spawnCasing}: Huelse am Drehpunkt, raucht. */
    private void spawnArtyCasing(Level level, BlockPos pos) {

        if (cachedCasingConfig == null) return;

        Vec3 spawn = artilleryTurretPos(pos);
        float yawDeg = (float) Math.toDegrees(origRotationYaw());
        float pitchDeg = (float) -Math.toDegrees(origRotationPitch());

        CasingCreator.composeEffect(level, spawn.x, spawn.y, spawn.z, yawDeg, pitchDeg,
                -0.6, 0.3, 0, 0.01, level.random.nextFloat() * 20F - 10F, 0,
                cachedCasingConfig.getName(), true, 200, 1, 20);

        cachedCasingConfig = null;
    }

    /** {@code TileEntityTurretHIMARS#updateFiringTick}: alle 40 Ticks eine Rakete aus dem geladenen Satz. */
    private void updateFiringTickHimars(Level level, BlockPos pos) {

        timer++;

        int delay = 40;

        if (timer % delay == 0) {

            if (himarsHasAmmo() && this.artyTPos != null) {
                spawnHimarsRocket(level, pos, this.himarsTypeLoaded);
                this.himarsAmmo--;
                playSoundNT(level, pos, "hbm:weapon.rocketFlame", 25.0F, 1.0F);
            }

            if (artilleryManual() && !this.targetQueue.isEmpty()) {
                this.targetQueue.remove(0);
                this.artyTPos = null;
            }
        }
    }

    /** {@code TileEntityTurretHIMARS#spawnShell}. */
    private void spawnHimarsRocket(Level level, BlockPos pos, int type) {

        Vec3 tp = artilleryTurretPos(pos);
        Vec3NT vec = muzzleOffset(artilleryBarrelLength());

        com.hbm_m.entity.projectile.EntityArtilleryRocket proj = new com.hbm_m.entity.projectile.EntityArtilleryRocket(com.hbm_m.entity.ModEntities.ARTILLERY_ROCKET.get(), level);
        proj.moveTo(tp.x + vec.xCoord, tp.y + vec.yCoord, tp.z + vec.zCoord, 0.0F, 0.0F);
        proj.setThrowableHeading(vec.xCoord, vec.yCoord, vec.zCoord, 25F, 0.0F);

        if (this.artyTarget != null)
            proj.setTarget(this.artyTarget);
        else
            proj.setTarget(artyTPos.x, artyTPos.y, artyTPos.z);

        proj.setType(type);

        level.addFreshEntity(proj);
    }

    /** {@code TileEntityTurretBaseArtillery#enqueueTarget} (oeffentlich wie im Original, fuer designator_arty_range). */
    public void enqueueTarget(double x, double y, double z) {
        Vec3 tp = artilleryTurretPos(worldPosition);
        if (new Vec3(x - tp.x, y - tp.y, z - tp.z).length() <= artilleryDetectorRange()) {
            this.targetQueue.add(new Vec3(x, y, z));
        }
    }

    /** Original {@code IRadarCommandReceiver} nur fuer Arty/Himars ({@code TileEntityTurretBaseArtillery}). */
    @Override
    public boolean sendCommandPosition(BlockPos target) {
        if (stats != TurretStats.ARTY && stats != TurretStats.HIMARS) return false;
        this.enqueueTarget(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        return true;
    }

    @Override
    public boolean sendCommandEntity(Entity target) {
        if (stats != TurretStats.ARTY && stats != TurretStats.HIMARS) return false;
        this.enqueueTarget(target.getX(), target.getY(), target.getZ());
        return true;
    }

    /**
     * Maxwell-Mikrowellenwaffe (Original: {@code TileEntityTurretMaxwell}): kein Munitionsverbrauch,
     * kontinuierlicher Tick-Schaden waehrend Ausrichtung, skaliert per nicht-verbrauchten Upgrade-Karten
     * in den Munitionsslots (Original-Formel {@code (blackLevel*10 + redLevel + 1) * 0.25}, hier
     * powerLevel/effectLevel genannt).
     */
    private void tickMaxwell(Level level, BlockPos pos, LivingEntity target) {
        if (!isAimed()) return;

        int powerLevel = upgradeLevel(ModItems.UPGRADE_POWER_1.get(), ModItems.UPGRADE_POWER_2.get(), ModItems.UPGRADE_POWER_3.get());
        int effectLevel = upgradeLevel(ModItems.UPGRADE_EFFECT_1.get(), ModItems.UPGRADE_EFFECT_2.get(), ModItems.UPGRADE_EFFECT_3.get());

        float damage = (powerLevel * 10 + effectLevel + 1) * 0.25F;
        long energyCost = stats.energyPerShot * (1 + powerLevel) / 20L;
        if (energyCost < 1L) energyCost = 1L;

        if (getEnergyStored() < energyCost) return;

        setEnergyStored(getEnergyStored() - energyCost);
        target.hurt(ModDamageSources.microwave(level), damage);
        // Original: shot = true; networkPackNT -> Client setzt beam = 5 (RenderTurretMaxwell)
        tauShots++;

        if (tickCountForSound++ % 10 == 0) {
            level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.4F, 0.6F);
        }
        setChanged();
    }

    private int tickCountForSound = 0;

    /** Schaltet Arty zwischen Artillerie/Kanone/Manuell, Himars zwischen Auto/Manuell (Original handleButtonPacket meta 5). */
    private void cycleFireMode() {
        fireMode = (fireMode + 1) % (stats == TurretStats.HIMARS ? 2 : 3);
        this.artyTPos = null;
        this.targetQueue.clear();
    }

    private int upgradeLevel(Item lvl1, Item lvl2, Item lvl3) {
        boolean has1 = false, has2 = false, has3 = false;
        for (int i = 0; i < AMMO_SLOT_COUNT; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (stack.is(lvl1)) has1 = true;
            else if (stack.is(lvl2)) has2 = true;
            else if (stack.is(lvl3)) has3 = true;
        }
        if (has3) return 3;
        if (has2) return 2;
        if (has1) return 1;
        return 0;
    }

    /** Original {@code TileEntityTurretChekhov}: accel +2 bis 45 Grad/Tick mit Ziel, sonst -2. */
    private void tickGatlingSpin(boolean revUp) {
        if (revUp) {
            spin = Math.min(45.0F, spin + 2.0F);
        } else {
            spin = Math.max(0.0F, spin - 2.0F);
        }
        barrelSpinAngle += spin;
    }

    public boolean isAcceptedAmmoPublic(ItemStack stack) {
        return isAcceptedAmmo(stack);
    }

    private int findAmmoSlot() {
        for (int i = 0; i < AMMO_SLOT_COUNT; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (isAcceptedAmmo(stack)) {
                return i;
            }
        }
        return -1;
    }

    /** Arty im Kanonenmodus (direkter Treffer). */
    private void fire(Level level, BlockPos pos, LivingEntity target, int ammoSlot) {
        inventory.getStackInSlot(ammoSlot).shrink(1);
        setEnergyStored(getEnergyStored() - stats.energyPerShot);
        cooldown = stats.cooldownTicks;

        if (stats == TurretStats.ARTY) {
            prevBarrelRecoilOffset = barrelRecoilOffset;
            barrelRecoilOffset = -0.4F;
        }
        target.hurt(level.damageSources().generic(), stats.damage);
        level.playSound(null, pos, SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 1.0F, 0.6F);
        setChanged();
    }

    public boolean hasTarget() {
        return hasTarget;
    }

    public ResourceLocation getGuiTexture() {
        return stats.getGuiTexture();
    }

    public TurretStats getStats() {
        return stats;
    }

    public FluidTank getTank() {
        return tank;
    }

    public boolean isOn() { return isOn; }
    public boolean isTargetingPlayers() { return targetPlayers; }
    public boolean isTargetingAnimals() { return targetAnimals; }
    public boolean isTargetingMobs() { return targetMobs; }
    public boolean isTargetingMachines() { return targetMachines; }

    /** Verarbeitet Klicks auf die GUI-Buttons (siehe {@link com.hbm_m.network.TurretControlPacket}). */
    public void handleButtonPress(int action) {
        switch (action) {
            case com.hbm_m.network.TurretControlPacket.ACTION_TOGGLE_ON -> isOn = !isOn;
            case com.hbm_m.network.TurretControlPacket.ACTION_TOGGLE_PLAYERS -> targetPlayers = !targetPlayers;
            case com.hbm_m.network.TurretControlPacket.ACTION_TOGGLE_ANIMALS -> targetAnimals = !targetAnimals;
            case com.hbm_m.network.TurretControlPacket.ACTION_TOGGLE_MOBS -> targetMobs = !targetMobs;
            case com.hbm_m.network.TurretControlPacket.ACTION_TOGGLE_MACHINES -> targetMachines = !targetMachines;
            case com.hbm_m.network.TurretControlPacket.ACTION_CYCLE_FIRE_MODE -> cycleFireMode();
            default -> { return; }
        }
        setChanged();
        sendUpdateToClient();
    }

    public int getFireMode() { return fireMode; }

    // --- Fluid (nur Fritz, Original IFluidStandardReceiver) ---

    @Override
    public FluidTank[] getReceivingTanks() {
        return stats == TurretStats.FRITZ ? new FluidTank[] { tank } : FluidTank.EMPTY_ARRAY;
    }

    @Override
    public FluidTank[] getAllTanks() {
        return stats == TurretStats.FRITZ ? new FluidTank[] { tank } : FluidTank.EMPTY_ARRAY;
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return stats == TurretStats.FRITZ && fromDir != Direction.UP && fromDir != Direction.DOWN;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (stats == TurretStats.FRITZ && cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return tank.getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?} elif neoforge {
    /*@Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (stats == TurretStats.FRITZ && cap == com.hbm_m.platform.HbmCap.FLUID_HANDLER) {
            return com.hbm_m.platform.LazyCap.ofObj(tank.getCapability()).cast();
        }
        return super.getHbmCapability(cap, side);
    }
    *///?}

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("cooldown", cooldown);
        tag.putFloat("yaw", yaw);
        tag.putFloat("prev_yaw", prevYaw);
        tag.putFloat("pitch", pitch);
        tag.putFloat("prev_pitch", prevPitch);
        tag.putBoolean("is_on", isOn);
        tag.putBoolean("target_players", targetPlayers);
        tag.putBoolean("target_animals", targetAnimals);
        tag.putBoolean("target_mobs", targetMobs);
        tag.putBoolean("target_machines", targetMachines);
        tag.putFloat("spin", spin);
        tag.putFloat("barrel_spin_angle", barrelSpinAngle);
        tag.putInt("timer", timer);
        tag.putInt("reload", reload);
        tag.putInt("loaded", loaded);
        tag.putFloat("himars_crane_progress", himarsCraneProgress);
        tag.putInt("fire_mode", fireMode);
        tag.putInt("himars_type", himarsTypeLoaded);
        tag.putInt("himars_ammo", himarsAmmo);
        tag.putInt("stattrak", stattrak);
        tag.putInt("tau_shots", tauShots);
        if (tPos != null) {
            tag.putDouble("tpos_x", tPos.x);
            tag.putDouble("tpos_y", tPos.y);
            tag.putDouble("tpos_z", tPos.z);
        }
        if (stats == TurretStats.FRITZ) tag.put("diesel", tank.writeNBT(new CompoundTag()));
        // Mehrblock-Format: fehlt der Schluessel, stammt der Turm aus der Einzelblock-Zeit
        tag.putInt("mb_format", 1);
        tag.putBoolean("mb_legacy", legacySingle);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        cooldown = tag.getInt("cooldown");
        yaw = tag.getFloat("yaw");
        prevYaw = tag.getFloat("prev_yaw");
        pitch = tag.getFloat("pitch");
        prevPitch = tag.getFloat("prev_pitch");
        isOn = !tag.contains("is_on") || tag.getBoolean("is_on");
        targetPlayers = tag.getBoolean("target_players");
        targetAnimals = tag.getBoolean("target_animals");
        targetMobs = !tag.contains("target_mobs") || tag.getBoolean("target_mobs");
        targetMachines = !tag.contains("target_machines") || tag.getBoolean("target_machines");
        spin = tag.getFloat("spin");
        barrelSpinAngle = tag.getFloat("barrel_spin_angle");
        prevBarrelSpinAngle = barrelSpinAngle;
        timer = tag.getInt("timer");
        reload = tag.getInt("reload");
        loaded = tag.getInt("loaded");
        himarsCraneProgress = tag.getFloat("himars_crane_progress");
        prevHimarsCraneProgress = himarsCraneProgress;
        fireMode = tag.getInt("fire_mode");
        himarsTypeLoaded = tag.contains("himars_type") ? tag.getInt("himars_type") : -1;
        himarsAmmo = tag.getInt("himars_ammo");
        stattrak = tag.getInt("stattrak");
        int shots = tag.getInt("tau_shots");
        if (clientSynced && shots != tauShots && level != null && level.isClientSide()) this.beam = stats == TurretStats.MAXWELL ? 5 : 3;
        tauShots = shots;
        clientSynced = level != null && level.isClientSide();
        tPos = tag.contains("tpos_x") ? new Vec3(tag.getDouble("tpos_x"), tag.getDouble("tpos_y"), tag.getDouble("tpos_z")) : null;
        if (tag.contains("diesel")) tank.readNBT(tag.getCompound("diesel"));
        legacySingle = !tag.contains("mb_format") || tag.getBoolean("mb_legacy");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(stats.getNameKey());
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < AMMO_SLOT_COUNT) {
            return isAcceptedAmmo(stack);
        }
        if (slot == BATTERY_SLOT) {
            return isEnergyProviderItem(stack);
        }
        if (slot == CHIP_SLOT) {
            return stack.is(ModItems.TURRET_CHIP.get());
        }
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new TurretMenu(id, inv, this);
    }

    // ── Redstone-over-Radio (1:1 TileEntityTurretBaseNT / TileEntityTurretBaseArtillery) ──

    /** Original {@code instanceof TileEntityTurretBaseArtillery}: Arty und HIMARS. */
    private boolean isArtilleryROR() {
        return stats == TurretStats.ARTY || stats == TurretStats.HIMARS;
    }

    @Override
    public String[] getFunctionInfo() {
        if (isArtilleryROR()) {
            return new String[] {
                    PREFIX_FUNCTION + "setactive" + NAME_SEPARATOR + "active (0 or 1)",
                    PREFIX_FUNCTION + "targetplayers" + NAME_SEPARATOR + "enabled (0 or 1)",
                    PREFIX_FUNCTION + "targetanimals" + NAME_SEPARATOR + "enabled (0 or 1)",
                    PREFIX_FUNCTION + "targetmobs" + NAME_SEPARATOR + "enabled (0 or 1)",
                    PREFIX_FUNCTION + "targetmachines" + NAME_SEPARATOR + "enabled (0 or 1)",
                    PREFIX_FUNCTION + "addwhitelist" + NAME_SEPARATOR + "name",
                    PREFIX_FUNCTION + "removewhitelist" + NAME_SEPARATOR + "name",
                    PREFIX_FUNCTION + "enqueue" + NAME_SEPARATOR + "x" + PARAM_SEPARATOR + "y" + PARAM_SEPARATOR + "z",
            };
        }
        return new String[] {
                PREFIX_FUNCTION + "setactive" + NAME_SEPARATOR + "active (0 or 1)",
                PREFIX_FUNCTION + "targetplayers" + NAME_SEPARATOR + "enabled (0 or 1)",
                PREFIX_FUNCTION + "targetanimals" + NAME_SEPARATOR + "enabled (0 or 1)",
                PREFIX_FUNCTION + "targetmobs" + NAME_SEPARATOR + "enabled (0 or 1)",
                PREFIX_FUNCTION + "targetmachines" + NAME_SEPARATOR + "enabled (0 or 1)",
                PREFIX_FUNCTION + "addwhitelist" + NAME_SEPARATOR + "name",
                PREFIX_FUNCTION + "removewhitelist" + NAME_SEPARATOR + "name",
        };
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setactive").equals(name) && params.length > 0) {
            try { this.isOn = (Integer.parseInt(params[0]) == 1); this.markChangedROR(); } catch (NumberFormatException e) {}
        }
        if ((PREFIX_FUNCTION + "targetplayers").equals(name) && params.length > 0) {
            try { this.targetPlayers = (Integer.parseInt(params[0]) == 1); this.markChangedROR(); } catch (NumberFormatException e) {}
        }
        if ((PREFIX_FUNCTION + "targetanimals").equals(name) && params.length > 0) {
            try { this.targetAnimals = (Integer.parseInt(params[0]) == 1); this.markChangedROR(); } catch (NumberFormatException e) {}
        }
        if ((PREFIX_FUNCTION + "targetmobs").equals(name) && params.length > 0) {
            try { this.targetMobs = (Integer.parseInt(params[0]) == 1); this.markChangedROR(); } catch (NumberFormatException e) {}
        }
        if ((PREFIX_FUNCTION + "targetmachines").equals(name) && params.length > 0) {
            try { this.targetMachines = (Integer.parseInt(params[0]) == 1); this.markChangedROR(); } catch (NumberFormatException e) {}
        }
        // Original ruft whitelist.contains ohne Nullpruefung auf - ohne Chip haette das den Server-Tick abgebrochen
        if ((PREFIX_FUNCTION + "addwhitelist").equals(name) && params.length > 0) {
            String playerName = params[0];
            List<String> whitelist = this.getWhitelist();
            if (whitelist == null || !whitelist.contains(playerName)) this.addName(playerName);
            this.markChangedROR();
        }
        if ((PREFIX_FUNCTION + "removewhitelist").equals(name) && params.length > 0) {
            String playerName = params[0];
            List<String> whitelist = this.getWhitelist();
            if (whitelist != null && whitelist.contains(playerName)) this.removeName(whitelist.indexOf(playerName));
            this.markChangedROR();
        }

        // TileEntityTurretBaseArtillery: super.runRORFunction(...), dann enqueue
        if (isArtilleryROR() && (PREFIX_FUNCTION + "enqueue").equals(name) && params.length > 2) {
            try {
                int x = Integer.parseInt(params[0]);
                int y = Integer.parseInt(params[1]);
                int z = Integer.parseInt(params[2]);
                this.sendCommandPosition(new BlockPos(x, y, z));
                this.markChangedROR();
            } catch (NumberFormatException e) {}
        }
        return null;
    }

    /** Original {@code markChanged()}. */
    private void markChangedROR() {
        setChanged();
        sendUpdateToClient();
    }
}
