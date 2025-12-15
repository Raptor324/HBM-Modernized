package com.hbm_m.network;

import com.hbm_m.item.custom.grenades_and_activators.MultiDetonatorItem;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class SetActivePointPacket {

    private int pointIndex;

    public SetActivePointPacket(int pointIndex) {
        this.pointIndex = pointIndex;
    }

    public SetActivePointPacket() {
        this.pointIndex = 0;
    }

    public static void encode(SetActivePointPacket msg, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeInt(msg.pointIndex);
    }

    public static SetActivePointPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new SetActivePointPacket(buf.readInt());
    }

    public static boolean handle(SetActivePointPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                handleSetActivePoint(player, msg.pointIndex);
            }
        });
        return true;
    }

    private static void handleSetActivePoint(ServerPlayer player, int pointIndex) {
        // Получаем предмет из руки
        net.minecraft.world.item.ItemStack mainItem = player.getMainHandItem();
        net.minecraft.world.item.ItemStack offItem = player.getOffhandItem();
        net.minecraft.world.item.ItemStack detonatorStack = net.minecraft.world.item.ItemStack.EMPTY;

        if (mainItem.getItem() instanceof MultiDetonatorItem) {
            detonatorStack = mainItem;
        } else if (offItem.getItem() instanceof MultiDetonatorItem) {
            detonatorStack = offItem;
        }

        if (!detonatorStack.isEmpty()) {
            MultiDetonatorItem detonatorItem =
                    (MultiDetonatorItem) detonatorStack.getItem();
            detonatorItem.setActivePoint(detonatorStack, pointIndex);
        }
    }
}
