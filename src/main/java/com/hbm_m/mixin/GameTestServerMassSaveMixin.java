package com.hbm_m.mixin;

import com.hbm_m.util.ChunkSaveParallelizer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Runs the game-test server under {@link ChunkSaveParallelizer} mass-op mode for its whole
 * lifetime.
 *
 * <p>The gameTestServer world is disposable, yet vanilla saves it with full server-thread
 * serialization: completed batches release their chunk tickets, the unload waves hit
 * {@code ChunkMap.saveChunkIfNeeded}/{@code scheduleUnload}, and every save runs
 * {@code ChunkSerializer.write} right on the Server thread. JFR on the 1.21.1 suite measured
 * ~46% of the server thread inside those saves - the 300-test run took 20-30 minutes instead
 * of ~2. The existing MK5-crater parallelizer is exactly the right tool: it offloads the
 * serialization of non-ticking chunks (ticket level &gt; 31 - unloaded test leftovers always
 * qualify) to a worker pool and keeps vanilla unload bookkeeping intact, so the visible-chunk
 * map stays bounded and per-tick chunk maintenance stays cheap.
 *
 * <p>The flag is never released: a game-test server only ever runs tests. The parallelizer
 * kill-switch ({@code -Dhbm.parallelChunkSave=false}) still wins.
 */
@Mixin(net.minecraft.gametest.framework.GameTestServer.class)
public abstract class GameTestServerMassSaveMixin {

    @Inject(method = "initServer", at = @At("TAIL"))
    private void hbm$parallelizeGameTestWorldSaves(CallbackInfoReturnable<Boolean> cir) {
        ChunkSaveParallelizer.acquireMassOp();
    }
}
