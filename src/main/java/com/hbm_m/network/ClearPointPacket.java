package com.hbm_m.network;

import java.util.function.Supplier;

import com.hbm_m.item.custom.grenades_and_activators.MultiDetonatorItem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * Пакет для синхронизации очистки точки между клиентом и сервером.
 * Гарантирует что имя сохраняется при очистке координат (если clearPoint так реализован).
 */
public class ClearPointPacket {

    private final int pointIndex;

    public ClearPointPacket(int pointIndex) {
        this.pointIndex = pointIndex;
    }

    public ClearPointPacket() {
        this(0);
    }

    public static void encode(ClearPointPacket msg, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeInt(msg.pointIndex);
    }

    public static ClearPointPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new ClearPointPacket(buf.readInt());
    }

    public static boolean handle(ClearPointPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                handleClearPoint(player, msg.pointIndex);
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }

    private static void handleClearPoint(ServerPlayer player, int pointIndex) {
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

            detonatorItem.clearPoint(detonatorStack, pointIndex);

            // Принудительно синхронизируем изменения ItemStack
            player.containerMenu.broadcastChanges();
        }
    }
}
