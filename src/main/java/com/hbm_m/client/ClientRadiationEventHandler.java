package com.hbm_m.client;

// Обработчик клиентских событий для очистки данных о радиации при входе в мир или отключении.
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientRadiationEventHandler {

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientRadiationData.clearAll();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientRadiationData.clearAll();
    }

    @SubscribeEvent
    public static void onPlayerJoinWorld(EntityJoinLevelEvent event) {
        // Проверяем, что событие произошло на клиентской стороне
        if (!event.getLevel().isClientSide()) {
            return;
        }

        // Проверяем, что сущность, которая вошла в мир - это именно наш игрок
        if (event.getEntity() == Minecraft.getInstance().player) {
            // Если да, то очищаем кэш.
            // Это сработает и при входе в мир, и при смене измерения.
            ClientRadiationData.clearAll();
        }
    }
}
