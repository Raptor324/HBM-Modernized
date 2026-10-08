// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (RadiationSystemNT.PendingRad), Commit 3f9a261a.

package com.hbm_m.radiation.ntmnext;

import java.util.Arrays;
import javax.annotation.Nullable;

/**
 * Aus der Chunk-Datei gelesene, aber noch nicht in das Raster uebernommene Sektionsdichten.
 * Werden beim ersten Rebuild der Sektion (Pockets neu berechnet) eingemischt.
 */
final class PendingRad {
    final SavedSection[] bySy;
    int nonEmptySections;

    PendingRad(int sectionsPerChunk) {
        this.bySy = new SavedSection[sectionsPerChunk];
    }

    boolean hasSy(int sy) {
        return bySy[sy] != null;
    }

    boolean isEmpty() {
        return nonEmptySections == 0;
    }

    void clearAll() {
        Arrays.fill(bySy, null);
        nonEmptySections = 0;
    }

    void clearSy(int sy) {
        if (bySy[sy] != null) {
            bySy[sy] = null;
            nonEmptySections--;
        }
    }

    void put(int sy, SavedSection section) {
        if (bySy[sy] == null) nonEmptySections++;
        bySy[sy] = section;
    }

    SavedSection get(int sy) {
        return bySy[sy];
    }

    SavedSection take(int sy) {
        SavedSection section = bySy[sy];
        clearSy(sy);
        return section;
    }

    record SavedSection(
            int pocketCount, @Nullable short[] pocketData, int[] volumes, double[] densities) {

        boolean hasSameTopology(int currentPocketCount, @Nullable short[] currentPocketData) {
            return pocketCount == currentPocketCount
                    && Arrays.equals(pocketData, currentPocketData);
        }
    }
}
