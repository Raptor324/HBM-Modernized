package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.machines.HeatingOvenBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Menu for the Heating Oven machine.
 * Simple furnace-like layout with fuel, input and output slots.
 */
public class HeatingOvenMenu extends AbstractContainerMenu {
    public final HeatingOvenBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    private static final int DATA_INDEX_BURN_TIME = 0;
    private static final int DATA_INDEX_MAX_BURN_TIME = 1;
    private static final int DATA_INDEX_COOK_PROGRESS = 2;
    private static final int DATA_INDEX_COOK_TIME = 3;
    private static final int DATA_INDEX_IS_ON = 4;

    // Slot indices
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;

    private static final int MACHINE_SLOT_COUNT = 3;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int MACHINE_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    public HeatingOvenMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(7));
    }

    public HeatingOvenMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.HEATING_OVEN_MENU.get(), containerId);
        if (entity == null || !(entity instanceof HeatingOvenBlockEntity)) {
            throw new IllegalStateException("Expected HeatingOvenBlockEntity at position, got: " + entity);
        }
        blockEntity = (HeatingOvenBlockEntity) entity;
        checkContainerDataCount(data, 7);
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            this.addSlot(new FuelSlot(iItemHandler, 0, 56, 53)); // Fuel slot
            this.addSlot(new SlotItemHandler(iItemHandler, 1, 56, 17)); // Input slot
            this.addSlot(new OutputSlot(iItemHandler, 2, 116, 35)); // Output slot
        });

        addDataSlots(data);
    }

    public boolean isBurning() {
        return data.get(DATA_INDEX_BURN_TIME) > 0;
    }

    public boolean isCooking() {
        return data.get(DATA_INDEX_COOK_PROGRESS) > 0;
    }

    public int getScaledProgress() {
        int progress = this.data.get(DATA_INDEX_COOK_PROGRESS);
        int maxProgress = this.data.get(DATA_INDEX_COOK_TIME);
        int arrowPixelSize = 24; // Arrow width

        return maxProgress != 0 && progress != 0 ? progress * arrowPixelSize / maxProgress : 0;
    }

    public int getScaledFuel() {
        int burnTime = this.data.get(DATA_INDEX_BURN_TIME);
        int maxBurnTime = this.data.get(DATA_INDEX_MAX_BURN_TIME);
        int flamePixelHeight = 14; // Flame height

        return maxBurnTime != 0 ? burnTime * flamePixelHeight / maxBurnTime : 0;
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
    private static class FuelSlot extends SlotItemHandler {
        public FuelSlot(net.minecraftforge.items.IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return net.minecraftforge.common.ForgeHooks.getBurnTime(stack, null) > 0;
        }
    }

    // Output slot - cannot receive items
    private static class OutputSlot extends SlotItemHandler {
        public OutputSlot(net.minecraftforge.items.IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
