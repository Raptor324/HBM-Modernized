package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineSolidifierBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
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

public class MachineSolidifierMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachineSolidifierBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_SLOT_START  = MACHINE_SLOT_COUNT;

    private final MachineSolidifierBlockEntity blockEntity;

    public MachineSolidifierMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineSolidifierMenu(int id, Inventory inventory, MachineSolidifierBlockEntity blockEntity) {
        super(ModMenuTypes.SOLIDIFIER_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(container, MachineSolidifierBlockEntity.SLOT_OUTPUT, 71, 45) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        this.addSlot(new Slot(container, MachineSolidifierBlockEntity.SLOT_BATTERY, 134, 72));
        this.addSlot(new Slot(container, MachineSolidifierBlockEntity.SLOT_UPGRADE_1, 98, 36));
        this.addSlot(new Slot(container, MachineSolidifierBlockEntity.SLOT_UPGRADE_2, 98, 54));
        this.addSlot(new Slot(container, MachineSolidifierBlockEntity.SLOT_FLUID_ID, 71, 72));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 180));
        }
    }

    public static MachineSolidifierMenu create(int id, Inventory inventory, MachineSolidifierBlockEntity blockEntity) {
        return new MachineSolidifierMenu(id, inventory, blockEntity);
    }

    private static MachineSolidifierBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineSolidifierBlockEntity solidifierBlockEntity) {
            return solidifierBlockEntity;
        }
        throw new IllegalStateException("No MachineSolidifierBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":solidifier_menu");
    }

    public MachineSolidifierBlockEntity getBlockEntity() {
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
            if (!this.moveItemStackTo(stack, MachineSolidifierBlockEntity.SLOT_FLUID_ID, MachineSolidifierBlockEntity.SLOT_FLUID_ID + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof ItemMachineUpgrade) {
            if (!this.moveItemStackTo(stack, MachineSolidifierBlockEntity.SLOT_UPGRADE_1, MachineSolidifierBlockEntity.SLOT_UPGRADE_2 + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, MachineSolidifierBlockEntity.SLOT_BATTERY, MachineSolidifierBlockEntity.SLOT_BATTERY + 1, false)) {
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
