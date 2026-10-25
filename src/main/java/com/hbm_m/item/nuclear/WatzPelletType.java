package com.hbm_m.item.nuclear;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.Item;

import com.hbm_m.util.function.Function;
import com.hbm_m.util.function.Function.FunctionLinear;
import com.hbm_m.util.function.Function.FunctionQuadratic;
import com.hbm_m.util.function.Function.FunctionSqrt;
import com.hbm_m.util.function.Function.FunctionSqrtFalling;

/**
 * 1:1 {@code ItemWatzPellet.EnumWatzType} (Watz Isotropic Fuel, Oxidized). Im Port ist jede Metadatenstufe ein
 * eigenes Item ({@code watz_pellet_<typ>} / {@code watz_pellet_depleted_<typ>}), die Reihenfolge entspricht der
 * Original-Metadatenreihenfolge.
 *
 * <p>Space-API (W-R4b-5): frueher ein {@code enum}; jetzt eine Klasse mit denselben Konstanten, damit Addons weitere
 * Typen anmelden koennen ({@link #register}, NTM Space: PU241, AMRG, AMF, CMRG, CMF, BK247, CF251, CF252, ES253).
 * {@link #values()} liefert weiterhin nur die HBM-Typen (eigene Items {@code watz_pellet_<typ>}), {@link #all()} alle.
 * Addon-Typen bringen ihre Items selbst mit ({@link #withItems}); der Watz kennt sie ueber {@link #depletedItem()}.
 */
public final class WatzPelletType {

    private static final List<WatzPelletType> BUILTIN = new ArrayList<>();
    private static final List<WatzPelletType> ALL = new CopyOnWriteArrayList<>();

    public static final WatzPelletType SCHRABIDIUM = builtin("SCHRABIDIUM", 0x32FFFF, 0x005C5C, 2_000, 20D, 0.01D, new FunctionLinear(1.5D), new FunctionSqrtFalling(10D), null);
    public static final WatzPelletType HES = builtin("HES", 0x66DCD6, 0x023933, 1_750, 20D, 0.005D, new FunctionLinear(1.25D), new FunctionSqrtFalling(15D), null);
    public static final WatzPelletType MES = builtin("MES", 0xCBEADF, 0x28473C, 1_500, 15D, 0.0025D, new FunctionLinear(1.15D), new FunctionSqrtFalling(15D), null);
    public static final WatzPelletType LES = builtin("LES", 0xABB4A8, 0x0C1105, 1_250, 15D, 0.00125D, new FunctionLinear(1D), new FunctionSqrtFalling(20D), null);
    public static final WatzPelletType HEN = builtin("HEN", 0xA6B2A6, 0x030F03, 0, 10D, 0.0005D, new FunctionSqrt(100), new FunctionSqrtFalling(10D), null);
    public static final WatzPelletType MEU = builtin("MEU", 0xC1C7BD, 0x2B3227, 0, 10D, 0.0005D, new FunctionSqrt(75), new FunctionSqrtFalling(10D), null);
    public static final WatzPelletType MEP = builtin("MEP", 0x9AA3A0, 0x111A17, 0, 15D, 0.0005D, new FunctionSqrt(150), new FunctionSqrtFalling(10D), null);
    /** Standardabsorber, negativer Koeffizient. */
    public static final WatzPelletType LEAD = builtin("LEAD", 0xA6A6B2, 0x03030F, 0, 0, 0.0025D, null, null, new FunctionSqrt(10));
    /** Verbesserter Absorber, linear. */
    public static final WatzPelletType BORON = builtin("BORON", 0xBDC8D2, 0x29343E, 0, 0, 0.0025D, null, null, new FunctionLinear(10));
    /** Absorber mit positivem Koeffizienten. */
    public static final WatzPelletType DU = builtin("DU", 0xC1C7BD, 0x2B3227, 0, 0, 0.0025D, null, null, new FunctionQuadratic(1D, 1D).withDiv(100));
    public static final WatzPelletType NQD = builtin("NQD", 0x4B4B4B, 0x121212, 2_000, 20, 0.01D, new FunctionLinear(2D), new FunctionSqrt(1D / 25D).withOff(25D * 25D), null);
    public static final WatzPelletType NQR = builtin("NQR", 0x2D2D2D, 0x0B0B0B, 2_500, 30, 0.01D, new FunctionLinear(1.5D), new FunctionSqrt(1D / 25D).withOff(25D * 25D), null);

    private static final WatzPelletType[] BUILTIN_ARRAY = BUILTIN.toArray(new WatzPelletType[0]);

    private final String name;
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

    @Nullable private Supplier<? extends Item> freshItem;
    @Nullable private Supplier<? extends Item> depletedItem;

    private WatzPelletType(String name, int colorLight, int colorDark, double passive, double heatEmission, double mudContent,
                           Function burnFunction, Function heatDivisor, Function absorbFunction) {
        this.name = name;
        this.colorLight = colorLight;
        this.colorDark = colorDark;
        this.passive = passive;
        this.heatEmission = heatEmission;
        this.mudContent = mudContent / 2D;
        this.burnFunc = burnFunction;
        this.heatDiv = heatDivisor;
        this.absorbFunc = absorbFunction;
    }

    private static WatzPelletType builtin(String name, int colorLight, int colorDark, double passive, double heatEmission, double mudContent,
                                          Function burnFunction, Function heatDivisor, Function absorbFunction) {
        WatzPelletType t = new WatzPelletType(name, colorLight, colorDark, passive, heatEmission, mudContent, burnFunction, heatDivisor, absorbFunction);
        BUILTIN.add(t);
        ALL.add(t);
        return t;
    }

    /**
     * Space-API (W-R4b-5): weiteren Pellettyp anmelden (Parameter wie {@code ItemWatzPellet.EnumWatzType}, Schlamm wird
     * wie dort halbiert). Der Name muss eindeutig sein. Das Addon legt die Items selbst an
     * ({@code new WatzPelletItem(props, typ, false/true)}) und verknuepft sie mit {@link #withItems}.
     */
    public static synchronized WatzPelletType register(String name, int colorLight, int colorDark, double passive, double heatEmission,
                                                      double mudContent, @Nullable Function burnFunction, @Nullable Function heatDivisor,
                                                      @Nullable Function absorbFunction) {
        for (WatzPelletType t : ALL) {
            if (t.name.equalsIgnoreCase(name)) throw new IllegalArgumentException("Watz pellet type already registered: " + name);
        }
        WatzPelletType t = new WatzPelletType(name.toUpperCase(Locale.ROOT), colorLight, colorDark, passive, heatEmission, mudContent,
                burnFunction, heatDivisor, absorbFunction);
        ALL.add(t);
        return t;
    }

    /** Items eines Addon-Typs (frisch/verbraucht); HBM-Typen nutzen {@code ModItems.WATZ_PELLET(_DEPLETED)}. */
    public WatzPelletType withItems(Supplier<? extends Item> fresh, Supplier<? extends Item> depleted) {
        this.freshItem = fresh;
        this.depletedItem = depleted;
        return this;
    }

    /** Frisches Pellet-Item dieses Typs oder {@code null}. */
    @Nullable
    public Item freshItem() {
        if (freshItem != null) return freshItem.get();
        var sup = com.hbm_m.item.ModItems.WATZ_PELLET.get(this);
        return sup != null ? sup.get() : null;
    }

    /** Verbrauchtes Pellet-Item dieses Typs oder {@code null}. */
    @Nullable
    public Item depletedItem() {
        if (depletedItem != null) return depletedItem.get();
        var sup = com.hbm_m.item.ModItems.WATZ_PELLET_DEPLETED.get(this);
        return sup != null ? sup.get() : null;
    }

    /** Die HBM-eigenen Typen in Original-Metadatenreihenfolge (wie frueher {@code Enum.values()}). */
    public static WatzPelletType[] values() { return BUILTIN_ARRAY.clone(); }

    /** HBM- und Addon-Typen. */
    public static List<WatzPelletType> all() { return Collections.unmodifiableList(ALL); }

    public String name() { return name; }

    public String id() { return name.toLowerCase(Locale.US); }

    @Override
    public String toString() { return name; }
}
