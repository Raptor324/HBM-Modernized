package com.hbm_m.api.space;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.inventory.fluid.FluidContainerDefs;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.trait.FluidTrait;

import dev.architectury.registry.registries.DeferredRegister;

import net.minecraft.world.level.material.Fluid;

/**
 * Oeffentliche Fluid-Registrierung fuer Addons (NTM Space: 62 neue {@code Fluids}). Die HBM-eigene Registrierung
 * ({@code ModFluids.registerFluid/registerGas}) ist privat und an den Namensraum {@code hbm_m} gebunden; diese Fassade
 * legt Fluessigkeiten im <b>Addon</b>-DeferredRegister an (Architectury, auf beiden Loadern gleich) und traegt sie in die
 * HBM-Tabellen ein, damit HBM-Tanks, Netze, Fluid-Identifier und GUIs sie als {@code FluidType} fuehren.
 *
 * <p>Ablauf im Addon (Mod-Konstruktor, vor {@code register.register()}):
 * <pre>{@code
 * FluidEntry DUNAAIR = HbmFluidApi.registerGas(SPACE_FLUIDS, "space_m", "dunaair", 0xD8A887);
 * HbmFluidApi.insertAfter(ModFluids.OXYGEN, DUNAAIR);           // GUI-Reihenfolge
 * // nach der Registrierung (Common-Setup): Traits/Temperatur/Gefahrenraute
 * HbmFluidApi.addTraits(DUNAAIR.getSource(), new FluidTraitSimple.FT_Gaseous());
 * }</pre>
 *
 * <p>Hinweise: Der Fluidname (Registry-Pfad ohne Namensraum) ist HBM-weit eindeutig ({@code FluidType.getName()}).
 * Die GUI-Textur sucht HBM unter {@code hbm_m:textures/gui/fluids/<name>.png} - das Addon liefert sie in seinen Assets
 * unter diesem Pfad mit; die Weltdarstellung nutzt {@code <namespace>:gui/fluids/<name>} (Blockatlas).
 */
public final class HbmFluidApi {

    private HbmFluidApi() {}

    /** Dichte/Viskositaet wie {@code ModFluids.registerFluid(name, color)}. */
    public static ModFluids.FluidEntry registerFluid(DeferredRegister<Fluid> register, String namespace, String name, int color) {
        return register(register, namespace, name, 1000, 1000, color);
    }

    /** Wie {@code ModFluids.registerGas}. */
    public static ModFluids.FluidEntry registerGas(DeferredRegister<Fluid> register, String namespace, String name, int color) {
        return register(register, namespace, name, -100, 100, color);
    }

    /** Wie {@code ModFluids.registerHeavyFluid}. */
    public static ModFluids.FluidEntry registerHeavyFluid(DeferredRegister<Fluid> register, String namespace, String name, int color) {
        return register(register, namespace, name, 1500, 2000, color);
    }

    /** Wie {@code ModFluids.registerPlasma}. */
    public static ModFluids.FluidEntry registerPlasma(DeferredRegister<Fluid> register, String namespace, String name, int color) {
        return register(register, namespace, name, -50, 50, color);
    }

    /**
     * Legt Quelle + Fliessform im Addon-Register an und traegt sie in {@code ModFluids.getEntry/getAllEntries/getTintColor}
     * ein. Ohne {@link #insertAfter} erscheint die Fluessigkeit nicht im Fluid-Identifier.
     */
    public static ModFluids.FluidEntry register(DeferredRegister<Fluid> register, String namespace, String name,
                                                int density, int viscosity, int color) {
        ModFluids.FluidEntry e = ModFluids.createFluid(register, namespace, name, density, viscosity, color);
        ModFluids.registerExternal(name, e, color);
        return e;
    }

    /** Einsortieren in die GUI-Reihenfolge (hinter {@code anchor}, null = ans Ende). */
    public static void insertAfter(@Nullable ModFluids.FluidEntry anchor, ModFluids.FluidEntry entry) {
        HbmFluidRegistry.insertAfter(anchor, entry);
    }

    /** Traits anhaengen (nach der Registrierung, z.B. Common-Setup). */
    public static void addTraits(Fluid fluid, FluidTrait... traits) {
        FluidType.forFluid(fluid).addTraits(traits);
    }

    /** {@code FluidType} der Fluessigkeit (Temperatur, Gefahrenraute, Traits). */
    public static FluidType type(Fluid fluid) {
        return FluidType.forFluid(fluid);
    }

    // ═════════════════════════ Behaelter (W-R4A-1) ═════════════════════════

    /**
     * Kanister ({@code canister_full}) fuer eine Fluessigkeit anmelden - Original {@code FluidType.addContainers(new
     * CD_Canister(color))}. Spaetestens im Common-Setup aufrufen (vor der ersten Rezept-/Behaelterabfrage).
     */
    public static void addCanister(Fluid fluid, int color) {
        FluidContainerDefs.registerCanister(FluidType.forFluid(fluid).getName(), color);
    }

    /** Gasflasche ({@code gas_full}) - Original {@code addContainers(new CD_Gastank(bottle, label))}. */
    public static void addGastank(Fluid fluid, int bottleColor, int labelColor) {
        FluidContainerDefs.registerGastank(FluidType.forFluid(fluid).getName(), bottleColor, labelColor);
    }

    // ═════════════════════ Reihenfolge Fluid-Setup (W-R4A-2) ═════════════════════

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<Runnable> AFTER_DEFAULTS = new ArrayList<>();
    private static boolean defaultsApplied;

    /**
     * Fuehrt {@code action} aus, sobald HBM seine Fluid-Vorbelegung ({@code ModFluidTraitsBootstrap.registerAll}:
     * Traits, Temperaturen, Heiz-/Kuehlketten, berechnete Brennwerte, geschaetzte Gefahrenraute aller
     * {@code ModFluids}-Eintraege) abgeschlossen hat - also garantiert <b>danach</b>, damit Addon-Werte nicht
     * ueberschrieben werden. Ist die Vorbelegung schon gelaufen, wird {@code action} sofort im aufrufenden Thread
     * ausgefuehrt. Jede Aktion laeuft genau einmal; Ausnahmen werden geloggt und unterbrechen die anderen nicht.
     * Aufruf typischerweise im Addon-Mod-Konstruktor.
     */
    public static void afterHbmFluidDefaults(Runnable action) {
        synchronized (AFTER_DEFAULTS) {
            if (!defaultsApplied) {
                AFTER_DEFAULTS.add(action);
                return;
            }
        }
        runSafely(action);
    }

    /** true, sobald HBMs Fluid-Vorbelegung gelaufen ist. */
    public static boolean fluidDefaultsApplied() {
        synchronized (AFTER_DEFAULTS) {
            return defaultsApplied;
        }
    }

    /** Nur HBM: am Ende von {@code ModFluidTraitsBootstrap.registerAll}. Weitere Aufrufe sind wirkungslos. */
    public static void fireFluidDefaultsApplied() {
        List<Runnable> pending;
        synchronized (AFTER_DEFAULTS) {
            if (defaultsApplied) return;
            defaultsApplied = true;
            pending = new ArrayList<>(AFTER_DEFAULTS);
            AFTER_DEFAULTS.clear();
        }
        for (Runnable r : pending) runSafely(r);
    }

    private static void runSafely(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException e) {
            LOGGER.error("[hbm_m] afterHbmFluidDefaults action {} failed", r, e);
        }
    }
}
