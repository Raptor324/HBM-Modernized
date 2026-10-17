package com.hbm_m.client.render.raptorpath;

import com.hbm_m.main.MainRegistry;

/**
 * Render-Migration: HBM-BERs auf Raptors Renderpfad ({@code com.hbm_m.client.render.machine.MachineRenderers}:
 * VBO je Teil, Instancing/MDI, Iris-Begleitmesh, Occlusion-Culling, 8-Ecken-Licht). Die alten immediate-BERs
 * bleiben unveraendert im Code; die Anmeldung hier ersetzt nur ihre Registrierung (gleicher BE-Typ, spaeter
 * angemeldet - {@code BlockEntityRenderers.register} ueberschreibt).
 *
 * <p>Aufruf am Ende von {@code ClientSetup.registerRenderersCommon()} (gleicher Thread wie HBMs eigene
 * {@code MachineRenderers}-Anmeldungen, beide Loader).
 *
 * <p>A/B-Vergleich im Dev-Client: JVM-Argument {@code -Dhbm_m.legacyMachineRender=true} laesst die alten
 * immediate-BERs angemeldet.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class HbmRaptorRenders {

    private HbmRaptorRenders() {}

    private static boolean registered;

    /** Alte immediate-BERs statt Raptors Pfad (nur zum Vergleich). */
    public static boolean enabled() {
        return !Boolean.getBoolean("hbm_m.legacyMachineRender");
    }

    public static void registerAll() {
        if (registered) return;
        registered = true;
        if (!enabled()) {
            MainRegistry.LOGGER.info("[hbm_m] Maschinen-BERs: alter immediate-Pfad (hbm_m.legacyMachineRender)");
            return;
        }
        // Pakete in com.hbm_m.client.render.implementations (Zugriff auf die paketinternen Modelle der alten BERs)
        com.hbm_m.client.render.implementations.RaptorPathP1.register();
        com.hbm_m.client.render.implementations.RaptorPathP2.register();
        com.hbm_m.client.render.implementations.RaptorPathP3.register();
        com.hbm_m.client.render.implementations.RaptorPathP4.register();
        com.hbm_m.client.render.implementations.RaptorPathP5.register();
        com.hbm_m.client.render.implementations.RaptorPathP6.register();
        com.hbm_m.client.render.implementations.RaptorPathP7.register();
        MainRegistry.LOGGER.info("[hbm_m] Maschinen-BERs der Render-Migration auf Raptors Renderpfad angemeldet");
    }
}
