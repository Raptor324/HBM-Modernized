package com.hbm_m.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Пакет для синхронизации ПОЛНЫХ данных точки (координаты + имя).
 * Отправляется с клиента на сервер.
 */
public class SyncPointPacket {

    private final int pointIndex;
    private final String pointName;
    private final int x, y, z;
    private final boolean hasTarget;

    public SyncPointPacket(int pointIndex, String pointName, int x, int y, int z, boolean hasTarget) {
        this.pointIndex = pointIndex;
        this.pointName = (pointName == null) ? "" : pointName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.hasTarget = hasTarget;
    }

    public SyncPointPacket() {
        this(0, "", 0, 0, 0, false);
    }

    public static void encode(SyncPointPacket msg, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeInt(msg.pointIndex);
        buf.writeUtf(msg.pointName, 16);
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

    public static boolean handle(SyncPointPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                handleSyncPoint(player, msg);
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }

    private static void handleSyncPoint(ServerPlayer player, SyncPointPacket msg) {
        if (msg.pointIndex < 0) return;

        ItemStack mainItem = player.getMainHandItem();
        ItemStack offItem = player.getOffhandItem();

        ItemStack detonatorStack = ItemStack.EMPTY;
        if (mainItem.getItem() instanceof com.hbm_m.item.MultiDetonatorItem) {
            detonatorStack = mainItem;
        } else if (offItem.getItem() instanceof com.hbm_m.item.MultiDetonatorItem) {
            detonatorStack = offItem;
        }

        if (detonatorStack.isEmpty()) return;

        CompoundTag nbt = detonatorStack.getOrCreateTag();

        ListTag pointsList;
        if (!nbt.contains("Points", Tag.TAG_LIST)) {
            pointsList = new ListTag();
            nbt.put("Points", pointsList);
        } else {
            pointsList = nbt.getList("Points", Tag.TAG_COMPOUND);
        }

        // Расширяем список до нужного индекса (сохраняя существующие точки)
        while (pointsList.size() <= msg.pointIndex) {
            CompoundTag emptyTag = new CompoundTag();
            emptyTag.putInt("X", 0);
            emptyTag.putInt("Y", 0);
            emptyTag.putInt("Z", 0);
            emptyTag.putString("Name", "");
            emptyTag.putBoolean("HasTarget", false);
            pointsList.add(emptyTag);
        }

        CompoundTag pointTag = pointsList.getCompound(msg.pointIndex);
        pointTag.putInt("X", msg.x);
        pointTag.putInt("Y", msg.y);
        pointTag.putInt("Z", msg.z);
        pointTag.putString("Name", msg.pointName);
        pointTag.putBoolean("HasTarget", msg.hasTarget);

        pointsList.set(msg.pointIndex, pointTag);
        nbt.put("Points", pointsList);

        // Принудительно синхронизируем изменения ItemStack
        player.containerMenu.broadcastChanges();
    }
}
