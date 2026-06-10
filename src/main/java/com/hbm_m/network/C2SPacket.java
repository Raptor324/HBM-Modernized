package com.hbm_m.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Базовый интерфейс для всех клиентских (C2S) пакетов.
 */
public interface C2SPacket {
    void write(FriendlyByteBuf buf);
}