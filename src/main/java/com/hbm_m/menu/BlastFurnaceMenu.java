package com.hbm_m.menu;
// Меню для доменной печи.
// Обеспечивает взаимодействие между игроком и блоком, включая слоты для предметов и синхронизацию данных.
// Содержит логику для обработки Shift-клика и отображения прогресса плавки и уровня топлива.

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.custom.machines.BlastFurnaceBlockEntity;
import com.hbm_m.main.MainRegistry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraft.world.inventory.ClickType;

public class BlastFurnaceMenu extends AbstractContainerMenu {
    public final BlastFurnaceBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    private static final int DATA_INDEX_PROGRESS = 0;
    private static final int DATA_INDEX_MAX_PROGRESS = 1;
    private static final int DATA_INDEX_FUEL = 2;
    private static final int DATA_INDEX_MAX_FUEL = 3;
    private static final int DATA_INDEX_SIDE_UPPER = 4;
    private static final int DATA_INDEX_SIDE_LOWER = 5;
    private static final int DATA_INDEX_SIDE_FUEL = 6;

    public BlastFurnaceMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(7));
    }

    public BlastFurnaceMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.BLAST_FURNACE_MENU.get(), containerId);
        checkContainerSize(inv, 4);
        blockEntity = ((BlastFurnaceBlockEntity) entity);
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            this.addSlot(new FuelSlot(iItemHandler, 0, 8, 36)); // Топливо (лава ведро)
            this.addSlot(new SlotItemHandler(iItemHandler, 1, 80, 18)); // Первый входной слот
            this.addSlot(new SlotItemHandler(iItemHandler, 2, 80, 54)); // Второй входной слот
            this.addSlot(new OutputSlot(iItemHandler, 3, 134, 36)); // Выходной слот
        });

        addDataSlots(data);
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        Slot slot = slotId >= 0 && slotId < this.slots.size() ? this.slots.get(slotId) : null;
        int configSlot = resolveConfigSlotIndex(slotId);
        if (slot != null && configSlot >= 0 && clickType == ClickType.PICKUP && dragType == 1 && getCarried().isEmpty()) {
            if (!slot.hasItem()) {
                if (!player.level().isClientSide()) {
                    blockEntity.cycleSide(configSlot);
                }
            }
            return;
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    public boolean isCrafting() {
        return data.get(DATA_INDEX_PROGRESS) > 0;
    }

    public boolean hasFuel() {
        return data.get(DATA_INDEX_FUEL) > 0;
    }

    public int getScaledProgress() {
        int progress = this.data.get(DATA_INDEX_PROGRESS);
        int maxProgress = this.data.get(DATA_INDEX_MAX_PROGRESS);
        int arrowPixelSize = 24; // Ширина стрелки прогресса

        return maxProgress != 0 && progress != 0 ? progress * arrowPixelSize / maxProgress : 0;
    }

    public int getScaledFuelProgress() {
        int fuelLevel = this.data.get(DATA_INDEX_FUEL);
        int maxFuelLevel = this.data.get(DATA_INDEX_MAX_FUEL);
        int fuelPixelHeight = 53; // Высота индикатора топлива

        return maxFuelLevel != 0 ? fuelLevel * fuelPixelHeight / maxFuelLevel : 0;
    }

    public Direction getConfiguredDirectionForSlot(int slot) {
        int dataIndex = switch (slot) {
            case 0 -> DATA_INDEX_SIDE_UPPER;
            case 1 -> DATA_INDEX_SIDE_LOWER;
            case 2 -> DATA_INDEX_SIDE_FUEL;
            default -> -1;
        };
        if (dataIndex < 0) {
            return Direction.UP;
        }
        int encoded = data.get(dataIndex);
        if (encoded < 0 || encoded >= Direction.values().length) {
            return Direction.UP;
        }
        return Direction.from3DDataValue(encoded);
    }

    private static int resolveConfigSlotIndex(int slotId) {
        if (slotId == TE_INVENTORY_FIRST_SLOT_INDEX + 1) {
            return 0; // upper input
        }
        if (slotId == TE_INVENTORY_FIRST_SLOT_INDEX + 2) {
            return 1; // lower input
        }
        if (slotId == TE_INVENTORY_FIRST_SLOT_INDEX) {
            return 2; // fuel slot
        }
        return -1;
    }

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT = 4;

    public int getMachineSlotOffset() {
        return TE_INVENTORY_FIRST_SLOT_INDEX;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                    + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            MainRegistry.LOGGER.debug("Invalid slotIndex:" + index);
            return ItemStack.EMPTY;
        }

        // If stack size == 0 (the entire stack was moved) set slot contents to null
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
                player, ModBlocks.BLAST_FURNACE.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    private static class FuelSlot extends SlotItemHandler {
        public FuelSlot(net.minecraftforge.items.IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return BlastFurnaceBlockEntity.isFuel(stack);
        }
    }

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