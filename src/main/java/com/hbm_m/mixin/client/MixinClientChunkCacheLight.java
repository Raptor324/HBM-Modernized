package com.hbm_m.mixin.client;

import com.hbm_m.client.render.LightSampleCache;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Event-driven light for the Nucleus instancing pipeline (CrankShaft
 * ClientChunkCacheMixin pattern): light updates land in a per-frame dirty-section
 * set (O(1)); the frame drain invalidates the light caches of the machines in
 * those sections and marks them render-dirty.
 */
@Mixin(ClientChunkCache.class)
public class MixinClientChunkCacheLight {

    @Inject(method = "onLightUpdate", at = @At("HEAD"))
    private void hbm_m$onLightUpdate(LightLayer layer, SectionPos pos, CallbackInfo ci) {
        LightSampleCache.onSectionLightUpdate(pos.asLong());
        com.hbm_m.client.render.retained.RetainedInstanceGraph.get().onSectionLightDirty(pos.asLong());
    }
}
