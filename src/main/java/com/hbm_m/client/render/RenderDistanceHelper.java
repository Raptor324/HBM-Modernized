package com.hbm_m.client.render;

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
 * <p>The fade zone spans the last 16 blocks (1 chunk) before the cutoff
 * distance. Within this zone, {@link #computeFade} returns a value in
 * {@code (0, 1]} that renderers multiply into their alpha / color to
 * smoothly dissolve the part instead of popping it out abruptly.
 */
public final class RenderDistanceHelper {

    private RenderDistanceHelper() {}

    private static final double FADE_ZONE_BLOCKS = 16.0;

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