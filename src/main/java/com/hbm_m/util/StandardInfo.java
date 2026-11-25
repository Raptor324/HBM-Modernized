package com.hbm_m.util;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/** 1:1 {@code ITooltipProvider.addStandardInfo}: mit gedrueckter Umschalttaste die $-getrennte Beschreibung, sonst der Hinweis. */
public final class StandardInfo {

    private StandardInfo() {}

    public static boolean isShiftDown() {
        return dev.architectury.platform.Platform.getEnvironment() == dev.architectury.utils.Env.CLIENT
                && com.hbm_m.client.ClientAccess.hasShiftDown();
    }

    public static void add(List<Component> list, String key) {
        if (isShiftDown()) {
            for (String s : Language.getInstance().getOrDefault(key).split(java.util.regex.Pattern.quote("$"))) list.add(Component.literal(s).withStyle(ChatFormatting.YELLOW));
        } else {
            list.add(Component.literal("Hold <").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                    .append(Component.literal("LSHIFT").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC))
                    .append(Component.literal("> to display more info").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
        }
    }
}
