package com.hbm_m.item.nuclear;

import java.util.function.DoubleUnaryOperator;

import com.hbm_m.util.BobMathUtil;

/**
 * PWR fuel archetypes, ported from the original {@code ItemPWRFuel.EnumPWRFuel} (1.7.10), which
 * defined 15 variants driven by the same small {@code Function} algebra library dropped for
 * {@link WatzPelletType} (see that class's doc for the rationale). Following the same convention,
 * this uses 5 representative types spanning the original's log/sqrt reactivity curves and
 * heat-emission tiers, as plain per-type items (see {@code PWRFuelItem}).
 * <p>
 * {@code burnFunc} mirrors {@code Function.effonix}: converts flux-per-rod into output-per-rod.
 * {@code heatEmission} is heat produced per unit of output (TU). {@code yield} is the total
 * output budget a single loaded rod produces before turning into spent/hot fuel (mirrors the
 * original's {@code processTime}).
 */
public enum PWRFuelType {

    /** Low-enrichment, log-curve fuel. Analogous to MEU/MEN/MOX/MEP in the original. */
    MEU(5.0D, x -> Math.log10(Math.max(0D, x) / 2_500D + 1D) * 600D, 1_000_000_000D),

    /** Medium-enrichment, sqrt-curve fuel. Analogous to HEU233/HEU235. */
    HEU(7.5D, x -> BobMathUtil.squirt(Math.max(0D, x)) * 22.5D, 1_000_000_000D),

    /** Reprocessed log-curve fuel. Analogous to MOX. */
    MOX(7.5D, x -> Math.log10(Math.max(0D, x) / 2_500D + 1D) * 600D, 1_000_000_000D),

    /** High-enrichment plutonium fuel, sqrt curve. Analogous to HEP239/HEP241. */
    HEP(10.0D, x -> BobMathUtil.squirt(Math.max(0D, x)) * 22.5D, 1_000_000_000D),

    /** Top-tier schrabidium fuel, steepest sqrt curve. Analogous to HES326/HES327. */
    SCHRABIDIUM(12.5D, x -> BobMathUtil.squirt(Math.max(0D, x)) * 30D, 1_000_000_000D);

    public final double heatEmission;
    public final DoubleUnaryOperator burnFunc;
    public final double yield;

    PWRFuelType(double heatEmission, DoubleUnaryOperator burnFunc, double yield) {
        this.heatEmission = heatEmission;
        this.burnFunc = burnFunc;
        this.yield = yield;
    }
}
