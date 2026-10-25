package com.hbm_m.api.space;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Fassade der Space-Schnittstelle von HBM-Modernized.
 *
 * <p>Ohne Addon liefert {@link #env()} die Erde ({@link #EARTH}); HBM verhaelt sich dann exakt wie ohne diese API.
 * Ein Addon (z.B. Space Modernized) setzt seine Implementierung einmal im Mod-Konstruktor ueber
 * {@link #setEnvironment(SpaceEnvironment)}. HBM-Modernized kennt das Addon nicht.
 *
 * <p>Die statischen Kurzformen ({@link #breatheAir}, {@link #gravity}, ...) sind die Aufrufstellen in HBM - sie
 * fangen {@code level == null} ab und liefern dann den Erdwert.
 */
public final class SpaceApi {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Erd-Standardumgebung (alle Methoden = Standardwerte aus {@link SpaceEnvironment}). */
    public static final SpaceEnvironment EARTH = new SpaceEnvironment() { };

    private static volatile SpaceEnvironment environment = EARTH;
    private static volatile boolean installed = false;
    private static final List<NuclearDetonation.Listener> NUKE_LISTENERS = new CopyOnWriteArrayList<>();

    private SpaceApi() {}

    // ───────────────────────────── Registrierung ─────────────────────────────

    /** Aktive Umgebung, nie null. */
    public static SpaceEnvironment env() {
        return environment;
    }

    /** true, sobald ein Addon eine Umgebung gesetzt hat. Dient Stellen, deren Space-Fassung auch auf der Erde abweicht. */
    public static boolean isInstalled() {
        return installed;
    }

    private static volatile SatelliteHooks satelliteHooks = SatelliteHooks.DEFAULT;

    /** W-R6C-2: Satelliten-Haken des Addons setzen (einmal, Mod-Konstruktor). */
    public static void setSatelliteHooks(SatelliteHooks hooks) {
        satelliteHooks = hooks == null ? SatelliteHooks.DEFAULT : hooks;
    }

    private static volatile Lpw2Hooks lpw2Hooks = Lpw2Hooks.DEFAULT;

    /**
     * W-R6B-1: LPW-2 als Stationstriebwerk. <b>Nur im Addon-Mod-Konstruktor</b> (vor der Blockregistrierung) setzen.
     */
    public static void setLpw2Hooks(Lpw2Hooks hooks) {
        lpw2Hooks = hooks == null ? Lpw2Hooks.DEFAULT : hooks;
    }

    /** Aktuelle LPW-2-Haken (nie null). */
    public static Lpw2Hooks lpw2() {
        return lpw2Hooks;
    }

    private static volatile BedrockOreHooks bedrockOreHooks = BedrockOreHooks.DEFAULT;

    /** W-R8B-1: Bedrock-Erz-Haken des Addons (Mod-Konstruktor, Client + Server). */
    public static void setBedrockOreHooks(BedrockOreHooks hooks) {
        bedrockOreHooks = hooks == null ? BedrockOreHooks.DEFAULT : hooks;
    }

    /** Aktuelle Bedrock-Erz-Haken (nie null, ohne Addon {@link BedrockOreHooks#DEFAULT}). */
    public static BedrockOreHooks bedrockOres() {
        return bedrockOreHooks;
    }

    private static volatile ForkRules forkRules = ForkRules.MASTER;

    /** E1-E17: Fork-Regeln des Addons (einmal, Mod-Konstruktor). null = Master. */
    public static void setForkRules(ForkRules rules) {
        forkRules = rules == null ? ForkRules.MASTER : rules;
    }

    /** Aktuelle Fork-Regeln (nie null, ohne Addon {@link ForkRules#MASTER}). */
    public static ForkRules fork() {
        return forkRules;
    }

    /** Kurzform: {@code fork().active()} - true nur mit Addon, das die Fork-Abweichungen einschaltet. */
    public static boolean forkActive() {
        return forkRules.active();
    }

    private static volatile WandDHook wandDHook = WandDHook.DEFAULT;

    /** Debug-Zauberstab {@code wand_d}: Space-Fassung des Addons (einmal, Mod-Konstruktor). null = HBM. */
    public static void setWandDHook(WandDHook hook) {
        wandDHook = hook == null ? WandDHook.DEFAULT : hook;
    }

    /** Aktueller {@code wand_d}-Haken (nie null, ohne Addon {@link WandDHook#DEFAULT}). */
    public static WandDHook wandD() {
        return wandDHook;
    }

    /** Aktuelle Satelliten-Haken (nie null, ohne Addon {@link SatelliteHooks#DEFAULT}). */
    public static SatelliteHooks satellites() {
        return satelliteHooks;
    }

    /**
     * Setzt die Umgebung (einmalig, im Mod-Konstruktor des Addons). Ein zweiter Aufruf ersetzt die erste Umgebung und
     * wird als Warnung protokolliert (zwei Space-Addons gleichzeitig sind nicht vorgesehen).
     */
    public static synchronized void setEnvironment(SpaceEnvironment env) {
        Objects.requireNonNull(env, "env");
        if (installed && env != environment) {
            LOGGER.warn("[hbm_m] SpaceApi.setEnvironment called twice: {} replaces {}", env.getClass().getName(),
                    environment.getClass().getName());
        }
        environment = env;
        installed = env != EARTH;
    }

    /** Nur fuer Tests: zurueck auf die Erde. */
    public static synchronized void resetToEarth() {
        environment = EARTH;
        installed = false;
        satelliteHooks = SatelliteHooks.DEFAULT;
        lpw2Hooks = Lpw2Hooks.DEFAULT;
        bedrockOreHooks = BedrockOreHooks.DEFAULT;
        forkRules = ForkRules.MASTER;
        wandDHook = WandDHook.DEFAULT;
    }

    // ───────────────────────────── Nukleare Detonationen ─────────────────────────────

    /** Meldet einen Listener fuer nukleare Detonationen an (Original {@code CelestialNukeShockHandler}). */
    public static void onNuclearDetonation(NuclearDetonation.Listener listener) {
        NUKE_LISTENERS.add(Objects.requireNonNull(listener, "listener"));
    }

    public static void removeNuclearDetonationListener(NuclearDetonation.Listener listener) {
        NUKE_LISTENERS.remove(listener);
    }

    /** Aufruf aus HBM (z.B. {@code ExplosionNukeSmall}); nur serverseitig, Fehler eines Listeners stoeren HBM nicht. */
    public static void fireNuclearDetonation(Level level, double x, double y, double z, float blastRadius, NuclearDetonation.Kind kind) {
        if (level == null || level.isClientSide || NUKE_LISTENERS.isEmpty()) return;
        NuclearDetonation d = new NuclearDetonation(level, x, y, z, blastRadius, kind);
        for (NuclearDetonation.Listener l : NUKE_LISTENERS) {
            try {
                l.onDetonation(d);
            } catch (RuntimeException e) {
                LOGGER.error("[hbm_m] nuclear detonation listener {} failed", l, e);
            }
        }
    }

    // ───────────────────────────── Kurzformen fuer HBM-Aufrufstellen ─────────────────────────────

    /** {@link SpaceEnvironment#consumeOxygen}; Original {@code TileEntityMachineBase.breatheAir}. */
    public static boolean breatheAir(@Nullable Level level, BlockPos pos, int amount) {
        if (level == null) return true;
        return environment.consumeOxygen(level, pos, amount);
    }

    /** {@link SpaceEnvironment#gravity(LivingEntity)} in NTM-Einheiten. */
    public static float gravity(LivingEntity entity) {
        if (entity == null || entity.level() == null) return SpaceEnvironment.STANDARD_GRAVITY;
        return environment.gravity(entity);
    }

    /** {@link SpaceEnvironment#inOrbit}. */
    public static boolean inOrbit(@Nullable Level level) {
        return level != null && environment.inOrbit(level);
    }

    /** {@link SpaceEnvironment#overridesSky}. */
    public static boolean overridesSky(@Nullable Level level) {
        return level != null && environment.overridesSky(level);
    }

    /** {@link SpaceEnvironment#solarIntensity}. */
    public static float solarIntensity(@Nullable Level level, BlockPos pos) {
        if (level == null) return 1.0F;
        return environment.solarIntensity(level, pos);
    }

    /** {@link SpaceEnvironment#emitGas}. */
    public static void emitGas(@Nullable Level level, BlockPos pos, ResourceLocation fluid, int mb) {
        if (level == null || mb <= 0) return;
        environment.emitGas(level, pos, fluid, mb);
    }

    /** {@link SpaceEnvironment#pressure}. */
    public static double pressure(@Nullable Level level, BlockPos pos) {
        if (level == null) return SpaceEnvironment.STANDARD_PRESSURE;
        return environment.pressure(level, pos);
    }

    /** {@link SpaceEnvironment#bodyPressure}. */
    public static double bodyPressure(@Nullable Level level) {
        if (level == null) return SpaceEnvironment.STANDARD_PRESSURE;
        return environment.bodyPressure(level);
    }

    /** {@link SpaceEnvironment#drawAtmosphere} (Ansaugstutzen). */
    public static boolean drawAtmosphere(@Nullable Level level, BlockPos pos, int mb) {
        if (level == null) return true;
        return environment.drawAtmosphere(level, pos, Math.max(0, mb));
    }

    /**
     * Original {@code TileEntityCondenser.updateEntity} (NTM Space): true = Dampf <b>und</b> Wasser verdampfen, statt
     * zu kondensieren (Tom-Feuersturm unter freiem Himmel, oder ohne {@code vacuumOptimised} im Vakuum/Orbit bzw.
     * unter 0.01 atm). Ohne Addon immer false - auch der Feuersturm-Teil ist Space-Fork-eigen (Master 5771 hat ihn
     * nicht) und bleibt deshalb an {@link #isInstalled()} gebunden.
     */
    public static boolean condenserEvaporates(@Nullable Level level, BlockPos pos, boolean vacuumOptimised) {
        if (level == null || !installed || level.isClientSide) return false;
        int light = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
        boolean evaporate = light > 7 && com.hbm_m.saveddata.TomSaveData.forWorld(level).fire > 1e-5;
        if (!evaporate && !vacuumOptimised) {
            evaporate = environment.inOrbit(level) || environment.bodyPressure(level) < 0.01D;
        }
        return evaporate;
    }

    /** Client-Overlay "REQUIRES ATMOSPHERE" (Original {@code MachineCondenser/MachineTower*.printHook}). */
    public static boolean condenserNeedsAtmosphere(@Nullable Level level, boolean vacuumOptimised) {
        if (level == null || vacuumOptimised) return false;
        return environment.inOrbit(level) || environment.bodyPressure(level) < 0.01D;
    }

    /**
     * Blinkende Overlay-Zeile {@code "! ! ! REQUIRES ATMOSPHERE ! ! !"} (Original {@code "&[" + (getBlink() ? 0xff0000 :
     * 0xffff00) + "&]! ! ! " + atmosphere.noVacuum + " ! ! !"}). Der Sprachschluessel kommt aus dem Addon (Space-Lang),
     * Rueckfall = englischer Originaltext.
     */
    public static net.minecraft.network.chat.Component noVacuumWarning() {
        int color = com.hbm_m.util.BobMathUtil.getBlink() ? 0xff0000 : 0xffff00;
        return net.minecraft.network.chat.Component.literal("! ! ! ")
                .append(net.minecraft.network.chat.Component.translatableWithFallback("atmosphere.noVacuum", "REQUIRES ATMOSPHERE"))
                .append(" ! ! !")
                .withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(color)));
    }

    /** {@link SpaceEnvironment#onLivingTick} (nur Server). */
    public static void livingTick(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) return;
        environment.onLivingTick(entity);
    }

    /** {@link SpaceEnvironment#onDecontaminate} (nur Server). */
    public static void decontaminate(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) return;
        environment.onDecontaminate(entity);
    }

    /** {@link SpaceEnvironment#ambientRadiation}. */
    public static float ambientRadiation(@Nullable Level level, BlockPos pos) {
        if (level == null) return 0.0F;
        return environment.ambientRadiation(level, pos);
    }

    /** {@link SpaceEnvironment#body}. */
    public static ResourceLocation body(@Nullable Level level) {
        if (level == null) return SpaceEnvironment.EARTH;
        return environment.body(level);
    }

    /**
     * Wassertabelle als Fluid; {@code null} = keine Tabelle bzw. im Orbit (Original {@code TileEntityMachinePumpBase}:
     * {@code WorldProviderOrbit -> false}, {@code CBT_Water == null -> false}). Erde: {@code defaultWater}.
     */
    @Nullable
    public static Fluid waterTableFluid(@Nullable Level level, Fluid defaultWater) {
        if (level == null) return defaultWater;
        if (environment.inOrbit(level)) return null;
        ResourceLocation id = environment.waterTableFluid(level);
        if (id == null) return null;
        if (SpaceEnvironment.HBM_WATER.equals(id)) return defaultWater;
        Fluid f = BuiltInRegistries.FLUID.get(id);
        return f == null || f == Fluids.EMPTY ? null : f;
    }
}
