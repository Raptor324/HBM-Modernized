package com.hbm_m.handler.pollution;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 1:1-Port von {@code PollutionHandler.PollutionPerWorld} (1.7.10).
 *
 * <p>Das Original schreibt eine eigene Datei {@code hbmpollution.dat} und haengt sich dafuer an
 * {@code WorldEvent.Load/Save}. Hier uebernimmt das die {@link SavedData} der Welt - dieselbe
 * Ablage, die im Port auch {@code AnnihilatorPoolManager} und {@code SatelliteManager} nutzen.
 * Das NBT-Format innerhalb der Liste {@code entries} ist unveraendert.</p>
 */
public class PollutionSavedData extends SavedData {

    private static final String DATA_NAME = "hbm_modernized_pollution";

    /**
     * Schluessel ist <b>keine</b> Chunkposition, sondern eine Rasterzelle von 64x64 Bloecken
     * ({@code x >> 6}) - genau wie im Original, das dafuer ebenfalls
     * {@code ChunkCoordIntPair} zweckentfremdet.
     */
    public final Map<ChunkPos, PollutionData> pollution = new HashMap<>();

    public static PollutionSavedData get(ServerLevel level) {
        //? if < 1.21.1 {
        return level.getDataStorage().computeIfAbsent(
                PollutionSavedData::load,
                PollutionSavedData::new,
                DATA_NAME
        );
        //?} else {
        /*return level.getDataStorage().computeIfAbsent(
                new net.minecraft.world.level.saveddata.SavedData.Factory<>(
                        PollutionSavedData::new,
                        (nbt, provider) -> load(nbt),
                        null
                ),
                DATA_NAME
        );
        *///?}
    }

    private static PollutionSavedData load(CompoundTag nbt) {
        PollutionSavedData data = new PollutionSavedData();
        ListTag list = nbt.getList("entries", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            data.pollution.put(
                    new ChunkPos(entry.getInt("chunkX"), entry.getInt("chunkZ")),
                    PollutionData.fromNBT(entry));
        }
        return data;
    }

    //? if < 1.21.1 {
    @Override
    public CompoundTag save(CompoundTag nbt) {
    //?} else {
    /*@Override
    public CompoundTag save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
    *///?}
        ListTag list = new ListTag();

        for (Map.Entry<ChunkPos, PollutionData> entry : pollution.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("chunkX", entry.getKey().x);
            tag.putInt("chunkZ", entry.getKey().z);
            entry.getValue().toNBT(tag);
            list.add(tag);
        }

        nbt.put("entries", list);
        return nbt;
    }
}
