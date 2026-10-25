// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (RadiationSystemNT: RAD_SOURCE_STATES, markRadSource,
// registerConstantSources, RadSource), Commit 3f9a261a.

package com.hbm_m.radiation.ntmnext;

import com.hbm_m.main.MainRegistry;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.List;
import java.util.function.ToDoubleFunction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Konstante Strahlungsquellen je BlockState. Sektionen mit solchen Bloecken emittieren in jedem
 * Schritt (siehe {@link SectionSources}), ohne dass der Block selbst ticken muss.
 *
 * <p>Erweiterungspunkt (Stufe 3): Kontamination/Isotope koennen als weitere Quellenart neben
 * {@link RadSource} eingetragen werden.
 */
public final class RadiationSources {

    static final Reference2ObjectOpenHashMap<BlockState, RadSource> RAD_SOURCE_STATES =
            new Reference2ObjectOpenHashMap<>(64);

    record RadSource(double emission, double saturation) {}

    private RadiationSources() {}

    public static void markRadSource(
            Block block,
            ToDoubleFunction<BlockState> emission,
            ToDoubleFunction<BlockState> saturation) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            double e = emission.applyAsDouble(state);
            double s = saturation.applyAsDouble(state);

            if (e == 0.0D || !Double.isFinite(e) || !Double.isFinite(s)) continue;
            RAD_SOURCE_STATES.put(state, new RadSource(e, s));
        }
    }

    public static void markRadSource(Block block, double emission, double saturation) {
        markRadSource(block, state -> emission, state -> saturation);
    }

    static void clear() {
        RAD_SOURCE_STATES.clear();
    }

    /** Original registerConstantSources: alle Regeln in Zustandseintraege aufloesen. */
    static void registerConstantSources(List<RadSourceRule> rules) {
        for (RadSourceRule rule : rules) {
            for (Block block : rule.targets()) {
                markRadSource(
                        block,
                        state -> rule.matches(state) ? rule.rate().emission(block) : 0.0D,
                        state -> rule.matches(state) ? rule.rate().saturation(block) : 0.0D);
            }
        }
        MainRegistry.LOGGER.info(
                "[NtmRadiation] {} constant-source rules, {} states resolved",
                rules.size(),
                RAD_SOURCE_STATES.size());
    }
}
