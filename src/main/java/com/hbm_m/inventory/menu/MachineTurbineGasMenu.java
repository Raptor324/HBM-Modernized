package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineTurbineGasBlockEntity;
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

public class MachineTurbineGasMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachineTurbineGasBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_SLOT_START  = MACHINE_SLOT_COUNT;

    private final MachineTurbineGasBlockEntity blockEntity;
    private final ModItemStackHandlerContainer machineContainer;

    public MachineTurbineGasMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineTurbineGasMenu(int id, Inventory inventory, MachineTurbineGasBlockEntity blockEntity) {
        super(ModMenuTypes.TURBINEGAS_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.machineContainer = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(machineContainer, MachineTurbineGasBlockEntity.SLOT_BATTERY,  8, 109));
        this.addSlot(new Slot(machineContainer, MachineTurbineGasBlockEntity.SLOT_FLUID_ID, 36,  17));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 141 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 199));
        }
    }

    public static MachineTurbineGasMenu create(int id, Inventory inventory, MachineTurbineGasBlockEntity blockEntity) {
        return new MachineTurbineGasMenu(id, inventory, blockEntity);
    }

    private static MachineTurbineGasBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineTurbineGasBlockEntity turbineBlockEntity) {
            return turbineBlockEntity;
        }
        throw new IllegalStateException("No MachineTurbineGasBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":turbinegas_menu");
    }

    public MachineTurbineGasBlockEntity getBlockEntity() {
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
            } else if (!this.moveItemStackTo(stack, MachineTurbineGasBlockEntity.SLOT_BATTERY, MachineTurbineGasBlockEntity.SLOT_BATTERY + 1, false)
                    && !this.moveItemStackTo(stack, MachineTurbineGasBlockEntity.SLOT_FLUID_ID, MachineTurbineGasBlockEntity.SLOT_FLUID_ID + 1, false)) {
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
}
