package com.hbm_m.saveddata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 1:1 {@code com.hbm.saveddata.TomSaveData}: Zustand nach dem Tom-Einschlag je Welt (Dimension) -
 * {@code dust} (Staub in der Atmosphaere, 0..1), {@code fire} (Feuersturm, 0..1), {@code impact} (Einschlag erfolgt).
 * Ablage wie im Original unter dem Schluessel {@code impactData} im Weltspeicher der Dimension.
 */
public class TomSaveData extends SavedData {

    public static final String key = "impactData";
    public float dust;
    public float fire;
    public boolean impact;

    private static TomSaveData lastCachedUnsafe = null;

    /* no caching for data per world needed, minecraft's save structure already does that! call forWorld as much as you want. */
    public static TomSaveData forWorld(ServerLevel world) {
        //? if < 1.21.1 {
        TomSaveData result = world.getDataStorage().computeIfAbsent(TomSaveData::load, TomSaveData::new, key);
        //?} else {
        /*TomSaveData result = world.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TomSaveData::new, (nbt, provider) -> load(nbt), null), key);
        *///?}
        lastCachedUnsafe = result;
        return result;
    }

    /** Fuer Weltgen-Zugriffe ({@code WorldGenRegion}) und Mixins; ohne Serverwelt gibt es keine Daten. */
    public static TomSaveData forWorld(LevelAccessor world) {
        if (world instanceof ServerLevel sl) return forWorld(sl);
        if (world instanceof ServerLevelAccessor sla) return forWorld(sla.getLevel());
        return null;
    }

    /**
     * Certain biome events do not have access to a world instance (very very bad), in those cases we have to rely on a possibly incorrect cached result.
     * However, due to the world gen invoking TomSaveData.forWorld() quite a lot, it is safe to say that in most cases, we do end up with the correct result.
     */
    public static TomSaveData getLastCachedOrNull() {
        return lastCachedUnsafe;
    }

    public static void resetLastCached() {
        lastCachedUnsafe = null;
    }

    public TomSaveData() {
    }

    private static TomSaveData load(CompoundTag compound) {
        TomSaveData data = new TomSaveData();
        data.dust = compound.getFloat("dust");
        data.fire = compound.getFloat("fire");
        data.impact = compound.getBoolean("impact");
        return data;
    }

    //? if < 1.21.1 {
    @Override
    public CompoundTag save(CompoundTag nbt) {
    //?} else {
    /*@Override
    public CompoundTag save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
    *///?}
        nbt.putFloat("dust", dust);
        nbt.putFloat("fire", fire);
        nbt.putBoolean("impact", impact);
        return nbt;
    }

    /** Original {@code markDirty}. */
    public void markDirty() {
        setDirty();
    }
}
