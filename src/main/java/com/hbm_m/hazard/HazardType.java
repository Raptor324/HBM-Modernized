package com.hbm_m.hazard;

import net.minecraft.ChatFormatting;

public enum HazardType {
    RADIATION("hazard.hbm_m.radiation", "%s RAD/s", ChatFormatting.GREEN, true),
    HYDRO_REACTIVE("hazard.hbm_m.hydro_reactive", " Сила взрыва - %s", ChatFormatting.RED, true),
    EXPLOSIVE_ON_FIRE("hazard.hbm_m.explosive_on_fire", " Сила взрыва - %s", ChatFormatting.RED, true),
    PYROPHORIC("hazard.hbm_m.pyrophoric", "", ChatFormatting.GOLD, false);

    private final String translationKey;
    private final String displayFormat;
    private final ChatFormatting color;
    private final boolean showValueInTooltip;

    HazardType(String translationKey, String displayFormat, ChatFormatting color, boolean showValueInTooltip) {
        this.translationKey = translationKey;
        this.displayFormat = displayFormat;
        this.color = color;
        this.showValueInTooltip = showValueInTooltip;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public String getDisplayFormat() {
        return displayFormat;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public boolean shouldShowValueInTooltip() {
        return showValueInTooltip;
    }
}