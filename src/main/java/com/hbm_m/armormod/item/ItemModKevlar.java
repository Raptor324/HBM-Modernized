package com.hbm_m.armormod.item;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Модификация кевлара - увеличивает защиту от пуль и осколков
 */
public class ItemModKevlar extends ItemArmorMod {

    public ItemModKevlar(Properties properties) {
        super(properties, ArmorModificationHelper.kevlar);
    }

    @Override
    public List<Component> getEffectTooltipLines() {
        return List.of(Component.translatable("tooltip.hbm_m.mod.kevlar.effect"));
    }
}
