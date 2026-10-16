package com.hbm_m.satellite;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Frequency -> orbited {@link Satellite} registry. Port of legacy
 * {@code com.hbm.saveddata.SatelliteSavedData}, using the vanilla SavedData pattern.
 */
public class SatelliteManager extends SavedData {

    private static final String DATA_NAME = "hbm_modernized_satellites";

    private final Map<Integer, Satellite> byFrequency = new HashMap<>();

    public SatelliteManager() {
    }

    public static SatelliteManager get(ServerLevel level) {
        //? if < 1.21.1 {
        return level.getDataStorage().computeIfAbsent(
                SatelliteManager::load,
                SatelliteManager::new,
                DATA_NAME
        );
        //?} else {
        /*return level.getDataStorage().computeIfAbsent(
                new net.minecraft.world.level.saveddata.SavedData.Factory<>(
                        SatelliteManager::new,
                        (nbt, provider) -> load(nbt),
                        null
                ),
                DATA_NAME
        );
        *///?}
    }

    private static SatelliteManager load(CompoundTag nbt) {
        SatelliteManager manager = new SatelliteManager();
        int count = nbt.getInt("satCount");
        for (int i = 0; i < count; i++) {
            Satellite sat = Satellite.create(nbt.getInt("sat_id_" + i));
            if (sat == null) {
                continue;
            }
            sat.readFromNBT(nbt.getCompound("sat_data_" + i));
            manager.byFrequency.put(nbt.getInt("sat_freq_" + i), sat);
        }
        return manager;
    }

    public boolean isFreqTaken(int freq) {
        return byFrequency.containsKey(freq);
    }

    public Satellite getSatFromFreq(int freq) {
        return byFrequency.get(freq);
    }

    public void orbit(ServerLevel level, int freq, Item item, double x, double y, double z) {
        int id = Satellite.getIDFromItem(item);
        Satellite sat = Satellite.create(id);
        if (sat == null) {
            return;
        }
        byFrequency.put(freq, sat);
        sat.onOrbit(level, x, y, z);
        setDirty();
    }

    //? if < 1.21.1 {
    @Override
    public CompoundTag save(CompoundTag nbt) {
    //?} else {
    /*@Override
    public CompoundTag save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
    *///?}
        nbt.putInt("satCount", byFrequency.size());
        int i = 0;
        for (Map.Entry<Integer, Satellite> entry : byFrequency.entrySet()) {
            CompoundTag data = new CompoundTag();
            entry.getValue().writeToNBT(data);

            nbt.putInt("sat_id_" + i, entry.getValue().getID());
            nbt.put("sat_data_" + i, data);
            nbt.putInt("sat_freq_" + i, entry.getKey());
            i++;
        }
        return nbt;
    }
}