package com.hbm_m.menu;

// Меню для энергохранилища.
// Имеет слоты для ввода и вывода предметов, а также синхронизирует данные с клиентом.
// Содержит логику для обработки Shift-клика и взаимодействия с инвентарем игрока. Можно настроить режимы работы и приоритеты.

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.MachineBatteryBlockEntity;
// [ИСПРАВЛЕНО] Добавляем импорт LongDataPacker
import com.hbm_m.energy.LongDataPacker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.SlotItemHandler;

import java.util.Optional;

public class MachineBatteryMenu extends AbstractContainerMenu {
    public final MachineBatteryBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public MachineBatteryMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.MACHINE_BATTERY_MENU.get(), pContainerId);
        checkContainerSize(inv, 2);
        blockEntity = (MachineBatteryBlockEntity) entity;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            this.addSlot(new SlotItemHandler(handler, 0, 26, 17));  // Слот INPUT (для разрядки предметов), индекс 36
            this.addSlot(new SlotItemHandler(handler, 1, 26, 53)); // Слот OUTPUT (для зарядки предметов), индекс 37
        });

        addDataSlots(data);
    }

    public MachineBatteryMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        // [ИСПРАВЛЕНО] Устанавливаем правильный размер ContainerData (8)
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(8));
    }

    // --- [ИСПРАВЛЕНО] Геттеры ---

    /**
     * @return Текущее кол-во энергии (long), собранное из двух int.
     */
    public long getEnergy() {
        // high = data.get(1), low = data.get(0)
        return LongDataPacker.unpack(this.data.get(1), this.data.get(0));
    }

    /**
     * @return Максимальная ёмкость (long), собранная из двух int.
     */
    public long getMaxEnergy() {
        // high = data.get(3), low = data.get(2)
        return LongDataPacker.unpack(this.data.get(3), this.data.get(2));
    }

    /**
     * @return Изменение энергии за тик (int).
     */
    public int getEnergyDelta() {
        return this.data.get(4);
    }

    /**
     * @return Режим работы при отсутствии redstone-сигнала (int).
     */
    public int getModeOnNoSignal() {
        return this.data.get(5);
    }

    /**
     * @return Режим работы при наличии redstone-сигнала (int).
     */
    public int getModeOnSignal() {
        return this.data.get(6);
    }

    /**
     * @return Порядковый номер приоритета (int).
     */
    public int getPriorityOrdinal() {
        return this.data.get(7);
    }

    // [ИСПРАВЛЕНО] Удален isTransferLocked(), т.к. его больше нет в data

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                pPlayer, ModBlocks.MACHINE_BATTERY.get());
    }

    // --- КОНСТАНТЫ ДЛЯ QUICKMOVE ---
    // (Логика QuickMove оставлена из твоего файла, она корректна для перемещения предметов)
    private static final int PLAYER_INVENTORY_START_INDEX = 0;
    private static final int PLAYER_INVENTORY_END_INDEX = 36; // 9 hotbar + 27 inventory
    private static final int TE_INPUT_SLOT_INDEX = 36;
    private static final int TE_OUTPUT_SLOT_INDEX = 37;

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Перемещение из инвентаря игрока в наш BlockEntity
        if (index >= PLAYER_INVENTORY_START_INDEX && index < PLAYER_INVENTORY_END_INDEX) {
            Optional<IEnergyStorage> energyCapability = sourceStack.getCapability(ForgeCapabilities.ENERGY).resolve();

            if (energyCapability.isPresent()) {
                IEnergyStorage itemEnergy = energyCapability.get();
                boolean moved = false;

                // Если предмет может ОТДАВАТЬ энергию, пытаемся поместить его в СЛОТ ВХОДА (INPUT)
                if (itemEnergy.canExtract()) {
                    if (moveItemStackTo(sourceStack, TE_INPUT_SLOT_INDEX, TE_INPUT_SLOT_INDEX + 1, false)) {
                        moved = true;
                    }
                }

                // Если не переместили и предмет может ПРИНИМАТЬ энергию, пытаемся поместить в СЛОТ ВЫХОДА (OUTPUT)
                if (!moved && itemEnergy.canReceive()) {
                    if (moveItemStackTo(sourceStack, TE_OUTPUT_SLOT_INDEX, TE_OUTPUT_SLOT_INDEX + 1, false)) {
                        moved = true;
                    }
                }

                // Если перемещение не удалось ни в один из слотов
                if (!moved) {
                    return ItemStack.EMPTY;
                }

            } else {
                // Если у предмета нет энергии, перемещение не имеет смысла
                return ItemStack.EMPTY;
            }

            // Перемещение из BlockEntity в инвентарь игрока
        } else if (index == TE_INPUT_SLOT_INDEX || index == TE_OUTPUT_SLOT_INDEX) {
            if (!moveItemStackTo(sourceStack, PLAYER_INVENTORY_START_INDEX, PLAYER_INVENTORY_END_INDEX, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Непредвиденный индекс слота
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
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
}