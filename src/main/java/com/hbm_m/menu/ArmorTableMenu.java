package com.hbm_m.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ArmorTableBlockEntity; // Импорт нужен для размера
// import com.hbm_m.menu.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class ArmorTableMenu extends AbstractContainerMenu {

     // --- КОНСТАНТЫ ДЛЯ QUICKMOVESTACK ---
    private static final int PLAYER_INVENTORY_START = ArmorTableBlockEntity.INVENTORY_SIZE; // 10
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27; // 36
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_END; // 36
    private static final int PLAYER_HOTBAR_END = PLAYER_HOTBAR_START + 9; // 45

    private final IItemHandler machineInventory;
    private final ContainerLevelAccess access;

    // Конструктор для открытия GUI из блока
    public ArmorTableMenu(int windowId, Inventory playerInventory, IItemHandler machineInventory, ContainerLevelAccess access) {
        super(ModMenuTypes.ARMOR_TABLE_MENU.get(), windowId);
        this.machineInventory = machineInventory;
        this.access = access;

        // --- НОВЫЕ, ВЫРОВНЕННЫЕ КООРДИНАТЫ СЛОТОВ ---

        // Слот 0: Центральный (основная броня)
        this.addSlot(new SlotItemHandler(machineInventory, 0, 44, 63) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ArmorItem;
            }
        });

        // --- Слоты 1-4: Броня-модули (вокруг центрального) ---
        // Слот 1 (Шлем)
        this.addSlot(new ArmorSlot(machineInventory, 1, 26, 27, ArmorItem.Type.HELMET));
        // Слот 2 (Нагрудник)
        this.addSlot(new ArmorSlot(machineInventory, 2, 62, 27, ArmorItem.Type.CHESTPLATE));
        // Слот 3 (Поножи)
        this.addSlot(new ArmorSlot(machineInventory, 3, 98, 27, ArmorItem.Type.LEGGINGS));
        // Слот 4 (Ботинки)
        this.addSlot(new ArmorSlot(machineInventory, 4, 134, 45, ArmorItem.Type.BOOTS));

        // --- Слоты 5-8: Материалы (по углам) ---
        // Слот 5 (Аккумулятор/верхний левый)
        this.addSlot(new SlotItemHandler(machineInventory, 5, 8, 63));
        // Слот 6 (Особое/нижний левый)
        this.addSlot(new SlotItemHandler(machineInventory, 6, 26, 99));
        // Слот 7 (Пластина/нижний правый)
        this.addSlot(new SlotItemHandler(machineInventory, 7, 62, 99));
        // Слот 8 (Обшивка/верхний правый)
        this.addSlot(new SlotItemHandler(machineInventory, 8, 98, 99));
        // Слот 9 (Сервоприводы)
        this.addSlot(new SlotItemHandler(machineInventory, 9, 134, 81));

        // --- Слоты инвентаря игрока ---
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    // Вспомогательные методы с НОВЫМИ КООРДИНАТАМИ
    private void addPlayerInventory(Inventory playerInventory) {
        // Располагаем основной инвентарь
        int inventoryY = 140; 
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, inventoryY + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        // Располагаем хотбар
        int hotbarY = 198;
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, hotbarY));
        }
    }

    // Конструктор для клиента также должен быть обновлен, чтобы вызывать новый конструктор
    public ArmorTableMenu(int windowId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(windowId, playerInventory, new ItemStackHandler(ArmorTableBlockEntity.INVENTORY_SIZE), ContainerLevelAccess.NULL);
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        return stillValid(access, player, ModBlocks.ARMOR_TABLE.get());
    }

    @Override
    public @Nonnull ItemStack quickMoveStack(@Nonnull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            
            // Если клик в слоте машины
            if (index < ArmorTableBlockEntity.INVENTORY_SIZE) {
                if (!this.moveItemStackTo(itemstack1, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            // Если клик в слоте инвентаря/хотбара игрока
            } else {
                // Пытаемся поместить в слоты машины.
                // ВАЖНО: moveItemStackTo автоматически учтет isItemValid для нашего ArmorSlot!
                if (!this.moveItemStackTo(itemstack1, 0, ArmorTableBlockEntity.INVENTORY_SIZE, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    // Внутренний класс для слотов, принимающих только определенный тип брони
     public static class ArmorSlot extends SlotItemHandler {
        private final ArmorItem.Type type;
        
        // Конструктор теперь принимает IItemHandler
        public ArmorSlot(IItemHandler itemHandler, int index, int x, int y, ArmorItem.Type type) {
            super(itemHandler, index, x, y);
            this.type = type;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // Проверяем, является ли предмет броней нужного типа
            return stack.getItem() instanceof ArmorItem armorItem && armorItem.getType() == this.type;
        }
    }
}