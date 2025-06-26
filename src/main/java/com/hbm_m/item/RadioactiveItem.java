package com.hbm_m.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Базовый класс для радиоактивных предметов
 */
public class RadioactiveItem extends Item {
    
    private final float radiationLevel;
    private final String[] oreDict;

    /**
     * Создает радиоактивный предмет
     * @param properties Свойства предмета
     * @param radiationLevel Уровень радиации в рад/с
     * @param oreDict Словарь руд для этого предмета (может быть пустым)
     */
    public RadioactiveItem(Properties properties, float radiationLevel, String... oreDict) {
        super(properties);
        this.radiationLevel = radiationLevel;
        this.oreDict = oreDict;
    }

    /**
     * Получить уровень радиации предмета
     * @return уровень радиации в рад/с
     */
    public float getRadiationLevel() {
        return radiationLevel;
    }
    
    /**
     * Получить словарь руд для этого предмета
     * @return массив строк с именами в словаре руд
     */
    public String[] getOreDict() {
        return oreDict;
    }
    
    @Override
    public void appendHoverText(@javax.annotation.Nonnull ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        // Добавляем информацию о радиоактивности в подсказку
        tooltip.add(Component.translatable("item.hbm_m.radioactive").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal(radiationLevel + "RAD/s").withStyle(ChatFormatting.GREEN));
        
        // Добавляем информацию о словаре руд, если он есть
        if (oreDict != null && oreDict.length > 0) {
            tooltip.add(Component.translatable("item.hbm_m.ore_dict").withStyle(ChatFormatting.DARK_PURPLE));
            for (String dict : oreDict) {
                tooltip.add(Component.literal("-" + dict).withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
    }
}