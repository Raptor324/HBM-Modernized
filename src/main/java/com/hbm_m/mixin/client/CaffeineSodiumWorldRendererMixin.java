package com.hbm_m.mixin.client;

import com.hbm_m.client.render.NucleusDispatcherBypass;
import com.hbm_m.client.render.NucleusTrackedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sodium 0.6+ (native, 1.21.1 NeoForge, package {@code net.caffeinemc.mods.sodium})
 * counterpart of {@link SodiumWorldRendererMixin}, with the identical
 * early-cancellation policy: Sodium 0.6+ kept
 * {@code SodiumWorldRenderer#renderBlockEntity} but extended its signature with
 * {@code LocalPlayer} and a Sodium-internal {@code LocalBooleanRef} - types we
 * cannot reference on the compile classpath. The 12th parameter is declared as
 * {@code @Coerce Object}: SpongePowered Mixin accepts the supertype for the
 * Sodium-internal argument, no Sodium classes needed at compile time (MixinExtras
 * {@code @Local} would work too, but it is not on the compile classpath here).
 * <p>
 * Cancelling at HEAD skips Sodium's {@code pushPose}/{@code translate},
 * the {@code damageProgress} lookup and the dispatcher call in one step - a
 * redirect at the tail would double-run {@code shouldBypass} with
 * {@link MixinBlockEntityRenderDispatcher} and risk ghost shadows for machines
 * already unregistered by a demolition.
 * <p>
 * {@code @Pseudo} + {@code require = 0}: without native Sodium (or with a
 * signature drift in a future minor) the mixin silently does not apply.
 * {@code remap = false}: Sodium is not obfuscated.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer", remap = false)
public class CaffeineSodiumWorldRendererMixin {

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
            net.minecraft.client.player.LocalPlayer player,
            @Coerce Object isGlowing,
            CallbackInfo ci) {
        if (blockEntity == null) {
            return;
        }
        // Teardown ghost: Sodium's compiled section can still list a BE for
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
