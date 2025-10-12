package com.hbm_m.hazard;

import net.minecraft.ChatFormatting;

public enum HazardType {

    RADIATION("hazard.hbm_m.radiation", "hazard.hbm_m.radiation.format", ChatFormatting.GREEN, true),
    
    HYDRO_REACTIVE("hazard.hbm_m.hydro_reactive", "hazard.hbm_m.explosion_strength.format", ChatFormatting.RED, true),
    
    EXPLOSIVE_ON_FIRE("hazard.hbm_m.explosive_on_fire", "hazard.hbm_m.explosion_strength.format", ChatFormatting.RED, true),
    
    PYROPHORIC("hazard.hbm_m.pyrophoric", "", ChatFormatting.GOLD, false);

    private final String translationKey;
    private final String formatTranslationKey;
    private final ChatFormatting color;
    private final boolean showValueInTooltip;

    HazardType(String translationKey, String formatTranslationKey, ChatFormatting color, boolean showValueInTooltip) {
        this.translationKey = translationKey;
        this.formatTranslationKey = formatTranslationKey;
        this.color = color;
        this.showValueInTooltip = showValueInTooltip;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public String getFormatTranslationKey() {
        return formatTranslationKey;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public boolean shouldShowValueInTooltip() {
        return showValueInTooltip;
    }
}
