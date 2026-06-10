package com.hbm_m.inventory.menu;

import com.hbm_m.block.entity.machines.MachineTurbineBlockEntity;
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

public class MachineTurbineMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachineTurbineBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_SLOT_START  = MACHINE_SLOT_COUNT;

    private final MachineTurbineBlockEntity blockEntity;
    private final ModItemStackHandlerContainer machineContainer;

    public MachineTurbineMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineTurbineMenu(int id, Inventory inventory, MachineTurbineBlockEntity blockEntity) {
        super(ModMenuTypes.TURBINE_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.machineContainer = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(machineContainer, MachineTurbineBlockEntity.SLOT_FLUID_ID_IN,   8,  17));
        this.addSlot(new TakeOnlySlot(machineContainer, MachineTurbineBlockEntity.SLOT_FLUID_ID_OUT,  8,  53));
        this.addSlot(new Slot(machineContainer, MachineTurbineBlockEntity.SLOT_INPUT_IO_IN,  44,  17));
        this.addSlot(new TakeOnlySlot(machineContainer, MachineTurbineBlockEntity.SLOT_INPUT_IO_OUT, 44,  53));
        this.addSlot(new Slot(machineContainer, MachineTurbineBlockEntity.SLOT_BATTERY,      98,  53));
        this.addSlot(new Slot(machineContainer, MachineTurbineBlockEntity.SLOT_OUTPUT_IO_IN, 152, 17));
        this.addSlot(new TakeOnlySlot(machineContainer, MachineTurbineBlockEntity.SLOT_OUTPUT_IO_OUT, 152, 53));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public static MachineTurbineMenu create(int id, Inventory inventory, MachineTurbineBlockEntity blockEntity) {
        return new MachineTurbineMenu(id, inventory, blockEntity);
    }

    private static MachineTurbineBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineTurbineBlockEntity turbineBlockEntity) {
            return turbineBlockEntity;
        }
        throw new IllegalStateException("No MachineTurbineBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":turbine_menu");
    }

    public MachineTurbineBlockEntity getBlockEntity() {
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

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(stack, PLAYER_SLOT_START, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, MachineTurbineBlockEntity.SLOT_BATTERY, MachineTurbineBlockEntity.SLOT_BATTERY + 1, false)
                    && !this.moveItemStackTo(stack, MachineTurbineBlockEntity.SLOT_INPUT_IO_IN, MachineTurbineBlockEntity.SLOT_INPUT_IO_IN + 1, false)
                    && !this.moveItemStackTo(stack, MachineTurbineBlockEntity.SLOT_OUTPUT_IO_IN, MachineTurbineBlockEntity.SLOT_OUTPUT_IO_IN + 1, false)
                    && !this.moveItemStackTo(stack, MachineTurbineBlockEntity.SLOT_FLUID_ID_IN, MachineTurbineBlockEntity.SLOT_FLUID_ID_IN + 1, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            slot.onTake(player, stack);
        }

        return result;
    }

    private static final class TakeOnlySlot extends Slot {
        private TakeOnlySlot(net.minecraft.world.Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
