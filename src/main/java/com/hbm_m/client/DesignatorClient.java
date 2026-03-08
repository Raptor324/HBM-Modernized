package com.hbm_m.client;

import net.minecraft.world.entity.player.Player;

/**
 * Client-only helper to open the designator screen without loading client-only classes on the server.
 * Uses reflection so the server never loads Minecraft, Screen, or DesignatorScreen.
 */
public final class DesignatorClient {

    private DesignatorClient() {}

    /**
     * Call only from client (e.g. via DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)).
     */
    public static void openScreen(Player player) {
        if (player == null) return;
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Object mc = mcClass.getMethod("getInstance").invoke(null);
            if (mc == null) return;
            Class<?> screenClass = Class.forName("com.hbm_m.inventory.gui.DesignatorScreen");
            Object screen = screenClass.getConstructor(Player.class).newInstance(player);
            mcClass.getMethod("setScreen", Class.forName("net.minecraft.client.gui.screens.Screen")).invoke(mc, screen);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to open designator screen", e);
        }
    }
}
