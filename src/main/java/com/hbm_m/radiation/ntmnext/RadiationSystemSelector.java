// Port-eigene Klasse (HBM-Modernized): Auswahl zwischen Raptors Strahlungssystem und dem
// NTM-Next-Port sowie die Lebenszyklus-Anbindung des NTM-Next-Systems.

package com.hbm_m.radiation.ntmnext;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.radiation.ChunkRadiationHandler;
import com.hbm_m.radiation.ChunkRadiationHandlerSimple;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;

/**
 * Waehlt beim Serverstart das Welt-Strahlungssystem ({@link ModClothConfig#radiationSystem}).
 *
 * <p>Der Modus wird nur in {@code SERVER_BEFORE_START} uebernommen, ein Umschalten in der
 * Konfiguration wirkt daher erst nach dem naechsten Welt-Neustart. {@code ChunkRadiationManager
 * .getProxy()} fragt {@link #needsNewProxy} und erzeugt den Handler hoechstens einmal pro
 * Serverstart neu.
 */
public final class RadiationSystemSelector {

    private static volatile ModClothConfig.RadiationSystemMode activeMode =
            ModClothConfig.RadiationSystemMode.RAPTOR;
    private static boolean registered;

    private RadiationSystemSelector() {}

    /** Aus MainRegistry (gemeinsame Initialisierung) aufrufen. */
    public static void init() {
        if (registered) return;
        registered = true;

        LifecycleEvent.SERVER_BEFORE_START.register(
                server -> {
                    activeMode = ModClothConfig.get().radiationSystem;
                    if (activeMode == null) activeMode = ModClothConfig.RadiationSystemMode.RAPTOR;
                    MainRegistry.LOGGER.info("[NtmRadiation] world radiation system: {}", activeMode);
                    if (isAdvanced()) {
                        NtmRadiationSystem.serverStopping = false;
                        NtmRadiationSystem.ticks = 0L;
                        NtmRadiationSystem.radiationFuture = NtmRadiationSystem.COMPLETED;
                        NtmRadiationSystem.worldMap.clear();
                        NtmRadiationSystem.nearFields.clear();
                        NtmRadiationDataLoader.load(server);
                        NtmRadiationSystem.onLoadComplete();
                    }
                });

        TickEvent.SERVER_POST.register(
                server -> {
                    if (isAdvanced()) NtmRadiationSystem.tickSim(server);
                });

        LifecycleEvent.SERVER_LEVEL_SAVE.register(
                level -> {
                    if (isAdvanced()) NtmRadiationSystem.onLevelSave(level);
                });

        LifecycleEvent.SERVER_STOPPING.register(
                server -> {
                    if (isAdvanced()) NtmRadiationSystem.onServerStopping(server);
                });
    }

    /** Aktiver Modus dieses Serverlaufs. */
    public static ModClothConfig.RadiationSystemMode activeMode() {
        return activeMode;
    }

    public static boolean isAdvanced() {
        return activeMode == ModClothConfig.RadiationSystemMode.ADVANCED;
    }

    /** Passt der bestehende Handler nicht (mehr) zum Modus dieses Serverlaufs? */
    public static boolean needsNewProxy(ChunkRadiationHandler current) {
        return (current instanceof ChunkRadiationHandlerNTMNext) != isAdvanced();
    }

    /** Erzeugt den Handler fuer den aktiven Modus. */
    public static ChunkRadiationHandler createProxy() {
        return isAdvanced() ? new ChunkRadiationHandlerNTMNext() : new ChunkRadiationHandlerSimple();
    }
}
