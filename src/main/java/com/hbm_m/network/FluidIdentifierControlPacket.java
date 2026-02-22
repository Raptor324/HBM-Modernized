package com.hbm_m.network;

import java.util.function.Supplier;

import com.hbm_m.item.IItemControlReceiver;
import com.hbm_m.item.custom.liquids.FluidIdentifierItem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * Packet for fluid identifier GUI: sends primary/secondary fluid selection to server.
 */
public class FluidIdentifierControlPacket {

    private final CompoundTag data;

    public FluidIdentifierControlPacket(CompoundTag data) {
        this.data = data;
    }

    public static void encode(FluidIdentifierControlPacket msg, FriendlyByteBuf buffer) {
        buffer.writeNbt(msg.data);
    }

    public static FluidIdentifierControlPacket decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new FluidIdentifierControlPacket(tag != null ? tag : new CompoundTag());
    }

    public static void handle(FluidIdentifierControlPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            ItemStack held = player.getMainHandItem();
            if (held.isEmpty() || !(held.getItem() instanceof IItemControlReceiver)) {
                return;
            }

            ((IItemControlReceiver) held.getItem()).receiveControl(held, msg.data);
        });
        ctx.setPacketHandled(true);
    }

    public static void send(String primary, String secondary) {
        CompoundTag tag = new CompoundTag();
        if (primary != null) tag.putString("fluid1", primary);
        if (secondary != null) tag.putString("fluid2", secondary);
        ModPacketHandler.INSTANCE.sendToServer(new FluidIdentifierControlPacket(tag));
    }
}
