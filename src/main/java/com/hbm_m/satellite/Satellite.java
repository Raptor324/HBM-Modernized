package com.hbm_m.satellite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.item.ModItems;
import com.hbm_m.main.MainRegistry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Port of legacy {@code com.hbm.saveddata.satellites.Satellite}: a satellite "type" that gets
 * tracked in orbit by {@link SatelliteManager} once its payload item reaches altitude via the
 * Soyuz Launcher. Only {@link SatelliteHorizons} (Gerald) is registered for now - the other
 * legacy satellite types (Mapper/Scanner/Radar/Laser/Resonator/Relay/Miner/LunarMiner) can be
 * added later via {@link #registerSatellite(Class, Item)}.
 */
public abstract class Satellite {

    public static final List<Class<? extends Satellite>> SATELLITES = new ArrayList<>();
    public static final Map<Item, Class<? extends Satellite>> ITEM_TO_CLASS = new HashMap<>();

    public static void register() {
        registerSatellite(SatelliteHorizons.class, ModItems.SAT_GERALD.get());
    }

    public static void registerSatellite(Class<? extends Satellite> sat, Item item) {
        if (!ITEM_TO_CLASS.containsKey(item) && !ITEM_TO_CLASS.containsValue(sat)) {
            SATELLITES.add(sat);
            ITEM_TO_CLASS.put(item, sat);
        }
    }

    public static Satellite create(int id) {
        if (id < 0 || id >= SATELLITES.size()) {
            return null;
        }
        try {
            return SATELLITES.get(id).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to instantiate satellite id {}", id, e);
            return null;
        }
    }

    public static int getIDFromItem(Item item) {
        return SATELLITES.indexOf(ITEM_TO_CLASS.get(item));
    }

    public int getID() {
        return SATELLITES.indexOf(this.getClass());
    }

    public void writeToNBT(CompoundTag nbt) { }

    public void readFromNBT(CompoundTag nbt) { }

    /** Called once when the satellite reaches orbit (rocket deploys its payload). */
    public void onOrbit(ServerLevel level, double x, double y, double z) { }

    /** Called by a matching-frequency {@code ItemSatDesignator} raytrace target. */
    public void onCoordAction(ServerLevel level, Player player, int x, int y, int z) { }
}
