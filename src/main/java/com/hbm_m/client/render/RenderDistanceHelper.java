package com.hbm_m.client.render;


import com.hbm_m.config.ModClothConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Shared utility for distance-based LOD and fade-out calculations used by
 * all BER renderers. Centralizes the distance check and fade factor math
 * so every renderer behaves identically.
 *
 * <p>The fade zone spans the last 16 blocks (1 chunk) before the cutoff
 * distance. Within this zone, {@link #computeFade} returns a value in
 * {@code (0, 1]} that renderers multiply into their alpha / color to
 * smoothly dissolve the part instead of popping it out abruptly.
 */
public final class RenderDistanceHelper {

    private RenderDistanceHelper() {}

    private static final double FADE_ZONE_BLOCKS = 16.0;

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
     * Computes a fade factor for animated parts at the given position.
     *
     * @return 1.0 when fully visible, 0.0 when at/beyond cutoff, smooth
     *         transition in the last {@link #FADE_ZONE_BLOCKS} before cutoff.
     *         Returns -1 if beyond cutoff (caller should skip rendering entirely).
     */
    public static float computeAnimatedFade(BlockPos blockPos) {
        return computeFade(blockPos, getAnimatedDistanceBlocks());
    }

    /**
     * Computes a fade factor for static parts at the given position.
     *
     * @return 1.0 when fully visible, 0.0 when at/beyond cutoff, smooth
     *         transition in the last {@link #FADE_ZONE_BLOCKS} before cutoff.
     *         Returns -1 if beyond cutoff (caller should skip rendering entirely).
     */
    public static float computeStaticFade(BlockPos blockPos) {
        return computeFade(blockPos, getStaticDistanceBlocks());
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
        double distSq = distanceSqToCamera(blockPos);
        double maxSq = maxBlocks * maxBlocks;
        if (distSq > maxSq) return -1f;

        double fadeStartBlocks = Math.max(0, maxBlocks - FADE_ZONE_BLOCKS);
        double fadeStartSq = fadeStartBlocks * fadeStartBlocks;
        if (distSq <= fadeStartSq) return 1.0f;

        double dist = Math.sqrt(distSq);
        float t = (float) ((maxBlocks - dist) / FADE_ZONE_BLOCKS);
        return Math.max(0f, Math.min(1f, t));
    }

    /**
     * Converts a BER view distance config (in chunks) to blocks, matching
     * {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer#getViewDistance()}.
     */
    public static int getStaticViewDistanceBlocks() {
        return ModClothConfig.get().modelStaticRenderDistance * 16;
    }
}
