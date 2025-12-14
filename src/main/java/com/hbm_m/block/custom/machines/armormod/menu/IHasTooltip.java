package com.hbm_m.block.custom.machines.armormod.menu;

// Интерфейс для слотов, которые могут показывать подсказки, когда они пусты
import net.minecraft.network.chat.Component;

public interface IHasTooltip {
    /**
     * @return Компонент подсказки для этого слота, когда он пуст.
     */
    Component getEmptyTooltip();
}