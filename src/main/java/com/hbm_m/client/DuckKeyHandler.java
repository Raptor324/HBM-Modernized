package com.hbm_m.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;

/**
 * 1:1 {@code ModEventHandlerClient}: Taste O (fest, keine Tastenbelegung) schickt einmal pro Sitzung
 * den Enten-Wurf an den Server ({@code AuxButtonPacket 999} -> {@link com.hbm_m.network.DuckC2SPacket}).
 */
public final class DuckKeyHandler {

    private DuckKeyHandler() { }

    public static boolean ducked = false;

    public static void tick(Minecraft mc) {
        if (mc.player == null) return;
        if (!ducked && InputConstants.isKeyDown(mc.getWindow().getWindow(), InputConstants.KEY_O) && mc.screen == null) {
            ducked = true;
            com.hbm_m.network.DuckC2SPacket.send();
        }
    }
}
