package com.hbm_m.api.space;

import com.hbm_m.blockentity.machines.MachineLpw2BlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;

/**
 * Space-API (W-R6B-1): macht HBMs {@code machine_lpw2} (Master-Stand: Deko ohne Logik) zum Stationstriebwerk von NTM
 * Space ({@code TileEntityMachineLPW2 implements IPropulsion, IFluidStandardReceiver}). Standard = Deko wie bisher.
 *
 * <p>Mit {@link #active()} == true gilt 1:1 Space {@code MachineLPW2}: {@code getOffset 9}, {@code getDirModified =
 * dir.getRotation(DOWN)} (Blickrichtung im Uhrzeigersinn gedreht), Anschlusszellen ({@code makeExtra}) bei
 * {@code +-dir*3 - rot}, y+3; der Block-Entity tickt und nimmt ueber diese Zellen Fluessigkeiten in {@link #createTanks()}
 * an; der Renderer animiert ueber {@code lastTime/time} statt der Weltzeit.
 * <b>Muss im Addon-Mod-Konstruktor gesetzt werden</b> ({@link SpaceApi#setLpw2Hooks}) - die Blockstruktur wird bei der
 * Blockregistrierung festgelegt.
 */
public interface Lpw2Hooks {

    Lpw2Hooks DEFAULT = new Lpw2Hooks() { };

    /** true = Space-Triebwerk (Struktur, Ticker, Tanks, Animation); false = HBM-Deko. */
    default boolean active() {
        return false;
    }

    /** Tanks eines neuen Block-Entitys (Space: {@code KEROSENE_REFORM} + {@code OXYGEN}, je 256 000 mB). */
    default FluidTank[] createTanks() {
        return new FluidTank[0];
    }

    /** Server-Tick (nur aktiv): Brennen, Schub, Abonnieren der Anschluesse, Sync ueber {@code be.markForSync()}. */
    default void serverTick(MachineLpw2BlockEntity be) { }

    /**
     * Client-Tick (nur aktiv): Space-Animation ({@code speed}, {@code lastTime = time; time += speed}), Klang, Abgas.
     */
    default void clientTick(MachineLpw2BlockEntity be) { }

    /**
     * W-R9B4-1: Block-Entity entfernt oder Chunk entladen (Original {@code invalidate/onChunkUnload ->
     * unregisterPropulsion}). Nur aufgerufen, wenn {@link #active()}.
     */
    default void onRemoved(MachineLpw2BlockEntity be) { }

    /**
     * W-R9B4-2: Uebersetzungsschluessel des Blocks, wenn aktiv (Space: "LPW-2 Station Thruster"); null = HBM-Name.
     */
    default String descriptionId() {
        return null;
    }
}
