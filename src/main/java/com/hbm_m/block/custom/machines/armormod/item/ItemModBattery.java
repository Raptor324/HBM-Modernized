package com.hbm_m.block.custom.machines.armormod.item;

import com.hbm_m.block.custom.machines.armormod.util.ArmorModificationHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.List;

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
