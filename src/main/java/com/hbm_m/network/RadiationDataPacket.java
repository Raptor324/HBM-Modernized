package com.hbm_m.network;

import com.hbm_m.client.overlay.GeigerOverlay;
import com.hbm_m.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RadiationDataPacket {
    private final float radiationValue;

    public RadiationDataPacket(float radiationValue) {
        this.radiationValue = radiationValue;
    }

    // Метод для кодирования пакета в FriendlyByteBuf
    public static void encode(RadiationDataPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.radiationValue);
    }

    // Метод для декодирования пакета из FriendlyByteBuf
    public static RadiationDataPacket decode(FriendlyByteBuf buf) {
        return new RadiationDataPacket(buf.readFloat());
    }

    // Метод для обработки пакета
    public static void handle(RadiationDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Обрабатываем пакет на клиентской стороне
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // Получаем текущего игрока на клиенте
                Minecraft mc = Minecraft.getInstance();
                LocalPlayer player = mc.player;

                if (player != null && (player.isCreative() || player.isSpectator())) {
                    // Если игрок в креативе или наблюдателе, сбрасываем радиацию на клиенте
                    GeigerOverlay.clientPlayerRadiation = 0.0f;
                    MainRegistry.LOGGER.debug("CLIENT: Player in creative/spectator mode. Resetting clientPlayerRadiation to 0.0.");
                } else {
                    // Иначе обновляем статическое поле в GeigerOverlay
                    GeigerOverlay.clientPlayerRadiation = msg.radiationValue;
                    MainRegistry.LOGGER.debug("CLIENT: Received RadiationDataPacket. Updating clientPlayerRadiation to {}", msg.radiationValue);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}