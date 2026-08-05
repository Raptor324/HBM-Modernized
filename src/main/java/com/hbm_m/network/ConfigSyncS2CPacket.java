package com.hbm_m.network;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.config.schema.ConfigSchema;
import com.hbm_m.config.schema.ConfigSide;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * S2C: полный снапшот серверных настроек конфига (сторона {@link ConfigSide#SERVER}).
 *
 * <p>Сервер — единственный источник правды для серверных полей (радиация, взрывы, оружие, ...).
 * При входе игрока и после изменения op'ом сервер рассылает этот пакет всем клиентам, чтобы
 * клиентские чтения серверных полей (например {@code maxPlayerRad} в оверлее гейгера,
 * {@code craterBiomeRad} в клиентских проверках биома) отражали актуальную серверную конфигурацию.
 *
 * <p>Клиентские поля сюда НЕ входят — они живут в локальном {@code client.json} и не синхронизируются.
 */
public class ConfigSyncS2CPacket implements S2CPacket {

    private final Map<String, String> values;

    public ConfigSyncS2CPacket(Map<String, String> values) {
        this.values = values;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static ConfigSyncS2CPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(), buf.readUtf());
        }
        return new ConfigSyncS2CPacket(map);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(values.size());
        for (Map.Entry<String, String> e : values.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeUtf(e.getValue());
        }
    }

    // ── Handler (клиент) ──────────────────────────────────────────────────────

    public static void handle(ConfigSyncS2CPacket msg, PacketContext context) {
        context.queue(() -> {
            // Применяем только серверные поля к клиентскому синглтону.
            // applyAll игнорирует неизвестные ключи и клэмпит по границам схемы.
            ConfigSchema.applyAll(ModClothConfig.get(), ConfigSide.SERVER, msg.values);
        });
    }

    // ── Send helpers (сервер) ──────────────────────────────────────────────────

    /** Сервер: снять снапшот серверных полей и отправить конкретному игроку. */
    public static void sendTo(ServerPlayer player) {
        Map<String, String> snap = ConfigSchema.snapshot(ModClothConfig.get(), ConfigSide.SERVER);
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.CONFIG_SYNC, new ConfigSyncS2CPacket(snap));
    }
}
