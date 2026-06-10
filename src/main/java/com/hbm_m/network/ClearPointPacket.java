package com.hbm_m.network;

import com.hbm_m.network.C2SPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import com.hbm_m.item.grenades_and_activators.MultiDetonatorItem;

public class ClearPointPacket implements C2SPacket {

    private final int pointIndex;

    public ClearPointPacket(int pointIndex) {
        this.pointIndex = pointIndex;
    }

    public ClearPointPacket() {
        this(0);
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static ClearPointPacket decode(FriendlyByteBuf buf) {
        return new ClearPointPacket(buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(pointIndex);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(ClearPointPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            ItemStack mainItem = player.getMainHandItem();
            ItemStack offItem  = player.getOffhandItem();

            ItemStack detonatorStack = ItemStack.EMPTY;
            if (mainItem.getItem() instanceof MultiDetonatorItem) {
                detonatorStack = mainItem;
            } else if (offItem.getItem() instanceof MultiDetonatorItem) {
                detonatorStack = offItem;
            }

            if (detonatorStack.isEmpty()) return;

            MultiDetonatorItem detonatorItem = (MultiDetonatorItem) detonatorStack.getItem();
            detonatorItem.clearPoint(detonatorStack, msg.pointIndex);
            player.containerMenu.broadcastChanges();
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer(int pointIndex) {
        ModPacketHandler.sendToServer(ModPacketHandler.CLEAR_POINT, new ClearPointPacket(pointIndex));
    }
}