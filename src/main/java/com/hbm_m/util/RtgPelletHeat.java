package com.hbm_m.util;

import java.util.Map;

import com.hbm_m.item.ModItems;

import net.minecraft.world.item.Item;

/**
 * Shared RTG-pellet heat lookup, used by both the removed RTG block entity and
 * {@code MachineDifurnaceRtgBlockEntity} - see the removed RTG block entity's class javadoc for
 * the scope note on why decay/depletion isn't ported (values below are the original's non-decay
 * heat constants).
 */
public final class RtgPelletHeat {

    private RtgPelletHeat() {}

    private static Map<Item, Integer> heatMap;

    public static Map<Item, Integer> map() {
        if (heatMap == null) {
            heatMap = Map.ofEntries(
                    Map.entry(ModItems.PELLET_RTG_RADIUM.get(), 3),
                    Map.entry(ModItems.PELLET_RTG_WEAK.get(), 5),
                    Map.entry(ModItems.PELLET_RTG.get(), 10),
                    Map.entry(ModItems.PELLET_RTG_STRONTIUM.get(), 15),
                    Map.entry(ModItems.PELLET_RTG_COBALT.get(), 15),
                    Map.entry(ModItems.PELLET_RTG_ACTINIUM.get(), 20),
                    Map.entry(ModItems.PELLET_RTG_AMERICIUM.get(), 20),
                    Map.entry(ModItems.PELLET_RTG_POLONIUM.get(), 50),
                    Map.entry(ModItems.PELLET_RTG_GOLD.get(), 100),
                    Map.entry(ModItems.PELLET_RTG_LEAD.get(), 200)
            );
        }
        return heatMap;
    }

    public static int getHeat(Item item) {
        return map().getOrDefault(item, 0);
    }
}
