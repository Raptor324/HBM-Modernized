package com.hbm_m.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import com.hbm_m.inventory.menu.AnvilMenu;

import dev.architectury.networking.NetworkManager.PacketContext;

public class AnvilCraftC2SPacket implements C2SPacket {
    private final BlockPos pos;
    private final boolean craftAll;

    public AnvilCraftC2SPacket(BlockPos pos, boolean craftAll) {
        this.pos = pos;
        this.craftAll = craftAll;
    }

    public static AnvilCraftC2SPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        boolean craftAll = buffer.readBoolean();
        return new AnvilCraftC2SPacket(pos, craftAll);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeBoolean(this.craftAll);
    }

    public static void handle(AnvilCraftC2SPacket packet, PacketContext context) {
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) {
                return;
            }
            if (player.containerMenu instanceof AnvilMenu menu &&
                    menu.blockEntity.getBlockPos().equals(packet.pos)) {
                menu.tryCraft(player, packet.craftAll);
            }
        });
    }
}