package com.hbm_m.annihilator;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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
}
