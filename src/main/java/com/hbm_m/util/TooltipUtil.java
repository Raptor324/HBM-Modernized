// TooltipUtil.java
package com.hbm_m.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class TooltipUtil {
    public static void addNuclearTooltip(List<Component> tooltip, Level level,
                                         ItemStack stack, BlockState state, TooltipFlag flag) {
        tooltip.add(Component.literal("⚠️ ОПАСНО! ЯДЕРНЫЙ УРОН")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x8B0000)))); // Тёмно-красный

        tooltip.add(Component.literal("Урон: 150+ сердец в радиусе 25 бл.")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF0000)))); // Красный

        tooltip.add(Component.literal("Вызывает радиацию и тошноту")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x808080)))); // Серый
    }
}
