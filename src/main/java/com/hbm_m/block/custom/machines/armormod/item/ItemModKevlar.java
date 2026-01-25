package com.hbm_m.block.custom.machines.armormod.item;

import com.hbm_m.block.custom.machines.armormod.util.ArmorModificationHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * Модификация кевлара - увеличивает защиту от пуль и осколков
 */
public class ItemModKevlar extends ItemArmorMod {

    public ItemModKevlar(Item.Properties properties) {
        super(properties, ArmorModificationHelper.kevlar);
    }

    @Override
    public List<Component> getEffectTooltipLines() {
        return List.of(Component.translatable("tooltip.hbm_m.mod.kevlar.effect"));
    }
}
