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

    public double progress;
    public static final double SPEED = 1D / (15 * 60 * 20);

    public SatelliteMiner() { }

    @Override public String getType() { return "ASTEROID_MINER"; }

    @Override
    public java.util.List<net.minecraft.network.chat.Component> getInfo(net.minecraft.server.level.ServerLevel world) {
        int pct = (int) Math.round(this.progress * 100);
        return java.util.List.of(
                net.minecraft.network.chat.Component.translatable(com.hbm_m.item.ModItems.SATELLITE_MINER_ASTRO.get().getDescriptionId()),
                net.minecraft.network.chat.Component.translatable("satellite.minerprogress", pct + "%")
        );
    }

    @Override
    public void onUpdateTick(net.minecraft.server.level.ServerLevel world) {
        if (this.requestableSlots.length <= 0) {
            this.progress += SPEED;
            if (this.progress >= 1D) {
                this.progress = 0D;
                int itemAmount = 10 + world.random.nextInt(6);
                this.requestableSlots = new ItemStack[itemAmount];
                for (int i = 0; i < itemAmount; i++) {
                    this.requestableSlots[i] = SatellitePools.getStack(getCargo(), world.random);
                }
                this.markDirty();
            }
            if (world.getGameTime() % 1200 == 0) this.markDirty();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putDouble("progress", progress);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        this.progress = nbt.getDouble("progress");
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
