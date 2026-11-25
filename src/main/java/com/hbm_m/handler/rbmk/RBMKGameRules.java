package com.hbm_m.handler.rbmk;

import net.minecraft.world.level.GameRules;

/**
 * The RBMK "dials" as real game rules, matching the original's {@code RBMKDials.RBMKKeys} keys
 * one for one (see {@code com.hbm.tileentity.machine.rbmk.RBMKDials}).
 *
 * <p>In the original every dial is a world game rule, so a pack or a player can retune the whole
 * reactor simulation per world with {@code /gamerule}. This port previously hard-coded them as
 * static fields with no way to change them at all, which meant e.g. the ReaSim boiler dial could
 * never be switched on and the steam inlet/outlet blocks were dead weight.</p>
 *
 * <p>Vanilla game rules only carry booleans and integers, so the boolean/int dials keep the
 * original key names verbatim here. The remaining dials are floating-point in the original and
 * live in {@link RBMKDials} as configurable statics instead.</p>
 */
public class RBMKGameRules {

    public static GameRules.Key<GameRules.BooleanValue> SAVE_DIALS;
    public static GameRules.Key<GameRules.BooleanValue> REASIM_BOILERS;
    public static GameRules.Key<GameRules.BooleanValue> PERMANENT_SCRAP;
    public static GameRules.Key<GameRules.BooleanValue> DISABLE_MELTDOWNS;
    public static GameRules.Key<GameRules.BooleanValue> MELTDOWN_OVERPRESSURE;
    public static GameRules.Key<GameRules.BooleanValue> DISABLE_DEPLETION;
    public static GameRules.Key<GameRules.BooleanValue> DISABLE_XENON;

    public static GameRules.Key<GameRules.IntegerValue> COLUMN_HEIGHT;
    public static GameRules.Key<GameRules.IntegerValue> FLUX_RANGE;
    public static GameRules.Key<GameRules.IntegerValue> REASIM_RANGE;

    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;

        SAVE_DIALS = GameRules.register("dialSaveDials",
                GameRules.Category.MISC, GameRules.BooleanValue.create(true));
        REASIM_BOILERS = GameRules.register("dialReasimBoilers",
                GameRules.Category.MISC, GameRules.BooleanValue.create(false));
        PERMANENT_SCRAP = GameRules.register("dialEnablePermaScrap",
                GameRules.Category.MISC, GameRules.BooleanValue.create(true));
        DISABLE_MELTDOWNS = GameRules.register("dialDisableMeltdowns",
                GameRules.Category.MISC, GameRules.BooleanValue.create(false));
        MELTDOWN_OVERPRESSURE = GameRules.register("dialEnableMeltdownOverpressure",
                GameRules.Category.MISC, GameRules.BooleanValue.create(false));
        DISABLE_DEPLETION = GameRules.register("dialDisableDepletion",
                GameRules.Category.MISC, GameRules.BooleanValue.create(false));
        DISABLE_XENON = GameRules.register("dialDisableXenon",
                GameRules.Category.MISC, GameRules.BooleanValue.create(false));

        // The original's dialColumnHeight counts the whole column and subtracts one internally
        // (RBMKDials:96); RBMKDials#getColumnHeight below does that same conversion.
        COLUMN_HEIGHT = GameRules.register("dialColumnHeight",
                GameRules.Category.MISC, GameRules.IntegerValue.create(4));
        FLUX_RANGE = GameRules.register("dialFluxRange",
                GameRules.Category.MISC, GameRules.IntegerValue.create(5));
        REASIM_RANGE = GameRules.register("dialReasimRange",
                GameRules.Category.MISC, GameRules.IntegerValue.create(10));
    }

    /**
     * CE {@code RBMKDials.createDials(world, forceRecreate)} runs at server start: when the
     * {@code dialSaveDials} gamerule is false (default true), every dial gamerule is force-reset
     * to its default so an experiment on one world cannot leak into the next. This is the port's
     * counterpart for the world-start reset.
     *
     * <p>Limitation: the port's floating-point dials live in the global mod config rather than in
     * per-world game rules, so they cannot be reset per world here - only the nine gamerule dials
     * (six booleans, three integers) are reset. The vanilla rules themselves persist natively when
     * {@code dialSaveDials} is left at its default {@code true}.</p>
     */
    public static void resetDialsOnWorldStart(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;
        net.minecraft.world.level.Level overworld = server.overworld();
        if (overworld == null) return;
        GameRules rules = overworld.getGameRules();
        // dialSaveDials true (the default) leaves every dial exactly as the world stored it.
        if (rules.getBoolean(SAVE_DIALS)) return;

        rules.getRule(REASIM_BOILERS).set(false, server);
        rules.getRule(PERMANENT_SCRAP).set(true, server);
        rules.getRule(DISABLE_MELTDOWNS).set(false, server);
        rules.getRule(MELTDOWN_OVERPRESSURE).set(false, server);
        rules.getRule(DISABLE_DEPLETION).set(false, server);
        rules.getRule(DISABLE_XENON).set(false, server);
        rules.getRule(COLUMN_HEIGHT).set(4, server);
        rules.getRule(FLUX_RANGE).set(5, server);
        rules.getRule(REASIM_RANGE).set(10, server);
    }
}
