package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineCyclotronBlockEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachineCyclotronMenu extends AbstractContainerMenu {

    private final MachineCyclotronBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final HandlerContainer machineInventory;

    private static final int SLOT_INPUT_START = 0;
    private static final int SLOT_INPUT_END_EXCLUSIVE = 3;
    private static final int SLOT_TARGET_START = 3;
    private static final int SLOT_TARGET_END_EXCLUSIVE = 6;
    private static final int SLOT_OUTPUT_START = 6;
    private static final int SLOT_OUTPUT_END_EXCLUSIVE = 9;
    private static final int SLOT_BATTERY = 9;
    private static final int SLOT_UPGRADE_START = 10;
    private static final int SLOT_UPGRADE_END_EXCLUSIVE = 12;
    private static final int SLOT_MACHINE_END_EXCLUSIVE = 12;

    public MachineCyclotronMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineCyclotronMenu(int id, Inventory inventory, MachineCyclotronBlockEntity blockEntity) {
        super(ModMenuTypes.CYCLOTRON_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.machineInventory = new HandlerContainer(blockEntity.getInventory());

        // Input (rendered on the left, as in the 1.7.10 original)
        this.addSlot(new Slot(machineInventory, 0, 11, 18));
        this.addSlot(new Slot(machineInventory, 1, 11, 36));
        this.addSlot(new Slot(machineInventory, 2, 11, 54));

        // Targets (rendered on the right, as in the 1.7.10 original)
        this.addSlot(new Slot(machineInventory, 3, 101, 18));
        this.addSlot(new Slot(machineInventory, 4, 101, 36));
        this.addSlot(new Slot(machineInventory, 5, 101, 54));

        // Output (take only)
        this.addSlot(new TakeOnlySlot(machineInventory, 6, 131, 18));
        this.addSlot(new TakeOnlySlot(machineInventory, 7, 131, 36));
        this.addSlot(new TakeOnlySlot(machineInventory, 8, 131, 54));

        // Battery
        this.addSlot(new Slot(machineInventory, 9, 168, 83));

        // Upgrades
        this.addSlot(new UpgradeSlot(machineInventory, 10, 60, 81));
        this.addSlot(new UpgradeSlot(machineInventory, 11, 78, 81));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 15 + col * 18, 133 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 15 + col * 18, 191));
        }
    }

    public static MachineCyclotronMenu create(int id, Inventory inventory, MachineCyclotronBlockEntity blockEntity) {
        return new MachineCyclotronMenu(id, inventory, blockEntity);
    }

    private static MachineCyclotronBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineCyclotronBlockEntity cyclotronBlockEntity) {
            return cyclotronBlockEntity;
        }
        throw new IllegalStateException("No MachineCyclotronBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":cyclotron_menu");
    }

    public MachineCyclotronBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, this.blockEntity.getBlockState().getBlock());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            // From machine to player inventory
            if (index < SLOT_MACHINE_END_EXCLUSIVE) {
                if (!this.moveItemStackTo(stack, SLOT_MACHINE_END_EXCLUSIVE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player inventory to machine
                if (isBattery(stack)) {
                    if (!this.moveItemStackTo(stack, SLOT_BATTERY, SLOT_BATTERY + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isUpgrade(stack)) {
                    if (!this.moveItemStackTo(stack, SLOT_UPGRADE_START, SLOT_UPGRADE_END_EXCLUSIVE, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, SLOT_INPUT_START, SLOT_INPUT_END_EXCLUSIVE, false)
                        && !this.moveItemStackTo(stack, SLOT_TARGET_START, SLOT_TARGET_END_EXCLUSIVE, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    private static boolean isBattery(ItemStack stack) {
        return stack.is(ModItems.CREATIVE_BATTERY.get())
                || MachineCyclotronBlockEntity.isCyclotronBatteryCandidate(stack);
    }

    private static boolean isUpgrade(ItemStack stack) {
        return stack.getItem() instanceof ItemMachineUpgrade;
    }

    private static final class UpgradeSlot extends Slot {
        private UpgradeSlot(net.minecraft.world.Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isUpgrade(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static final class TakeOnlySlot extends Slot {
        private TakeOnlySlot(net.minecraft.world.Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    private static final class HandlerContainer implements net.minecraft.world.Container {
        private final ModItemStackHandler handler;

        private HandlerContainer(ModItemStackHandler handler) {
            this.handler = handler;
        }

        @Override
        public int getContainerSize() {
            return handler.getSlots();
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack existing = handler.getStackInSlot(slot);
            if (existing.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack split = existing.split(amount);
            handler.setStackInSlot(slot, existing);
            setChanged();
            return split;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack existing = handler.getStackInSlot(slot);
            handler.setStackInSlot(slot, ItemStack.EMPTY);
            return existing;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            handler.setStackInSlot(slot, stack);
            setChanged();
        }

        @Override
        public void setChanged() {
            // Changes are tracked by the wrapped handler.
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < handler.getSlots(); i++) {
                handler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return handler.isItemValid(slot, stack);
        }
    }
}