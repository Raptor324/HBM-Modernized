package com.hbm_m.network;

import com.hbm_m.item.grenades_and_activators.MultiDetonatorItem;
import com.hbm_m.network.C2SPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class SyncPointPacket implements C2SPacket {

    private final int     pointIndex;
    private final String  pointName;
    private final int     x, y, z;
    private final boolean hasTarget;

    public SyncPointPacket(int pointIndex, String pointName, int x, int y, int z, boolean hasTarget) {
        this.pointIndex = pointIndex;
        this.pointName  = (pointName == null) ? "" : pointName;
        this.x          = x;
        this.y          = y;
        this.z          = z;
        this.hasTarget  = hasTarget;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static SyncPointPacket decode(FriendlyByteBuf buf) {
        return new SyncPointPacket(
                buf.readInt(),
                buf.readUtf(16),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(pointIndex);
        buf.writeUtf(pointName, 16);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeBoolean(hasTarget);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(SyncPointPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            if (msg.pointIndex < 0) return;

            ItemStack mainItem = player.getMainHandItem();
            ItemStack offItem  = player.getOffhandItem();

            ItemStack detonatorStack = ItemStack.EMPTY;
            if (mainItem.getItem() instanceof MultiDetonatorItem) detonatorStack = mainItem;
            else if (offItem.getItem() instanceof MultiDetonatorItem) detonatorStack = offItem;

            if (detonatorStack.isEmpty()) return;

            CompoundTag nbt = detonatorStack.getOrCreateTag();

            ListTag pointsList;
            if (!nbt.contains("Points", Tag.TAG_LIST)) {
                pointsList = new ListTag();
                nbt.put("Points", pointsList);
            } else {
                pointsList = nbt.getList("Points", Tag.TAG_COMPOUND);
            }

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

            player.containerMenu.broadcastChanges();
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer(int pointIndex, String pointName, int x, int y, int z, boolean hasTarget) {
        ModPacketHandler.sendToServer(ModPacketHandler.SYNC_POINT,
                new SyncPointPacket(pointIndex, pointName, x, y, z, hasTarget));
    }
}