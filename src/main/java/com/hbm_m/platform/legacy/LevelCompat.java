package com.hbm_m.platform.legacy;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelHeightAccessor;

/**
 * [26.x] Umbenannte oder semantisch geänderte Vanilla-Methoden, für die eine reine Umbenennung nicht reicht.
 * Aufgerufen über die Stonecutter-Regeln {@code mc26_maxbuildheight} / {@code mc26_displayclientmessage}.
 */
public final class LevelCompat {

    private LevelCompat() {}

    /** Alt: {@code getMaxBuildHeight()} (exklusiv). Neu: {@code getMaxY()} (inklusiv). */
    public static int getMaxBuildHeight(LevelHeightAccessor level) {
        return level.getMaxY() + 1;
    }

    /** Alt: {@code player.displayClientMessage(msg, actionBar)}. */
    public static void displayClientMessage(Player player, Component message, boolean actionBar) {
        if (actionBar) {
            player.sendOverlayMessage(message);
        } else {
            player.sendSystemMessage(message);
        }
    }
}
