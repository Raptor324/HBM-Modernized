package com.hbm_m.network;

import com.hbm_m.item.custom.grenades_and_activators.MultiDetonatorItem;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.function.Supplier;

/**
 * ⭐ Пакет для синхронизации ПОЛНЫХ данных точки (координаты + имя)
 * Отправляется с КЛИЕНТА на СЕРВЕР когда игрок вводит название в GUI
 */
public class SyncPointPacket {

    private int pointIndex;
    private String pointName;
    private int x, y, z;
    private boolean hasTarget;

    public SyncPointPacket(int pointIndex, String pointName, int x, int y, int z, boolean hasTarget) {
        this.pointIndex = pointIndex;
        this.pointName = pointName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.hasTarget = hasTarget;
    }

    public SyncPointPacket() {
        this.pointIndex = 0;
        this.pointName = "";
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.hasTarget = false;
    }

    public static void encode(SyncPointPacket msg, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeInt(msg.pointIndex);
        buf.writeUtf(msg.pointName);
        buf.writeInt(msg.x);
        buf.writeInt(msg.y);
        buf.writeInt(msg.z);
        buf.writeBoolean(msg.hasTarget);
    }

    public static SyncPointPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new SyncPointPacket(
                buf.readInt(),
                buf.readUtf(16),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    public static boolean handle(SyncPointPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                handleSyncPoint(player, msg);
            }
        });
        return true;
    }

    private static void handleSyncPoint(ServerPlayer player, SyncPointPacket msg) {
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

            if (!detonatorStack.hasTag()) {
                detonatorStack.setTag(new CompoundTag());
            }

            CompoundTag nbt = detonatorStack.getTag();

            // Инициализируем список точек если его нет
            if (!nbt.contains("Points", Tag.TAG_LIST)) {
                nbt.put("Points", new ListTag());
            }

            ListTag pointsList = nbt.getList("Points", Tag.TAG_COMPOUND);

            // ⭐ ГЛАВНОЕ: Расширяем список если нужно, СОХРАНЯЯ существующие точки
            while (pointsList.size() <= msg.pointIndex) {
                CompoundTag emptyTag = new CompoundTag();
                emptyTag.putInt("X", 0);
                emptyTag.putInt("Y", 0);
                emptyTag.putInt("Z", 0);
                emptyTag.putString("Name", "");
                emptyTag.putBoolean("HasTarget", false);
                pointsList.add(emptyTag);
            }

            // ⭐ КРИТИЧНО: Получаем СУЩЕСТВУЮЩУЮ точку и обновляем её поля
            CompoundTag pointTag = pointsList.getCompound(msg.pointIndex);

            // Обновляем ВСЕ данные точки
            pointTag.putInt("X", msg.x);
            pointTag.putInt("Y", msg.y);
            pointTag.putInt("Z", msg.z);
            pointTag.putString("Name", msg.pointName);
            pointTag.putBoolean("HasTarget", msg.hasTarget);

            // Возвращаем обновлённую точку в список
            pointsList.set(msg.pointIndex, pointTag);
            nbt.put("Points", pointsList);
        }
    }
}
