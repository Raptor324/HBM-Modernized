package com.hbm_m.api.energy; // <-- Убедись, что package правильный

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Этот обработчик запускает перестройку энергосетей
 * ОДИН РАЗ, когда сервер полностью загрузился.
 * Это предотвращает дедлок при загрузке мира.
 */
@Mod.EventBusSubscriber(modid = "hbm_m") // <-- Укажи свой MOD_ID
public class ServerLifecycleHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Сервер полностью запущен, мир стабилен
        LOGGER.info("[HBM-NETWORK] Server has started, rebuilding energy networks for all dimensions...");

        for (ServerLevel level : event.getServer().getAllLevels()) {
            // Мы "будим" менеджер и запускаем перестройку
            EnergyNetworkManager.get(level).rebuildAllNetworks();
        }

        LOGGER.info("[HBM-NETWORK] Energy network rebuild complete.");
    }
}
