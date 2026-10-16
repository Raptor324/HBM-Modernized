package com.hbm_m.mixin;

import com.hbm_m.entity.mob.EntityCreeperNuclear;
import com.hbm_m.entity.mob.EntityCreeperGold;
import com.hbm_m.entity.mob.EntityCreeperPhosgene;
import com.hbm_m.entity.mob.EntityCreeperTainted;
import com.hbm_m.entity.mob.EntityCreeperVolatile;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Перехват ванильного взрыва крипера для HBM-вариантов
 * ({@code explodeCreeper()} private, без cross-module invokespecial).
 */
@Mixin(Creeper.class)
public class CreeperMixin {

    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void hbm_m$explodeCreeper(CallbackInfo ci) {
        if ((Object) this instanceof EntityCreeperTainted tainted) {
            tainted.taintedExplode();
            ci.cancel();
        } else if ((Object) this instanceof EntityCreeperVolatile volatileCreeper) {
            volatileCreeper.volatileExplode();
            ci.cancel();
        } else if ((Object) this instanceof EntityCreeperPhosgene phosgene) {
            phosgene.phosgeneExplode();
            ci.cancel();
        } else if ((Object) this instanceof EntityCreeperGold goldCreeper) {
            goldCreeper.goldExplode();
            ci.cancel();
        }
    }
}
