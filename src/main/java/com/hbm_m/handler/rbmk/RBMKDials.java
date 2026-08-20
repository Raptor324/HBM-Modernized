package com.hbm_m.handler.rbmk;

import net.minecraft.world.level.Level;

/**
 * Central config dial system for all RBMK parameters.
 * Defaults match the original HBM game-rule defaults.
 */
public class RBMKDials {

    // --- Cooling ---
    public static double PASSIVE_COOLING       = 2.5;
    public static double PASSIVE_COOLING_INNER = 0.1;
    public static double COLUMN_HEAT_FLOW      = 0.2;

    // --- Fuel ---
    public static double FUEL_DIFFUSION_MOD    = 1.0;
    public static double HEAT_PROVISION        = 0.2;
    public static double REACTIVITY_MOD        = 1.0;

    // --- Structure ---
    /** Number of dummy blocks above the base (0-indexed, default 3 = 4-block-tall column). */
    public static int    COLUMN_HEIGHT         = 3;

    // --- Boilers ---
    public static double BOILER_HEAT_CONSUMPTION = 0.1;

    // --- Control ---
    public static double CONTROL_SPEED_MOD     = 1.0;
    public static double OUTGASSER_MOD         = 1.0;
    public static double SURGE_MOD             = 1.0;

    // --- Neutron ---
    public static int    FLUX_RANGE            = 5;
    public static int    REASIM_RANGE          = 10;

    // --- ReaSim ---
    public static boolean REASIM_BOILERS       = false;
    public static double  REASIM_BOILER_SPEED  = 0.05;

    // --- Safety ---
    public static boolean DISABLE_MELTDOWNS    = false;
    public static boolean ENABLE_OVERPRESSURE  = false;
    public static boolean PERMANENT_SCRAP      = true;

    // --- Efficiency ---
    public static double MODERATOR_EFFICIENCY  = 1.0;
    public static double ABSORBER_EFFICIENCY   = 1.0;
    public static double REFLECTOR_EFFICIENCY  = 1.0;
    public static double ABSORBER_HEAT_CONV    = 0.05;

    // --- Xenon / Depletion ---
    public static boolean DISABLE_DEPLETION    = false;
    public static boolean DISABLE_XENON        = false;

    // --- Static accessors (world-independent, matching original API) ---

    public static double getPassiveCooling(Level world)          { return dial(d -> d.passiveCooling, PASSIVE_COOLING); }
    public static double getPassiveCoolingInner(Level world)     { return dial(d -> d.passiveCoolingInner, PASSIVE_COOLING_INNER); }
    public static double getColumnHeatFlow(Level world)          { return dial(d -> d.columnHeatFlow, COLUMN_HEAT_FLOW); }
    public static double getFuelDiffusionMod(Level world)        { return dial(d -> d.fuelDiffusionMod, FUEL_DIFFUSION_MOD); }
    public static double getFuelHeatProvision(Level world)       { return dial(d -> d.heatProvision, HEAT_PROVISION); }
    /** Original counts the full column and subtracts one (RBMKDials:96). */
    public static int    getColumnHeight(Level world)            {
        return rule(world, RBMKGameRules.COLUMN_HEIGHT, COLUMN_HEIGHT + 1) - 1;
    }
    public static boolean getPermaScrap(Level world)             { return rule(world, RBMKGameRules.PERMANENT_SCRAP, PERMANENT_SCRAP); }
    public static double getBoilerHeatConsumption(Level world)   { return dial(d -> d.boilerHeatConsumption, BOILER_HEAT_CONSUMPTION); }
    public static double getControlSpeed(Level world)            { return dial(d -> d.controlSpeedMod, CONTROL_SPEED_MOD); }
    public static double getReactivityMod(Level world)           { return dial(d -> d.reactivityMod, REACTIVITY_MOD); }
    public static double getOutgasserMod(Level world)            { return dial(d -> d.outgasserMod, OUTGASSER_MOD); }
    public static double getSurgeMod(Level world)                { return dial(d -> d.surgeMod, SURGE_MOD); }
    public static int    getFluxRange(Level world)               { return rule(world, RBMKGameRules.FLUX_RANGE, FLUX_RANGE); }
    public static int    getReaSimRange(Level world)             { return rule(world, RBMKGameRules.REASIM_RANGE, REASIM_RANGE); }
    public static boolean getReasimBoilers(Level world)          { return rule(world, RBMKGameRules.REASIM_BOILERS, REASIM_BOILERS); }
    public static double getReaSimBoilerSpeed(Level world)       { return dial(d -> d.reasimBoilerSpeed, REASIM_BOILER_SPEED); }
    public static boolean getMeltdownsDisabled(Level world)      { return rule(world, RBMKGameRules.DISABLE_MELTDOWNS, DISABLE_MELTDOWNS); }
    public static boolean getOverpressure(Level world)           { return rule(world, RBMKGameRules.MELTDOWN_OVERPRESSURE, ENABLE_OVERPRESSURE); }
    public static double getModeratorEfficiency(Level world)     { return dial(d -> d.moderatorEfficiency, MODERATOR_EFFICIENCY); }
    public static double getAbsorberEfficiency(Level world)      { return dial(d -> d.absorberEfficiency, ABSORBER_EFFICIENCY); }
    public static double getAbsorberHeatConversion(Level world)  { return dial(d -> d.absorberHeatConversion, ABSORBER_HEAT_CONV); }
    public static double getReflectorEfficiency(Level world)     { return dial(d -> d.reflectorEfficiency, REFLECTOR_EFFICIENCY); }
    public static boolean getDepletion(Level world)              { return !rule(world, RBMKGameRules.DISABLE_DEPLETION, DISABLE_DEPLETION); }
    public static boolean getXenon(Level world)                  { return !rule(world, RBMKGameRules.DISABLE_XENON, DISABLE_XENON); }

    // ─── Game rule lookup ─────────────────────────────────────────────────────

    private static boolean rule(Level world, net.minecraft.world.level.GameRules.Key<net.minecraft.world.level.GameRules.BooleanValue> key, boolean fallback) {
        if (world == null || key == null) return fallback;
        return world.getGameRules().getBoolean(key);
    }

    private static int rule(Level world, net.minecraft.world.level.GameRules.Key<net.minecraft.world.level.GameRules.IntegerValue> key, int fallback) {
        if (world == null || key == null) return fallback;
        return world.getGameRules().getInt(key);
    }

    /**
     * Floating-point dials come from the mod config (see {@code ModClothConfig.RBMKDialSettings}),
     * since vanilla game rules only carry booleans and integers. These are read on hot paths -
     * passive cooling runs for every column every tick - so the lookup is a direct field read
     * through a lambda rather than reflection. The static field stays as the fallback for when no
     * config is loaded at all, e.g. during datagen.
     */
    private static double dial(java.util.function.ToDoubleFunction<com.hbm_m.config.ModClothConfig.RBMKDialSettings> getter,
                                double fallback) {
        com.hbm_m.config.ModClothConfig config = com.hbm_m.config.ModClothConfig.get();
        if (config == null || config.rbmkDials == null) return fallback;
        return getter.applyAsDouble(config.rbmkDials);
    }
}
