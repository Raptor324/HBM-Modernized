package com.hbm_m.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLEnvironment;

// Nur in der Dev-Umgebung: zeigt für ALLE Spieler den eigenen Skin aus
// assets/hbm_m/textures/entity/dev_skin.png statt des Standard-Steve/Alex-Skins.
@Mixin(AbstractClientPlayer.class)
public class MixinAbstractClientPlayerSkin {

    private static final ResourceLocation HBM_M$DEV_SKIN =
            new ResourceLocation("hbm_m", "textures/entity/dev_skin.png");

    @Inject(method = "getSkinTextureLocation", at = @At("RETURN"), cancellable = true)
    private void hbm_m$overrideDevSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        if (FMLEnvironment.production) {
            return;
        }
        cir.setReturnValue(HBM_M$DEV_SKIN);
    }
}
