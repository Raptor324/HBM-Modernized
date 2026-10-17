package com.hbm_m.api.space;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Space-API (W-R6C-2/3): Satelliten-Haken fuer NTM Space. Alle Methoden {@code default} = HBM-Verhalten (Erde); das Addon
 * setzt eine Implementierung mit {@link SpaceApi#setSatelliteHooks}.
 */
public interface SatelliteHooks {

    /** Standard ohne Addon. */
    SatelliteHooks DEFAULT = new SatelliteHooks() { };

    /**
     * Welt, deren Satellitendaten an Position x/z gelten. Original NTM Space {@code SatelliteSavedData.getData(world, x, z)}:
     * in der Orbit-Dimension die Welt des von der Station an x/z umkreisten Koerpers. Standard: {@code level}.
     */
    default ServerLevel satelliteLevel(ServerLevel level, int x, int z) {
        return level;
    }

    /**
     * Original {@code XSatelliteRegistry.orbit} (Space: Bahndaten aus dem Gegenstand, Zielkoerper je Typ). {@code true} =
     * vom Addon erledigt; {@code false} = HBM-Standard {@code SatelliteManager.orbit} in {@link #satelliteLevel}.
     */
    default boolean orbit(ServerLevel level, ItemStack stack, int freq, double x, double y, double z) {
        return false;
    }

    /**
     * Satelliten-Verknuepfer hat die Frequenz von {@code source} nach {@code target} kopiert (Original Space
     * {@code TileEntityMachineSatLinker}: danach {@code SatelliteBase.copyItemData}, nur fuer Satelliten-Gegenstaende).
     */
    default void onLinkerCopy(ItemStack source, ItemStack target) { }

    /**
     * W-R6C-4: {@code false} blendet HBMs eigene {@code satchip.*}-Beschreibung eines Satelliten-Gegenstands aus (Original
     * Space: {@code ItemSatellite} mit eigenem Tooltip). Standard: immer anzeigen.
     */
    default boolean showHbmChipDescription(ItemStack stack) {
        return true;
    }
}
