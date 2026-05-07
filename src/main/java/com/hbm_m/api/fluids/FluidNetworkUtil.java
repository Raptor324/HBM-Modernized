package com.hbm_m.api.fluids;

import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Вспомогательная утилита для передачи жидкости из танка буровой/генераторов по соседним блокам.
// * TODO: УДАЛИТЬ НАХУЙ
 * В текущем порте может быть реализована минимально для компиляции Forge+Fabric.
 */
public final class FluidNetworkUtil {
    private FluidNetworkUtil() {}

    public static void pushFluidToNeighbor(
            BlockEntity source,
            Level level,
            BlockPos sourcePos,
            Direction dir,
            FluidTank tank) {
        // В текущем порте корректная обвязка capability-передачи не синхронизирована
        // с этим хелпером. Чтобы сборка Forge+Fabric не ломалась,
        // оставляем безопасную заглушку (no-op).
    }
}
