package com.hbm_m.satellite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.main.MainRegistry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1:1 {@code com.hbm.itempool.ItemPoolsSatellite}: die Ladung der Bergbausatelliten. {@link #getStack} entspricht
 * {@code ItemPool.getStack} (gewichtete Auswahl, Anzahl min..max). Die Gegenstaende stehen als Registry-IDs der
 * Port-Namen (Materialpulver heissen hier {@code <material>_powder}).
 */
public final class SatellitePools {

    public static final String POOL_SAT_MINER = "POOL_SAT_MINER";
    public static final String POOL_SAT_LUNAR = "POOL_SAT_LUNAR"; //woona

    private record Entry(String id, int min, int max, int weight) { }

    private static final Map<String, List<Entry>> POOLS = new HashMap<>();

    static {
        List<Entry> miner = new ArrayList<>();
        miner.add(new Entry("hbm_m:aluminum_powder", 3, 3, 10));
        miner.add(new Entry("hbm_m:iron_powder", 3, 3, 10));
        miner.add(new Entry("hbm_m:titanium_powder", 2, 2, 8));
        miner.add(new Entry("hbm_m:crystal_tungsten", 2, 2, 7));
        miner.add(new Entry("hbm_m:coal_powder", 4, 4, 15));
        miner.add(new Entry("hbm_m:uranium_powder", 2, 2, 5));
        miner.add(new Entry("hbm_m:plutonium_powder", 1, 1, 5));
        miner.add(new Entry("hbm_m:thorium_powder", 2, 2, 7));
        miner.add(new Entry("hbm_m:powder_desh_mix", 3, 3, 5));
        miner.add(new Entry("hbm_m:diamond_powder", 2, 2, 7));
        miner.add(new Entry("minecraft:redstone", 5, 5, 15));
        miner.add(new Entry("hbm_m:powder_nitan_mix", 2, 2, 5));
        miner.add(new Entry("hbm_m:powder_power", 2, 2, 5));
        miner.add(new Entry("hbm_m:copper_powder", 5, 5, 15));
        miner.add(new Entry("hbm_m:lead_powder", 3, 3, 10));
        miner.add(new Entry("hbm_m:fluorite", 4, 4, 15));
        miner.add(new Entry("hbm_m:lapis_powder", 4, 4, 10));
        miner.add(new Entry("hbm_m:crystal_aluminium", 1, 1, 5));
        miner.add(new Entry("hbm_m:crystal_gold", 1, 1, 5));
        miner.add(new Entry("hbm_m:crystal_phosphorus", 1, 1, 10));
        miner.add(new Entry("hbm_m:gravel_diamond", 1, 1, 3));
        miner.add(new Entry("hbm_m:crystal_uranium", 1, 1, 3));
        miner.add(new Entry("hbm_m:crystal_plutonium", 1, 1, 3));
        miner.add(new Entry("hbm_m:crystal_trixite", 1, 1, 1));
        miner.add(new Entry("hbm_m:crystal_starmetal", 1, 1, 1));
        miner.add(new Entry("hbm_m:crystal_lithium", 2, 2, 4));
        POOLS.put(POOL_SAT_MINER, miner);

        List<Entry> lunar = new ArrayList<>();
        lunar.add(new Entry("hbm_m:moon_turf", 48, 48, 5));
        lunar.add(new Entry("hbm_m:moon_turf", 32, 32, 7));
        lunar.add(new Entry("hbm_m:moon_turf", 16, 16, 5));
        lunar.add(new Entry("hbm_m:lithium_powder", 3, 3, 5));
        lunar.add(new Entry("hbm_m:iron_powder", 3, 3, 5));
        lunar.add(new Entry("hbm_m:crystal_iron", 1, 1, 1));
        lunar.add(new Entry("hbm_m:crystal_lithium", 1, 1, 1));
        POOLS.put(POOL_SAT_LUNAR, lunar);
    }

    private SatellitePools() { }

    /** Die Eintraege eines Pools als Stapel mit Mindestmenge (fuer JEI/Anzeige). */
    public static List<ItemStack> preview(String pool) {
        List<ItemStack> out = new ArrayList<>();
        for (Entry e : POOLS.getOrDefault(pool, List.of())) {
            Item item = resolve(e.id);
            if (item != Items.AIR) out.add(new ItemStack(item, e.min));
        }
        return out;
    }

    public static ItemStack getStack(String pool, RandomSource rand) {
        List<Entry> entries = POOLS.get(pool);
        if (entries == null || entries.isEmpty()) return ItemStack.EMPTY;

        int total = 0;
        for (Entry e : entries) total += e.weight;
        int roll = rand.nextInt(total);

        for (Entry e : entries) {
            roll -= e.weight;
            if (roll < 0) {
                Item item = resolve(e.id);
                if (item == Items.AIR) return ItemStack.EMPTY;
                int count = e.min + (e.max > e.min ? rand.nextInt(e.max - e.min + 1) : 0);
                return new ItemStack(item, count);
            }
        }
        return ItemStack.EMPTY;
    }

    private static Item resolve(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
        if (item == Items.AIR) MainRegistry.LOGGER.warn("[HBM] Satellitenpool: unbekannter Gegenstand {}", id);
        return item;
    }
}
