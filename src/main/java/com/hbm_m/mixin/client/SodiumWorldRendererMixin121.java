package com.hbm_m.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.hbm_m.client.render.NucleusDispatcherBypass;
import com.hbm_m.client.render.NucleusTrackedBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.1 twin of {@link SodiumWorldRendererMixin}: the Sodium target package
 * moved between versions (Embeddium 1.20.1: {@code me.jellysquid.mods.sodium.*},
 * Sodium 1.21.1: {@code net.caffeinemc.mods.sodium.*}), so the 1.20.1 mixin
 * never applies on 1.21.1 and the pass-aware main-pass dedup / teardown-ghost
 * veto was silently missing there.
 * <p>
 * Target (Sodium 1.21.1/stable,
 * {@code common/src/main/java/net/caffeinemc/mods/sodium/client/render/SodiumWorldRenderer.java:398}):
 * <pre>
 * private static void renderBlockEntity(PoseStack, RenderBuffers,
 *         Long2ObjectMap&lt;SortedSet&lt;BlockDestructionProgress&gt;&gt;, float,
 *         MultiBufferSource.BufferSource, double x, y, z,
 *         BlockEntityRenderDispatcher, BlockEntity, LocalPlayer, LocalBooleanRef)
 * </pre>
 * The trailing {@code LocalPlayer} and {@code LocalBooleanRef} (a MixinExtras
 * sugar ref, NOT a Sodium type, but kept out of our signature anyway) are
 * omitted via Mixin's trailing-argument prefix capture - the handler declares
 * a strict prefix of the target parameters, which keeps this class compilable
 * without any Sodium compile-time dependency.
 * <p>
 * The name {@code renderBlockEntity} (singular) is unique in the 1.21.1 class -
 * the instance overloads are all plural ({@code renderBlockEntities}) - so
 * by-name matching cannot mis-target.
 * <p>
 * Pass-aware policy - identical to the 1.20.1 handler:
 * <ul>
 *   <li>ALWAYS cancel removed / demolished machines in BOTH passes - Sodium's
 *       compiled section keeps listing a BE for a frame or two after removal,
 *       which used to flash a ghost shadow once the registered flag cleared.</li>
 *   <li>Shadow pass (Iris): never cancel - although on 1.21.1 machines reach
 *       the shadow pass through the custom BE function installed by
 *       {@link com.hbm_m.client.render.IrisShadowBeFunction121} rather than
 *       through these lists, the veto stays symmetric with 1.20.1.</li>
 *   <li>Main pass: cancel only when the Nucleus dispatcher bypass is active
 *       AND the BE is a registered Nucleus machine (already drawn by
 *       {@code NucleusDispatcherBypass.collectMain}).</li>
 * </ul>
 * {@code @Pseudo} + {@code require = 0}: without Sodium (or on 1.20.1, where
 * this package does not exist) the mixin silently does not apply.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer", remap = false)
public class SodiumWorldRendererMixin121 {

    @Inject(method = "renderBlockEntity", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void hbm$cancelNucleusBlockEntityEarly(
            PoseStack matrices,
            RenderBuffers renderBuffers,
            Long2ObjectMap<?> damageProgress,
            float tickDelta,
            MultiBufferSource.BufferSource immediate,
            double x, double y, double z,
            BlockEntityRenderDispatcher dispatcher,
            BlockEntity blockEntity,
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
        // Shadow pass: fall through without cancelling (see class javadoc).
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
