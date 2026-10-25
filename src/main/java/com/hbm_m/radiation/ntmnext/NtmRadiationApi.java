// Port-eigene Klasse (HBM-Modernized): oeffentliche Abfrage-/Schreib-API des NTM-Next-Feldes.

package com.hbm_m.radiation.ntmnext;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stabile Fassade fuer Code ausserhalb des Pakets (Geigerzaehler, Maschinen, spaetere Stufen).
 * Alle Methoden nur auf dem Server-Thread aufrufen. Ist das System nicht aktiv
 * ({@link #isActive()} == false), liefern Abfragen 0 und Schreibzugriffe verpuffen.
 *
 * <p>Stufe 2: Strahlungsarten. Die untypisierten Methoden nutzen den Mix des Blocks an der
 * Position (bzw. den Standard-Mix); die typisierten schreiben/lesen genau eine Art.
 */
public final class NtmRadiationApi {

    private NtmRadiationApi() {}

    /** Laeuft in diesem Serverlauf das NTM-Next-System? */
    public static boolean isActive() {
        return RadiationSystemSelector.isAdvanced();
    }

    // ---------------- Abfragen ----------------

    /** Ungewichtete Feldsumme aller Arten (RAD) an der Position. */
    public static double fieldAt(ServerLevel level, BlockPos pos) {
        return isActive() ? NtmRadiationSystem.getRadForCoord(level, pos) : 0.0D;
    }

    /** Felddichte einer Art (RAD) an der Position, ohne Nahfeld. */
    public static double fieldAt(ServerLevel level, BlockPos pos, RadiationType type) {
        return isActive() ? NtmRadiationSystem.getRadForCoord(level, pos, type) : 0.0D;
    }

    /** Nahfeld einer Art (RAD/s) an der Position. */
    public static double nearFieldAt(ServerLevel level, BlockPos pos, RadiationType type) {
        if (!isActive() || !type.hasField) return 0.0D;
        return NtmRadiationSystem.nearFieldAt(level, pos)[type.ordinal()];
    }

    /** Gewichtete Summe inkl. Nahfeld (das, was getRadiation/Spieler sehen). */
    public static double totalAt(ServerLevel level, BlockPos pos) {
        return isActive() ? NtmRadiationSystem.totalAt(level, pos, true) : 0.0D;
    }

    /** Dosisleistung inkl. Hintergrund der Dimension. */
    public static double doseAt(ServerLevel level, BlockPos pos) {
        return isActive() ? NtmRadiationSystem.doseAt(level, pos) : 0.0D;
    }

    /** Hintergrundstrahlung der Dimension (radiation settings). */
    public static double ambient(ServerLevel level) {
        return isActive() ? NtmRadiationSystem.ambientRad(level) : 0.0D;
    }

    // ---------------- Schreiben ----------------

    /** Einmaliger additiver Eintrag, Mix aus dem Block an der Position. */
    public static void add(ServerLevel level, BlockPos pos, double amount) {
        if (isActive()) NtmRadiationSystem.incrementRad(level, pos, amount);
    }

    /** Additiver Eintrag genau einer Art. */
    public static void add(ServerLevel level, BlockPos pos, RadiationType type, double amount) {
        if (isActive() && type.hasField)
            NtmRadiationSystem.incrementRad(level, pos, amount, SourceMixTable.single(type));
    }

    /** Additiver Eintrag mit eigenem Mix (gamma, neutron, beta, alpha; wird normiert). */
    public static void add(ServerLevel level, BlockPos pos, double amount, float[] mix) {
        if (isActive())
            NtmRadiationSystem.incrementRad(level, pos, amount, SourceMixTable.normalize(mix));
    }

    /** Saettigende Emission (Rate gegen Saettigungswert), Mix aus dem Block. */
    public static void emit(ServerLevel level, BlockPos pos, double emission, double saturation) {
        if (isActive()) NtmRadiationSystem.incrementRad(level, pos, emission, saturation);
    }

    /** Saettigende Emission genau einer Art. */
    public static void emit(
            ServerLevel level, BlockPos pos, RadiationType type, double emission, double saturation) {
        if (isActive() && type.hasField)
            NtmRadiationSystem.emitRad(level, pos, emission, saturation, SourceMixTable.single(type));
    }

    /** Dichte an der Position setzen (Mix aus dem Block). */
    public static void set(ServerLevel level, BlockPos pos, double amount) {
        if (isActive()) NtmRadiationSystem.setRadForCoord(level, pos, amount);
    }

    /** Dichte einer Art setzen. */
    public static void set(ServerLevel level, BlockPos pos, RadiationType type, double amount) {
        if (isActive()) NtmRadiationSystem.setRadForCoord(level, pos, type, amount);
    }

    // ---------------- Kontamination (Stufe 3) ----------------

    /** Radioaktives Material einer Gruppe am Ort ablegen (Einheit: RAD-aequivalente Menge). */
    public static void contaminate(ServerLevel level, BlockPos pos, ContaminationGroup group, double amount) {
        if (isActive()) NtmRadiationSystem.contaminate(level, pos, group, amount);
    }

    /** Luftgetragene Kontamination ablegen (wird vom Wind verfrachtet, sinkt ab). */
    public static void contaminateAirborne(ServerLevel level, BlockPos pos, ContaminationGroup group, double amount) {
        if (isActive()) NtmRadiationSystem.contaminateAirborne(level, pos, group, amount);
    }

    /** Luftgetragener Anteil einer Gruppe in der Sektion von pos. */
    public static double airborneAt(ServerLevel level, BlockPos pos, ContaminationGroup group) {
        return isActive() ? NtmRadiationSystem.airborneAt(level, pos, group) : 0.0D;
    }

    /** Windvektor (Bloecke/s, {vx, vz}) der Dimension. */
    public static double[] wind(ServerLevel level) {
        return WindModel.velocity(level);
    }

    /** Kontamination einer Gruppe (Boden + luftgetragen) in der Sektion von pos. */
    public static double contaminationAt(ServerLevel level, BlockPos pos, ContaminationGroup group) {
        return isActive() ? NtmRadiationSystem.contaminationAt(level, pos, group) : 0.0D;
    }

    /** Summe aller Gruppen in der Sektion von pos. */
    public static double contaminationAt(ServerLevel level, BlockPos pos) {
        if (!isActive()) return 0.0D;
        double s = 0.0D;
        for (ContaminationGroup g : ContaminationGroup.values())
            s += NtmRadiationSystem.contaminationAt(level, pos, g);
        return s;
    }

    /** Anteil {@code fraction} (0..1) der Kontamination im Wuerfel mit Radius entfernen. */
    public static int decontaminate(ServerLevel level, BlockPos pos, int radius, double fraction) {
        return isActive() ? NtmRadiationSystem.decontaminate(level, pos, radius, fraction) : 0;
    }

    /**
     * Alpha-Gefahr am Ort (fuer spaeteres Einatmen): LONG-Kontamination der Sektion mal Alpha-Anteil
     * der Gruppe. Wird in Stufe 3 bewusst nicht an die Spielerseite angebunden.
     */
    public static double alphaHazardAt(ServerLevel level, BlockPos pos) {
        if (!isActive()) return 0.0D;
        int g = ContaminationGroup.LONG.ordinal();
        return NtmRadiationSystem.contaminationAt(level, pos, ContaminationGroup.LONG)
                * NtmRadiationConfig.contamMix[g][RadiationType.ALPHA.ordinal()];
    }

    /** Sekunden, bis sich die Kontamination der Sektion halbiert (gemischte Gruppen). */
    public static double contaminationHalvingSeconds(ServerLevel level, BlockPos pos) {
        return isActive() ? NtmRadiationSystem.contaminationHalvingSeconds(level, pos) : Double.POSITIVE_INFINITY;
    }

    // ---------------- Material ----------------

    /** Ist der Block (in irgendeinem Zustand) als volle Abschirmung (eigene Pocket) registriert? */
    public static boolean isShielding(Block block) {
        for (var state : block.getStateDefinition().getPossibleStates()) {
            if (RadiationShielding.isResistant(state)) return true;
        }
        return false;
    }

    /** Halbwertsschicht (Bloecke) des Zustands fuer die Art; unendlich = keine Daempfung. */
    public static double halfValueLayer(BlockState state, RadiationType type) {
        return RadiationShieldingTable.halfValueLayer(type, state);
    }

    /** Strahlungsarten-Mix eines Quellblocks (gamma, neutron, beta, alpha). Kopie. */
    public static float[] sourceMix(BlockState state) {
        return SourceMixTable.mixFor(state).clone();
    }
}
