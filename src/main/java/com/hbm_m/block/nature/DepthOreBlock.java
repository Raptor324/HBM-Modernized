package com.hbm_m.block.nature;

import com.hbm_m.api.item.IDepthRockTool;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

//? if neoforge {
/*import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
*///?}

//? if forge {
import net.minecraft.world.item.TooltipFlag;
//?}


import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Порт {@code BlockDepth}/{@code BlockDepthOre} из 1.7.10: глубинная порода.
 *
 * Механика оригинала:
 * <ul>
 * <li>{@code setBlockUnbreakable()} — без инструмента блок не копается вовсе
 *     (прогресс 0); взрывы его уничтожают (сопротивление 10), при этом
 *     выпадает стандартный лут из loot-таблицы (ванильная логика взрыва —
 *     отдельный оверрайд не нужен и был убран: он поп-ил сам блок предметом).</li>
 * <li>{@code getPlayerRelativeBlockHardness} — держатель
 *     {@code IDepthRockTool} ({@code canBreakRock}) копает блок с фиксированной
 *     скоростью {@code 1/50} за тик, то есть 50 тиков (2.5 с) на блок.</li>
 * </ul>
 */
public class DepthOreBlock extends Block {

    public DepthOreBlock(Properties properties) {
        super(properties
                // setBlockUnbreakable() + setResistance(10.0F) из BlockDepth
                .strength(-1.0F, 10.0F)
                .requiresCorrectToolForDrops()
                .pushReaction(PushReaction.BLOCK)
        );
    }

    //? if forge {
    @Override
    public void appendHoverText(ItemStack stack,
                                @Nullable BlockGetter level,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        addDepthOreTooltip(tooltip);
    }
    //?}

    //? if neoforge {
    /*@Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addDepthOreTooltip(tooltip);
    }
    *///?}


    private static void addDepthOreTooltip(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.hbm_m.depthstone.line1")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.hbm_m.depthstone.line4")
                .withStyle(ChatFormatting.YELLOW));
    }

    // Не ломается поршнями — через Properties.pushReaction (совместимо с 1.20–1.21+)

    // Прогресс добычи: 0 без ломателя породы, 1/50 за тик с ним (BlockDepth 1.7.10)
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        ItemStack held = player.getMainHandItem();
        if (level instanceof Level lvl
                && held.getItem() instanceof IDepthRockTool tool
                && tool.canBreakRock(lvl, player, held, state, pos)) {
            return (float) (1D / 50D);
        }
        return 0.0F;
    }

    // Дроп разрешён только «ломателю породы» (в оригинале MINER-инструменты
    // проходят canHarvestBlock — Material.rock в списке их эффективных материалов)
    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        ItemStack held = player.getMainHandItem();
        if (level instanceof Level lvl
                && held.getItem() instanceof IDepthRockTool tool
                && tool.canBreakRock(lvl, player, held, state, pos)) {
            return true;
        }
        return super.canHarvestBlock(state, level, pos, player);
    }
}
