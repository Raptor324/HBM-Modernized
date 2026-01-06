package com.hbm_m.armormod.item;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Модификация батареи MK2 - увеличивает емкость силовой брони на 50%
 */
public class ItemModBatteryMk2 extends ItemArmorMod {

    public ItemModBatteryMk2(Properties properties) {
        super(properties, ArmorModificationHelper.battery);
    }

    @Override
    public List<Component> getEffectTooltipLines() {
        return List.of(Component.translatable("tooltip.hbm_m.mod.battery_mk2.effect"));
    }
}
