package com.hbm_m.block.machines.icf;

import net.minecraft.util.StringRepresentable;

/**
 * 1:1-Port von {@code BlockICFLaserComponent.EnumICFPart} (1.7.10): die sechs Bauteile des
 * Laseraufbaus.
 *
 * <p>Die Reihenfolge entspricht den Metadaten des Originals, weil der Phantomblock sie so
 * abspeichert.</p>
 *
 * <p><b>Abweichung:</b> im Original sind das sechs Metadaten <b>eines</b> Blocks. In 1.20 gibt es
 * dafuer keine Gegenstandsmetadaten mehr, darum registriert dieser Port sechs eigene Bloecke -
 * dieses Enum haelt sie zusammen.</p>
 */
public enum ICFLaserPart implements StringRepresentable {

    /** Huelle. Beendet die Suche, sie gehoert zur Aussenhaut. */
    CASING("casing"),
    /** Anschluss. Wie die Huelle, nimmt aber Energie ins Netz auf. */
    PORT("port"),
    /** Laserzelle. Muss in gerader Linie vom Steuerpult weg liegen. */
    CELL("cell"),
    /** Strahler. Zaehlt nur, wenn er an einer gueltigen Zelle liegt. */
    EMITTER("emitter"),
    /** Kondensator. Zaehlt nur an einem gueltigen Strahler; bestimmt die Speicherleistung. */
    CAPACITOR("capacitor"),
    /** Turbolader. Zaehlt nur an einem gueltigen Kondensator; hebt die Leistung weiter an. */
    TURBO("turbocharger");

    private final String name;

    ICFLaserPart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** true, wenn das Bauteil zur Aussenhaut gehoert und die Suche dort endet. */
    public boolean isCasing() {
        return this == CASING || this == PORT;
    }
}
