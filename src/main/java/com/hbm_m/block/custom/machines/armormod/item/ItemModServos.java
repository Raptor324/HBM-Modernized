package com.hbm_m.block.custom.machines.armormod.item;

import com.hbm_m.block.custom.machines.armormod.util.ArmorModificationHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * Модификация сервоприводов - увеличивает скорость движения
 */
public class ItemModServos extends ItemArmorMod {

    public ItemModServos(Item.Properties properties) {
        super(properties, ArmorModificationHelper.servos);
    }

    @Override
    public List<Component> getEffectTooltipLines() {
        return List.of(Component.translatable("tooltip.hbm_m.mod.servos.effect"));
    }
}
