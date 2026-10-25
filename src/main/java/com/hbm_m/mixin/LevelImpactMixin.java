package com.hbm_m.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm_m.handler.ImpactWorldHandler;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * Original {@code WorldProviderNTM.getSunBrightnessFactor}/{@code isDaytime} (Oberwelt): der Tom-Staub dimmt die
 * Sonne - {@code calculateSkylightSubtracted} wird mit {@code sunBr * (1 - dust)} gerechnet. In 1.20 steckt das
 * in {@code skyDarken} ({@code isDay} = {@code skyDarken < 4}, ab Staub 0,75 ist es daher nie Tag).
 */
@Mixin(Level.class)
public abstract class LevelImpactMixin {

    @Shadow private int skyDarken;

    @Inject(method = "updateSkyBrightness", at = @At("TAIL"))
    private void hbm_m$impactSkyDarken(CallbackInfo ci) {
        Level self = (Level) (Object) this;
        if (!com.hbm_m.config.GeneralConfig.enableImpactWorldProvider) return; // Original: nur mit WorldProviderNTM
        if (self.dimension() != Level.OVERWORLD) return;
        float dust;
        try {
            dust = ImpactWorldHandler.getImpactDust(self);
        } catch (RuntimeException e) {
            return; // Weltspeicher noch nicht bereit (Konstruktor)
        }
        if (dust <= 0) return;
        double d0 = 1.0D - (double) (self.getRainLevel(1.0F) * 5.0F) / 16.0D;
        double d1 = 1.0D - (double) (self.getThunderLevel(1.0F) * 5.0F) / 16.0D;
        double d2 = 0.5D + 2.0D * Mth.clamp((double) Mth.cos(self.getTimeOfDay(1.0F) * ((float) Math.PI * 2F)), -0.25D, 0.25D);
        this.skyDarken = (int) ((1.0D - d2 * d0 * d1 * (1 - dust)) * 11.0D);
    }
}
