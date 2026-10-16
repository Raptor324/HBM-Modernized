package com.hbm_m.api.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@code api.hbm.item.IDepthRockTool} из 1.7.10.
 *
 * Предметы, реализующие этот интерфейс, могут добывать глубинную породу
 * (блоки {@link com.hbm_m.block.nature.DepthOreBlock}): в оригинале такие блоки
 * неразрушимы, но инструмент-«ломатель породы» снимает их с фиксированной
 * скоростью 1/50 за тик ({@code BlockDepth#getPlayerRelativeBlockHardness}).
 *
 * В оригинале интерфейс реализует только {@code ItemToolAbility} с флагом
 * {@code rockBreaker} (setDepthRockBreaker): кирки из бисмута, вулканическая,
 * хлорофитовая и месовая.
 */
public interface IDepthRockTool {

    /**
     * Может ли предмет ломать глубинную породу; параметры позволяют ограничить
     * добычу по измерению/позиции/состоянию блока (как в оригинале).
     */
    boolean canBreakRock(Level level, Player player, ItemStack tool, BlockState state, BlockPos pos);
}
