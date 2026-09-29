package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.main.MainRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Deferred chunk invalidation for doors.
 * Baked models (Iris/Oculus) do not update after the first opening without explicit
 * invalidation - Sodium/Embeddium caches chunks. Call processPendingInvalidations
 * from ClientTickEvent.END. Deduplication: the same position is not re-added
 * while still pending.
 */
@OnlyIn(Dist.CLIENT)
public class DoorChunkInvalidationHelper {

    private static final ConcurrentLinkedQueue<BlockPos> PENDING_INVALIDATIONS = new ConcurrentLinkedQueue<>();
    private static final Set<BlockPos> PENDING_POSITIONS = ConcurrentHashMap.newKeySet();

    /**
     * Schedule a chunk invalidation for a door position.
     * Deduplicated: a position already queued is not added again (prevents flicker).
     */
    public static void scheduleChunkInvalidation(BlockPos pos) {
        if (pos == null) return;
        BlockPos imm = pos.immutable();
        if (PENDING_POSITIONS.add(imm)) {
            PENDING_INVALIDATIONS.add(imm);
        }
    }

    /**
     * Process the invalidation queue. Call from ClientTickEvent.END.
     */
    public static void processPendingInvalidations() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.levelRenderer == null) return;

        BlockPos pos;
        while ((pos = PENDING_INVALIDATIONS.poll()) != null) {
            PENDING_POSITIONS.remove(pos);
            try {
                BlockState state = mc.level.getBlockState(pos);
                mc.levelRenderer.blockChanged(mc.level, pos, state, state, Block.UPDATE_CLIENTS);
                OcclusionCullingHelper.onClientWorldGeometryMayHaveChanged();
            } catch (Exception e) {
                MainRegistry.LOGGER.debug("Door chunk invalidation at {}: {}", pos, e.getMessage());
            }
        }
    }
}
