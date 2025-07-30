package com.hbm_m.event;

import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.hazard.HazardType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.text.DecimalFormat;
import java.util.List;

public class HazardTooltipHandler {

    // Форматтер для красивого отображения чисел (например, 1.5 вместо 1.5000001)
    private static final DecimalFormat df = new DecimalFormat("#.###");

    public static void appendHazardTooltips(ItemStack stack, List<Component> tooltip) {

        if (stack.isEmpty()) {
            return;
        }


        for (HazardType type : HazardType.values()) {
            float level = HazardSystem.getHazardLevelFromStack(stack, type);

            if (level > 0) {
                // Добавляем заголовок опасности (например, "Радиоактивный")
                tooltip.add(Component.translatable(type.getTranslationKey()).withStyle(type.getColor()));

                if (type.shouldShowValueInTooltip()) {
                    String line = String.format(type.getDisplayFormat(), df.format(level));
                    tooltip.add(Component.literal(line).withStyle(net.minecraft.ChatFormatting.YELLOW));
                    
                    int count = stack.getCount();
                    if (count > 1) {
                        float total = level * count;
                        String stackLine = String.format(type.getDisplayFormat(), df.format(total));
                        tooltip.add(Component.literal("Stack: " + stackLine).withStyle(net.minecraft.ChatFormatting.YELLOW));
                    }
                }
            }
        }
    }
}