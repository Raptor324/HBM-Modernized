// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (com.hbm.handler.radiation.RadSourceRule), Commit 3f9a261a.
// Original: Datapack-Registry "hbm:rad_source" mit Codec. Port: data/<ns>/ntm_radiation/sources/
// *.json im gleichen Format ("targets", optional "state", "rate" mit "type" fixed|item_hazard),
// geparst von NtmRadiationDataLoader.

package com.hbm_m.radiation.ntmnext;

import com.hbm_m.hazard.HazardRegistry;
import com.hbm_m.hazard.HazardSystem;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Regel "diese Bloecke sind konstante Strahlungsquellen". */
public record RadSourceRule(
        List<Block> targets, Optional<StatePropertiesPredicate> state, Rate rate) {

    public boolean matches(BlockState candidate) {
        return state.map(p -> p.matches(candidate)).orElse(true);
    }

    /** Emissionsrate einer Regel. */
    public sealed interface Rate permits Rate.Fixed, Rate.ItemHazard {

        String typeName();

        double emission(Block block);

        double saturation(Block block);

        /** Feste Rate aus der JSON-Datei. */
        record Fixed(double emission, double saturation) implements Rate {

            @Override
            public String typeName() {
                return "fixed";
            }

            @Override
            public double emission(Block block) {
                return emission;
            }

            @Override
            public double saturation(Block block) {
                return saturation;
            }
        }

        /** Rate aus dem Item-Hazard (RADIATION) des Blockitems, wie im Original. */
        record ItemHazard() implements Rate {

            @Override
            public String typeName() {
                return "item_hazard";
            }

            @Override
            public double emission(Block block) {
                return rawRads(block) * 0.1F / 20.0D;
            }

            @Override
            public double saturation(Block block) {
                return rawRads(block);
            }

            private static float rawRads(Block block) {
                // Original: HazardSystem.getRawRadsFromBlock(block)
                return HazardSystem.getHazardLevelFromStack(
                        new ItemStack(block.asItem()), HazardRegistry.RADIATION);
            }
        }
    }
}
