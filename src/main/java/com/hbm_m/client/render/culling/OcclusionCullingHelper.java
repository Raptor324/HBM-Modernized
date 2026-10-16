package com.hbm_m.client.render.culling;


import com.hbm_m.main.MainRegistry;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

import com.hbm_m.client.render.ClientRenderFlags;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
/**
 * CPU-based occlusion culling helper for BlockEntityRenderers (BERs).
 * <p>
 * Implements an Amanatides-Woo fast voxel traversal (DDA ray-march) from the camera
 * position to the bounding box corners and face centers of multiblock machines.
 * <p>
 * Features:
 * <ul>
 *   <li>Intra-frame and cross-frame distance-based caching to avoid redundant raycasts on static scenes.</li>
 *   <li>Integration with the vanilla view frustum captured prior to the BER pass.</li>
 *   <li>Compatibility guards for Iris shadow passes, Create contraptions, and far sub-level structures.</li>
 *   <li>Preserved as a reliable fallback when GPU occlusion culling is unsupported or disabled.</li>
 * </ul>
 *
 * @credit John Amanatides / Andrew Woo
 */
@OnlyIn(Dist.CLIENT)
public final class OcclusionCullingHelper {

    /**
     * Ray-march result cache: intra-frame + short cross-frame reuse
     * (see {@link #CROSS_FRAME_TTL_TICKS} and camera movement threshold),
     * preventing static BERs from recalculating 15 rays every render frame.
     */
    private static final Long2ObjectOpenHashMap<CachedResult> occlusionCache = new Long2ObjectOpenHashMap<>();
    private static long currentFrame = 0;

    /** Camera movement exceeding this distance squared triggers occlusion recalculation. */
    private static final double CAMERA_REUSE_MAX_DIST_SQ = 0.25;

    /** Maximum world ticks between recalculations (guards against level geometry changes). */
    private static final int CROSS_FRAME_TTL_TICKS = 20;

    /** Purge cache entries beyond this Manhattan distance from the player (blocks). */
    private static final int MAX_KEEP_MANHATTAN_BLOCKS = 192;

    /** Hard cap on cache entry count. */
    private static final int MAX_CACHE_ENTRIES = 16384;

    /** Global counter incremented when client world geometry may have changed. */
    private static long clientGeometryStamp = 0L;

    /**
     * Vanilla frustum captured immediately prior to the BER loop ({@code RenderLevelStageEvent.AFTER_ENTITIES}).
     */
    @Nullable
    private static volatile Frustum blockEntityPassFrustum;

    @Nullable
    private static TagKey<Block> transparentBlocksTag = null;

    /**
     * Render-thread scratch: mutable BlockPos for ray-marching.
     */
    private static final BlockPos.MutableBlockPos RAY_MARCH_SCRATCH = new BlockPos.MutableBlockPos();

    private OcclusionCullingHelper() {}

    /**
     * Cache key: raw {@code pos.asLong()}.
     */
    private static long occlusionCacheKey(BlockPos pos) {
        return pos.asLong();
    }

    private static long stripShadowKeyBit(long key) {
        return key;
    }

    public static void setTransparentBlocksTag(TagKey<Block> tag) {
        transparentBlocksTag = tag;
    }

    /**
     * Invoked when client world geometry may have changed (chunk reloads, baked refresh, etc.).
     * Invalidates cross-frame occlusion reuse.
     */
    public static void onClientWorldGeometryMayHaveChanged() {
        clientGeometryStamp++;
    }

    /** Invoked from {@code RenderLevelStageEvent.Stage.AFTER_ENTITIES} prior to the BER loop. */
    public static void captureBlockEntityPassFrustum(@Nullable Frustum frustum) {
        blockEntityPassFrustum = frustum;
    }

    /**
     * Arms the fallback {@link CpuFrustumCuller}: extracts planes from world-space
     * {@code projection * view} so that tests against world-space AABBs
     * (as returned by {@link net.minecraft.world.level.block.entity.BlockEntity#getRenderBoundingBox()})
     * remain mathematically correct.
     */
    public static void captureCpuFrustumFallback(Matrix4f projection, Vec3 cameraPos) {
        if (projection == null) {
            CpuFrustumCuller.invalidate();
            return;
        }
        try {
            Matrix4f rot = new Matrix4f(RenderSystem.getModelViewMatrix());           // R_cam
            Matrix4f trans = new Matrix4f().setTranslation(
                    (float) -cameraPos.x, (float) -cameraPos.y, (float) -cameraPos.z);
            Matrix4f view = rot.mul(trans);                                          // R * T(-cam)
            Matrix4f viewProj = new Matrix4f(projection).mul(view);                  // P * V
            CpuFrustumCuller.updateFrustum(viewProj);
        } catch (Throwable err) {
            CpuFrustumCuller.invalidate();
        }
    }

    /** Same AABB test that vanilla performs for BlockEntities prior to {@code render}. */
    private static boolean aabbPassesBerPassFrustum(AABB renderBounds) {
        Frustum f = blockEntityPassFrustum;
        if (f == null) {
            Minecraft mc = Minecraft.getInstance();
            LevelRenderer lr = mc.levelRenderer;
            if (lr != null) {
                f = lr.getFrustum();
            }
        }
        if (f != null) {
            return f.isVisible(renderBounds);
        }
        return CpuFrustumCuller.isVisible(renderBounds);
    }

    /**
     * Occlusion test via DDA ray-march (center -> AABB corners -> face centers).
     * Does not evaluate vanilla frustum — caller performs frustum check first.
     */
    private static boolean legacyRaycastVisibility(Vec3 cameraPos, Level level, AABB renderBounds) {
        double centerX = (renderBounds.minX + renderBounds.maxX) * 0.5;
        double centerY = (renderBounds.minY + renderBounds.maxY) * 0.5;
        double centerZ = (renderBounds.minZ + renderBounds.maxZ) * 0.5;

        double dx = centerX - cameraPos.x;
        double dy = centerY - cameraPos.y;
        double dz = centerZ - cameraPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq < 16.0) {
            return true;
        }

        if (!isRayOccluded(cameraPos, centerX, centerY, centerZ, level, renderBounds)) {
            return true;
        }

        boolean visible =
                !isRayOccluded(cameraPos, renderBounds.minX, renderBounds.minY, renderBounds.minZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.maxX, renderBounds.minY, renderBounds.minZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.minX, renderBounds.maxY, renderBounds.minZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.maxX, renderBounds.maxY, renderBounds.minZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.minX, renderBounds.minY, renderBounds.maxZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.maxX, renderBounds.minY, renderBounds.maxZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.minX, renderBounds.maxY, renderBounds.maxZ, level, renderBounds) ||
                !isRayOccluded(cameraPos, renderBounds.maxX, renderBounds.maxY, renderBounds.maxZ, level, renderBounds);

        if (!visible) {
            visible =
                    !isRayOccluded(cameraPos, centerX, renderBounds.minY, centerZ, level, renderBounds) ||
                    !isRayOccluded(cameraPos, centerX, renderBounds.maxY, centerZ, level, renderBounds) ||
                    !isRayOccluded(cameraPos, renderBounds.minX, centerY, centerZ, level, renderBounds) ||
                    !isRayOccluded(cameraPos, renderBounds.maxX, centerY, centerZ, level, renderBounds) ||
                    !isRayOccluded(cameraPos, centerX, centerY, renderBounds.minZ, level, renderBounds) ||
                    !isRayOccluded(cameraPos, centerX, centerY, renderBounds.maxZ, level, renderBounds);
        }

        return visible;
    }

    private static final class CachedResult {
        boolean visible;
        long frame;
        long checkGameTime;
        double lastCamX;
        double lastCamY;
        double lastCamZ;
        long geometryStampAtCheck;
        String lastReason; // For diagnostics

        CachedResult(boolean visible, long frame, long checkGameTime,
                     double lastCamX, double lastCamY, double lastCamZ, long geometryStampAtCheck, String reason) {
            this.visible = visible;
            this.frame = frame;
            this.checkGameTime = checkGameTime;
            this.lastCamX = lastCamX;
            this.lastCamY = lastCamY;
            this.lastCamZ = lastCamZ;
            this.geometryStampAtCheck = geometryStampAtCheck;
            this.lastReason = reason;
        }

        void setAll(boolean visible, long frame, long checkGameTime,
                    double lastCamX, double lastCamY, double lastCamZ, long geometryStampAtCheck, String reason) {
            this.visible = visible;
            this.frame = frame;
            this.checkGameTime = checkGameTime;
            this.lastCamX = lastCamX;
            this.lastCamY = lastCamY;
            this.lastCamZ = lastCamZ;
            this.geometryStampAtCheck = geometryStampAtCheck;
            this.lastReason = reason;
        }
    }

    public static long occlusionKeyForBlock(BlockPos pos) {
        return occlusionCacheKey(pos);
    }

    /**
     * Determines whether rendering is currently executing for a Create contraption virtual world.
     * Delegates to {@link com.hbm_m.compat.ContraptionRenderCompat}.
     */
    private static boolean isContraptionRenderLevel(@Nullable Level level) {
        return level != null && com.hbm_m.compat.ContraptionRenderCompat.isContraptionRenderLevel(level);
    }

    /**
     * Overload for BER path: checks contraption rendering via {@code be.getLevel()}.
     */
    public static boolean shouldRender(@Nullable net.minecraft.world.level.block.entity.BlockEntity be, AABB renderBounds) {
        if (be == null) return true;
        return shouldRender(be.getBlockPos(), be.getLevel(), renderBounds);
    }

    public static boolean shouldRender(BlockPos pos, Level level, AABB renderBounds) {
        ModClothConfig.OcclusionCullingMode mode = ModClothConfig.get().getEffectiveOcclusionCullingMode();
        if (mode != ModClothConfig.OcclusionCullingMode.CPU) return true;

        // Iris shadow pass uses the light-space frustum, not the main camera frustum
        // captured in blockEntityPassFrustum. Culling here would drop off-screen casters
        // whose shadows are still visible on screen.
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return true;
        }

        // Create contraptions: bypass culling as the virtual world has distinct coordinates
        // and contraption subsystems manage their own visibility.
        if (isContraptionRenderLevel(level)) {
            return true;
        }

        // Aeronautics/Sable sub-level plot-grid check: far anomalous coordinates indicate sub-level rendering.
        if (com.hbm_m.compat.ContraptionRenderCompat.isFarFromCamera(pos)) {
            return true;
        }

        long posLong = occlusionCacheKey(pos);
        CachedResult cached = occlusionCache.get(posLong);

        var mc = Minecraft.getInstance();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        if (cached != null && cached.frame == currentFrame) {
            return cached.visible;
        }

        if (cached != null && canReuseCrossFrame(cached, level, cameraPos)) {
            cached.frame = currentFrame;
            logDecision(pos, cached.visible, "CROSS_FRAME_REUSE", cached);
            return cached.visible;
        }

        boolean frustum = aabbPassesBerPassFrustum(renderBounds);
        if (!frustum) {
            putCache(posLong, cached, false, cameraPos, level, "FRUSTUM_CULLED");
            return false;
        }

        boolean visible = legacyRaycastVisibility(cameraPos, level, renderBounds);
        putCache(posLong, cached, visible, cameraPos, level, "RAYCAST_OCCLUSION");
        return visible;
    }

    private static boolean canReuseCrossFrame(CachedResult c, Level level, Vec3 cameraPos) {
        if (!c.visible) return false;
        if (ClientRenderFlags.useInstancedBatching()) return false;
        if (level == null) return false;
        if (c.geometryStampAtCheck != clientGeometryStamp) return false;
        long nowTick = level.getGameTime();
        long age = nowTick - c.checkGameTime;
        if (age < 0L || age > (long) CROSS_FRAME_TTL_TICKS) return false;
        double ddx = cameraPos.x - c.lastCamX;
        double ddy = cameraPos.y - c.lastCamY;
        double ddz = cameraPos.z - c.lastCamZ;
        return ddx * ddx + ddy * ddy + ddz * ddz < CAMERA_REUSE_MAX_DIST_SQ;
    }

    private static void putCache(long key, @Nullable CachedResult existing, boolean visible,
                                 Vec3 cameraPos, Level level, String reason) {
        long tick = level == null ? 0L : level.getGameTime();
        if (existing == null) {
            occlusionCache.put(key, new CachedResult(visible, currentFrame, tick,
                    cameraPos.x, cameraPos.y, cameraPos.z, clientGeometryStamp, reason));
            logDecision(BlockPos.of(stripShadowKeyBit(key)), visible, reason, null);
        } else {
            logDecision(BlockPos.of(stripShadowKeyBit(key)), visible, reason, existing);
            existing.setAll(visible, currentFrame, tick,
                    cameraPos.x, cameraPos.y, cameraPos.z, clientGeometryStamp, reason);
        }
        trimCacheIfNeeded();
    }

    // DIAGNOSTICS: Logs to console only when visibility state or reason changes.
    private static void logDecision(BlockPos pos, boolean visible, String reason, CachedResult cached) {
        try {
            if (ModClothConfig.get().mdiDebugLogDispatch) {
                if (cached == null || cached.visible != visible || !reason.equals(cached.lastReason)) {
                    MainRegistry.LOGGER.info("[HBM-Cull] BE at {} visibility changed to {}. Reason: {}",
                            pos.toShortString(), visible, reason);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void trimCacheIfNeeded() {
        int size = occlusionCache.size();
        if (size <= MAX_CACHE_ENTRIES) return;
        int toRemove = size - MAX_CACHE_ENTRIES + (MAX_CACHE_ENTRIES >> 3);
        var it = occlusionCache.long2ObjectEntrySet().iterator();
        while (toRemove-- > 0 && it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    private static boolean isRayOccluded(Vec3 start, double endX, double endY, double endZ, Level level, AABB renderBounds) {
        double startX = start.x;
        double startY = start.y;
        double startZ = start.z;

        BlockPos.MutableBlockPos mutablePos = RAY_MARCH_SCRATCH;

        int currentX = Mth.floor(startX);
        int currentY = Mth.floor(startY);
        int currentZ = Mth.floor(startZ);

        int targetX = Mth.floor(endX);
        int targetY = Mth.floor(endY);
        int targetZ = Mth.floor(endZ);

        int stepX = Integer.signum(targetX - currentX);
        int stepY = Integer.signum(targetY - currentY);
        int stepZ = Integer.signum(targetZ - currentZ);

        if (stepX == 0 && stepY == 0 && stepZ == 0) return false;

        double dx = endX - startX;
        double dy = endY - startY;
        double dz = endZ - startZ;

        double deltaX = (stepX == 0) ? Double.MAX_VALUE : Math.abs(1.0 / dx);
        double deltaY = (stepY == 0) ? Double.MAX_VALUE : Math.abs(1.0 / dy);
        double deltaZ = (stepZ == 0) ? Double.MAX_VALUE : Math.abs(1.0 / dz);

        double maxX = (stepX == 0) ? Double.MAX_VALUE : (stepX > 0 ? (currentX + 1 - startX) * deltaX : (startX - currentX) * deltaX);
        double maxY = (stepY == 0) ? Double.MAX_VALUE : (stepY > 0 ? (currentY + 1 - startY) * deltaY : (startY - currentY) * deltaY);
        double maxZ = (stepZ == 0) ? Double.MAX_VALUE : (stepZ > 0 ? (currentZ + 1 - startZ) * deltaZ : (startZ - currentZ) * deltaZ);

        int maxSteps = 100;

        while (maxSteps-- > 0) {
            if (currentX == targetX && currentY == targetY && currentZ == targetZ) return false;

            if (currentX != Mth.floor(startX) || currentY != Mth.floor(startY) || currentZ != Mth.floor(startZ)) {
                mutablePos.set(currentX, currentY, currentZ);

                if (renderBounds != null &&
                        currentX + 1 > renderBounds.minX && currentX < renderBounds.maxX &&
                        currentY + 1 > renderBounds.minY && currentY < renderBounds.maxY &&
                        currentZ + 1 > renderBounds.minZ && currentZ < renderBounds.maxZ) {
                } else if (isOccluder(level, mutablePos)) {
                    return true;
                }
            }

            if (maxX < maxY) {
                if (maxX < maxZ) {
                    currentX += stepX;
                    maxX += deltaX;
                } else {
                    currentZ += stepZ;
                    maxZ += deltaZ;
                }
            } else {
                if (maxY < maxZ) {
                    currentY += stepY;
                    maxY += deltaY;
                } else {
                    currentZ += stepZ;
                    maxZ += deltaZ;
                }
            }
        }
        return false;
    }

    private static boolean isOccluder(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        if (transparentBlocksTag != null && state.is(transparentBlocksTag)) return false;
        return state.isSolidRender(level, pos);
    }

    public static void onFrameStart() {
        currentFrame++;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !occlusionCache.isEmpty()) {
            BlockPos pp = mc.player.blockPosition();
            int px = pp.getX();
            int py = pp.getY();
            int pz = pp.getZ();
            occlusionCache.long2ObjectEntrySet().removeIf(e -> {
                long raw = stripShadowKeyBit(e.getLongKey());
                // Manhattan distance straight from the packed key — BlockPos.of
                // here allocated a throwaway object per cached entry per frame.
                int dist = Math.abs(BlockPos.getX(raw) - px)
                        + Math.abs(BlockPos.getY(raw) - py)
                        + Math.abs(BlockPos.getZ(raw) - pz);
                return dist > MAX_KEEP_MANHATTAN_BLOCKS;
            });
        }

        if (currentFrame % 600L == 0L) {
            occlusionCache.long2ObjectEntrySet().removeIf(e -> currentFrame - e.getValue().frame > 600L);
        }
    }

    public static void clearCache() {
        occlusionCache.clear();
        InstancedRenderFrame.clear();
    }
}
