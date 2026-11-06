package com.hbm_m.mixin.client;

import com.hbm_m.client.render.NucleusDispatcherBypass;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Transactional presence for the Nucleus dispatcher bypass: machine broken /
 * chunk unloaded → enqueue remove (drained at the start of collectMain; a
 * removed visual can render one extra frame - accepted, same as CrankShaft).
 */
@Mixin(BlockEntity.class)
public class MixinBlockEntitySetRemoved {

    @Inject(method = "setRemoved", at = @At("TAIL"))
    private void hbm_m$onSetRemoved(CallbackInfo ci) {
        NucleusDispatcherBypass.onMachineRemoved((BlockEntity) (Object) this);
    }
}
