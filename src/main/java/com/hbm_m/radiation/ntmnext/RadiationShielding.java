// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (RadiationSystemNT: RAD_RESISTANT_STATES, markRadResistant,
// resistantStateVersion), Commit 3f9a261a.
//
// Port-Abweichung: im Original werden die abschirmenden Bloecke in ModBlocks per
// afterRegistration(markRadResistant) eingetragen. Im Port kommt die Liste aus
// data/<ns>/ntm_radiation/resistant/*.json (NtmRadiationDataLoader), damit die Blockregistrierung
// unangetastet bleibt und Datapacks sie erweitern koennen.

package com.hbm_m.radiation.ntmnext;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.function.Predicate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Abschirmende ("rad-resistente") BlockStates. Sie teilen eine Sektion in getrennte Luft-Pockets;
 * Strahlung diffundiert nur ueber gemeinsame Pocket-Flaechen.
 *
 * <p>Erweiterungspunkt (Stufe 2): statt binaer "dicht/offen" koennen hier spaeter
 * Halbwertsschichten je Material und Strahlungsart hinterlegt werden.
 */
public final class RadiationShielding {

    static final ReferenceOpenHashSet<BlockState> RAD_RESISTANT_STATES =
            new ReferenceOpenHashSet<>(64);

    static volatile int resistantStateVersion;

    private RadiationShielding() {}

    public static void markRadResistant(Block block) {
        boolean changed = false;
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            changed |= RAD_RESISTANT_STATES.add(state);
        }
        if (changed) resistantStateVersion++;
    }

    public static void markRadResistant(Block block, Predicate<BlockState> filter) {
        boolean changed = false;
        for (BlockState s : block.getStateDefinition().getPossibleStates()) {
            if (filter.test(s)) changed |= RAD_RESISTANT_STATES.add(s);
        }
        if (changed) resistantStateVersion++;
    }

    public static boolean isResistant(BlockState state) {
        return RAD_RESISTANT_STATES.contains(state);
    }

    static void clear() {
        RAD_RESISTANT_STATES.clear();
        resistantStateVersion++;
    }
}
