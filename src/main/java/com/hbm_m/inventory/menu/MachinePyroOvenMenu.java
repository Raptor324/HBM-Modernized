package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachinePyroOvenBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachinePyroOvenMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachinePyroOvenBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_SLOT_START  = MACHINE_SLOT_COUNT;

    private final MachinePyroOvenBlockEntity blockEntity;

    public MachinePyroOvenMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachinePyroOvenMenu(int id, Inventory inventory, MachinePyroOvenBlockEntity blockEntity) {
        super(ModMenuTypes.PYROOVEN_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(container, MachinePyroOvenBlockEntity.SLOT_BATTERY, 152, 72));
        this.addSlot(new Slot(container, MachinePyroOvenBlockEntity.SLOT_ITEM_IN, 35, 45));
        this.addSlot(new Slot(container, MachinePyroOvenBlockEntity.SLOT_ITEM_OUT, 89, 45) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        this.addSlot(new Slot(container, MachinePyroOvenBlockEntity.SLOT_FLUID_ID, 8, 72));
        this.addSlot(new Slot(container, MachinePyroOvenBlockEntity.SLOT_UPGRADE_1, 71, 72));
        this.addSlot(new Slot(container, MachinePyroOvenBlockEntity.SLOT_UPGRADE_2, 89, 72));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 180));
        }
    }

    public static MachinePyroOvenMenu create(int id, Inventory inventory, MachinePyroOvenBlockEntity blockEntity) {
        return new MachinePyroOvenMenu(id, inventory, blockEntity);
    }

    private static MachinePyroOvenBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachinePyroOvenBlockEntity pyroBlockEntity) {
            return pyroBlockEntity;
        }
        throw new IllegalStateException("No MachinePyroOvenBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":pyrooven_menu");
    }

    public MachinePyroOvenBlockEntity getBlockEntity() {
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

        if (index < MACHINE_SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, PLAYER_SLOT_START, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FluidIdentifierItem) {
            if (!this.moveItemStackTo(stack, MachinePyroOvenBlockEntity.SLOT_FLUID_ID, MachinePyroOvenBlockEntity.SLOT_FLUID_ID + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof ItemMachineUpgrade) {
            if (!this.moveItemStackTo(stack, MachinePyroOvenBlockEntity.SLOT_UPGRADE_1, MachinePyroOvenBlockEntity.SLOT_UPGRADE_2 + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof ItemCreativeBattery) {
            if (!this.moveItemStackTo(stack, MachinePyroOvenBlockEntity.SLOT_BATTERY, MachinePyroOvenBlockEntity.SLOT_BATTERY + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, MachinePyroOvenBlockEntity.SLOT_ITEM_IN, MachinePyroOvenBlockEntity.SLOT_ITEM_IN + 1, false)
                    && !this.moveItemStackTo(stack, MachinePyroOvenBlockEntity.SLOT_BATTERY, MachinePyroOvenBlockEntity.SLOT_BATTERY + 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, stack);
        return result;
    }
}
