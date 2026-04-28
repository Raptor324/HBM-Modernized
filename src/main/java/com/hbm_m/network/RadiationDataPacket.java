package com.hbm_m.network;

import com.hbm_m.client.overlay.OverlayGeiger;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.S2CPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class RadiationDataPacket implements S2CPacket {

    private final float totalEnvironmentRad;
    private final float playerRad;

    // Кэш последних значений для подавления лишних логов
    private static float lastEnvRad    = -1f;
    private static float lastPlayerRad = -1f;

    public RadiationDataPacket(float totalEnvironmentRad, float playerRad) {
        this.totalEnvironmentRad = totalEnvironmentRad;
        this.playerRad           = playerRad;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static RadiationDataPacket decode(FriendlyByteBuf buf) {
        return new RadiationDataPacket(buf.readFloat(), buf.readFloat());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(totalEnvironmentRad);
        buf.writeFloat(playerRad);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(RadiationDataPacket msg, PacketContext context) {
        // context.queue() выполняется на главном потоке клиента — DistExecutor не нужен
        context.queue(() -> {
            OverlayGeiger.clientTotalEnvironmentRadiation = msg.totalEnvironmentRad;
            OverlayGeiger.clientPlayerRadiation           = msg.playerRad;

            if (msg.totalEnvironmentRad != lastEnvRad || msg.playerRad != lastPlayerRad) {
                MainRegistry.LOGGER.debug(
                        "CLIENT: Received RadiationDataPacket. EnvRad: {}, PlayerRad: {}",
                        msg.totalEnvironmentRad, msg.playerRad);
                lastEnvRad    = msg.totalEnvironmentRad;
                lastPlayerRad = msg.playerRad;
            }
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendTo(ServerPlayer player, float envRad, float playerRad) {
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.RADIATION_DATA,
                new RadiationDataPacket(envRad, playerRad));
    }
}