package com.hbm_m.armormod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.List;

import com.hbm_m.armormod.util.ArmorModificationHelper;

/**
 * Модификация батареи - увеличивает емкость силовой брони
 */
public class ItemModBattery extends ItemArmorMod {

    private final double capacityMultiplier;

    public ItemModBattery(Item.Properties properties, double capacityMultiplier) {
        super(properties, ArmorModificationHelper.battery);
        this.capacityMultiplier = capacityMultiplier;
    }

    public double getCapacityMultiplier() {
        return capacityMultiplier;
    }

    @Override
    public List<Component> getEffectTooltipLines() {
        return List.of(Component.translatable("tooltip.hbm_m.mod.battery.effect", String.format("%.0f%%", (capacityMultiplier - 1.0) * 100)));
    }
}
