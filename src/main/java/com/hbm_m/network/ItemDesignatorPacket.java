package com.hbm_m.network;

import java.util.function.Supplier;

import com.hbm_m.item.ModItems;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * C2S packet for manual designator GUI buttons.
 * operator: 0 = add, 1 = subtract, 2 = set to player position
 * reference: 0 = X, 1 = Z
 */
public class ItemDesignatorPacket {

    private final int operator;
    private final int value;
    private final int reference;

    public ItemDesignatorPacket(int operator, int value, int reference) {
        this.operator = operator;
        this.value = value;
        this.reference = reference;
    }

    public ItemDesignatorPacket(FriendlyByteBuf buf) {
        this.operator = buf.readInt();
        this.value = buf.readInt();
        this.reference = buf.readInt();
    }

    public static void encode(ItemDesignatorPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.operator);
        buf.writeInt(msg.value);
        buf.writeInt(msg.reference);
    }

    public static ItemDesignatorPacket decode(FriendlyByteBuf buf) {
        return new ItemDesignatorPacket(buf);
    }

    public static void handle(ItemDesignatorPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ItemStack stack = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (stack.isEmpty() || stack.getItem() != ModItems.DESIGNATOR_MANUAL.get()) {
                return;
            }

            var tag = stack.getOrCreateTag();
            int x = tag.getInt("xCoord");
            int z = tag.getInt("zCoord");

            int result = 0;
            if (msg.operator == 0) result += msg.value;
            if (msg.operator == 1) result -= msg.value;
            if (msg.operator == 2) {
                if (msg.reference == 0) {
                    tag.putInt("xCoord", (int) Math.round(player.getX()));
                } else {
                    tag.putInt("zCoord", (int) Math.round(player.getZ()));
                }
                context.setPacketHandled(true);
                return;
            }

            if (msg.reference == 0) {
                tag.putInt("xCoord", x + result);
            } else {
                tag.putInt("zCoord", z + result);
            }
        });
        context.setPacketHandled(true);
    }
}
