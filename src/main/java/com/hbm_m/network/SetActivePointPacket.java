package com.hbm_m.network;

import java.util.function.Supplier;

import com.hbm_m.item.custom.grenades_and_activators.MultiDetonatorItem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class SetActivePointPacket {

    private final int pointIndex;

    public SetActivePointPacket(int pointIndex) {
        this.pointIndex = pointIndex;
    }

    public SetActivePointPacket() {
        this(0);
    }

    public static void encode(SetActivePointPacket msg, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeInt(msg.pointIndex);
    }

    public static SetActivePointPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new SetActivePointPacket(buf.readInt());
    }

    public static boolean handle(SetActivePointPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                handleSetActivePoint(player, msg.pointIndex);
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }

    private static void handleSetActivePoint(ServerPlayer player, int pointIndex) {
        ItemStack mainItem = player.getMainHandItem();
        ItemStack offItem = player.getOffhandItem();

        ItemStack detonatorStack = ItemStack.EMPTY;
        if (mainItem.getItem() instanceof MultiDetonatorItem) {
            detonatorStack = mainItem;
        } else if (offItem.getItem() instanceof MultiDetonatorItem) {
            detonatorStack = offItem;
        }

        if (!detonatorStack.isEmpty()) {
            MultiDetonatorItem detonatorItem =
                    (MultiDetonatorItem) detonatorStack.getItem();

            detonatorItem.setActivePoint(detonatorStack, pointIndex);

            // Принудительно синхронизируем изменения ItemStack
            player.containerMenu.broadcastChanges();
        }
    }
}
