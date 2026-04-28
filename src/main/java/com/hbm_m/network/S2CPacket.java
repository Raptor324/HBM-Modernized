package com.hbm_m.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Базовый интерфейс для всех серверных (S2C) пакетов.
 */
public interface S2CPacket {
    void write(FriendlyByteBuf buf);
}