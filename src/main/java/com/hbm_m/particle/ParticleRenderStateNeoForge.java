//? if neoforge {
/*package com.hbm_m.particle;

import com.mojang.blaze3d.systems.RenderSystem;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/^*
 * Ersatz fuer {@code ParticleRenderType.end(Tesselator)} auf 1.21.1: den Hook gibt es nicht mehr, der Partikel-Pass
 * setzt danach nur depthMask/Blend zurueck. Additive/FullBright/LongRange stellen in {@code begin} aber auch Cull,
 * Blendfunktion, Tiefenfunktion und den Nebel um - ohne Ruecksetzen liefen Wetter, Wolken und spaetere Renderer
 * ohne Nebel und ohne Backface-Culling. Eigene Partikeltypen sortiert NeoForge hinter die Vanilla-Typen, und sie sind
 * transluzent; direkt nach diesem Pass feuert {@code AFTER_PARTICLES} - dort wird hier genau das zurueckgesetzt, was
 * die 1.20.1-{@code end()}-Methoden zuruecksetzen (Nebel auf den Wert vor dem ersten eigenen Typ dieses Frames).
 ^/
@EventBusSubscriber(value = Dist.CLIENT)
public final class ParticleRenderStateNeoForge {

    private static boolean active;
    private static float savedFogStart;
    private static float savedFogEnd;

    private ParticleRenderStateNeoForge() {}

    /^* Aus {@code begin(..)} der eigenen Partikeltypen, bevor sie den Zustand aendern. ^/
    public static void capture() {
        if (active) return;
        savedFogStart = RenderSystem.getShaderFogStart();
        savedFogEnd = RenderSystem.getShaderFogEnd();
        active = true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !active) return;
        active = false;
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthFunc(515);
        RenderSystem.setShaderFogStart(savedFogStart);
        RenderSystem.setShaderFogEnd(savedFogEnd);
    }
}
*///?}
