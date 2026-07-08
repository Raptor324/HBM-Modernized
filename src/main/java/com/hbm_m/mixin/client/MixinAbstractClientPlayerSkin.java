package com.hbm_m.mixin.client;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.player.AbstractClientPlayer;

// Nur in der Dev-Umgebung: zeigt für ALLE Spieler den eigenen Skin aus
// assets/hbm_m/textures/entity/dev_skin.png statt des Standard-Steve/Alex-Skins.
// Закомментировал чтобы случайно не папало в релизную версию
@Mixin(AbstractClientPlayer.class)
public class MixinAbstractClientPlayerSkin {

    // private static final ResourceLocation HBM_M$DEV_SKIN =
    //         new ResourceLocation("hbm_m", "textures/entity/dev_skin.png");

    // @Inject(method = "getSkinTextureLocation", at = @At("RETURN"), cancellable = true)
    // private void hbm_m$overrideDevSkin(CallbackInfoReturnable<ResourceLocation> cir) {
    //     if (FMLEnvironment.production) {
    //         return;
    //     }
    //     cir.setReturnValue(HBM_M$DEV_SKIN);
    // }

}
