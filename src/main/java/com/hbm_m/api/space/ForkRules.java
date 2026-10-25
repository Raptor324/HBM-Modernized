package com.hbm_m.api.space;

/**
 * Space-API (Nutzerentscheidung E1-E17, 2026-10-12): Abweichungen des NTM-Space-Forks, die auch das Spiel auf der Erde
 * veraendern (Balance, Kosmetik, Traenke, Multiblock-Platzierung ...). Sie gelten <b>nur mit installiertem Addon</b>:
 * der Standard {@link #MASTER} liefert ueberall das Verhalten von NTM-Master (= HBM-Modernized wie bisher).
 *
 * <p>Das Addon setzt seine Fassung einmal im Mod-Konstruktor ueber {@link SpaceApi#setForkRules(ForkRules)}.
 * HBM fragt die Regeln ueber {@link SpaceApi#fork()} bzw. die Kurzform {@link SpaceApi#forkActive()} ab.
 */
public interface ForkRules {

    /** NTM-Master (HBM ohne Addon). */
    ForkRules MASTER = new ForkRules() { };

    /**
     * true = alle Fork-Abweichungen E2-E17 sind aktiv (Jetpack/DNT, Ueberdruckfaesser, Koks-Pulver, Siege-Beute,
     * Watz-Ertrag, Wand-/Deckenplatzierung, Balance, Reparatur beschaedigter Turbinen, Kosmetik, Traenke,
     * Schrabidische Saeure/Tom, Strahlenimmunitaet, Teleporter-N-Masse ...).
     */
    default boolean active() {
        return false;
    }

    /**
     * E1, Original {@code GeneralConfig.enableUnavoidableGravity} (Fork-Config "1.45_enableUnavoidableGravity",
     * Standard true): {@code TiltType.UNAVOIDABLE} (Big-Ass-Tank) kippt ohne Fundament nur, wenn true. Master: immer.
     */
    default boolean unavoidableGravity() {
        return true;
    }

    /**
     * E4, Original {@code TileEntityDiFurnace.getItemPower}: Zusatz-Brennwerte des Forks fuer Gegenstaende, die HBM
     * nicht kennt ({@code powder_coke} = 400). Nur aufgerufen, wenn HBM selbst 0 liefert. Master: 0.
     */
    default int blastFurnaceFuel(net.minecraft.world.item.ItemStack stack) {
        return 0;
    }

    /**
     * E8, Original {@code RotaryFurnaceRecipes}: Rotguss aus 3 Kupfer + 1 <b>Zink</b> statt Aluminium. Zink gibt es nur
     * im Addon - es liefert hier sein Material. null (Master) = Aluminium.
     */
    @org.jetbrains.annotations.Nullable
    default com.hbm_m.inventory.material.NTMMaterial gunmetalZinc() {
        return null;
    }

    /**
     * E11, Original {@code TileEntityMachineShredder.canProcess}: ein Wackelkopf "Gwen" (Addon-Gegenstand) im Eingang
     * sprengt den Schredder. Master: nie.
     */
    default boolean shredderExplodes(net.minecraft.world.item.ItemStack stack) {
        return false;
    }

    /**
     * E16, Original {@code TileEntityMachineTeleporter}: Tankfluid (N-Masse, Addon-Fluid) - jeder Dimensionswechsel
     * kostet 100 mB. null (Master) = kein Tank, Teleporter wie bisher.
     */
    @org.jetbrains.annotations.Nullable
    default net.minecraft.world.level.material.Fluid teleporterFluid() {
        return null;
    }
}
