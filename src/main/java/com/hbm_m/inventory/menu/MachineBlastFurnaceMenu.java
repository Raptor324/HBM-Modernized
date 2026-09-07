package com.hbm_m.inventory.menu;
// Меню для доменной печи (обновлённая версия).
// Слоты: топливо, два входа, два выхода. Данные: прогресс, скорость, топливо, дутьё, газ.

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineBlastFurnaceBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.main.MainRegistry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.inventory.ClickType;

public class MachineBlastFurnaceMenu extends AbstractContainerMenu {
    public final MachineBlastFurnaceBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    private static final int DATA_INDEX_PROGRESS = 0;
    private static final int DATA_INDEX_SPEED = 1;
    private static final int DATA_INDEX_FUEL = 2;
    private static final int DATA_INDEX_AIR = 3;
    private static final int DATA_INDEX_FLUE = 4;
    private static final int DATA_INDEX_PROGRESSING = 5;

    public MachineBlastFurnaceMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(6));
    }

    public MachineBlastFurnaceMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.MACHINE_BLAST_FURNACE_MENU.get(), containerId);
        if (!(entity instanceof MachineBlastFurnaceBlockEntity be)) {
            throw new IllegalStateException("Expected MachineBlastFurnaceBlockEntity at position, got: " + entity);
        }
        blockEntity = be;
        checkContainerDataCount(data, 6);
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        var handler = this.blockEntity.getInventory();
        var container = new ModItemStackHandlerContainer(handler, this.blockEntity::setChanged);
        this.addSlot(new FuelSlot(container, MachineBlastFurnaceBlockEntity.FUEL_SLOT, 80, 81)); // Топливо
        this.addSlot(new Slot(container, MachineBlastFurnaceBlockEntity.INPUT_SLOT_FIRST, 80, 27)); // Первый вход
        this.addSlot(new Slot(container, MachineBlastFurnaceBlockEntity.INPUT_SLOT_SECOND, 80, 45)); // Второй вход
        this.addSlot(new OutputSlot(container, MachineBlastFurnaceBlockEntity.OUTPUT_SLOT_FIRST, 134, 72)); // Основной выход
        this.addSlot(new OutputSlot(container, MachineBlastFurnaceBlockEntity.OUTPUT_SLOT_SECOND, 134, 90)); // Шлак

        addDataSlots(data);
    }

    public boolean isProgressing() {
        return data.get(DATA_INDEX_PROGRESSING) != 0;
    }

    public double getSpeed() {
        return data.get(DATA_INDEX_SPEED) / 1_000D;
    }

    public int getFuel() {
        return data.get(DATA_INDEX_FUEL);
    }

    public int getAir() {
        return data.get(DATA_INDEX_AIR);
    }

    public int getFlue() {
        return data.get(DATA_INDEX_FLUE);
    }

    public double getProgressFraction() {
        return data.get(DATA_INDEX_PROGRESS) / 1_000_000D;
    }

    public int getScaledProgress(int pixelSize) {
        int progress = this.data.get(DATA_INDEX_PROGRESS);
        long scaled = progress * pixelSize / 1_000_000L;
        return (int) Math.min(pixelSize, Math.max(0, scaled));
    }

    public int getScaledFuelProgress(int pixelSize) {
        return (int) ((long) this.data.get(DATA_INDEX_FUEL) * pixelSize / MachineBlastFurnaceBlockEntity.MAX_FUEL);
    }

    public int getScaledAirProgress(int pixelSize) {
        return (int) ((long) this.data.get(DATA_INDEX_AIR) * pixelSize / MachineBlastFurnaceBlockEntity.AIR_CAPACITY_MB);
    }

    public int getScaledFlueProgress(int pixelSize) {
        return (int) ((long) this.data.get(DATA_INDEX_FLUE) * pixelSize / MachineBlastFurnaceBlockEntity.FLUE_CAPACITY_MB);
    }

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT = 5;

    public int getMachineSlotOffset() {
        return TE_INVENTORY_FIRST_SLOT_INDEX;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // Из инвентаря игрока: топливо в слот топлива, остальное - во входные слоты
            if (MachineBlastFurnaceBlockEntity.isFuel(sourceStack)) {
                if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX,
                        TE_INVENTORY_FIRST_SLOT_INDEX + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX + 1,
                    TE_INVENTORY_FIRST_SLOT_INDEX + 3, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            MainRegistry.LOGGER.debug("Invalid slotIndex:" + index);
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.MACHINE_BLAST_FURNACE.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 140 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 198));
        }
    }

    private static class FuelSlot extends Slot {
        public FuelSlot(net.minecraft.world.Container itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return MachineBlastFurnaceBlockEntity.isFuel(stack);
        }
    }

    private static class OutputSlot extends Slot {
        public OutputSlot(net.minecraft.world.Container itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
