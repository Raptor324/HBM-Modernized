package com.hbm_m.client.render;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.hbm_m.main.MainRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Отложенная инвалидация чанков для дверей.
 * Baked models (Iris/Oculus) не обновляются после первого открытия без явной инвалидации —
 * Sodium/Embeddium кэширует чанки. Вызывать processPendingInvalidations из ClientTickEvent.END.
 * Дедупликация: одна и та же позиция не добавляется повторно, пока не обработана.
 */
@OnlyIn(Dist.CLIENT)
public class DoorChunkInvalidationHelper {

    private static final ConcurrentLinkedQueue<BlockPos> PENDING_INVALIDATIONS = new ConcurrentLinkedQueue<>();
    private static final Set<BlockPos> PENDING_POSITIONS = ConcurrentHashMap.newKeySet();

    /**
     * Запланировать инвалидацию чанка для позиции двери.
     * Дедупликация: если позиция уже в очереди, повторно не добавляется (устраняет моргание).
     */
    public static void scheduleChunkInvalidation(BlockPos pos) {
        if (pos == null) return;
        BlockPos imm = pos.immutable();
        if (PENDING_POSITIONS.add(imm)) {
            PENDING_INVALIDATIONS.add(imm);
        }
    }

    /**
     * Обработать очередь инвалидаций. Вызывать из ClientTickEvent.END.
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
            } catch (Exception e) {
                MainRegistry.LOGGER.debug("Door chunk invalidation at {}: {}", pos, e.getMessage());
            }
        }
    }
}
