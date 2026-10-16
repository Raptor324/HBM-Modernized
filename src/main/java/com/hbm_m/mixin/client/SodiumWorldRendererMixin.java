package com.hbm_m.mixin.client;

import com.hbm_m.client.render.NucleusDispatcherBypass;
import com.hbm_m.client.render.NucleusTrackedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Early cancellation on SodiumWorldRenderer.renderBlockEntity (Embeddium /
 * Sodium terrain renderer, private static overload shared by the culled and
 * global BE loops) for Nucleus-managed BlockEntities.
 * <p>
 * Pass-aware policy (shadow-fix 2026-10-15):
 * <ul>
 *   <li>ALWAYS cancel removed / demolished machines in BOTH passes - Embeddium's
 *       compiled section keeps listing a BE for a frame or two after removal,
 *       which used to flash a ghost shadow (shadow pass) once the registered
 *       flag cleared.</li>
 *   <li>Shadow pass (Iris/Oculus): NEVER cancel registered machines - they must
 *       reach the shadow dispatcher through Embeddium's lists to cast shadows.
 *       The old unconditional cancel was the root cause of missing machine
 *       shadows (the main pass had registered every machine, so the shadow
 *       pass never reached the dispatcher).</li>
 *   <li>Main pass: cancel only when the Nucleus dispatcher bypass is actually
 *       enabled AND the BE is a registered Nucleus machine (already drawn by
 *       {@code NucleusDispatcherBypass.collectMain}).</li>
 * </ul>
 * {@code @Pseudo} + {@code require = 0}: without a terrain mod the mixin
 * silently does not apply.
 */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer", remap = false)
public class SodiumWorldRendererMixin {

    @Inject(method = "renderBlockEntity", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void hbm$cancelNucleusBlockEntityEarly(
            com.mojang.blaze3d.vertex.PoseStack matrices,
            net.minecraft.client.renderer.RenderBuffers renderBuffers,
            it.unimi.dsi.fastutil.longs.Long2ObjectMap<?> damageProgress,
            float tickDelta,
            net.minecraft.client.renderer.MultiBufferSource.BufferSource immediate,
            double x, double y, double z,
            net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher dispatcher,
            BlockEntity blockEntity,
            CallbackInfo ci) {
        if (blockEntity == null) {
            return;
        }
        // Teardown ghost: Embeddium's compiled section can still list a BE for
        // 1-2 frames after removal/demolition - never let it render.
        if (blockEntity.isRemoved()
                || NucleusDispatcherBypass.isDemolished(blockEntity.getBlockPos().asLong())) {
            ci.cancel();
            return;
        }
        // Shadow pass: machines must reach Iris'/Embeddium's shadow dispatcher
        // to cast shadows - fall through without cancelling.
        if (com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return;
        }
        // Main pass: bypass only when it is actually active and this BE was
        // already drawn by collectMain (registered presence flag).
        if (blockEntity instanceof NucleusTrackedBlockEntity tracked
                && tracked.hbm_m$isNucleusRegistered()
                && NucleusDispatcherBypass.isBypassActive()) {
            ci.cancel();
        }
    }
}
