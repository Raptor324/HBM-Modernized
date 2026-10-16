package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineFurnaceSteelBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Slot-Koordinaten (176x166 Textur): Input-Spuren (35,17)/(35,35)/(35,53), Output-Spuren
 *  (125,17)/(125,35)/(125,53) - 1:1 aus {@code ContainerFurnaceSteel} (1.7.10 Original)
 *  uebernommen. Das Original hatte keine Brennstoff-Slots (reine Waermequelle) - da dieser Port
 *  stattdessen Brennstoff-Slots benoetigt (siehe {@link MachineFurnaceSteelBlockEntity}), werden
 *  sie in bislang ungenutztem Platz zwischen Input- und Output-Spalte platziert: (80,17) und
 *  (80,53). */
public class MachineFurnaceSteelMenu extends AbstractContainerMenu {

    public final MachineFurnaceSteelBlockEntity blockEntity;
    private final ContainerData data;

    private static final int SLOT_INPUT_0 = MachineFurnaceSteelBlockEntity.SLOT_INPUT_0;
    private static final int SLOT_OUTPUT_0 = MachineFurnaceSteelBlockEntity.SLOT_OUTPUT_0;
    private static final int SLOT_FUEL_1 = MachineFurnaceSteelBlockEntity.SLOT_FUEL_1;
    private static final int SLOT_FUEL_2 = MachineFurnaceSteelBlockEntity.SLOT_FUEL_2;
    private static final int MACHINE_SLOT_COUNT = 8;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    private static final int DATA_LIT_TIME = 0;
    private static final int DATA_LIT_DURATION = 1;
    private static final int DATA_PROGRESS_0 = 2;
    private static final int LANE_COUNT = 3;
    private static final int DATA_COUNT = 2 + LANE_COUNT;

    public MachineFurnaceSteelMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData), new SimpleContainerData(DATA_COUNT));
    }

    public MachineFurnaceSteelMenu(int id, Inventory inventory, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.FURNACE_STEEL_MENU.get(), id);
        checkContainerDataCount(data, DATA_COUNT);
        this.blockEntity = (MachineFurnaceSteelBlockEntity) entity;
        this.data = data;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);
        for (int lane = 0; lane < 3; lane++) {
            this.addSlot(new Slot(container, SLOT_INPUT_0 + lane, 35, 17 + lane * 18));
            this.addSlot(new OutputSlot(container, SLOT_OUTPUT_0 + lane, 125, 17 + lane * 18));
        }
        this.addSlot(new FuelSlot(container, SLOT_FUEL_1, 80, 17));
        this.addSlot(new FuelSlot(container, SLOT_FUEL_2, 80, 53));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }

        addDataSlots(data);
    }

    private static MachineFurnaceSteelBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineFurnaceSteelBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No MachineFurnaceSteelBlockEntity found at " + pos);
    }

    public int getLitTime() { return data.get(DATA_LIT_TIME); }
    public int getLitDuration() { return data.get(DATA_LIT_DURATION); }
    public int getLaneProgress(int lane) { return data.get(DATA_PROGRESS_0 + lane); }

    public int getBurnProgressScaled(int scale) {
        int duration = getLitDuration();
        return duration > 0 ? getLitTime() * scale / duration : 0;
    }

    public int getCookProgressScaled(int lane, int scale) {
        return getLaneProgress(lane) * scale / 200;
    }

    public boolean isLit() {
        return getLitTime() > 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.FURNACE_STEEL.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (MachineFurnaceSteelBlockEntity.isFuel(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, SLOT_FUEL_1, SLOT_FUEL_2 + 1, false)) {
                        if (!this.moveItemStackTo(slotStack, SLOT_INPUT_0, SLOT_INPUT_0 + 3, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else if (!this.moveItemStackTo(slotStack, SLOT_INPUT_0, SLOT_INPUT_0 + 3, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, slotStack);
        }
        return result;
    }

    private static class FuelSlot extends Slot {
        public FuelSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return MachineFurnaceSteelBlockEntity.isFuel(stack);
        }
    }

    private static class OutputSlot extends Slot {
        public OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
