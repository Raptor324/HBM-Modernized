//? if forge || neoforge {
package com.hbm_m.client;

import com.hbm_m.config.RadiationConfig;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.PollutionSyncPacket;
import com.mojang.blaze3d.shaders.FogShape;

import net.minecraft.client.Minecraft;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?}

/**
 * 1:1 {@code ModEventHandlerRenderer.worldTick/thickenFog/tintFog} (Smog-Teil): der synchronisierte Russwert
 * ({@link PollutionSyncPacket#pollution}) wird weich nachgefuehrt und zieht ab {@code sootFogThreshold} den Nebel
 * heran und faerbt ihn grau ({@code RadiationConfig.enableSootFog}).
 * Abweichung: das Original fuehrt {@code renderSoot} im serverseitigen WorldTickEvent nach (nur Einzelspieler),
 * hier im Client-Tick.
 */
//? if forge {
@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, value = Dist.CLIENT)
//?} else {
/*@EventBusSubscriber(modid = MainRegistry.MOD_ID, value = Dist.CLIENT)
*///?}
public final class SootFogHandler {

    private SootFogHandler() { }

    static float renderSoot = 0;

    @SubscribeEvent
    //? if forge {
    public static void worldTick(TickEvent.ClientTickEvent event) {

        if (event.phase == TickEvent.Phase.START && RadiationConfig.enableSootFog) {
    //?} else {
    /*public static void worldTick(ClientTickEvent.Pre event) {

        if (RadiationConfig.enableSootFog) {
    *///?}

            float step = 0.05F;
            float soot = PollutionSyncPacket.pollution[PollutionType.SOOT.ordinal()];

            if (Math.abs(renderSoot - soot) < step) {
                renderSoot = soot;
            } else if (renderSoot < soot) {
                renderSoot += step;
            } else if (renderSoot > soot) {
                renderSoot -= step;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void thickenFog(ViewportEvent.RenderFog event) {
        float soot = (float) (renderSoot - RadiationConfig.sootFogThreshold);
        if (soot > 0 && RadiationConfig.enableSootFog) {

            float farPlaneDistance = (float) (Minecraft.getInstance().options.renderDistance().get() * 16);
            float fogDist = farPlaneDistance / (1 + soot * 5F / (float) RadiationConfig.sootFogDivisor);
            event.setNearPlaneDistance(0);
            event.setFarPlaneDistance(fogDist);
            // Original: GL_NV_fog_distance -> radialer Nebel
            event.setFogShape(FogShape.SPHERE);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void tintFog(ViewportEvent.ComputeFogColor event) {

        float soot = (float) (renderSoot - RadiationConfig.sootFogThreshold);
        float sootColor = 0.15F;
        float sootReq = (float) RadiationConfig.sootFogDivisor;
        if (soot > 0 && RadiationConfig.enableSootFog) {
            float interp = Math.min(soot / sootReq, 1F);
            event.setRed(event.getRed() * (1 - interp) + sootColor * interp);
            event.setGreen(event.getGreen() * (1 - interp) + sootColor * interp);
            event.setBlue(event.getBlue() * (1 - interp) + sootColor * interp);
        }
    }
}
//?}
