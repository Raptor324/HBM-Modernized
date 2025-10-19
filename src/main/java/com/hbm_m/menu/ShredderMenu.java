package com.hbm_m.menu;

import com.hbm_m.block.entity.ShredderBlockEntity;
import com.hbm_m.menu.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import com.hbm_m.block.ModBlocks;

public class ShredderMenu extends AbstractContainerMenu {

    private final ShredderBlockEntity blockEntity;
    private final Level level;

    // Индексы слотов
    private static final int INPUT_SLOTS = 9;
    private static final int BLADE_SLOTS = 2;
    private static final int OUTPUT_SLOTS = 18;

    public ShredderMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public ShredderMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModMenuTypes.SHREDDER_MENU.get(), containerId);
        this.blockEntity = (ShredderBlockEntity) blockEntity;
        this.level = playerInventory.player.level();

        ItemStackHandler itemHandler = this.blockEntity.getItemHandler();

        // Входные слоты (3x3 сетка) - верхняя левая часть GUI
        int startX = 44;
        int startY = 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                this.addSlot(new SlotItemHandler(itemHandler, index,
                        startX + col * 18, startY + row * 18));
            }
        }

        // Слоты для лезвий (2 слота) - внизу слева
        int bladeX = 44;
        int bladeY = 108;
        for (int i = 0; i < BLADE_SLOTS; i++) {
            this.addSlot(new SlotItemHandler(itemHandler, INPUT_SLOTS + i,
                    bladeX + i * 36, bladeY));
        }

        // Выходные слоты (3x6 сетка) - справа
        int outputX = 116;
        int outputY = 18;
        for (int col = 0; col < 6; col++) {
            for (int row = 0; row < 3; row++) {
                int index = col * 3 + row;
                this.addSlot(new SlotItemHandler(itemHandler, INPUT_SLOTS + BLADE_SLOTS + index,
                        outputX + row * 18, outputY + col * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false; // Нельзя помещать предметы в выходные слоты
                    }
                });
            }
        }

        // Инвентарь игрока
        int playerInvX = 8;
        int playerInvY = 151; // Стандартное положение относительно верха GUI

        // Основной инвентарь (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        playerInvX + col * 18, playerInvY + row * 18));
            }
        }

        // Хотбар (1x9)
        int hotbarY = 209; // Стандартное положение относительно верха GUI
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                    playerInvX + col * 18, hotbarY));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            int containerSlots = INPUT_SLOTS + BLADE_SLOTS + OUTPUT_SLOTS;

            // Из контейнера в инвентарь игрока
            if (index < containerSlots) {
                if (!this.moveItemStackTo(slotStack, containerSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // Из инвентаря игрока в контейнер
            else {
                // Проверяем, является ли предмет лезвием
                boolean isBlade = false; // Здесь нужна проверка: slotStack.is(ModItems.BLADE_TEST.get())

                if (isBlade) {
                    // Пытаемся поместить в слоты для лезвий
                    if (!this.moveItemStackTo(slotStack, INPUT_SLOTS, INPUT_SLOTS + BLADE_SLOTS, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Пытаемся поместить во входные слоты
                    if (!this.moveItemStackTo(slotStack, 0, INPUT_SLOTS, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.SHREDDER.get());
    }

    public ShredderBlockEntity getBlockEntity() {
        return blockEntity;
    }
}