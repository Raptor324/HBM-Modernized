package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachinePressBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.platform.PlatformHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachinePressMenu extends AbstractContainerMenu {

    // Machine slot indices inside this menu (player inventory is added first)
    public static final int FUEL_SLOT_IDX = 36;
    public static final int STAMP_SLOT_IDX = 37;
    public static final int MATERIAL_SLOT_IDX = 38;
    public static final int OUTPUT_SLOT_IDX = 39;
    public static final int STORAGE_FIRST_IDX = 40;
    public static final int STORAGE_SLOT_COUNT = 9;

    public final MachinePressBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public MachinePressMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(9));
    }

    public MachinePressMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.PRESS_MENU.get(), containerId);
        checkContainerSize(inv, MachinePressBlockEntity.SLOT_COUNT);
        blockEntity = ((MachinePressBlockEntity) entity);
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        var handler = this.blockEntity.getInventory();
        var container = new ModItemStackHandlerContainer(handler, this.blockEntity::setChanged);
        // Раскладка слотов как в 1.7.10 ContainerMachinePress
        this.addSlot(new Slot(container, 0, 26, 53));  // Coal
        this.addSlot(new Slot(container, 1, 80, 17));  // Stamp
        this.addSlot(new Slot(container, 2, 80, 53));  // Input
        this.addSlot(new OutputSlot(container, 3, 140, 35)); // Output
        // Extra Storage
        for (int i = 0; i < STORAGE_SLOT_COUNT; i++) {
            this.addSlot(new Slot(container, 4 + i, 8 + i * 18, 84));
        }

        addDataSlots(data);
    }

    // Методы доступа к данным (как в 1.7.10)
    public int getPress() {
        return this.data.get(0);
    }

    public int getMaxPress() {
        return this.data.get(1);
    }

    public int getBurnTime() {
        return this.data.get(2);
    }

    public int getFuelPerOperation() {
        return this.data.get(3);
    }

    public int getSpeed() {
        return this.data.get(4);
    }

    public int getMaxSpeed() {
        return this.data.get(5);
    }

    public int getHeatState() {
        return this.data.get(6);
    }

    public int getPressPosition() {
        return this.data.get(7);
    }

    public boolean isRetracting() {
        return this.data.get(8) == 1;
    }

    // Методы состояния
    public boolean isCrafting() {
        return getPress() > 0 || isRetracting();
    }

    public boolean isHeated() {
        return getBurnTime() >= getFuelPerOperation();
    }

    public boolean isBurning() {
        return getBurnTime() >= 20;
    }

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT = 4 + STORAGE_SLOT_COUNT;

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index >= TE_INVENTORY_FIRST_SLOT_INDEX) {
            // Машина -> игрок
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Игрок -> машина; маршрутизация как в 1.7.10 transferStackInSlot
            if (PlatformHooks.getFuelBurnTime(sourceStack) > 0) {
                if (!moveItemStackTo(sourceStack, FUEL_SLOT_IDX, FUEL_SLOT_IDX + 1, false)
                        && !moveItemStackTo(sourceStack, STORAGE_FIRST_IDX, STORAGE_FIRST_IDX + STORAGE_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (sourceStack.getItem() instanceof com.hbm_m.item.industrial.ItemStamp) {
                if (!moveItemStackTo(sourceStack, STAMP_SLOT_IDX, STAMP_SLOT_IDX + 1, false)
                        && !moveItemStackTo(sourceStack, STORAGE_FIRST_IDX, STORAGE_FIRST_IDX + STORAGE_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!moveItemStackTo(sourceStack, MATERIAL_SLOT_IDX, MATERIAL_SLOT_IDX + 1, false)
                        && !moveItemStackTo(sourceStack, STORAGE_FIRST_IDX, STORAGE_FIRST_IDX + STORAGE_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            }
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
                player, ModBlocks.PRESS.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        // Инвентарь игрока как в оригинале: y = 132
        for (int i = 0; i < PLAYER_INVENTORY_ROW_COUNT; ++i) {
            for (int l = 0; l < PLAYER_INVENTORY_COLUMN_COUNT; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 132 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < HOTBAR_SLOT_COUNT; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 190));
        }
    }

    /** Выходной слот — предметы класть нельзя (аналог SlotCraftingOutput). */
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
