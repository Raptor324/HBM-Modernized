package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineMassStorageBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Port of {@code ContainerMassStorage} (1.7.10 Original). */
public class MachineMassStorageMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachineMassStorageBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_SLOT_START = MACHINE_SLOT_COUNT;

    private final MachineMassStorageBlockEntity blockEntity;

    public MachineMassStorageMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineMassStorageMenu(int id, Inventory inventory, MachineMassStorageBlockEntity blockEntity) {
        super(ModMenuTypes.MACHINE_MASS_STORAGE_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);
        this.addSlot(new Slot(container, MachineMassStorageBlockEntity.SLOT_INPUT, 61, 17));
        this.addSlot(new Slot(container, MachineMassStorageBlockEntity.SLOT_FILTER, 61, 53));
        this.addSlot(new Slot(container, MachineMassStorageBlockEntity.SLOT_OUTPUT, 61, 89) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 139 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 197));
        }
    }

    public static MachineMassStorageMenu create(int id, Inventory inventory, MachineMassStorageBlockEntity blockEntity) {
        return new MachineMassStorageMenu(id, inventory, blockEntity);
    }

    private static MachineMassStorageBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineMassStorageBlockEntity massStorageBlockEntity) {
            return massStorageBlockEntity;
        }
        throw new IllegalStateException("No MachineMassStorageBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":mass_storage_menu");
    }

    public MachineMassStorageBlockEntity getBlockEntity() {
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
        } else {
            if (!this.moveItemStackTo(stack, MachineMassStorageBlockEntity.SLOT_INPUT, MachineMassStorageBlockEntity.SLOT_INPUT + 1, false)) {
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
