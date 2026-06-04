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

    public static double getPassiveCooling(Level world)          { return PASSIVE_COOLING; }
    public static double getPassiveCoolingInner(Level world)     { return PASSIVE_COOLING_INNER; }
    public static double getColumnHeatFlow(Level world)          { return COLUMN_HEAT_FLOW; }
    public static double getFuelDiffusionMod(Level world)        { return FUEL_DIFFUSION_MOD; }
    public static double getFuelHeatProvision(Level world)       { return HEAT_PROVISION; }
    public static int    getColumnHeight(Level world)            { return COLUMN_HEIGHT; }
    public static boolean getPermaScrap(Level world)             { return PERMANENT_SCRAP; }
    public static double getBoilerHeatConsumption(Level world)   { return BOILER_HEAT_CONSUMPTION; }
    public static double getControlSpeed(Level world)            { return CONTROL_SPEED_MOD; }
    public static double getReactivityMod(Level world)           { return REACTIVITY_MOD; }
    public static double getOutgasserMod(Level world)            { return OUTGASSER_MOD; }
    public static double getSurgeMod(Level world)                { return SURGE_MOD; }
    public static int    getFluxRange(Level world)               { return FLUX_RANGE; }
    public static int    getReaSimRange(Level world)             { return REASIM_RANGE; }
    public static boolean getReasimBoilers(Level world)          { return REASIM_BOILERS; }
    public static double getReaSimBoilerSpeed(Level world)       { return REASIM_BOILER_SPEED; }
    public static boolean getMeltdownsDisabled(Level world)      { return DISABLE_MELTDOWNS; }
    public static boolean getOverpressure(Level world)           { return ENABLE_OVERPRESSURE; }
    public static double getModeratorEfficiency(Level world)     { return MODERATOR_EFFICIENCY; }
    public static double getAbsorberEfficiency(Level world)      { return ABSORBER_EFFICIENCY; }
    public static double getAbsorberHeatConversion(Level world)  { return ABSORBER_HEAT_CONV; }
    public static double getReflectorEfficiency(Level world)     { return REFLECTOR_EFFICIENCY; }
    public static boolean getDepletion(Level world)              { return !DISABLE_DEPLETION; }
    public static boolean getXenon(Level world)                  { return !DISABLE_XENON; }
}
