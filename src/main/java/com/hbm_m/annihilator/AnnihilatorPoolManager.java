package com.hbm_m.annihilator;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Port of legacy {@code com.hbm.saveddata.AnnihilatorSavedData} - persistent, per-world,
 * per-named-"pool" cumulative counters of how much of an item/fluid has been destroyed by
 * Annihilators. Modeled on this codebase's {@link com.hbm_m.satellite.SatelliteManager}
 * SavedData pattern.
 * <p>
 * Keys are either {@code "item:<registry id>"} or {@code "fluid:<registry id>"}. Counts use
 * {@link BigInteger} since totals can exceed {@code long} range over a long game.
 */
public class AnnihilatorPoolManager extends SavedData {

    private static final String DATA_NAME = "hbm_modernized_annihilator_pools";

    private final Map<String, Map<String, BigInteger>> pools = new HashMap<>();

    public static AnnihilatorPoolManager get(ServerLevel level) {
        //? if < 1.21.1 {
        return level.getDataStorage().computeIfAbsent(
                AnnihilatorPoolManager::load,
                AnnihilatorPoolManager::new,
                DATA_NAME
        );
        //?} else {
        /*return level.getDataStorage().computeIfAbsent(
                new net.minecraft.world.level.saveddata.SavedData.Factory<>(
                        AnnihilatorPoolManager::new,
                        (nbt, provider) -> load(nbt),
                        null
                ),
                DATA_NAME
        );
        *///?}
    }

    private static AnnihilatorPoolManager load(CompoundTag nbt) {
        AnnihilatorPoolManager manager = new AnnihilatorPoolManager();
        CompoundTag poolsTag = nbt.getCompound("pools");
        for (String poolName : poolsTag.getAllKeys()) {
            CompoundTag poolTag = poolsTag.getCompound(poolName);
            Map<String, BigInteger> counts = new HashMap<>();
            for (String key : poolTag.getAllKeys()) {
                try {
                    counts.put(key, new BigInteger(poolTag.getString(key)));
                } catch (NumberFormatException ignored) {
                    // corrupted entry - skip, don't crash the world load
                }
            }
            manager.pools.put(poolName, counts);
        }
        return manager;
    }

    //? if < 1.21.1 {
    @Override
    public CompoundTag save(CompoundTag nbt) {
    //?} else {
    /*@Override
    public CompoundTag save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
    *///?}
        CompoundTag poolsTag = new CompoundTag();
        for (Map.Entry<String, Map<String, BigInteger>> poolEntry : pools.entrySet()) {
            CompoundTag poolTag = new CompoundTag();
            for (Map.Entry<String, BigInteger> countEntry : poolEntry.getValue().entrySet()) {
                poolTag.putString(countEntry.getKey(), countEntry.getValue().toString());
            }
            poolsTag.put(poolEntry.getKey(), poolTag);
        }
        nbt.put("pools", poolsTag);
        return nbt;
    }

    /** Adds {@code amount} to the counter for {@code key} in {@code pool} and returns the new total. */
    public BigInteger add(String pool, String key, long amount) {
        if (amount <= 0) return get(pool, key);
        Map<String, BigInteger> counts = pools.computeIfAbsent(pool, p -> new HashMap<>());
        BigInteger newVal = counts.getOrDefault(key, BigInteger.ZERO).add(BigInteger.valueOf(amount));
        counts.put(key, newVal);
        setDirty();
        return newVal;
    }

    public BigInteger get(String pool, String key) {
        Map<String, BigInteger> counts = pools.get(pool);
        if (counts == null) return BigInteger.ZERO;
        return counts.getOrDefault(key, BigInteger.ZERO);
    }

    // ═════════════════════════════════════════════════════════════════════════════════════
    //  1:1 AnnihilatorSavedData.pushToPool / AnnihilatorPool.increment
    // ═════════════════════════════════════════════════════════════════════════════════════

    /** Sammel-Oredict-Namen des Originals ohne eigenen Tag im Port. */
    public static final String ANY_PLASTIC = "hbm_m:any_plastic";
    public static final String ANY_HARDPLASTIC = "hbm_m:any_hardplastic";
    public static final String ANY_RESISTANTALLOY = "hbm_m:any_resistantalloy";

    public static String itemKey(Item item) { return "item:" + BuiltInRegistries.ITEM.getKey(item); }
    /** Original {@code ComparableStack(stack).makeSingular()} - im Port ohne Metadaten gleich der Item-ID. */
    public static String compKey(ItemStack stack) { return "comp:" + BuiltInRegistries.ITEM.getKey(stack.getItem()); }
    public static String fluidKey(Fluid fluid) { return "fluid:" + BuiltInRegistries.FLUID.getKey(fluid); }

    /** Original {@code ItemStackUtil.getOreDictNames}: die Item-Tags plus die Sammelnamen ANY_*. */
    public static List<String> dictNames(ItemStack stack) {
        List<String> names = new ArrayList<>();
        stack.getTags().forEach(t -> names.add(t.location().toString()));
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (id.equals("polymer_ingot") || id.equals("bakelite_ingot")) names.add(ANY_PLASTIC);
        if (id.equals("pc_ingot") || id.equals("pvc_ingot")) names.add(ANY_HARDPLASTIC);
        if (id.equals("tcalloy_ingot") || id.equals("cdalloy_ingot")) names.add(ANY_RESISTANTALLOY);
        return names;
    }

    /** Original {@code AnnihilatorPool.increment}: zaehlt hoch und liefert ggf. die Meilenstein-Auszahlung. */
    public ItemStack increment(String pool, String key, long amount, boolean alwaysPayOut) {
        Map<String, BigInteger> counts = pools.computeIfAbsent(pool, p -> new HashMap<>());
        ItemStack payout;
        BigInteger counter = counts.get(key);
        if (counter == null) {
            counter = BigInteger.valueOf(amount);
            payout = AnnihilatorRecipes.getHighestPayoutFromKey(key, BigInteger.ZERO, counter);
        } else {
            BigInteger prev = counter;
            counter = counter.add(BigInteger.valueOf(amount));
            payout = AnnihilatorRecipes.getHighestPayoutFromKey(key, alwaysPayOut ? null : prev, counter);
        }
        counts.put(key, counter);
        return payout;
    }

    /** Fuer Fluide. */
    public ItemStack pushToPool(String pool, Fluid type, long amount, boolean alwaysPayOut) {
        ItemStack payout = increment(pool, fluidKey(type), amount, alwaysPayOut);
        this.setDirty();
        return payout;
    }

    /** Fuer Items: Item (Wildcard), Item+Meta und alle Oredict-Namen. */
    public ItemStack pushToPool(String pool, ItemStack stack, boolean alwaysPayOut) {
        ItemStack itemPayout = increment(pool, itemKey(stack.getItem()), stack.getCount(), alwaysPayOut);
        ItemStack compPayout = increment(pool, compKey(stack), stack.getCount(), alwaysPayOut);
        ItemStack dictPayout = null;

        for (String name : dictNames(stack)) if (name != null && !name.isEmpty()) {
            ItemStack payout = increment(pool, "dict:" + name, stack.getCount(), alwaysPayOut);
            if (payout != null) dictPayout = payout;
        }

        this.setDirty();

        return dictPayout != null ? dictPayout : compPayout != null ? compPayout : itemPayout;
    }

    /** Original {@code monitor}: Zaehlerstand des Schluessels, sonst null (wie {@code pool.items.get(type)}). */
    public BigInteger getOrNull(String pool, String key) {
        Map<String, BigInteger> counts = pools.get(pool);
        return counts == null ? null : counts.get(key);
    }

    public boolean hasPool(String pool) {
        return pools.containsKey(pool);
    }
}
