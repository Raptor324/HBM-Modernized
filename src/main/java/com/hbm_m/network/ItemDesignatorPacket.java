package com.hbm_m.network;

import com.hbm_m.blockentity.machines.LaunchPadBaseBlockEntity;
import com.hbm_m.inventory.menu.LaunchPadLargeMenu;
import com.hbm_m.item.ModItems;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;

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

            ItemStack stack = resolveDesignatorStack(player);
            if (stack.isEmpty() || stack.getItem() != ModItems.DESIGNATOR_MANUAL.get()) {
                return;
            }
            applyOperator(stack, msg, player);
        });
    }

    private static ItemStack resolveDesignatorStack(ServerPlayer player) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu instanceof LaunchPadLargeMenu launchPadMenu) {
            LaunchPadBaseBlockEntity be = launchPadMenu.getBlockEntity();
            if (be != null) {
                ItemStack inPad = be.getInventory().getStackInSlot(be.getDesignatorSlot());
                if (!inPad.isEmpty() && inPad.getItem() == ModItems.DESIGNATOR_MANUAL.get()) {
                    return inPad;
                }
            }
        }
        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }

    private static void applyOperator(ItemStack stack, ItemDesignatorPacket msg, ServerPlayer player) {
        CompoundTag tag = stack.getOrCreateTag();
        int x = tag.getInt("xCoord");
        int z = tag.getInt("zCoord");

        if (msg.operator == 2) {
            if (msg.reference == 0) {
                tag.putInt("xCoord", (int) Math.round(player.getX()));
            } else {
                tag.putInt("zCoord", (int) Math.round(player.getZ()));
            }
            return;
        }

        int result = (msg.operator == 0) ? msg.value : -msg.value;
        if (msg.reference == 0) {
            tag.putInt("xCoord", x + result);
        } else {
            tag.putInt("zCoord", z + result);
        }
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer(int operator, int value, int reference) {
        ModPacketHandler.sendToServer(ModPacketHandler.ITEM_DESIGNATOR,
                new ItemDesignatorPacket(operator, value, reference));
    }
}