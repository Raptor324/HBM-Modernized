package com.hbm_m.armormod.item;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Модификация батареи MK3 - увеличивает емкость силовой брони вдвое
 */
public class ItemModBatteryMk3 extends ItemArmorMod {

    public ItemModBatteryMk3(Properties properties) {
        super(properties, ArmorModificationHelper.battery);
    }

    @Override
    public List<Component> getEffectTooltipLines() {
        return List.of(Component.translatable("tooltip.hbm_m.mod.battery_mk3.effect"));
    }
}
