package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineOilWellBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachineOilWellMenu extends AbstractContainerMenu {

    private final MachineOilWellBlockEntity blockEntity;

    public MachineOilWellMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineOilWellMenu(int id, Inventory inventory, MachineOilWellBlockEntity blockEntity) {
        super(ModMenuTypes.MACHINE_WELL_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        // slot 0: battery
        this.addSlot(new Slot(container, 0, 8, 53));
        // slot 1: oil canister input
        this.addSlot(new Slot(container, 1, 80, 17));
        // slot 2: oil canister output (take-only)
        this.addSlot(new Slot(container, 2, 80, 53) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        // slot 3: gas canister input
        this.addSlot(new Slot(container, 3, 125, 17));
        // slot 4: gas canister output (take-only)
        this.addSlot(new Slot(container, 4, 125, 53) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        // slots 5-7: upgrades
        this.addSlot(new Slot(container, 5, 152, 17));
        this.addSlot(new Slot(container, 6, 152, 35));
        this.addSlot(new Slot(container, 7, 152, 53));

        // player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public static MachineOilWellMenu create(int id, Inventory inventory, MachineOilWellBlockEntity blockEntity) {
        return new MachineOilWellMenu(id, inventory, blockEntity);
    }

    private static MachineOilWellBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineOilWellBlockEntity wellBlockEntity) {
            return wellBlockEntity;
        }
        throw new IllegalStateException("No MachineOilWellBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":machine_well_menu");
    }

    public MachineOilWellBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() != player.level()) {
            return false;
        }
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index <= 7) {
            if (!this.moveItemStackTo(stack, 8, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (stack.getItem() instanceof ItemMachineUpgrade) {
                if (!this.moveItemStackTo(stack, 5, 8, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stack, 0, 2, false)) {
                    if (!this.moveItemStackTo(stack, 3, 5, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }
}
