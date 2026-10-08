package com.hbm_m.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hbm_m.handler.ImpactWorldHandler;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Original {@code WorldProviderNTM} (Oberwelt) auf der Clientseite: Sonnenhelligkeit, Sterne, Himmels- und
 * Wolkenfarbe werden mit dem synchronisierten Tom-Staub/-Feuer ({@link ImpactWorldHandler}) verrechnet.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelImpactMixin {

    @Unique
    private boolean hbm_m$overworld() {
        // Original: WorldProviderNTM wird nur mit 1.32_enableImpactWorldProvider fuer Dimension 0 registriert
        return com.hbm_m.config.GeneralConfig.enableImpactWorldProvider && ((Level) (Object) this).dimension() == Level.OVERWORLD;
    }

    /** Original {@code getSunBrightness}: {@code (sunBr * 0.8F + 0.2F) * (1 - dust)}. */
    @Inject(method = "getSkyDarken(F)F", at = @At("RETURN"), cancellable = true, require = 0)
    private void hbm_m$sunBrightness(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (!hbm_m$overworld()) return;
        float dust = ImpactWorldHandler.getDustForClient((Level) (Object) this);
        if (dust > 0) cir.setReturnValue(cir.getReturnValue() * (1 - dust));
    }

    /** Original {@code getStarBrightness}: {@code starBr * (1 - dust)}. */
    @Inject(method = "getStarBrightness(F)F", at = @At("RETURN"), cancellable = true, require = 0)
    private void hbm_m$starBrightness(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (!hbm_m$overworld()) return;
        float dust = ImpactWorldHandler.getDustForClient((Level) (Object) this);
        if (dust > 0) cir.setReturnValue(cir.getReturnValue() * (1 - dust));
    }

    /** Original {@code getSkyColor}. */
    @Inject(method = "getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true, require = 0)
    private void hbm_m$skyColor(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (!hbm_m$overworld()) return;
        Level self = (Level) (Object) this;
        float dust = ImpactWorldHandler.getDustForClient(self);
        float fire = ImpactWorldHandler.getFireForClient(self);
        if (dust <= 0 && fire <= 0) return;
        Vec3 sky = cir.getReturnValue();

        float f4;
        float f5;
        float f6;

        if (fire > 0) {
            f4 = (float) (sky.x * 1.3f);
            f5 = (float) sky.y * ((Math.max((1 - (dust * 1.4f)), 0)));
            f6 = (float) sky.z * ((Math.max((1 - (dust * 4)), 0)));
        } else {
            f4 = (float) sky.x;
            f5 = (float) sky.y * (1 - (dust * 0.5F));
            f6 = (float) sky.z * (1 - dust);
        }

        cir.setReturnValue(new Vec3((double) f4 * (fire + (1 - dust)), (double) f5 * (fire + (1 - dust)), (double) f6 * (fire + (1 - dust))));
    }

    /** Original {@code drawClouds}: Wolkenfarbe mal {@code (1 - dust)}. */
    @Inject(method = "getCloudColor(F)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true, require = 0)
    private void hbm_m$cloudColor(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (!hbm_m$overworld()) return;
        float dust = ImpactWorldHandler.getDustForClient((Level) (Object) this);
        if (dust > 0) cir.setReturnValue(cir.getReturnValue().scale(1 - dust));
    }
}
