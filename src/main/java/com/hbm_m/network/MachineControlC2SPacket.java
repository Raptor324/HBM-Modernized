package com.hbm_m.network;

import com.hbm_m.interfaces.IControlReceiver;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * C2S: произвольные управляющие NBT-команды машины из её GUI - порт
 * {@code NBTControlPacket} (1.7.10). Клапан/поджиг факела ("valve"/"dial"),
 * зажигание/дроссель двигателя ("turnOn"/"setting") и т.п.
 */
public class MachineControlC2SPacket implements C2SPacket {

    private final BlockPos pos;
    private final CompoundTag data;

    public MachineControlC2SPacket(BlockPos pos, CompoundTag data) {
        this.pos = pos;
        this.data = data;
    }

    public static MachineControlC2SPacket decode(FriendlyByteBuf buf) {
        return new MachineControlC2SPacket(buf.readBlockPos(), buf.readNbt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeNbt(data);
    }

    public static void handle(MachineControlC2SPacket pkt, PacketContext ctx) {
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer player)) return;
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(pkt.pos);
            if (!(be instanceof IControlReceiver receiver)) return;

            Player accessor = player;
            if (!receiver.hasPermission(accessor)) return;

            receiver.receiveControl(pkt.data);
        });
    }

    public static void send(BlockPos pos, CompoundTag data) {
        ModPacketHandler.sendToServer(ModPacketHandler.MACHINE_CONTROL, new MachineControlC2SPacket(pos, data));
    }
}
