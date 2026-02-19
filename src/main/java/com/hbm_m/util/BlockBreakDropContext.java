package com.hbm_m.util;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;

/**
 * Контекст разрушения блока для управления ручными дропами из onRemove().
 * Нужен, чтобы в creative не выбрасывать инвентарь из контейнеров.
 */
public final class BlockBreakDropContext {

    private static final ThreadLocal<Set<Long>> SKIP_INVENTORY_DROPS =
            ThreadLocal.withInitial(HashSet::new);

    private BlockBreakDropContext() {
    }

    public static void markSkipInventoryDrop(BlockPos pos) {
        SKIP_INVENTORY_DROPS.get().add(pos.asLong());
    } 

    public static boolean consumeSkipInventoryDrop(BlockPos pos) {
        return SKIP_INVENTORY_DROPS.get().remove(pos.asLong());
    }
}
