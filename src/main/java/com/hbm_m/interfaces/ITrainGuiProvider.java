package com.hbm_m.interfaces;

import net.minecraft.server.level.ServerPlayer;

/** Original: {@code EntityRailCarBase} mit {@code IGUIProvider} - die Zug-Taste oeffnet das Waggon-GUI. */
public interface ITrainGuiProvider {
    void openTrainGui(ServerPlayer player);
}
