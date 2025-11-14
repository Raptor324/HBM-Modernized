package com.hbm_m.network;

import net.minecraftforge.network.NetworkEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.function.Supplier;

/**
 * Пакет для синхронизации очистки точки между клиентом и сервером
 * ⭐ КРИТИЧНО: Гарантирует что имя сохраняется при очистке координат!
 */
public class ClearPointPacket {

    private int pointIndex;

    public ClearPointPacket(int pointIndex) {
        this.pointIndex = pointIndex;
    }

    public ClearPointPacket() {
        this.pointIndex = 0;
    }

    public static void encode(ClearPointPacket msg, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeInt(msg.pointIndex);
    }

    public static ClearPointPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new ClearPointPacket(buf.readInt());
    }

    public static boolean handle(ClearPointPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                handleClearPoint(player, msg.pointIndex);
            }
        });
        return true;
    }

    private static void handleClearPoint(ServerPlayer player, int pointIndex) {
        // Получаем предмет из руки
        ItemStack mainItem = player.getMainHandItem();
        ItemStack offItem = player.getOffhandItem();
        ItemStack detonatorStack = ItemStack.EMPTY;

        if (mainItem.getItem() instanceof com.hbm_m.item.MultiDetonatorItem) {
            detonatorStack = mainItem;
        } else if (offItem.getItem() instanceof com.hbm_m.item.MultiDetonatorItem) {
            detonatorStack = offItem;
        }

        if (!detonatorStack.isEmpty()) {
            com.hbm_m.item.MultiDetonatorItem detonatorItem =
                    (com.hbm_m.item.MultiDetonatorItem) detonatorStack.getItem();

            // ⭐ Вызываем clearPoint который сохраняет имя!
            detonatorItem.clearPoint(detonatorStack, pointIndex);

            // ⭐ КРИТИЧНО: Отправляем обновленный NBT на клиент для синхронизации
            // Это гарантирует что все клиенты видят одинаковое состояние предмета
            player.containerMenu.broadcastChanges();
        }
    }
}