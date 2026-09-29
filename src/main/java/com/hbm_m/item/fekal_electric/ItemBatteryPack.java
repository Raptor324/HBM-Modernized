// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm_m.item.fekal_electric;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Большая батарея-пак, вставляемая в батарейный сокет (бэкпорт оригинального ItemBatteryPack).
 * Рендерится в сокете как объёмное тело части Battery/Capacitor с текстурой тира
 * (см. MachineBatterySocketBakedModel), а в руке — как та же OBJ-модель
 * (battery_pack_loader). Спавнится пустым, как в оригинале.
 */
public class ItemBatteryPack extends ModBatteryItem {

    public final EnumBatteryPack tier;

    public ItemBatteryPack(Properties properties, EnumBatteryPack tier) {
        super(properties, tier.capacity, tier.chargeRate, tier.dischargeRate);
        this.tier = tier;
    }

    @Override
    public void appendHbmTooltip(@NotNull ItemStack stack, @Nullable Level level,
            @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHbmTooltip(stack, level, tooltip, flag);

        double fullChargeMinutes = tier.capacity / (double) tier.chargeRate / EnumBatteryPack.TICKS_PER_MINUTE;
        double dischargeMinutes = tier.capacity / (double) tier.dischargeRate / EnumBatteryPack.TICKS_PER_MINUTE;
        tooltip.add(Component.translatable("tooltip.hbm_m.battery_pack.time_for_full_charge",
                String.format("%.1f", fullChargeMinutes)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.hbm_m.battery_pack.charge_lasts_for",
                String.format("%.1f", dischargeMinutes)).withStyle(ChatFormatting.GOLD));
    }

    /** Короткое имя текстуры тира — ключ рендера сокета и item-модели. */
    public String getTextureName() {
        return tier.tex;
    }
}
