package com.hbm_m.network;

import com.hbm_m.interfaces.IItemControlReceiver;
import com.hbm_m.network.C2SPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class FluidIdentifierControlPacket implements C2SPacket {

    private final CompoundTag data;

    public FluidIdentifierControlPacket(CompoundTag data) {
        this.data = data != null ? data : new CompoundTag();
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static FluidIdentifierControlPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new FluidIdentifierControlPacket(tag);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(FluidIdentifierControlPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            ItemStack held = player.getMainHandItem();
            if (held.isEmpty() || !(held.getItem() instanceof IItemControlReceiver receiver)) return;

            receiver.receiveControl(held, msg.data);
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void send(String primary, String secondary) {
        CompoundTag tag = new CompoundTag();
        if (primary   != null) tag.putString("fluid1", primary);
        if (secondary != null) tag.putString("fluid2", secondary);
        ModPacketHandler.sendToServer(ModPacketHandler.FLUID_IDENTIFIER_CTRL,
                new FluidIdentifierControlPacket(tag));
    }
}