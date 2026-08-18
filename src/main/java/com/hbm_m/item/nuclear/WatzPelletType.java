package com.hbm_m.item.nuclear;

import java.util.function.DoubleUnaryOperator;

/**
 * Fuel/absorber archetypes for the Watz reactor, ported from the original
 * {@code ItemWatzPellet.EnumWatzType} (1.7.10) enum-meta item.
 * <p>
 * SCOPE NOTE: the original defined 12 pellet variants driven by a small algebra library
 * ({@code Function}/{@code FunctionLinear}/{@code FunctionSqrt}/{@code FunctionSqrtFalling}/
 * {@code FunctionQuadratic}). Porting that whole function-object hierarchy was judged out of
 * scope; instead this enum captures the same qualitative behaviour (self-igniting fuel,
 * flux-dependent fuel, absorbers with different response curves) with 5 representative types
 * using plain {@link DoubleUnaryOperator} lambdas, each with equivalent burn/heat/absorb shapes.
 * <p>
 * Fields mirror the original: {@code passiveFlux} (base neutron flux emitted regardless of
 * reaction), {@code heatEmission} (heat produced per unit of burn/absorb), {@code mudContent}
 * (waste "Watz" fluid produced per unit of burn/absorb, in mB), {@code yield} (total burn/absorb
 * budget before depletion). {@code burnFunc} converts input flux -> burn rate (null for pure
 * absorbers). {@code heatDiv} throttles burn as stored heat rises (temperature coefficient,
 * null = no throttling). {@code absorbFunc} converts flux -> absorbed heat for
 * moderator/absorber pellets (null for pure fuel).
 */
public enum WatzPelletType {

    /** Self-igniting, high-output fuel. Analogous to SCHRABIDIUM/HES in the original. */
    SCHRABIDIUM_OXIDE(
            2_000D, 20D, 0.0050D, 1_500_000D,
            flux -> flux * 1.5D,
            heat -> 1D + Math.sqrt(Math.max(0D, heat)) / 10D,
            null
    ),

    /** Moderate-output enriched fuel that still self-sustains. Analogous to LES. */
    LES_OXIDE(
            1_250D, 15D, 0.0025D, 1_000_000D,
            flux -> flux * 1.0D,
            heat -> 1D + Math.sqrt(Math.max(0D, heat)) / 20D,
            null
    ),

    /** Needs external neutron flux from neighbours to sustain a reaction. Analogous to HEN/MEU. */
    NATURAL_URANIUM(
            0D, 10D, 0.0010D, 800_000D,
            flux -> Math.sqrt(Math.max(0D, flux) * 75D),
            heat -> 1D + Math.sqrt(Math.max(0D, heat)) / 10D,
            null
    ),

    /** Improved absorber/moderator, roughly linear response. Analogous to BORON. */
    BORON_CARBIDE(
            0D, 0D, 0.0025D, 2_000_000D,
            null, null,
            flux -> Math.sqrt(Math.max(0D, flux)) * 10D
    ),

    /** Standard absorber/shielding pellet, sqrt response. Analogous to LEAD. */
    LEAD_SHIELD(
            0D, 0D, 0.0025D, 2_000_000D,
            null, null,
            flux -> Math.sqrt(Math.max(0D, flux) * 10D)
    );

    public final double passiveFlux;
    public final double heatEmission;
    public final double mudContent;
    public final double yield;
    public final DoubleUnaryOperator burnFunc;
    public final DoubleUnaryOperator heatDiv;
    public final DoubleUnaryOperator absorbFunc;

    WatzPelletType(double passiveFlux, double heatEmission, double mudContent, double yield,
                   DoubleUnaryOperator burnFunc, DoubleUnaryOperator heatDiv, DoubleUnaryOperator absorbFunc) {
        this.passiveFlux = passiveFlux;
        this.heatEmission = heatEmission;
        this.mudContent = mudContent;
        this.yield = yield;
        this.burnFunc = burnFunc;
        this.heatDiv = heatDiv;
        this.absorbFunc = absorbFunc;
    }

    public boolean isFuel() {
        return burnFunc != null;
    }

    public boolean isAbsorber() {
        return absorbFunc != null;
    }
}
