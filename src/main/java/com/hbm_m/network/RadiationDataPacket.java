package com.hbm_m.network;

// Пакет для передачи данных о радиации от сервера к клиенту.
// Содержит два значения: общую радиацию в окружающей среде и радиацию игрока.
// Используется для обновления оверлея радиации на клиенте.

import com.hbm_m.client.overlay.OverlayGeiger;
import com.hbm_m.main.MainRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RadiationDataPacket {

    private final float totalEnvironmentRad;
    private final float playerRad;

    public RadiationDataPacket(float totalEnvironmentRad, float playerRad) {
        this.totalEnvironmentRad = totalEnvironmentRad;
        this.playerRad = playerRad;
    }

    // Кодируем оба значения в буфер
    public static void encode(RadiationDataPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.totalEnvironmentRad);
        buf.writeFloat(msg.playerRad);
    }

    // Декодируем оба значения из буфера
    public static RadiationDataPacket decode(FriendlyByteBuf buf) {
        float envRad = buf.readFloat();
        float pRad = buf.readFloat();
        return new RadiationDataPacket(envRad, pRad);
    }

    // Обрабатываем пакет на клиенте
    public static void handle(RadiationDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Выполняем код только на клиенте
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // Обновляем ОБА статических поля в нашем классе-оверлее
                OverlayGeiger.clientTotalEnvironmentRadiation = msg.totalEnvironmentRad;
                OverlayGeiger.clientPlayerRadiation = msg.playerRad;
                
                if (msg.totalEnvironmentRad > 0 || msg.playerRad > 0) {
                    MainRegistry.LOGGER.debug("CLIENT: Received RadiationDataPacket. EnvRad: {}, PlayerRad: {}", msg.totalEnvironmentRad, msg.playerRad);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}