package com.hbm_m.block.custom.machines.armormod.item;

import com.hbm_m.block.custom.machines.armormod.util.ArmorModificationHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * Модификация обшивки - защищает от коррозии и внешних повреждений
 */
public class ItemModCladding extends ItemArmorMod {

    public ItemModCladding(Item.Properties properties) {
        super(properties, ArmorModificationHelper.cladding);
    }

    @Override
    public List<Component> getEffectTooltipLines() {
        return List.of(Component.translatable("tooltip.hbm_m.mod.cladding.effect"));
    }
}
