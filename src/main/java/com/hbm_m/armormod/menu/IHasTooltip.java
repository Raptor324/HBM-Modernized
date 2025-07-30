package com.hbm_m.armormod.menu;

import net.minecraft.network.chat.Component;

public interface IHasTooltip {
    /**
     * @return Компонент подсказки для этого слота, когда он пуст.
     */
    Component getEmptyTooltip();
}