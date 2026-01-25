package com.hbm_m.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.custom.machines.MachineFluidTankBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.SlotItemHandler;

public class MachineFluidTankMenu extends AbstractContainerMenu {

    // Ссылки на тайл и данные
    public final MachineFluidTankBlockEntity blockEntity;
    private final ContainerData data;

    // Константы слотов для удобства (0-3 это слоты машины)
    private static final int SLOT_INPUT_L = 0;
    private static final int SLOT_OUTPUT_L = 1;
    private static final int SLOT_INPUT_R = 2;
    private static final int SLOT_OUTPUT_R = 3;

    // Начало инвентаря игрока (после слотов машины)
    private static final int PLAYER_INVENTORY_START = 4;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    // Конструктор клиента (вызывается автоматически)
    public MachineFluidTankMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(2));
    }

    // Конструктор сервера
    public MachineFluidTankMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.FLUID_TANK_MENU.get(), id); // Убедись, что зарегистрировал этот тип в ModMenuTypes
        this.blockEntity = (MachineFluidTankBlockEntity) entity;
        this.data = data;

        checkContainerSize(inv, 4);

        // Добавляем данные для синхронизации (Amount, FluidID)
        addDataSlots(data);

        // --- СЛОТЫ МАШИНЫ ---
        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            // Левый Верхний (Input - Ведро с жижей) -> x34 y16
            this.addSlot(new SlotItemHandler(handler, SLOT_INPUT_L, 34, 16));
            // Левый Нижний (Output - Пустое ведро) -> x34 y52
            this.addSlot(new SlotItemHandler(handler, SLOT_OUTPUT_L, 34, 52));

            // Правый Верхний (Input - Пустое ведро) -> x124 y16
            this.addSlot(new SlotItemHandler(handler, SLOT_INPUT_R, 124, 16));
            // Правый Нижний (Output - Полное ведро) -> x124 y52
            this.addSlot(new SlotItemHandler(handler, SLOT_OUTPUT_R, 124, 52));
        });

        // --- ИНВЕНТАРЬ ИГРОКА (Стандартные координаты) ---
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // --- ХОТБАР ---
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    // Метод для получения жидкости на клиенте (для отрисовки)
    public FluidStack getFluid() {
        int amount = this.data.get(0);
        int fluidId = this.data.get(1);

        // Если ID странный (-1) или количество 0 — возвращаем пустоту
        if (fluidId < 0 || amount <= 0) {
            return FluidStack.EMPTY;
        }

        // 1. Превращаем число (ID) обратно в Жидкость
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);

        // 2. Создаем стопку жидкости
        return new FluidStack(fluid, amount);
    }

    // Shift+Click логика (перемещение предметов)
    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack sourceStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            sourceStack = stackInSlot.copy();

            // Если кликнули по слоту МАШИНЫ (0-3) -> переносим в инвентарь игрока
            if (index < PLAYER_INVENTORY_START) {
                if (!moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // Если кликнули в ИНВЕНТАРЕ ИГРОКА -> пытаемся засунуть в машину
            else {
                // Проверяем, является ли предмет контейнером для жидкости (ведро, канистра)
                if (stackInSlot.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
                    // Простая логика: пробуем засунуть в левый вход (0) или правый вход (2)
                    // (Можно усложнить: полные ведра влево, пустые вправо, но это требует проверки содержимого)
                    if (!moveItemStackTo(stackInSlot, SLOT_INPUT_L, SLOT_INPUT_L + 1, false)) {
                        if (!moveItemStackTo(stackInSlot, SLOT_INPUT_R, SLOT_INPUT_R + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else {
                    return ItemStack.EMPTY; // Если не жидкостный предмет - не переносим
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == sourceStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, stackInSlot);
        }
        return sourceStack;
    }

    @Override
    public boolean stillValid(Player player) {
        // Проверка дистанции до блока (используй свой блок FLUID_TANK)
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.FLUID_TANK.get());
    }
}