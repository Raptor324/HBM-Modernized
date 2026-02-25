package com.hbm_m.menu;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.custom.machines.MachineHydraulicFrackiningTowerBlockEntity;
import com.hbm_m.item.custom.industrial.ItemMachineUpgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

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

    public final MachineHydraulicFrackiningTowerBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    
    //=====================================================================================//
    // КОНСТРУКТОРЫ
    //=====================================================================================//

    public MachineFrackingTowerMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, extraData.readBlockPos());
    }

    private MachineFrackingTowerMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, 
             inv.player.level().getBlockEntity(pos) instanceof MachineHydraulicFrackiningTowerBlockEntity be ? be.getInventory() : null,
             inv.player.level().getBlockEntity(pos) instanceof MachineHydraulicFrackiningTowerBlockEntity be ? be : null,
             ContainerLevelAccess.create(inv.player.level(), pos));
    }

    /**
     * Основной конструктор.
     */
    public MachineFrackingTowerMenu(int containerId, Inventory playerInventory,
                                          @Nullable IItemHandler handler,
                                          @Nullable MachineHydraulicFrackiningTowerBlockEntity blockEntity,
                                          ContainerLevelAccess access) {
        super(ModMenuTypes.FRACTURING_TOWER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = access;

        // Если handler null, создаём пустой
        if (handler == null) {
            handler = new net.minecraftforge.items.ItemStackHandler(8);
        }

        // Слот 0: Батарея (8, 53)
        this.addSlot(new SlotItemHandler(handler, 
                MachineHydraulicFrackiningTowerBlockEntity.SLOT_BATTERY, 8, 53));

        // Слот 1: Канистра ввод (80, 17)
        this.addSlot(new SlotItemHandler(handler, 
                MachineHydraulicFrackiningTowerBlockEntity.SLOT_CANISTER_IN, 80, 17));

        // Слот 2: Канистра вывод - только вывод (80, 53)
        this.addSlot(new SlotItemHandler(handler, 
                MachineHydraulicFrackiningTowerBlockEntity.SLOT_CANISTER_OUT, 80, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Только вывод
            }
        });

        // Слот 3: Газ баллон ввод (125, 17)
        this.addSlot(new SlotItemHandler(handler, 
                MachineHydraulicFrackiningTowerBlockEntity.SLOT_GAS_IN, 125, 17));

        // Слот 4: Газ баллон вывод - только вывод (125, 53)
        this.addSlot(new SlotItemHandler(handler, 
                MachineHydraulicFrackiningTowerBlockEntity.SLOT_GAS_OUT, 125, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Только вывод
            }
        });

        // Слоты 5-7: Апгрейды (152, 17), (152, 35), (152, 53)
        this.addSlot(new SlotUpgrade(handler, 
                MachineHydraulicFrackiningTowerBlockEntity.SLOT_UPGRADE_1, 152, 17));
        this.addSlot(new SlotUpgrade(handler, 
                MachineHydraulicFrackiningTowerBlockEntity.SLOT_UPGRADE_2, 152, 35));
        this.addSlot(new SlotUpgrade(handler, 
                MachineHydraulicFrackiningTowerBlockEntity.SLOT_UPGRADE_3, 152, 53));

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
                                                        MachineHydraulicFrackiningTowerBlockEntity blockEntity) {
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
    public MachineHydraulicFrackiningTowerBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    //=====================================================================================//
    // ВЛОЖЕННЫЙ КЛАСС: СЛОТ АПГРЕЙДА
    //=====================================================================================//

    /**
     * Специализированный слот для апгрейдов машины.
     */
    private static class SlotUpgrade extends SlotItemHandler {

        public SlotUpgrade(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
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
}
