package com.hbm_m.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hbm_m.handler.ImpactWorldHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.level.Level;

/** Original {@code WorldProviderNTM.calcSunriseSunsetColors}: Morgen-/Abendrot mal {@code (1 - dust)}. */
@Mixin(DimensionSpecialEffects.class)
public abstract class DimensionSpecialEffectsImpactMixin {

    @Inject(method = "getSunriseColor(FF)[F", at = @At("RETURN"), cancellable = true, require = 0)
    private void hbm_m$sunrise(float timeOfDay, float partialTick, CallbackInfoReturnable<float[]> cir) {
        float[] col = cir.getReturnValue();
        if (col == null) return;
        Level level = Minecraft.getInstance().level;
        if (!com.hbm_m.config.GeneralConfig.enableImpactWorldProvider) return; // Original: nur mit WorldProviderNTM
        if (level == null || level.dimension() != Level.OVERWORLD) return;
        float dust = ImpactWorldHandler.getDustForClient(level);
        if (dust <= 0) return;
        float[] out = new float[] { col[0] * (1 - dust), col[1] * (1 - dust), col[2] * (1 - dust), col[3] * (1 - dust) };
        cir.setReturnValue(out);
    }
}
