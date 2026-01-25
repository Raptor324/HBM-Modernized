package com.hbm_m.block.custom.machines.armormod.item;

import com.hbm_m.block.custom.machines.armormod.util.ArmorModificationHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * Модификация батареи MK3 - увеличивает емкость силовой брони вдвое
 */
public class ItemModBatteryMk3 extends ItemArmorMod {

    public ItemModBatteryMk3(Item.Properties properties) {
        super(properties, ArmorModificationHelper.battery);
    }

    @Override
    public List<Component> getEffectTooltipLines() {
        return List.of(Component.translatable("tooltip.hbm_m.mod.battery_mk3.effect"));
    }
}
