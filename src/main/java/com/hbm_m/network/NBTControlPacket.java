package com.hbm_m.network;

import com.hbm_m.api.tile.IControlReceiver;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** 1:1 {@code NBTControlPacket}: GUI -> Blockentity, geprueft ueber {@link IControlReceiver#hasPermission}. */
public class NBTControlPacket implements C2SPacket {

    private final BlockPos pos;
    private final CompoundTag data;

    public NBTControlPacket(BlockPos pos, CompoundTag data) {
        this.pos = pos;
        this.data = data != null ? data : new CompoundTag();
    }

    public static NBTControlPacket decode(FriendlyByteBuf buf) {
        return new NBTControlPacket(buf.readBlockPos(), buf.readNbt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeNbt(data);
    }

    public static void handle(NBTControlPacket packet, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            if (player.level().getBlockEntity(packet.pos) instanceof IControlReceiver rec && rec.hasPermission(player))
                rec.receiveControl(packet.data);
        });
    }

    public static void sendToServer(BlockPos pos, CompoundTag data) {
        ModPacketHandler.sendToServer(ModPacketHandler.NBT_CONTROL, new NBTControlPacket(pos, data));
    }
}
