package com.hbm_m.armormod.item;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Модификация обшивки - защищает от коррозии и внешних повреждений
 */
public class ItemModCladding extends ItemArmorMod {

    public ItemModCladding(Properties properties) {
        super(properties, ArmorModificationHelper.cladding);
    }

    @Override
    public List<Component> getEffectTooltipLines() {
        return List.of(Component.translatable("tooltip.hbm_m.mod.cladding.effect"));
    }
}
