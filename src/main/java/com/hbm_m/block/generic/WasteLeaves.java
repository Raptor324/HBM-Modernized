package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.List;

/**
 * Порт {@code com.hbm.blocks.generic.WasteLeaves} (1.7.10) под 1.20.1.
 *
 * "Мёртвая листва" после ядерного взрыва. Не опадает как обычные листья
 * и не спавнит дроп / падающие блоки: при случайном тике тихо исчезает.
 * Это нужно, чтобы после взрыва не появлялись тысячи {@code ItemEntity}
 * и {@code FallingBlockEntity} от листвы — они сажают FPS.
 */
public class WasteLeaves extends LeavesBlock {

    public WasteLeaves(Properties properties) {
        super(properties);
    }

    /**
     * Вместо стандартного распада листвы (which drops saplings/sticks)
     * просто удаляем блок без дропа.
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Собственный таймер распада: ~1 к 30 тикам блок исчезает.
        if (random.nextInt(30) == 0) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        // Ванильное decay-логику НЕ вызываем — именно она дропает предметы.
    }

    /**
     * 1.20.1: отключаем ванильное decay-дропми через getDrops —
     * на случай, если что-то всё же дёргает добычу блока напрямую.
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return Collections.emptyList();
    }

    /**
     * Предотвращаем спаун FallingBlockEntity при снятии блока с опоры
     * (LeavesBlock сам по себе этого не делает, но на всякий случай).
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        // intentionally no-op — no falling entity spawn
    }
}
