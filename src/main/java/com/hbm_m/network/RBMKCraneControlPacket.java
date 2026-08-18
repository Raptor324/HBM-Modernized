package com.hbm_m.network;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S packet reporting the client's currently-held crane movement keys, sent every client tick
 * the state changes (see {@code ModConfigKeybindHandler}). Matches the original's continuous
 * key-hold reporting via {@code HbmPlayerProps}, just re-expressed as an explicit packet since
 * this port has no equivalent synced player-capability system.
 */
public class RBMKCraneControlPacket implements C2SPacket {

    private final boolean up, down, left, right, load;

    public RBMKCraneControlPacket(boolean up, boolean down, boolean left, boolean right, boolean load) {
        this.up = up; this.down = down; this.left = left; this.right = right; this.load = load;
    }

    public static RBMKCraneControlPacket decode(FriendlyByteBuf buf) {
        return new RBMKCraneControlPacket(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(up);
        buf.writeBoolean(down);
        buf.writeBoolean(left);
        buf.writeBoolean(right);
        buf.writeBoolean(load);
    }

    public static void handle(RBMKCraneControlPacket pkt, PacketContext ctx) {
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer player)) return;
            RBMKCraneKeyState.set(player.getUUID(), pkt.up, pkt.down, pkt.left, pkt.right, pkt.load);
        });
    }

    public static void send(boolean up, boolean down, boolean left, boolean right, boolean load) {
        ModPacketHandler.sendToServer(ModPacketHandler.RBMK_CRANE_CONTROL,
                new RBMKCraneControlPacket(up, down, left, right, load));
    }
}
