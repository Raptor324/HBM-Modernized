package com.hbm_m.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum CompletionStatus {
    COMPLETE("✔ Fully complete", ChatFormatting.GREEN),
    WIP("⚠ Work In Progress", ChatFormatting.YELLOW),
    NOT_IMPLEMENTED("❌ Not Implemented", ChatFormatting.RED);

    private final Component tooltip;

    CompletionStatus(String text, ChatFormatting color) {
        this.tooltip = Component.literal(text).withStyle(color);
    }

    public Component getTooltip() {
        return tooltip;
    }
}
