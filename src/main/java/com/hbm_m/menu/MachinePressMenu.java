package com.hbm_m.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.custom.machines.MachinePressBlockEntity;
import com.hbm_m.item.custom.industrial.ItemStamp;
import com.hbm_m.item.ModItems;
import com.hbm_m.main.MainRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

public class MachinePressMenu extends AbstractContainerMenu {

    public final MachinePressBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public MachinePressMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(9));
    }

    public MachinePressMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.PRESS_MENU.get(), containerId);
        checkContainerSize(inv, 4);
        blockEntity = ((MachinePressBlockEntity) entity);
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            this.addSlot(new FuelSlot(iItemHandler, 0, 26, 53));
            this.addSlot(new StampSlot(iItemHandler, 1, 80, 17));
            this.addSlot(new MaterialSlot(iItemHandler, 2, 80, 53));
            this.addSlot(new OutputSlot(iItemHandler, 3, 139, 34));
        });

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
    private static final int TE_INVENTORY_SLOT_COUNT = 4;

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                    + TE_INVENTORY_SLOT_COUNT, false)) {
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
                player, ModBlocks.PRESS.get());
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

    // Слоты с ограничениями
    private static class FuelSlot extends SlotItemHandler {
        public FuelSlot(net.minecraftforge.items.IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            net.minecraft.world.item.Item item = stack.getItem();
            // Разрешаем кастомное топливо
            if (item == ModItems.LIGNITE.get()) return true;

            // Используем встроенную систему Forge для определения ванильного топлива
            return net.minecraftforge.common.ForgeHooks.getBurnTime(stack, null) > 0;
        }
    }

    private static class StampSlot extends SlotItemHandler {
        public StampSlot(net.minecraftforge.items.IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // Проверяем, является ли предмет штампом через класс
            if (stack.getItem() instanceof ItemStamp) {
                return true;
            }
            // Оставляем старую проверку для совместимости
            return stack.getItem().toString().contains("stamp");
        }
    }

    private static class MaterialSlot extends SlotItemHandler {
        public MaterialSlot(net.minecraftforge.items.IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            String itemName = stack.getItem().toString();
            return itemName.contains("ingot") || itemName.contains("metal");
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