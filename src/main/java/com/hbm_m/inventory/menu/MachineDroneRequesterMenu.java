package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.network.MachineDroneRequesterBlockEntity;
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

public class MachineDroneRequesterMenu extends AbstractContainerMenu {

    private static final int FILTER_START = MachineDroneRequesterBlockEntity.FILTER_START;
    private static final int FILTER_END = MachineDroneRequesterBlockEntity.FILTER_END;
    private static final int STOCK_START = MachineDroneRequesterBlockEntity.STOCK_START;
    private static final int STOCK_END = MachineDroneRequesterBlockEntity.STOCK_END;
    private static final int MACHINE_SLOT_COUNT = MachineDroneRequesterBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_SLOT_START = MACHINE_SLOT_COUNT;

    private final MachineDroneRequesterBlockEntity blockEntity;

    public MachineDroneRequesterMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineDroneRequesterMenu(int id, Inventory inventory, MachineDroneRequesterBlockEntity blockEntity) {
        super(ModMenuTypes.DRONE_REQUESTER_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int filterIndex = col + row * 3 + FILTER_START;
                this.addSlot(new Slot(container, filterIndex, 98 + col * 18, 17 + row * 18) {
                    @Override
                    public void set(ItemStack stack) {
                        super.set(stack);
                        blockEntity.getMatcher().initPattern(filterIndex, stack);
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(container, col + row * 3 + STOCK_START, 26 + col * 18, 17 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 161));
        }
    }

    public static MachineDroneRequesterMenu create(int id, Inventory inventory, MachineDroneRequesterBlockEntity blockEntity) {
        return new MachineDroneRequesterMenu(id, inventory, blockEntity);
    }

    private static MachineDroneRequesterBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineDroneRequesterBlockEntity requesterBlockEntity) {
            return requesterBlockEntity;
        }
        throw new IllegalStateException("No MachineDroneRequesterBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":drone_requester_menu");
    }

    public MachineDroneRequesterBlockEntity getBlockEntity() {
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

    /** Filter slots (0-8) are never shift-click transferable, matching the original. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= FILTER_START && index <= FILTER_END) return ItemStack.EMPTY;

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
            if (!this.moveItemStackTo(stack, STOCK_START, STOCK_END + 1, false)) {
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
