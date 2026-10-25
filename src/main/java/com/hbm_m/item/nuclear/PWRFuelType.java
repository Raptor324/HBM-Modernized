package com.hbm_m.item.nuclear;

import com.hbm_m.util.function.Function;
import com.hbm_m.util.function.Function.FunctionLogarithmic;
import com.hbm_m.util.function.Function.FunctionSqrt;

/**
 * 1:1 {@code ItemPWRFuel.EnumPWRFuel}. Wie im Original setzt der Konstruktor mit Ertragsparameter den Ertrag nicht -
 * auch die BFB-Staebe haben daher {@code yield = 1_000_000_000}.
 */
public enum PWRFuelType {

    MEU(05.0D, new FunctionLogarithmic(20 * 30).withDiv(2_500)),
    HEU233(07.5D, new FunctionSqrt(25)),
    HEU235(07.5D, new FunctionSqrt(22.5)),
    MEN(07.5D, new FunctionLogarithmic(22.5 * 30).withDiv(2_500)),
    HEN237(07.5D, new FunctionSqrt(27.5)),
    MOX(07.5D, new FunctionLogarithmic(20 * 30).withDiv(2_500)),
    MEP(07.5D, new FunctionLogarithmic(22.5 * 30).withDiv(2_500)),
    HEP239(10.0D, new FunctionSqrt(22.5)),
    HEP241(10.0D, new FunctionSqrt(25)),
    MEA(07.5D, new FunctionLogarithmic(25 * 30).withDiv(2_500)),
    HEA242(10.0D, new FunctionSqrt(25)),
    HES326(12.5D, new FunctionSqrt(27.5)),
    HES327(12.5D, new FunctionSqrt(30)),
    BFB_AM_MIX(2.5D, new FunctionSqrt(15), 250_000_000),
    BFB_PU241(2.5D, new FunctionSqrt(15), 250_000_000);

    public double yield = 1_000_000_000;
    public final double heatEmission;
    public final Function function;

    PWRFuelType(double heatEmission, Function function, double yield) {
        this.heatEmission = heatEmission;
        this.function = function;
    }

    PWRFuelType(double heatEmission, Function function) {
        this(heatEmission, function, 1_000_000_000);
    }
}
