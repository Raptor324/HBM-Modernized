package com.hbm_m.item.tool;

import com.hbm_m.api.item.IDepthRockTool;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import java.util.List;

//? if neoforge {
/*import net.minecraft.world.item.Item.TooltipContext;
*///?}

/**
 * Предмет-«ломатель глубинной породы» — минимальный порт флага
 * {@code rockBreaker} из {@code ItemToolAbility} 1.7.10
 * ({@code setDepthRockBreaker()} + {@code canBreakRock}).
 *
 * В оригинале флаг стоит у четырёх кирок (bismuth/volcanic/chlorophyte/mese
 * pickaxe); в порту они пока являются простыми предметами-заглушками, поэтому
 * общий класс здесь один на всех.
 */
public class ItemDepthRockBreaker extends Item implements IDepthRockTool {

    public ItemDepthRockBreaker(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canBreakRock(Level level, Player player, ItemStack tool, BlockState state, BlockPos pos) {
        // Оригинал: canOperate(tool) && this.rockBreaker — у заглушек «заряд» не тратится.
        return true;
    }

    //? if forge {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        addDepthRockTooltip(tooltip);
    }
    //?}

    //? if neoforge {
    /*@Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addDepthRockTooltip(tooltip);
    }
    *///?}

    private static void addDepthRockTooltip(List<Component> tooltip) {
        // Оригинал ItemToolAbility: "Can break depth rock!" (красным)
        tooltip.add(Component.translatable("tooltip.hbm_m.depth_rock_breaker")
                .withStyle(ChatFormatting.RED));
    }
}
