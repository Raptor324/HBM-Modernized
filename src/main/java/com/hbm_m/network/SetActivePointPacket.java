package com.hbm_m.network;

import com.hbm_m.item.grenades_and_activators.MultiDetonatorItem;
import com.hbm_m.network.C2SPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class SetActivePointPacket implements C2SPacket {

    private final int pointIndex;

    public SetActivePointPacket(int pointIndex) {
        this.pointIndex = pointIndex;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static SetActivePointPacket decode(FriendlyByteBuf buf) {
        return new SetActivePointPacket(buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(pointIndex);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(SetActivePointPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            ItemStack mainItem = player.getMainHandItem();
            ItemStack offItem  = player.getOffhandItem();

            ItemStack detonatorStack = ItemStack.EMPTY;
            if (mainItem.getItem() instanceof MultiDetonatorItem) detonatorStack = mainItem;
            else if (offItem.getItem() instanceof MultiDetonatorItem) detonatorStack = offItem;

            if (detonatorStack.isEmpty()) return;

            MultiDetonatorItem detonatorItem = (MultiDetonatorItem) detonatorStack.getItem();
            detonatorItem.setActivePoint(detonatorStack, msg.pointIndex);
            player.containerMenu.broadcastChanges();
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer(int pointIndex) {
        ModPacketHandler.sendToServer(ModPacketHandler.SET_ACTIVE_POINT,
                new SetActivePointPacket(pointIndex));
    }
}