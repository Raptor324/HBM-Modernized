package com.hbm_m.api.pneumatic;

import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code ISlotMonitorProvider} (1.7.10): was ein Lager dem Netz anbieten muss.
 *
 * <p>Ueber diese Schnittstelle reden die {@link SlotMonitor Platzwaechter} und die Terminals mit
 * dem tatsaechlichen Inhalt eines Lagers. Die Mengen laufen als {@code long}, weil ein Massenlager
 * mehr fassen kann, als ein {@code ItemStack} zaehlen koennte.</p>
 */
public interface ISlotMonitorProvider {

    /** Die Waechter dieses Lagers - moeglichst einer je Platz. */
    SlotMonitor[] getMonitors();

    /** Der <b>tatsaechliche</b> Stapel an dieser Stelle, damit der Waechter Aenderungen sieht. */
    ItemStack getSlotAt(int index);

    /** Die Menge an dieser Stelle. Massenlager zaehlen ueber die Stapelgroesse hinaus. */
    long getAmountAt(int index);

    /** Entnimmt so viel wie moeglich und gibt zurueck, was nicht entnommen werden konnte. */
    long useUpItem(int index, long amount);

    /** Legt so viel wie moeglich hinein und gibt zurueck, was nicht mehr hineinpasste. */
    long addItem(int index, long amount);

    /** Belegt einen leeren Platz mit einem neuen Gegenstand; zurueck kommt der Rest. */
    long setupType(int index, ItemStack zeroStack, long amount);

    /** Ob sich ueber das Terminal ueberhaupt neue Gegenstandsarten hineinlegen lassen. */
    boolean allowTypeSetting();

    /** Ob dieses Lager von dem gegebenen Zugangspunkt aus erreichbar ist. */
    boolean isAvailableToCache(StackCache cache);

    /** Ueber das Netz finden die Waechter alle Zugangspunkte. */
    @Nullable
    PneumaticNet getRelevantNetwork();

    /** Original: laeuft, sobald ein neues Terminal ins Netz kommt - es holt sich alle Waechter. */
    default void onNewCacheHasJoined(StackCache stackCache) {
        for (SlotMonitor monitor : getMonitors()) {
            if (!stackCache.hasExpired && isAvailableToCache(stackCache)) {
                stackCache.addToCache(monitor);
            }
        }
    }

    default void updateMonitors() {
        for (SlotMonitor monitor : getMonitors()) monitor.checkUpdate();
    }
}
