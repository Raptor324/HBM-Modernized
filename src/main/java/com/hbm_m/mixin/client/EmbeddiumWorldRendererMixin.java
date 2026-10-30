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
 * Same early-cancellation policy as {@link SodiumWorldRendererMixin}, but for
 * <b>Embeddium 1.21.1</b> (NeoForge): Embeddium 1.0.x renamed the renderer to
 * {@code org.embeddedt.embeddium.impl.render.EmbeddiumWorldRenderer} (the old
 * {@code me.jellysquid.mods.sodium} package is gone entirely on this version),
 * while the {@code renderBlockEntity} private static signature stayed the
 * 10-parameter shape it had in 1.20.1. Sodium 0.6+ (native) changed the
 * signature (extra {@code LocalPlayer}/{@code LocalBooleanRef}) and is covered
 * separately by {@link CaffeineSodiumWorldRendererMixin}.
 * <p>
 * {@code @Pseudo} + {@code require = 0}: without Embeddium 1.21.1 (e.g. on
 * 1.20.1, or with native Sodium installed instead) the mixin silently does
 * not apply.
 */
@Pseudo
@Mixin(targets = "org.embeddedt.embeddium.impl.render.EmbeddiumWorldRenderer", remap = false)
public class EmbeddiumWorldRendererMixin {

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
        // Shadow pass: machines must reach the shadow dispatcher
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
