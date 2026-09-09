package com.hbm_m.api.bomb;

import com.hbm_m.interfaces.IDetonatable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Единая точка «детонатор нажал на блок».
 *
 * The port split the original's single {@code IBomb} contract in two: charges implement
 * {@link IDetonatable}, bombs implement {@link IBomb}. Every detonator only ever checked
 * IDetonatable, so nine nuke blocks, the multi-purpose bomb and the landmine could not be
 * detonated at all - only by redstone. Upstream's ItemDetonator triggers {@code IBomb.explode}
 * and reports its {@link IBomb.BombReturnCode}.
 */
public final class BombDetonation {

    private BombDetonation() {}

    /** true if the block accepted the signal. */
    public static boolean trigger(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        Block block = state.getBlock();

        if (block instanceof IDetonatable detonatable) {
            return detonatable.onDetonate(level, pos, state, player);
        }
        if (block instanceof IBomb bomb) {
            return bomb.explode(level, pos).wasSuccessful();
        }
        return false;
    }

    /** Понимает ли блок сигнал детонатора вообще (для сообщения «блок несовместим»). */
    public static boolean isTriggerable(BlockState state) {
        Block block = state.getBlock();
        return block instanceof IDetonatable || block instanceof IBomb;
    }

    /**
     * Цепная детонация: сосед подрывается с задержкой по расстоянию.
     *
     * The chain schedules every neighbour it saw, and several charges schedule the same one, so by
     * the time the task runs the block is usually gone - the old code fired anyway with the stale
     * BlockState, detonating it twice and crashing the air bomb, whose onDetonate reads FACING off
     * whatever stands there now (air).
     */
    public static void triggerDetonatableLater(ServerLevel level, BlockPos pos, Block expected,
                                               @Nullable Player player, int delayTicks) {
        level.getServer().tell(new TickTask(delayTicks, () -> {
            BlockState now = level.getBlockState(pos);
            if (now.getBlock() != expected || !(now.getBlock() instanceof IDetonatable detonatable)) return;
            detonatable.onDetonate(level, pos, now, player);
        }));
    }
}
