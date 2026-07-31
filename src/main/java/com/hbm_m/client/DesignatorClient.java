package com.hbm_m.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import com.hbm_m.inventory.gui.DesignatorScreen;

/**
 * Client-only helper to open the designator screen without loading client-only classes on the server.
 */
public final class DesignatorClient {

    private DesignatorClient() {}

    /**
     * Call only from client (e.g. via EnvExecutor.runInEnv(Env.CLIENT, ...)).
     */
    public static void openScreen(Player player) {
        if (player == null) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.setScreen(new DesignatorScreen(player));
        }
    }
}