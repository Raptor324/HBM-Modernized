package com.hbm_m.armormod.item;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Модификация сервоприводов - увеличивает скорость движения
 */
public class ItemModServos extends ItemArmorMod {

    public ItemModServos(Properties properties) {
        super(properties, ArmorModificationHelper.servos);
    }

    @Override
    public List<Component> getEffectTooltipLines() {
        return List.of(Component.translatable("tooltip.hbm_m.mod.servos.effect"));
    }
}
