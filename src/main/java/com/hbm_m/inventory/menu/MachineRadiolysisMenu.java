package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineRadiolysisBlockEntity;

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

/** Slot-Koordinaten angelehnt an {@code ContainerRadiolysis} (1.7.10 Original): Fluid-ID (34,17),
 *  Batterie (8,53). RTG- und Sterilisations-Slots des Originals entfallen (siehe
 *  {@link MachineRadiolysisBlockEntity}). */
public class MachineRadiolysisMenu extends AbstractContainerMenu {

    private final MachineRadiolysisBlockEntity blockEntity;
    private static final int MACHINE_SLOTS = 2;

    public MachineRadiolysisMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf));
    }

    public MachineRadiolysisMenu(int id, Inventory inv, MachineRadiolysisBlockEntity be) {
        super(ModMenuTypes.RADIOLYSIS_MENU.get(), id);
        this.blockEntity = be;

        //? if forge {
        var handler = be.getInventory();
        addSlot(new SlotItemHandler(handler, MachineRadiolysisBlockEntity.SLOT_FLUID_ID, 34, 17));
        addSlot(new SlotItemHandler(handler, MachineRadiolysisBlockEntity.SLOT_BATTERY, 8, 53));
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

    public static MachineRadiolysisMenu create(int id, Inventory inv, MachineRadiolysisBlockEntity be) {
        return new MachineRadiolysisMenu(id, inv, be);
    }

    private static MachineRadiolysisBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof MachineRadiolysisBlockEntity r) return r;
        throw new IllegalStateException("No MachineRadiolysisBlockEntity at " + pos);
    }

    public MachineRadiolysisBlockEntity getBlockEntity() { return blockEntity; }

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
            if (!moveItemStackTo(stack, MachineRadiolysisBlockEntity.SLOT_BATTERY, MachineRadiolysisBlockEntity.SLOT_BATTERY + 1, false)
             && !moveItemStackTo(stack, MachineRadiolysisBlockEntity.SLOT_FLUID_ID, MachineRadiolysisBlockEntity.SLOT_FLUID_ID + 1, false))
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
