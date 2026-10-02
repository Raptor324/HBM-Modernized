package com.hbm_m.item.nuclear;

import java.util.Locale;

import com.hbm_m.util.function.Function;
import com.hbm_m.util.function.Function.FunctionLinear;
import com.hbm_m.util.function.Function.FunctionQuadratic;
import com.hbm_m.util.function.Function.FunctionSqrt;
import com.hbm_m.util.function.Function.FunctionSqrtFalling;

/**
 * 1:1 {@code ItemWatzPellet.EnumWatzType} (Watz Isotropic Fuel, Oxidized). Im Port ist jede Metadatenstufe ein
 * eigenes Item ({@code watz_pellet_<typ>} / {@code watz_pellet_depleted_<typ>}), die Reihenfolge entspricht der
 * Original-Metadatenreihenfolge.
 */
public enum WatzPelletType {

    SCHRABIDIUM(0x32FFFF, 0x005C5C, 2_000, 20D, 0.01D, new FunctionLinear(1.5D), new FunctionSqrtFalling(10D), null),
    HES(0x66DCD6, 0x023933, 1_750, 20D, 0.005D, new FunctionLinear(1.25D), new FunctionSqrtFalling(15D), null),
    MES(0xCBEADF, 0x28473C, 1_500, 15D, 0.0025D, new FunctionLinear(1.15D), new FunctionSqrtFalling(15D), null),
    LES(0xABB4A8, 0x0C1105, 1_250, 15D, 0.00125D, new FunctionLinear(1D), new FunctionSqrtFalling(20D), null),
    HEN(0xA6B2A6, 0x030F03, 0, 10D, 0.0005D, new FunctionSqrt(100), new FunctionSqrtFalling(10D), null),
    MEU(0xC1C7BD, 0x2B3227, 0, 10D, 0.0005D, new FunctionSqrt(75), new FunctionSqrtFalling(10D), null),
    MEP(0x9AA3A0, 0x111A17, 0, 15D, 0.0005D, new FunctionSqrt(150), new FunctionSqrtFalling(10D), null),
    /** Standardabsorber, negativer Koeffizient. */
    LEAD(0xA6A6B2, 0x03030F, 0, 0, 0.0025D, null, null, new FunctionSqrt(10)),
    /** Verbesserter Absorber, linear. */
    BORON(0xBDC8D2, 0x29343E, 0, 0, 0.0025D, null, null, new FunctionLinear(10)),
    /** Absorber mit positivem Koeffizienten. */
    DU(0xC1C7BD, 0x2B3227, 0, 0, 0.0025D, null, null, new FunctionQuadratic(1D, 1D).withDiv(100)),
    NQD(0x4B4B4B, 0x121212, 2_000, 20, 0.01D, new FunctionLinear(2D), new FunctionSqrt(1D / 25D).withOff(25D * 25D), null),
    NQR(0x2D2D2D, 0x0B0B0B, 2_500, 30, 0.01D, new FunctionLinear(1.5D), new FunctionSqrt(1D / 25D).withOff(25D * 25D), null);

    public double yield = 500_000_000;
    public final int colorLight;
    public final int colorDark;
    /** Schlamm pro Reaktionsfluss. */
    public final double mudContent;
    /** Grundfluss. */
    public final double passive;
    /** Waerme pro ausgehendem Fluss. */
    public final double heatEmission;
    /** Fluss -> Reaktivitaet(0). */
    public final Function burnFunc;
    /** Reaktivitaet(0) -> Reaktivitaet(1) abhaengig von der Hitze (Temperaturkoeffizient). */
    public final Function heatDiv;
    /** Fluss -> Waerme (Absorption fuer nicht-aktive Pellets). */
    public final Function absorbFunc;

    WatzPelletType(int colorLight, int colorDark, double passive, double heatEmission, double mudContent,
                   Function burnFunction, Function heatDivisor, Function absorbFunction) {
        this.colorLight = colorLight;
        this.colorDark = colorDark;
        this.passive = passive;
        this.heatEmission = heatEmission;
        this.mudContent = mudContent / 2D;
        this.burnFunc = burnFunction;
        this.heatDiv = heatDivisor;
        this.absorbFunc = absorbFunction;
    }

    public String id() { return name().toLowerCase(Locale.US); }
}
