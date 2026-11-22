package com.hbm_m.mixin.client;

import com.hbm_m.client.render.retained.RetainedInstanceGraph;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Connects ClientLevel chunk loading and unloading to the RetainedInstanceGraph.
 * Dispatches async tasks for chunk load/unload to populate and evict retained instances.
 */
@Mixin(ClientLevel.class)
public class MixinClientLevelChunkLifecycle {

    @Inject(method = "onChunkLoaded", at = @At("TAIL"))
    private void hbm_m$onChunkLoaded(ChunkPos chunkPos, CallbackInfo ci) {
        ClientLevel level = (ClientLevel) (Object) this;
        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        if (chunk != null) {
            RetainedInstanceGraph rig = RetainedInstanceGraph.get();
            if (rig != null) {
                rig.onChunkLoadedAsync(chunk);
            }
        }
    }

    @Inject(method = "unload", at = @At("HEAD"))
    private void hbm_m$onChunkUnload(LevelChunk chunk, CallbackInfo ci) {
        if (chunk != null) {
            RetainedInstanceGraph rig = RetainedInstanceGraph.get();
            if (rig != null) {
                rig.onChunkUnloadedAsync(chunk.getPos().toLong());
            }
        }
    }
}
