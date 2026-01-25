package com.hbm_m.armormod.item;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Модификация "Дополнительно" - расширяет возможности брони
 */
public class ItemModExtra extends ItemArmorMod {

    public ItemModExtra(Properties properties) {
        super(properties, ArmorModificationHelper.extra);
    }

    @Override
    public List<Component> getEffectTooltipLines() {
        return List.of(Component.translatable("tooltip.hbm_m.mod.extra.effect"));
    }
}
