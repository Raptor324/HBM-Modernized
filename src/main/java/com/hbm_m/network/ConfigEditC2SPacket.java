package com.hbm_m.network;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.config.schema.ConfigSchema;
import com.hbm_m.config.schema.ConfigSide;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * C2S: оператор применяет серверные настройки, отредактированные в GUI.
 *
 * <p>Полезная нагрузка — карта {@code key→value} только серверных полей (клиентские поля
 * GUI сохраняет локально через {@code ModClothConfig.saveClient()}, без пакета).
 *
 * <p><b>Безопасность:</b> сервер повторно проверяет права оператора
 * ({@code hasPermissions(2)}) — клиентская видимость вкладки не является гарантией прав.
 * {@link ConfigSchema#applyAll} дополнительно отсекает не-серверные ключи.
 *
 * <p>После применения: сохранение {@code server.json} и широковещательная синхронизация
 * всем игрокам через {@link ConfigSyncS2CPacket} (включая редактирующего).
 */
public class ConfigEditC2SPacket implements C2SPacket {

    private final Map<String, String> edits;

    public ConfigEditC2SPacket(Map<String, String> edits) {
        this.edits = edits;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static ConfigEditC2SPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(), buf.readUtf());
        }
        return new ConfigEditC2SPacket(map);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(edits.size());
        for (Map.Entry<String, String> e : edits.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeUtf(e.getValue());
        }
    }

    // ── Handler (сервер) ──────────────────────────────────────────────────────

    public static void handle(ConfigEditC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            // Повторная серверная проверка прав оператора (защита от подделанного пакета).
            if (!player.hasPermissions(2)) return;

            // applyAll применяет только поля совпадающей стороны (SERVER),
            // игнорирует неизвестные ключи и клэмпит по границам схемы + валидирует.
            ModClothConfig cfg = ModClothConfig.get();
            ConfigSchema.applyAll(cfg, ConfigSide.SERVER, msg.edits);
            ModClothConfig.saveServer();

            // Широковещательная синхронизация: все клиенты обновят серверные поля.
            MinecraftServer server = player.getServer();
            if (server != null) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    ConfigSyncS2CPacket.sendTo(p);
                }
            }
        });
    }

    // ── Send helper (клиент) ───────────────────────────────────────────────────

    /** Клиент: отправить набор правок серверных полей. Вызывает GUI для op-пользователя. */
    public static void sendToServer(Map<String, String> edits) {
        ModPacketHandler.sendToServer(ModPacketHandler.CONFIG_EDIT, new ConfigEditC2SPacket(edits));
    }
}
