package com.hbm_m.blockentity.machines;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.inventory.menu.TurretMenu;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * MVP-Port aller Turret-Varianten aus der 1.7.10-Originalmod (TileEntityTurretBaseNT-Familie).
 * Eine gemeinsame Klasse fuer alle 11 Varianten, parametrisiert per {@link TurretStats}.
 * Feuerlogik ist eigenstaendig (direkter Schaden statt des vollen, noch nicht portierten
 * Waffen-/Munitionssystems) - siehe {@link com.hbm_m.item.ModItems#TURRET_AMMO}.
 */
public class TurretBaseBlockEntity extends BaseMachineBlockEntity {

    private static final int AMMO_SLOT_COUNT = 9;
    private static final int BATTERY_SLOT = 9;
    private static final int SLOT_COUNT = 10;

    private static final long CAPACITY = 50_000L;
    private static final long MAX_RECEIVE = 500L;

    private static final float AIM_TOLERANCE_DEG = 3.0F;

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

    /** Rueckstoss-Animation der beiden Sentry-Laeufe (abwechselnd, Original: {@code RenderTurretSentry}). */
    public float barrelLeftOffset, prevBarrelLeftOffset;
    public float barrelRightOffset, prevBarrelRightOffset;
    private boolean fireLeftBarrelNext = true;

    /** Gatling-Spinup/Spindown (Chekhov/Friendly, Original: {@code TileEntityTurretChekhov#spin}). */
    private float spin = 0.0F;
    public float barrelSpinAngle, prevBarrelSpinAngle;

    /** CIWS-Magazin (Howard, Original: {@code TileEntityTurretHoward#loaded}). */
    private static final int HOWARD_MAGAZINE = 200;
    private static final int HOWARD_RELOAD_TICKS = 200;
    private static final float HOWARD_HIT_CHANCE = 0.6F;
    private int howardLoaded = 0;
    private int howardReloadCooldown = 0;

    /** Raketen-Magazin (Richard, Original: 17er-Magazin, 100-Tick-Reload). */
    private static final int RICHARD_MAGAZINE = 17;
    private static final int RICHARD_RELOAD_TICKS = 100;
    private int richardLoaded = 0;
    private int richardReloadCooldown = 0;

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
        super(type, pos, state, SLOT_COUNT, CAPACITY, MAX_RECEIVE, 0L);
        this.stats = stats;
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

    public static void tick(Level level, BlockPos pos, BlockState state, TurretBaseBlockEntity be) {
        if (level.isClientSide()) return;
        be.serverTick(level, pos);
    }

    private void serverTick(Level level, BlockPos pos) {
        ensureNetworkInitialized();
        chargeFromBatterySlot(BATTERY_SLOT);

        if (cooldown > 0) cooldown--;

        if (!isOn) {
            hasTarget = false;
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
        prevBarrelLeftOffset = barrelLeftOffset;
        prevBarrelRightOffset = barrelRightOffset;
        prevBarrelSpinAngle = barrelSpinAngle;
        prevBarrelRecoilOffset = barrelRecoilOffset;
        barrelLeftOffset *= 0.6F;
        barrelRightOffset *= 0.6F;
        barrelRecoilOffset *= 0.7F;

        if (hasTarget && !hadTargetLastTick) {
            level.playSound(null, pos, com.hbm_m.sound.ModSounds.TOOL_TECH_BLEEP.get(), SoundSource.BLOCKS, 1.0F, 1.4F);
        }
        hadTargetLastTick = hasTarget;

        boolean isGatling = stats == TurretStats.CHEKHOV || stats == TurretStats.FRIENDLY;

        if (target == null) {
            if (isGatling) tickGatlingSpin(false);
            if (hasTarget != wasActiveLastTick) {
                setChanged();
                sendUpdateToClient();
            }
            wasActiveLastTick = hasTarget;
            return;
        }

        updateAim(pos, target);

        if (isGatling) {
            tickGatlingSpin(isAimed());
        }

        if (stats == TurretStats.HOWARD) {
            tickHoward(level, pos, target);
        } else if (stats == TurretStats.RICHARD) {
            tickRichard(level, pos, target);
        } else if (stats == TurretStats.HIMARS) {
            tickHimars(level, pos, target);
        } else if (stats == TurretStats.MAXWELL) {
            tickMaxwell(level, pos, target);
        } else if (stats == TurretStats.FRITZ) {
            tickFritz(level, pos, target);
        } else if (stats == TurretStats.ARTY && fireMode != ARTY_MODE_CANNON) {
            tickArtyIndirect(level, pos, target);
        } else {
            boolean readyToFire = isAimed() && cooldown <= 0 && getEnergyStored() >= stats.energyPerShot;
            if (isGatling) {
                readyToFire &= spin >= maxSpinFor(stats) * 0.95F;
            }
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
     *
     * <p>These three had drifted apart in this port: aiming measured from the block centre, the
     * sight check from half a block above it, and shots left from the pivot height. The turret was
     * therefore aiming at one point, testing visibility from a second and firing from a third,
     * which is why its tracking never lined up with where the rounds went.</p>
     */
    private Vec3 turretOrigin(BlockPos pos) {
        return Vec3.atCenterOf(pos).add(0.0D, stats.pivotY, 0.0D);
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

    private boolean isAimed() {
        return Math.abs(Mth.wrapDegrees(yaw - desiredYaw)) < AIM_TOLERANCE_DEG
                && Math.abs(Mth.wrapDegrees(pitch - desiredPitch)) < AIM_TOLERANCE_DEG;
    }

    @Nullable
    private LivingEntity findTarget(Level level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(stats.range);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && isAcceptableTarget(e));

        LivingEntity closest = null;
        double closestDistSqr = Double.MAX_VALUE;
        Vec3 turretCenter = Vec3.atCenterOf(pos);

        boolean requireLos = !(stats == TurretStats.ARTY && fireMode != ARTY_MODE_CANNON);

        for (LivingEntity candidate : candidates) {
            double distSqr = candidate.position().distanceToSqr(turretCenter);
            if (distSqr < closestDistSqr && (!requireLos || hasLineOfSight(level, pos, candidate))) {
                closestDistSqr = distSqr;
                closest = candidate;
            }
        }
        return closest;
    }

    /**
     * Ziel-Kategorie-Filter (Original: {@code TileEntityTurretBaseNT#entityAcceptableTarget}).
     * Vereinfachte 1:1-Naeherung ohne Whitelist-Chip/Mod-Kompat-Listen: echte Spieler -> targetPlayers,
     * Tiere -> targetAnimals, feindliche Mobs -> targetMobs, alles uebrige (Golems, Doerfler usw.) -> targetMachines.
     */
    private boolean isAcceptableTarget(LivingEntity e) {
        if (e instanceof net.minecraft.world.entity.player.Player) {
            return targetPlayers;
        }
        if (e instanceof net.minecraft.world.entity.animal.Animal) {
            return targetAnimals;
        }
        if (e instanceof Enemy) {
            return targetMobs;
        }
        return targetMachines;
    }

    private boolean hasLineOfSight(BlockGetter level, BlockPos pos, LivingEntity target) {
        Vec3 from = turretOrigin(pos);
        Vec3 to = targetPoint(target);
        ClipContext ctx = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target);
        BlockHitResult hit = ((Level) level).clip(ctx);
        return hit.getType() == HitResult.Type.MISS;
    }

    /** Munitionsakzeptanz je Turret-Typ (Original: {@code BulletConfig}-Zuordnung pro Waffensystem). */
    private boolean isAcceptedAmmo(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stats == TurretStats.SENTRY) {
            return stack.is(ModItems.AMMO_9MM_SP.get()) || stack.is(ModItems.AMMO_9MM_FMJ.get())
                    || stack.is(ModItems.AMMO_9MM_JHP.get()) || stack.is(ModItems.AMMO_9MM_AP.get());
        }
        if (stats == TurretStats.CHEKHOV) {
            return stack.is(ModItems.AMMO_50_SP.get()) || stack.is(ModItems.AMMO_50_FMJ.get())
                    || stack.is(ModItems.AMMO_50_JHP.get()) || stack.is(ModItems.AMMO_50_AP.get())
                    || stack.is(ModItems.AMMO_50_DU.get());
        }
        if (stats == TurretStats.FRIENDLY) {
            return stack.is(ModItems.AMMO_556_SP.get()) || stack.is(ModItems.AMMO_556_FMJ.get())
                    || stack.is(ModItems.AMMO_556_JHP.get()) || stack.is(ModItems.AMMO_556_AP.get());
        }
        if (stats == TurretStats.JEREMY) {
            return stack.is(ModItems.AMMO_SHELL.get()) || stack.is(ModItems.AMMO_SHELL_APFSDS_DU.get())
                    || stack.is(ModItems.AMMO_SHELL_APFSDS_T.get()) || stack.is(ModItems.AMMO_SHELL_EXPLOSIVE.get())
                    || stack.is(ModItems.AMMO_SHELL_W9.get());
        }
        if (stats == TurretStats.HOWARD) {
            return stack.is(ModItems.AMMO_DGK.get());
        }
        if (stats == TurretStats.RICHARD) {
            // XFactoryRocket.rocket_ml - the launcher takes the whole five-type family.
            return stack.is(ModItems.ROCKET_TURRET_STANDARD.get()) || stack.is(ModItems.ROCKET_TURRET_HEAT.get())
                    || stack.is(ModItems.ROCKET_TURRET_DEMO.get()) || stack.is(ModItems.ROCKET_TURRET_INC.get())
                    || stack.is(ModItems.ROCKET_TURRET_PHOSPHORUS.get());
        }
        if (stats == TurretStats.HIMARS) {
            // ItemAmmoHIMARS ships eight variants - six small-calibre plus the two large ones.
            return stack.is(ModItems.ROCKET_HIMARS_STANDARD.get()) || stack.is(ModItems.ROCKET_HIMARS_HE.get())
                    || stack.is(ModItems.ROCKET_HIMARS_LAVA.get()) || stack.is(ModItems.ROCKET_HIMARS_MINI_NUKE.get())
                    || stack.is(ModItems.ROCKET_HIMARS_WP.get()) || stack.is(ModItems.ROCKET_HIMARS_THERMOBARIC.get())
                    || stack.is(ModItems.ROCKET_HIMARS_SINGLE.get()) || stack.is(ModItems.ROCKET_HIMARS_SINGLE_TB.get());
        }
        if (stats == TurretStats.TAUON) {
            return stack.is(ModItems.AMMO_TAU_URANIUM.get());
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
        if (stats == TurretStats.FRITZ) {
            return stack.is(ModItems.AMMO_FLAME_DIESEL.get());
        }
        if (stats == TurretStats.ARTY) {
            return stack.is(ModItems.AMMO_ARTY.get()) || stack.is(ModItems.AMMO_ARTY_CARGO.get())
                    || stack.is(ModItems.AMMO_ARTY_CHLORINE.get()) || stack.is(ModItems.AMMO_ARTY_CLASSIC.get())
                    || stack.is(ModItems.AMMO_ARTY_HE.get()) || stack.is(ModItems.AMMO_ARTY_MINI_NUKE.get())
                    || stack.is(ModItems.AMMO_ARTY_MINI_NUKE_MULTI.get()) || stack.is(ModItems.AMMO_ARTY_MUSTARD_GAS.get())
                    || stack.is(ModItems.AMMO_ARTY_NUKE.get()) || stack.is(ModItems.AMMO_ARTY_PHOSGENE.get())
                    || stack.is(ModItems.AMMO_ARTY_PHOSPHORUS.get()) || stack.is(ModItems.AMMO_ARTY_PHOSPHORUS_MULTI.get());
        }
        return stack.is(ModItems.TURRET_AMMO.get());
    }

    private static float explosionRadiusFor(Item ammoItem) {
        if (ammoItem == ModItems.ROCKET_HIMARS_MINI_NUKE.get()) return 8.0F;
        // "single" rockets are the large calibre: the original gives them a 50F/5F blast against
        // the small ones' 20F/3F.
        if (ammoItem == ModItems.ROCKET_HIMARS_SINGLE_TB.get()) return 6.5F;
        if (ammoItem == ModItems.ROCKET_HIMARS_SINGLE.get()) return 5.0F;
        if (ammoItem == ModItems.ROCKET_HIMARS_THERMOBARIC.get() || ammoItem == ModItems.ROCKET_HIMARS_HE.get()) return 4.0F;
        return 2.5F;
    }

    /** Raketen-Magazin-Feuerlogik (Richard: 17er-Magazin, 100-Tick-Reload, Homing-Projektil). */
    private void tickRichard(Level level, BlockPos pos, LivingEntity target) {
        if (richardReloadCooldown > 0) richardReloadCooldown--;

        if (richardLoaded <= 0) {
            if (richardReloadCooldown > 0) return;
            int ammoSlot = findAmmoSlot();
            if (ammoSlot < 0) return;
            inventory.getStackInSlot(ammoSlot).shrink(1);
            richardLoaded = RICHARD_MAGAZINE;
            richardReloadCooldown = RICHARD_RELOAD_TICKS;
        }

        if (!isAimed() || cooldown > 0 || getEnergyStored() < stats.energyPerShot) return;

        richardLoaded--;
        setEnergyStored(getEnergyStored() - stats.energyPerShot);
        cooldown = stats.cooldownTicks;
        Vec3 muzzle = turretOrigin(pos);
        var rocket = com.hbm_m.entity.projectile.TurretRocketEntity.create(level,
                muzzle.x, muzzle.y, muzzle.z, target, stats.damage, 2.5F, ModItems.ROCKET_TURRET_STANDARD.get());
        level.addFreshEntity(rocket);
        level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 1.0F, 1.0F);
        setChanged();
    }

    /** Kran-Ladeanimation + Feuerlogik (Himars: 20-Tick-Ladevorgang vor jedem gelenkten Raketenschuss). */
    private void tickHimars(Level level, BlockPos pos, LivingEntity target) {
        prevHimarsCraneProgress = himarsCraneProgress;

        if (!isAimed() || getEnergyStored() < stats.energyPerShot) {
            himarsCraneProgress = Math.max(0.0F, himarsCraneProgress - 0.1F);
            return;
        }

        int ammoSlot = findAmmoSlot();
        if (ammoSlot < 0 || cooldown > 0) {
            himarsCraneProgress = Math.max(0.0F, himarsCraneProgress - 0.1F);
            return;
        }

        himarsCraneProgress = Math.min(1.0F, himarsCraneProgress + 0.05F);
        if (himarsCraneProgress < 1.0F) return;

        Item ammoItem = inventory.getStackInSlot(ammoSlot).getItem();
        inventory.getStackInSlot(ammoSlot).shrink(1);
        setEnergyStored(getEnergyStored() - stats.energyPerShot);
        cooldown = stats.cooldownTicks;
        himarsCraneProgress = 0.0F;

        Vec3 muzzle = turretOrigin(pos);
        var rocket = com.hbm_m.entity.projectile.TurretRocketEntity.create(level,
                muzzle.x, muzzle.y, muzzle.z, target, stats.damage, explosionRadiusFor(ammoItem), ammoItem);
        level.addFreshEntity(rocket);
        level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 1.0F, 0.8F);
        setChanged();
    }

    /**
     * CIWS-Feuerlogik (Original: {@code TileEntityTurretHoward}): laedt bei Bedarf ein 200er-Magazin
     * aus einem DGK-Item, feuert dann 2 Schuss/Tick mit Trefferchance ohne weiteren Munitionsverbrauch,
     * bis das Magazin leer ist, gefolgt von einer 200-Tick-Nachladesperre.
     */
    private void tickHoward(Level level, BlockPos pos, LivingEntity target) {
        if (howardReloadCooldown > 0) howardReloadCooldown--;

        if (howardLoaded <= 0) {
            if (howardReloadCooldown > 0) return;
            int ammoSlot = findAmmoSlot();
            if (ammoSlot < 0) return;
            inventory.getStackInSlot(ammoSlot).shrink(1);
            howardLoaded = HOWARD_MAGAZINE;
            howardReloadCooldown = HOWARD_RELOAD_TICKS;
        }

        if (!isAimed() || getEnergyStored() < stats.energyPerShot) return;

        for (int i = 0; i < 2 && howardLoaded > 0; i++) {
            howardLoaded--;
            setEnergyStored(getEnergyStored() - stats.energyPerShot);
            if (level.random.nextFloat() < HOWARD_HIT_CHANCE) {
                target.hurt(level.damageSources().generic(), stats.damage);
            }
        }
        level.playSound(null, pos, SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 0.6F, 1.6F);
        setChanged();
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

        if (tickCountForSound++ % 10 == 0) {
            level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.4F, 0.6F);
        }
        setChanged();
    }

    private int tickCountForSound = 0;
    private int fritzFuelTicks = 0;

    /**
     * Fritz-Flammenwerfer (Original: kontinuierlicher Flammschaden aus einem Fluid-Tank, hier
     * vereinfacht auf ein Brennstoff-Item, das alle 20 Ticks im Dauerfeuer verbraucht wird).
     */
    private void tickFritz(Level level, BlockPos pos, LivingEntity target) {
        if (!isAimed() || getEnergyStored() < stats.energyPerShot / 20) return;

        if (fritzFuelTicks <= 0) {
            int ammoSlot = findAmmoSlot();
            if (ammoSlot < 0) return;
            inventory.getStackInSlot(ammoSlot).shrink(1);
            fritzFuelTicks = 20;
        }
        fritzFuelTicks--;

        setEnergyStored(getEnergyStored() - Math.max(1L, stats.energyPerShot / 20));
        target.setSecondsOnFire(3);
        target.hurt(level.damageSources().generic(), stats.damage / 10.0F);

        if (level.getGameTime() % 5 == 0) {
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, 1.2F);
        }
        setChanged();
    }

    /**
     * Arty im Artillerie-/Manuell-Modus (Original: ballistischer Wurf ohne LOS-Pruefung, langsamer
     * Reload, sehr hohe Reichweite). Wiederverwendet {@link com.hbm_m.entity.projectile.TurretRocketEntity}
     * als vereinfachte Wurfparabel (kein echtes Ballistik-Loesen der Wurfgleichung).
     */
    private void tickArtyIndirect(Level level, BlockPos pos, LivingEntity target) {
        if (!isAimed() || cooldown > 0 || getEnergyStored() < stats.energyPerShot) return;

        int ammoSlot = findAmmoSlot();
        if (ammoSlot < 0) return;

        Item ammoItem = inventory.getStackInSlot(ammoSlot).getItem();
        inventory.getStackInSlot(ammoSlot).shrink(1);
        setEnergyStored(getEnergyStored() - stats.energyPerShot);
        cooldown = stats.cooldownTicks;
        prevBarrelRecoilOffset = barrelRecoilOffset;
        barrelRecoilOffset = -0.4F;

        Vec3 muzzle = turretOrigin(pos);
        var shell = com.hbm_m.entity.projectile.TurretRocketEntity.create(level,
                muzzle.x, muzzle.y, muzzle.z, target, stats.damage, explosionRadiusFor(ammoItem), ammoItem);
        level.addFreshEntity(shell);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, 0.7F);
        setChanged();
    }

    /** Schaltet Arty zwischen Artillerie/Kanone/Manuell (siehe {@link com.hbm_m.network.TurretControlPacket}). */
    private void cycleFireMode() {
        fireMode = (fireMode + 1) % 3;
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

    /** Maximale Spin-Geschwindigkeit (Original: Chekhov 45 deg/tick, Friendly langsamer). */
    private static float maxSpinFor(TurretStats stats) {
        return stats == TurretStats.CHEKHOV ? 45.0F : 20.0F;
    }

    private void tickGatlingSpin(boolean revUp) {
        float max = maxSpinFor(stats);
        if (revUp) {
            spin = Math.min(max, spin + 2.0F);
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

    private void fire(Level level, BlockPos pos, LivingEntity target, int ammoSlot) {
        Item ammoItem = inventory.getStackInSlot(ammoSlot).getItem();
        inventory.getStackInSlot(ammoSlot).shrink(1);
        setEnergyStored(getEnergyStored() - stats.energyPerShot);
        cooldown = stats.cooldownTicks;

        if (stats == TurretStats.SENTRY || stats == TurretStats.CHEKHOV || stats == TurretStats.FRIENDLY) {
            fireBullet(level, pos, target, ammoItem);
        } else if (stats == TurretStats.JEREMY && ammoItem == ModItems.AMMO_SHELL_W9.get()) {
            detonateW9Shell(level, target);
        } else if (stats == TurretStats.TAUON) {
            fireBeam(level, pos, target);
        } else {
            if (stats == TurretStats.ARTY) {
                prevBarrelRecoilOffset = barrelRecoilOffset;
                barrelRecoilOffset = -0.4F;
            }
            target.hurt(level.damageSources().generic(), stats.damage);
            level.playSound(null, pos, SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 1.0F, 0.6F);
        }
        setChanged();
    }

    /** Tauon-Teilchenstrahl (Original: instant-hit Elektroschaden + Strahl-Visual zwischen Muendung und Ziel). */
    private void fireBeam(Level level, BlockPos pos, LivingEntity target) {
        target.hurt(ModDamageSources.electricity(level), stats.damage);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 2.0F);

        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            Vec3 muzzle = turretOrigin(pos);
            Vec3 to = target.getEyePosition();
            int steps = 12;
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double x = Mth.lerp(t, muzzle.x, to.x);
                double y = Mth.lerp(t, muzzle.y, to.y);
                double z = Mth.lerp(t, muzzle.z, to.z);
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    /**
     * Jeremy mit W9-Granate (Original: {@code shell_w9} loest eine Mini-Nuklearexplosion beim
     * Einschlag aus). Vereinfacht: sofortige kleine Explosion am Zielort statt eines separaten
     * Granaten-Flugkoerpers, reuse der vorhandenen Vanilla-Explosion statt einer eigenen Krater-Portierung.
     */
    private void detonateW9Shell(Level level, LivingEntity target) {
        var pos3 = target.position();
        level.explode(null, pos3.x, pos3.y, pos3.z, 4.0F, Level.ExplosionInteraction.MOB);
    }

    private static final double BULLET_SPEED = 3.0D;

    /** Feuert ein echtes {@link com.hbm_m.entity.projectile.TurretBulletEntity} statt direktem Treffer-Schaden. */
    private void fireBullet(Level level, BlockPos pos, LivingEntity target, Item ammoItem) {
        Vec3 muzzle = turretOrigin(pos);
        Vec3 to = target.getEyePosition().subtract(muzzle).normalize();

        if (stats == TurretStats.SENTRY) {
            boolean useLeft = fireLeftBarrelNext;
            fireLeftBarrelNext = !fireLeftBarrelNext;
            if (useLeft) {
                barrelLeftOffset = -0.35F;
            } else {
                barrelRightOffset = -0.35F;
            }
        }

        var bullet = com.hbm_m.entity.projectile.TurretBulletEntity.create(level,
                muzzle.x, muzzle.y, muzzle.z,
                to.x * BULLET_SPEED, to.y * BULLET_SPEED, to.z * BULLET_SPEED,
                stats.damage, ammoItem);
        level.addFreshEntity(bullet);
        level.playSound(null, pos, SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 1.0F, 1.2F);
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

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
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
        tag.putInt("howard_loaded", howardLoaded);
        tag.putInt("howard_reload_cooldown", howardReloadCooldown);
        tag.putInt("richard_loaded", richardLoaded);
        tag.putInt("richard_reload_cooldown", richardReloadCooldown);
        tag.putFloat("himars_crane_progress", himarsCraneProgress);
        tag.putInt("fire_mode", fireMode);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
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
        howardLoaded = tag.getInt("howard_loaded");
        howardReloadCooldown = tag.getInt("howard_reload_cooldown");
        richardLoaded = tag.getInt("richard_loaded");
        richardReloadCooldown = tag.getInt("richard_reload_cooldown");
        himarsCraneProgress = tag.getFloat("himars_crane_progress");
        prevHimarsCraneProgress = himarsCraneProgress;
        fireMode = tag.getInt("fire_mode");
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
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new TurretMenu(id, inv, this);
    }
}
