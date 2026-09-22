package com.hbm_m.blockentity.machines.pile;

import java.util.ArrayList;
import java.util.List;

/**
 * 1:1-Port von {@code TileEntityPileCore.PileSegment}: eine senkrechte Scheibe des Meilers, von
 * vorn gesehen.
 *
 * <p>Die Aufteilung in Scheiben macht die Simulation ueberschaubar: Neutronen wandern innerhalb
 * einer Scheibe frei und von Scheibe zu Scheibe nur noch gedaempft. Alle Kanaele einer Scheibe sind
 * durch die Bauform zwangslaeufig derselben Art.</p>
 */
public class PileSegment {

    public final List<PileChannel> channels = new ArrayList<>();
    public final PileChannelType segType;

    public PileSegment(PileChannelType segType) {
        this.segType = segType;
    }

    public PileSegment addChan(PileChannel chan) {
        this.channels.add(chan);
        return this;
    }

    /**
     * Original: {@code getNeutronMult} - wieviel Fluss diese Scheibe durchlaesst.
     *
     * <p>Nur Steuerscheiben daempfen ueberhaupt. {@code size} ist die Tiefe minus eins, weil ein
     * Muster aus Stab-Luecke-Stab-Luecke bei geradem Abzug genau die Haelfte abdeckt; darum ist
     * auch bei ganz eingefahrenen Staeben bei 0,5 Schluss - ein Meiler laesst sich nie voellig
     * abwuergen.</p>
     */
    public double getNeutronMult(PileCoreBlockEntity core) {
        if (segType != PileChannelType.CONTROL) return 1D;

        int size = core.getDepth() - 1;
        if (size < 3) return 0D; // so kleine Meiler gibt es gar nicht

        double total = 0D;
        for (PileChannel chan : channels) total += chan.control;

        return Math.max(0D, Math.min(0.5D, total / size));
    }
}
