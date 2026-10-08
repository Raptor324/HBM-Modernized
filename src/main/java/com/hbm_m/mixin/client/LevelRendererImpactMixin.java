package com.hbm_m.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.hbm_m.handler.ImpactWorldHandler;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.Level;

/**
 * Original {@code RenderNTMSkyboxImpact}: Sonne, Mond und Sterne werden nach dem Einschlag mit
 * {@code max(1 - dust * 2, 0) * (1 - rain)} statt nur {@code (1 - rain)} gezeichnet. Vanilla nimmt dafuer genau den
 * Regenwert aus {@code getRainLevel} in {@code renderSky}; der wird hier entsprechend umgerechnet.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererImpactMixin {

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"), require = 0)
    private float hbm_m$impactRain(ClientLevel level, float partialTick) {
        float rain = level.getRainLevel(partialTick);
        // Original: der Impact-Himmel wird nur mit 1.31_enableSkyboxes eingesetzt
        if (!com.hbm_m.config.GeneralConfig.enableSkyboxes) return rain;
        if (level.dimension() != Level.OVERWORLD) return rain;
        float atmosphericDust = ImpactWorldHandler.getDustForClient(level);
        float fire = ImpactWorldHandler.getFireForClient(level);
        if (atmosphericDust <= 0 && fire <= 0) return rain;
        float dust = Math.max((1.0F - (atmosphericDust * 2)), 0);
        return 1.0F - dust * (1.0F - rain);
    }

    /**
     * {@code RenderNTMSkyboxImpact.render} zeichnet kein Morgen-/Abendrot - solange Staub oder Feuer in der Luft
     * liegen, faellt es am Himmel weg (der Nebel nutzt weiter {@code getSunriseColor} mit {@code (1 - dust)}).
     */
    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;getSunriseColor(FF)[F"), require = 0)
    private float[] hbm_m$impactSunrise(net.minecraft.client.renderer.DimensionSpecialEffects effects, float timeOfDay, float partialTick) {
        ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        if (com.hbm_m.config.GeneralConfig.enableSkyboxes && level != null && level.dimension() == Level.OVERWORLD
                && (ImpactWorldHandler.getDustForClient(level) > 0 || ImpactWorldHandler.getFireForClient(level) > 0)) {
            return null;
        }
        return effects.getSunriseColor(timeOfDay, partialTick);
    }
}
