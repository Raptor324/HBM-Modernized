package com.hbm_m.network;

import com.hbm_m.blockentity.bomb.NukeFstbmbBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S для кнопки «Start» балефайр-бомбы.
 *
 * The GUI called startCountdown() straight from the button lambda, i.e. on the client only:
 * the server never set {@code started}, so its tick returned immediately and the bomb never
 * went off. Patterned on {@link SoyuzLauncherControlPacket}.
 */
public class NukeFstbmbControlPacket implements C2SPacket {

    private final BlockPos pos;

    public NukeFstbmbControlPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static NukeFstbmbControlPacket decode(FriendlyByteBuf buf) {
        return new NukeFstbmbControlPacket(buf.readBlockPos());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public static void handle(NukeFstbmbControlPacket pkt, PacketContext ctx) {
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer player)) return;
            if (ModPacketHandler.blockEntityAt(player, pkt.pos) instanceof NukeFstbmbBlockEntity bomb) {
                bomb.startCountdown();
            }
        });
    }
}
