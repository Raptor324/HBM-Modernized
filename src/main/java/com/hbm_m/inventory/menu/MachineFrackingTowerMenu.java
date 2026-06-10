package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.machines.MachineFrackingTowerBlockEntity;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu (Container) для Fracking Tower.
 * Порт из версии 1.7.10 (ContainerMachineOilWell).
 *
 * Слоты инвентаря (как в оригинале):
 * - Слот 0: Батарея (8, 53)
 * - Слот 1: Канистра ввод (80, 17)
 * - Слот 2: Канистра вывод (80, 53)
 * - Слот 3: Газ баллон ввод (125, 17)
 * - Слот 4: Газ баллон вывод (125, 53)
 * - Слоты 5-7: Апгрейды (152, 17), (152, 35), (152, 53)
 */
public class MachineFrackingTowerMenu extends AbstractContainerMenu {

    public final MachineFrackingTowerBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final HandlerContainer machineInventory;

    //=====================================================================================//
    // КОНСТРУКТОРЫ
    //=====================================================================================//

    public MachineFrackingTowerMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, extraData.readBlockPos());
    }

    private MachineFrackingTowerMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv,
                inv.player.level().getBlockEntity(pos) instanceof MachineFrackingTowerBlockEntity be ? be.getInventory() : null,
                inv.player.level().getBlockEntity(pos) instanceof MachineFrackingTowerBlockEntity be ? be : null,
                ContainerLevelAccess.create(inv.player.level(), pos));
    }

    /**
     * Основной конструктор.
     */
    public MachineFrackingTowerMenu(int containerId, Inventory playerInventory,
                                    @Nullable ModItemStackHandler handler,
                                    @Nullable MachineFrackingTowerBlockEntity blockEntity,
                                    ContainerLevelAccess access) {
        super(ModMenuTypes.FRACTURING_TOWER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = access;

        // Если handler null, создаём пустой
        if (handler == null) {
            handler = new ModItemStackHandler(8) {
                @Override
                protected void onContentsChanged(int slot) {
                    // noop: client-side placeholder handler
                }
            };
        }
        this.machineInventory = new HandlerContainer(handler);

        // Слот 0: Батарея (8, 53)
        this.addSlot(new Slot(machineInventory,
                MachineFrackingTowerBlockEntity.SLOT_BATTERY, 8, 53));

        // Слот 1: Канистра ввод (80, 17)
        this.addSlot(new Slot(machineInventory,
                MachineFrackingTowerBlockEntity.SLOT_CANISTER_IN, 80, 17));

        // Слот 2: Канистра вывод - только вывод (80, 53)
        this.addSlot(new Slot(machineInventory,
                MachineFrackingTowerBlockEntity.SLOT_CANISTER_OUT, 80, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Только вывод
            }
        });

        // Слот 3: Газ баллон ввод (125, 17)
        this.addSlot(new Slot(machineInventory,
                MachineFrackingTowerBlockEntity.SLOT_GAS_IN, 125, 17));

        // Слот 4: Газ баллон вывод - только вывод (125, 53)
        this.addSlot(new Slot(machineInventory,
                MachineFrackingTowerBlockEntity.SLOT_GAS_OUT, 125, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Только вывод
            }
        });

        // Слоты 5-7: Апгрейды (152, 17), (152, 35), (152, 53)
        this.addSlot(new SlotUpgrade(machineInventory,
                MachineFrackingTowerBlockEntity.SLOT_UPGRADE_1, 152, 17));
        this.addSlot(new SlotUpgrade(machineInventory,
                MachineFrackingTowerBlockEntity.SLOT_UPGRADE_2, 152, 35));
        this.addSlot(new SlotUpgrade(machineInventory,
                MachineFrackingTowerBlockEntity.SLOT_UPGRADE_3, 152, 53));

        // Инвентарь игрока (3 строки по 9 слотов)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory,
                        col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Хотбар игрока (9 слотов)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    //=====================================================================================//
    // ФАБРИКА
    //=====================================================================================//

    public static MachineFrackingTowerMenu create(int containerId, Inventory playerInventory,
                                                  MachineFrackingTowerBlockEntity blockEntity) {
        return new MachineFrackingTowerMenu(
                containerId,
                playerInventory,
                blockEntity.getInventory(),
                blockEntity,
                ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
        );
    }

    //=====================================================================================//
    // ВЗАИМОДЕЙСТВИЕ
    //=====================================================================================//

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            // Слоты машины (0-7)
            if (slotIndex < 8) {
                // Перемещение в инвентарь игрока
                if (!this.moveItemStackTo(slotStack, 8, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // Слоты игрока (8+)
            else {
                // Проверка на апгрейд
                if (slotStack.getItem() instanceof ItemMachineUpgrade) {
                    // Сначала пробуем в слоты апгрейдов (5-8)
                    if (!this.moveItemStackTo(slotStack, 5, 8, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // Проверка на батарею или жидкостной контейнер
                else {
                    // Пробуем в слот батареи (0-1) или слоты контейнеров (1, 3)
                    if (!this.moveItemStackTo(slotStack, 0, 5, false)) {
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

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return true; // Для клиента
        return stillValid(this.access, player, blockEntity.getBlockState().getBlock());
    }

    //=====================================================================================//
    // ГЕТТЕРЫ
    //=====================================================================================//

    @Nullable
    public MachineFrackingTowerBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    //=====================================================================================//
    // ВЛОЖЕННЫЙ КЛАСС: СЛОТ АПГРЕЙДА
    //=====================================================================================//

    /**
     * Специализированный слот для апгрейдов машины.
     */
    private static class SlotUpgrade extends Slot {

        public SlotUpgrade(net.minecraft.world.Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // Принимаем только апгрейды
            return stack.getItem() instanceof ItemMachineUpgrade;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** Vanilla-адаптер для {@link ModItemStackHandler}, чтобы использовать обычные {@link Slot}. */
    private static final class HandlerContainer implements net.minecraft.world.Container {
        private final ModItemStackHandler handler;

        private HandlerContainer(ModItemStackHandler handler) {
            this.handler = handler;
        }

        @Override
        public int getContainerSize() {
            return handler.getSlots();
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack existing = handler.getStackInSlot(slot);
            if (existing.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }

            ItemStack split = existing.split(amount);
            handler.setStackInSlot(slot, existing);
            setChanged();
            return split;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack existing = handler.getStackInSlot(slot);
            handler.setStackInSlot(slot, ItemStack.EMPTY);
            return existing;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            handler.setStackInSlot(slot, stack);
            setChanged();
        }

        @Override
        public void setChanged() {
            // Изменения уже отслеживаются в handler, но Slot ожидает этот вызов.
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < handler.getSlots(); i++) {
                handler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return handler.isItemValid(slot, stack);
        }
    }
}