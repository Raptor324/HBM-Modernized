package com.hbm_m.network;

import com.hbm_m.item.ModItems;
import com.hbm_m.network.C2SPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class ItemDesignatorPacket implements C2SPacket {

    private final int operator;
    private final int value;
    private final int reference;

    public ItemDesignatorPacket(int operator, int value, int reference) {
        this.operator  = operator;
        this.value     = value;
        this.reference = reference;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static ItemDesignatorPacket decode(FriendlyByteBuf buf) {
        return new ItemDesignatorPacket(buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(operator);
        buf.writeInt(value);
        buf.writeInt(reference);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(ItemDesignatorPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (stack.isEmpty() || stack.getItem() != ModItems.DESIGNATOR_MANUAL.get()) return;

            CompoundTag tag = stack.getOrCreateTag();
            int x = tag.getInt("xCoord");
            int z = tag.getInt("zCoord");

            // operator: 0=add, 1=subtract, 2=set to player position
            if (msg.operator == 2) {
                if (msg.reference == 0) tag.putInt("xCoord", (int) Math.round(player.getX()));
                else                    tag.putInt("zCoord", (int) Math.round(player.getZ()));
                return;
            }

            int result = (msg.operator == 0) ? msg.value : -msg.value;
            if (msg.reference == 0) tag.putInt("xCoord", x + result);
            else                    tag.putInt("zCoord", z + result);
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer(int operator, int value, int reference) {
        ModPacketHandler.sendToServer(ModPacketHandler.ITEM_DESIGNATOR,
                new ItemDesignatorPacket(operator, value, reference));
    }
}