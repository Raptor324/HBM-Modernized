//? if forge {
package com.hbm_m.client;

import com.hbm_m.handler.ImpactWorldHandler;
import com.hbm_m.main.MainRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Original {@code WorldProviderNTM.getFogColor} (Oberwelt): Nebelfarbe nach dem Tom-Einschlag - gruen/blau
 * gedaempft durch Staub, waehrend des Feuersturms bis auf {@code max(1 - dust * 2, 0)} verdunkelt.
 */
@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, value = Dist.CLIENT)
public class ImpactFogHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Level level = Minecraft.getInstance().level;
        if (!com.hbm_m.config.GeneralConfig.enableImpactWorldProvider) return; // Original: nur mit WorldProviderNTM
        if (level == null || level.dimension() != Level.OVERWORLD) return;

        float dust = ImpactWorldHandler.getDustForClient(level);
        float fire = ImpactWorldHandler.getFireForClient(level);
        if (dust <= 0 && fire <= 0) return;

        float f3 = event.getRed();
        float f4 = event.getGreen() * (1 - (dust * 0.5F));
        float f5 = event.getBlue() * (1 - dust);

        if (fire > 0) {
            float m = Math.max((1 - (dust * 2)), 0);
            event.setRed(f3 * m);
            event.setGreen(f4 * m);
            event.setBlue(f5 * m);
        } else {
            event.setRed(f3 * (1 - dust));
            event.setGreen(f4 * (1 - dust));
            event.setBlue(f5 * (1 - dust));
        }
    }
}
//?}
