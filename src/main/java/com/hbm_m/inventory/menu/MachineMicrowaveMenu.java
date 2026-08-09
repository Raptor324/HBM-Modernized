package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineMicrowaveBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if forge {
import net.minecraftforge.items.SlotItemHandler;
//?}

/** Slot-Koordinaten 1:1 aus {@code ContainerMicrowave} (1.7.10 Original): Input (80,35),
 *  Output (140,35, extraktionsonly), Batterie (8,53). */
public class MachineMicrowaveMenu extends AbstractContainerMenu {

    private final MachineMicrowaveBlockEntity blockEntity;
    private static final int MACHINE_SLOTS = 3;

    public MachineMicrowaveMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf));
    }

    public MachineMicrowaveMenu(int id, Inventory inv, MachineMicrowaveBlockEntity be) {
        super(ModMenuTypes.MICROWAVE_MENU.get(), id);
        this.blockEntity = be;

        //? if forge {
        var handler = be.getInventory();
        addSlot(new SlotItemHandler(handler, MachineMicrowaveBlockEntity.SLOT_INPUT, 80, 35));
        addSlot(new SlotItemHandler(handler, MachineMicrowaveBlockEntity.SLOT_OUTPUT, 140, 35) {
            @Override public boolean mayPlace(ItemStack s) { return false; }
        });
        addSlot(new SlotItemHandler(handler, MachineMicrowaveBlockEntity.SLOT_BATTERY, 8, 53));
        //?}

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    public static MachineMicrowaveMenu create(int id, Inventory inv, MachineMicrowaveBlockEntity be) {
        return new MachineMicrowaveMenu(id, inv, be);
    }

    private static MachineMicrowaveBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof MachineMicrowaveBlockEntity m) return m;
        throw new IllegalStateException("No MachineMicrowaveBlockEntity at " + pos);
    }

    public MachineMicrowaveBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() == player.level()
            && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, MachineMicrowaveBlockEntity.SLOT_INPUT, MachineMicrowaveBlockEntity.SLOT_INPUT + 1, false)
             && !moveItemStackTo(stack, MachineMicrowaveBlockEntity.SLOT_BATTERY, MachineMicrowaveBlockEntity.SLOT_BATTERY + 1, false))
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
