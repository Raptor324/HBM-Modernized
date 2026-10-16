package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import com.hbm_m.compat.ContraptionRenderCompat;
import com.hbm_m.config.ModClothConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Shared utility for distance-based LOD and fade-out calculations used by
 * all BER renderers. Centralizes the distance check and fade factor math
 * so every renderer behaves identically.
 *
 * <p>The fade zone spans the tail of the cutoff distance — a fraction of it
 * ({@link #FADE_ZONE_FRACTION}), not a fixed block count. Within this zone,
 * {@link #computeFade} returns a value in {@code (0, 1]} that renderers
 * multiply into their alpha / color to smoothly dissolve the part instead of
 * popping it out abruptly.
 */

@OnlyIn(Dist.CLIENT)
public final class RenderDistanceHelper {

    private RenderDistanceHelper() {}

    /**
     * Fade zone width: a fraction of the cutoff distance, clamped to a sane
     * maximum. A fixed 16 blocks read as instant disappearance: at flight speed
     * a machine crosses them in a fraction of a second while the fade ring stays
     * put - rows of farms "fly through" it, which looks like a pop cutoff.
     * A proportion gives a long visible dissolution at any distance
     * (128 blocks -> ~38 block zone, 256 -> 64).
     */
    private static final double FADE_ZONE_FRACTION = 0.3;
    private static final double FADE_ZONE_MIN_BLOCKS = 16.0;
    private static final double FADE_ZONE_MAX_BLOCKS = 64.0;

    private static double fadeZoneBlocks(double maxBlocks) {
        double zone = maxBlocks * FADE_ZONE_FRACTION;
        return Math.max(FADE_ZONE_MIN_BLOCKS, Math.min(zone, FADE_ZONE_MAX_BLOCKS));
    }

    /**
     * Within this many blocks of {@link #getStaticDistanceBlocks()}, 8-corner spatial
     * light sampling runs; farther machines use flat vanilla packed light (farm LOD).
     */
    private static final double LIGHT_CORNER_DETAIL_MARGIN_BLOCKS = 48.0;

    /**
     * Computes the squared distance from the camera to the block center.
     */
    public static double distanceSqToCamera(BlockPos blockPos) {
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cam = camera.getPosition();
        double dx = blockPos.getX() + 0.5 - cam.x;
        double dy = blockPos.getY() + 0.5 - cam.y;
        double dz = blockPos.getZ() + 0.5 - cam.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Returns the maximum render distance for animated/dynamic parts in blocks.
     */
    public static double getAnimatedDistanceBlocks() {
        return ModClothConfig.get().modelUpdateDistance * 16.0;
    }

    /**
     * Returns the maximum render distance for static parts in blocks.
     */
    public static double getStaticDistanceBlocks() {
        return ModClothConfig.get().modelStaticRenderDistance * 16.0;
    }

    /**
     * Whether the animated parts should be completely skipped (beyond cutoff).
     */
    public static boolean shouldSkipAnimation(BlockPos blockPos) {
        double maxDist = getAnimatedDistanceBlocks();
        return distanceSqToCamera(blockPos) > maxDist * maxDist;
    }

    /**
     * Safely checks if animation should be skipped, bypassing the check on Create contraptions.
     */
    public static boolean shouldSkipAnimation(BlockEntity blockEntity) {
        if (ContraptionRenderCompat.isContraptionRender(blockEntity)) return false;
        return shouldSkipAnimation(blockEntity.getBlockPos());
    }

    /**
     * Computes a fade factor for animated parts at the given position.
     */
    public static float computeAnimatedFade(BlockPos blockPos) {
        return computeFade(blockPos, getAnimatedDistanceBlocks());
    }

    /**
     * Safely computes animated fade factor, bypassing fading entirely on Create contraptions.
     */
    public static float computeAnimatedFade(BlockEntity blockEntity) {
        if (ContraptionRenderCompat.isContraptionRender(blockEntity)) return 1.0f;
        return computeAnimatedFade(blockEntity.getBlockPos());
    }

    /** Variant taking a precomputed distance - no repeated camera fetch (Nucleus hot path). */
    public static float computeAnimatedFade(BlockEntity blockEntity, double distSq) {
        if (ContraptionRenderCompat.isContraptionRender(blockEntity)) return 1.0f;
        return computeFade(blockEntity.getBlockPos(), getAnimatedDistanceBlocks(), distSq);
    }

    /**
     * Computes a fade factor for static parts at the given position.
     */
    public static float computeStaticFade(BlockPos blockPos) {
        return computeFade(blockPos, getStaticDistanceBlocks());
    }

    /**
     * Safely computes static fade factor, bypassing fading entirely on Create contraptions.
     */
    public static float computeStaticFade(BlockEntity blockEntity) {
        if (ContraptionRenderCompat.isContraptionRender(blockEntity)) return 1.0f;
        return computeStaticFade(blockEntity.getBlockPos());
    }

    /** Variant taking a precomputed distance - no repeated camera fetch (Nucleus hot path). */
    public static float computeStaticFade(BlockEntity blockEntity, double distSq) {
        if (ContraptionRenderCompat.isContraptionRender(blockEntity)) return 1.0f;
        return computeFade(blockEntity.getBlockPos(), getStaticDistanceBlocks(), distSq);
    }

    /**
     * Core fade calculation.
     *
     * @param blockPos  block to measure distance to
     * @param maxBlocks cutoff distance in blocks
     * @return fade factor in [0, 1], or -1 if fully beyond cutoff
     */
    public static float computeFade(BlockPos blockPos, double maxBlocks) {
        if (maxBlocks <= 0) return -1f;
        return computeFade(blockPos, maxBlocks, distanceSqToCamera(blockPos));
    }

    /**
     * Core fade calculation with a precomputed distance - does not read the camera itself.
     */
    public static float computeFade(BlockPos blockPos, double maxBlocks, double distSq) {
        if (maxBlocks <= 0) return -1f;
        double maxSq = maxBlocks * maxBlocks;
        if (distSq > maxSq) return -1f;

        double zone = fadeZoneBlocks(maxBlocks);
        double fadeStartBlocks = Math.max(0, maxBlocks - zone);
        double fadeStartSq = fadeStartBlocks * fadeStartBlocks;
        if (distSq <= fadeStartSq) return 1.0f;

        double dist = Math.sqrt(distSq);
        float t = (float) ((maxBlocks - dist) / zone);
        return Math.max(0f, Math.min(1f, t));
    }

    // ── GPU fade ramp (camera-independent records) ─────────────────────
    // The instance record stores a static fade MODE (InstancedStaticPartRenderer
    // .FADE_MODE_*), not a per-frame alpha: an alpha derived from the camera
    // position changes every frame of motion and the span-diff never converges.
    // The ramp is evaluated on the GPU per instance (vertex shader) from the
    // view-space distance; the CPU mirrors the exact same formula here for the
    // opaque/fading membership split (the two must stay in sync).

    /** Linear ramp, must match fadeRamp() in block_lit_instanced.vsh. */
    private static float evalRamp(double distSq, double maxBlocks) {
        if (maxBlocks <= 0) return 1.0f;
        double maxSq = maxBlocks * maxBlocks;
        if (distSq >= maxSq) return 0.0f;
        double zone = fadeZoneBlocks(maxBlocks);
        double fadeStartBlocks = Math.max(0, maxBlocks - zone);
        if (distSq <= fadeStartBlocks * fadeStartBlocks) return 1.0f;
        double dist = Math.sqrt(distSq);
        float t = (float) ((maxBlocks - dist) / zone);
        return Math.max(0f, Math.min(1f, t));
    }

    /** Static-zone ramp for the membership split. Mirrors the vertex shader. */
    public static float evalStaticFadeAlpha(double distSq) {
        return evalRamp(distSq, getStaticDistanceBlocks());
    }

    /**
     * Animation-zone ramp for the membership split. CONTINUOUS with the
     * animated-part skip: past the anim cutoff the ramp returns 0 (the record
     * renders invisible), matching MachineBer.renderAll which does not record
     * animated parts beyond the cutoff. A part origin offset from the machine
     * anchor can carry a record while the anchor is inside the cutoff - the
     * former "1.0 past the end" branch rendered it frozen-opaque (opaque flash
     * on approach, then the ramp). Must mirror gpuFadeAlpha in
     * block_lit_instanced.vsh. Disabled zone (maxBlocks &lt;= 0) - always 1.
     */
    public static float evalAnimFadeAlpha(double distSq) {
        double maxBlocks = getAnimatedDistanceBlocks();
        if (maxBlocks <= 0) return 1.0f;
        if (distSq >= maxBlocks * maxBlocks) return 0.0f;
        return evalRamp(distSq, maxBlocks);
    }

    /**
     * Per-instance alpha from the stored fade mode and the instance-origin
     * distance squared (anchor-relative pos vs {@code FrameViewState.relCam} -
     * the same distance the vertex shader computes in view space).
     */
    public static float evalInstanceAlpha(double distSq, float fadeMode) {
        // R4-2.3: FADE_MODE_INVISIBLE (-1) records are clipped out entirely by the
        // vertex shader (freed slots, conditionally hidden parts). They must not
        // enter the fading membership - classifying them as fading made every
        // frame with freed slots run the opaque/fading partition (which also
        // permutes the retained buffer) and deferred them to the phase-2 draw.
        if (fadeMode < 0.0f) {
            return 1.0f;
        }
        if (fadeMode >= com.hbm_m.client.render.InstancedStaticPartRenderer.FADE_MODE_NONE_DIVIDER) {
            return 1.0f;
        }
        float staticAlpha = evalStaticFadeAlpha(distSq);
        if (fadeMode >= com.hbm_m.client.render.InstancedStaticPartRenderer.FADE_MODE_MIN_ANIM_DIVIDER) {
            return staticAlpha;
        }
        float animAlpha = evalAnimFadeAlpha(distSq);
        return Math.min(staticAlpha, animAlpha);
    }

    /** Ramp start (blocks) for the static zone, for the vertex shader uniform. Disabled zone - huge (ramp pinned to 1). */
    public static float gpuStaticFadeStart() {
        double m = getStaticDistanceBlocks();
        if (m <= 0) return GPU_FADE_DISABLED;
        return (float) Math.max(0, m - fadeZoneBlocks(m));
    }

    /** Ramp end (blocks) for the static zone. */
    public static float gpuStaticFadeEnd() {
        double m = getStaticDistanceBlocks();
        return m <= 0 ? GPU_FADE_DISABLED + 1.0f : (float) m;
    }

    /** Ramp start (blocks) for the animation zone. Disabled - huge. */
    public static float gpuAnimFadeStart() {
        double m = getAnimatedDistanceBlocks();
        if (m <= 0) return GPU_FADE_DISABLED;
        return (float) Math.max(0, m - fadeZoneBlocks(m));
    }

    /** Ramp end (blocks) for the animation zone. */
    public static float gpuAnimFadeEnd() {
        double m = getAnimatedDistanceBlocks();
        return m <= 0 ? GPU_FADE_DISABLED + 1.0f : (float) m;
    }

    /** Ramp start sentinel: every real distance is below it, so the ramp stays at 1. */
    public static final float GPU_FADE_DISABLED = 1.0e9f;

    /**
     * Safely computes fade factor, bypassing fading entirely on Create contraptions.
     */
    public static float computeFade(BlockEntity blockEntity, double maxBlocks) {
        if (ContraptionRenderCompat.isContraptionRender(blockEntity)) return 1.0f;
        return computeFade(blockEntity.getBlockPos(), maxBlocks);
    }

    /**
     * Converts a BER view distance config (in chunks) to blocks, matching
     * {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer#getViewDistance()}.
     */
    public static int getStaticViewDistanceBlocks() {
        return ModClothConfig.get().modelStaticRenderDistance * 16;
    }

    /**
     * Squared camera distance threshold for full 8-corner {@link LightSampleCache} sampling.
     * Beyond this, callers should use uniform corner UV from vanilla packed light.
     */
    public static double getLightCornerDetailDistanceSq() {
        double maxBlocks = getStaticDistanceBlocks();
        double detailBlocks = Math.max(0, maxBlocks - LIGHT_CORNER_DETAIL_MARGIN_BLOCKS);
        return detailBlocks * detailBlocks;
    }
}