package com.hbm_m.event;

import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.hazard.HazardType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.text.DecimalFormat;
import java.util.List;

public class HazardTooltipHandler {

    private static final DecimalFormat df = new DecimalFormat("#.###");

    public static void appendHazardTooltips(ItemStack stack, List<Component> tooltip) {
        if (stack.isEmpty()) {
            return;
        }

        for (HazardType type : HazardType.values()) {
            float level = HazardSystem.getHazardLevelFromStack(stack, type);
            if (level > 0) {
                // Добавляем заголовок опасности
                tooltip.add(Component.translatable(type.getTranslationKey())
                    .withStyle(type.getColor()));
                
                if (type.shouldShowValueInTooltip()) {
                    // Используем локализованный формат с параметром
                    tooltip.add(Component.translatable(type.getFormatTranslationKey(), df.format(level))
                        .withStyle(net.minecraft.ChatFormatting.YELLOW));
                    
                    int count = stack.getCount();
                    if (count > 1) {
                        float total = level * count;
                        tooltip.add(Component.translatable("hazard.hbm_m.stack", 
                            Component.translatable(type.getFormatTranslationKey(), df.format(total)))
                            .withStyle(net.minecraft.ChatFormatting.YELLOW));
                    }
                }
            }
        }
    }
}
