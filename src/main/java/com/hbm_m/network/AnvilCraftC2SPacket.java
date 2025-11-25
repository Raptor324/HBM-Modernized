package com.hbm_m.network;

import com.hbm_m.menu.AnvilMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AnvilCraftC2SPacket {
    private final BlockPos pos;
    private final boolean craftAll;

    public AnvilCraftC2SPacket(BlockPos pos, boolean craftAll) {
        this.pos = pos;
        this.craftAll = craftAll;
    }

    public AnvilCraftC2SPacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readBoolean());
    }

    public static void encode(AnvilCraftC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeBoolean(packet.craftAll);
    }

    public static AnvilCraftC2SPacket decode(FriendlyByteBuf buffer) {
        return new AnvilCraftC2SPacket(buffer);
    }

    public static void handle(AnvilCraftC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (player.containerMenu instanceof AnvilMenu menu &&
                    menu.blockEntity.getBlockPos().equals(packet.pos)) {
                menu.tryCraft(player, packet.craftAll);
            }
        });
        context.setPacketHandled(true);
    }
}

