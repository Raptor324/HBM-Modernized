package com.hbm_m.satellite;

import java.util.HashMap;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code SatelliteMiner} (Asteroiden-Bergbauschiff): die Andockstation ({@code sat_dock}) holt alle 10 Minuten
 * eine Ladung aus dem Beutepool {@link SatellitePools#POOL_SAT_MINER}. Wie im Original schreibt/liest er nur "lastOp".
 */
public class SatelliteMiner extends Satellite {

    private static final HashMap<Class<? extends SatelliteMiner>, String> CARGO = new HashMap<>();

    public long lastOp;

    public SatelliteMiner() { }

    @Override public String getType() { return "ASTEROID_MINER"; }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putLong("lastOp", lastOp);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        lastOp = nbt.getLong("lastOp");
    }

    public static void registerCargo(Class<? extends SatelliteMiner> minerSatelliteClass, String cargo) {
        CARGO.put(minerSatelliteClass, cargo);
    }

    public String getCargo() {
        return CARGO.get(getClass());
    }

    /** Beutepool fuer ein Satellitenitem, null wenn es kein Bergbausatellit ist. */
    @Nullable
    public static String getCargoForItem(ItemStack satelliteItem) {
        Class<? extends Satellite> satelliteClass = ITEM_TO_CLASS.get(satelliteItem.getItem());
        return satelliteClass != null ? CARGO.get(satelliteClass) : null;
    }

    static {
        registerCargo(SatelliteMiner.class, SatellitePools.POOL_SAT_MINER);
        registerCargo(SatelliteLunarMiner.class, SatellitePools.POOL_SAT_LUNAR);
    }
}
