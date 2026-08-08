package com.hbm_m.item.nuclear;

import java.util.function.DoubleUnaryOperator;

import com.hbm_m.util.BobMathUtil;

/**
 * PWR fuel archetypes, a 1:1 port of the original {@code ItemPWRFuel.EnumPWRFuel} (1.7.10).
 * The original drove these off a small {@code Function} algebra library
 * ({@code FunctionLogarithmic}/{@code FunctionSqrt}, each with a {@code div}/{@code off} pair);
 * here the exact same curves are inlined as {@link DoubleUnaryOperator} lambdas:
 * <ul>
 *   <li>Logarithmic: {@code log10(x / 2500 + 1) * level}  (original: div=2500, off=1)</li>
 *   <li>Sqrt: {@code squirt(x) * level}  (original: {@link BobMathUtil#squirt}, no div/off)</li>
 * </ul>
 * {@code burnFunc} mirrors {@code Function.effonix}: converts flux-per-rod into output-per-rod.
 * {@code heatEmission} is heat produced per unit of output (TU). {@code yield} is the total
 * output budget a single loaded rod produces before turning into spent/hot fuel (mirrors the
 * original's {@code processTime}).
 */
public enum PWRFuelType {

    MEU(5.0D, log(600D), 1_000_000_000D),
    HEU233(7.5D, sqrt(25D), 1_000_000_000D),
    HEU235(7.5D, sqrt(22.5D), 1_000_000_000D),
    MEN(7.5D, log(675D), 1_000_000_000D),
    HEN237(7.5D, sqrt(27.5D), 1_000_000_000D),
    MOX(7.5D, log(600D), 1_000_000_000D),
    MEP(7.5D, log(675D), 1_000_000_000D),
    HEP239(10.0D, sqrt(22.5D), 1_000_000_000D),
    HEP241(10.0D, sqrt(25D), 1_000_000_000D),
    MEA(7.5D, log(750D), 1_000_000_000D),
    HEA242(10.0D, sqrt(25D), 1_000_000_000D),
    HES326(12.5D, sqrt(27.5D), 1_000_000_000D),
    HES327(12.5D, sqrt(30D), 1_000_000_000D),
    BFB_AM_MIX(2.5D, sqrt(15D), 250_000_000D),
    BFB_PU241(2.5D, sqrt(15D), 250_000_000D);

    public final double heatEmission;
    public final DoubleUnaryOperator burnFunc;
    public final double yield;

    PWRFuelType(double heatEmission, DoubleUnaryOperator burnFunc, double yield) {
        this.heatEmission = heatEmission;
        this.burnFunc = burnFunc;
        this.yield = yield;
    }

    private static DoubleUnaryOperator log(double level) {
        return x -> Math.log10(Math.max(0D, x) / 2500D + 1D) * level;
    }

    private static DoubleUnaryOperator sqrt(double level) {
        return x -> BobMathUtil.squirt(Math.max(0D, x)) * level;
    }
}
