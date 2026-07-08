package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.HeatingOvenBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/**
 * Menu for the Heating Oven machine.
 * A pure combustion source with a single fuel slot, burning fuel into TU.
 */
public class HeatingOvenMenu extends AbstractContainerMenu {
    public final HeatingOvenBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    private static final int DATA_INDEX_BURN_TIME = 0;
    private static final int DATA_INDEX_MAX_BURN_TIME = 1;
    private static final int DATA_INDEX_IS_ON = 2;

    // Slot indices
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;

    private static final int MACHINE_SLOT_COUNT = 1;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int MACHINE_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    public HeatingOvenMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(5));
    }

    public HeatingOvenMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.HEATING_OVEN_MENU.get(), containerId);
        if (entity == null || !(entity instanceof HeatingOvenBlockEntity)) {
            throw new IllegalStateException("Expected HeatingOvenBlockEntity at position, got: " + entity);
        }
        blockEntity = (HeatingOvenBlockEntity) entity;
        checkContainerDataCount(data, 5);
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        var handler = this.blockEntity.getInventory();
        var container = new ModItemStackHandlerContainer(handler, this.blockEntity::setChanged);
        this.addSlot(new FuelSlot(container, 0, 44, 27)); // Fuel slot

        addDataSlots(data);
    }

    public boolean isBurning() {
        return data.get(DATA_INDEX_BURN_TIME) > 0;
    }

    public boolean isOn() {
        return data.get(DATA_INDEX_IS_ON) != 0;
    }

    public int getBurnTimeScaled(int scale) {
        int burnTime = this.data.get(DATA_INDEX_BURN_TIME);
        int maxBurnTime = this.data.get(DATA_INDEX_MAX_BURN_TIME);

        return maxBurnTime != 0 ? burnTime * scale / maxBurnTime : 0;
    }

    public long getEnergyStored() {
        return blockEntity.getEnergyStored();
    }

    public long getMaxEnergyStored() {
        return blockEntity.getMaxEnergyStored();
    }

    public int getEnergyScaled(int scale) {
        long maxEnergy = Math.max(getMaxEnergyStored(), 1L);
        return (int) (getEnergyStored() * scale / maxEnergy);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < VANILLA_SLOT_COUNT) {
            // Player inventory -> machine
            if (!moveItemStackTo(sourceStack, MACHINE_FIRST_SLOT_INDEX, MACHINE_FIRST_SLOT_INDEX + MACHINE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Machine -> player inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.HEATING_OVEN.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    // Fuel slot - only accepts burnable items
    private static class FuelSlot extends Slot {
        public FuelSlot(net.minecraft.world.Container itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0) > 0;
        }
    }
}
