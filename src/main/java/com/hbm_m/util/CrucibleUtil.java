package com.hbm_m.util;

import com.hbm_m.api.block.ICrucibleAcceptor;
import com.hbm_m.inventory.material.MaterialStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Port of the 1.7.10 com.hbm.util.CrucibleUtil.
 * The original casts a hitscan straight down to find a pouring target;
 * here we scan block positions downward until we hit the first non-air block.
 */
public class CrucibleUtil {

    /**
     * Finds the first non-air block from startPos (inclusive) downward within range
     * and returns its position if its block entity is an ICrucibleAcceptor, else null.
     */
    public static @Nullable BlockPos getPouringTarget(Level level, BlockPos startPos, int range) {
        for (int i = 0; i <= range; i++) {
            BlockPos pos = startPos.below(i);
            if (level.getBlockState(pos).isAir()) continue;
            BlockEntity be = level.getBlockEntity(pos);
            return be instanceof ICrucibleAcceptor ? pos : null;
        }
        return null;
    }

    /**
     * Standard pouring: finds a target below startPos and pours the stack into it.
     * Modifies the passed stack. Returns the amount that was actually poured.
     * "Safe" semantics of the original (safe = true): nothing is lost when no
     * valid target exists, pouring simply does not happen.
     */
    public static int pourSingleStack(Level level, BlockPos startPos, int range, MaterialStack stack) {
        if (stack.isEmpty()) return 0;

        BlockPos target = getPouringTarget(level, startPos, range);
        if (target == null) return 0;

        BlockEntity be = level.getBlockEntity(target);
        if (!(be instanceof ICrucibleAcceptor acc)) return 0;

        if (!acc.canAcceptPartialPour(level, target, Direction.UP, stack)) return 0;

        int before = stack.amount;
        MaterialStack left = acc.pour(level, target, Direction.UP, stack);
        int poured = left == null ? before : before - left.amount;
        stack.amount = left == null ? 0 : left.amount;
        return poured;
    }
}
