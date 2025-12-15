package com.hbm_m.block.custom.explosives;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Интерфейс для блоков, которые могут быть активированы детонатором
 */
public interface IDetonatable {
    /**
     * Вызывается когда детонатор отправляет сигнал на этот блок
     *
     * @param level Мир
     * @param pos Позиция блока
     * @param state Состояние блока
     * @param player Игрок, использовавший детонатор (может быть null)
     * @return true если блок успешно отреагировал на сигнал, false если нет
     */
    boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player);
}