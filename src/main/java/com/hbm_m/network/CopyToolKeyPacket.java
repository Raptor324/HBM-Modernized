package com.hbm_m.network;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S: репорт зажатых клавиш копирки (оригинальный KeybindPacket для TOOL_ALT/TOOL_CTRL).
 * Шлётся клиентом по фронту из {@link com.hbm_m.config.ModConfigKeybindHandler}.
 */
public class CopyToolKeyPacket implements C2SPacket {

    private final boolean alt, ctrl;

    public CopyToolKeyPacket(boolean alt, boolean ctrl) {
        this.alt = alt;
        this.ctrl = ctrl;
    }

    public static CopyToolKeyPacket decode(FriendlyByteBuf buf) {
        return new CopyToolKeyPacket(buf.readBoolean(), buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(alt);
        buf.writeBoolean(ctrl);
    }

    public static void handle(CopyToolKeyPacket pkt, PacketContext ctx) {
        ctx.queue(() -> {
            if (ctx.getPlayer() instanceof ServerPlayer player) {
                // Фронт ALT: стартовый дебаунс листания индекса (оригинальный inputDelay).
                var keys = CopyToolKeyState.get(player.getUUID());
                if (pkt.alt && !keys.alt) CopyToolKeyState.setCooldown(player.getUUID(), 5);
                CopyToolKeyState.set(player.getUUID(), pkt.alt, pkt.ctrl);
            }
        });
    }

    public static void send(boolean alt, boolean ctrl) {
        ModPacketHandler.sendToServer(ModPacketHandler.COPY_TOOL_KEY, new CopyToolKeyPacket(alt, ctrl));
    }
}
