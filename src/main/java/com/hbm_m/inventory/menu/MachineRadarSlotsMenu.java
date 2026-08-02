package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineRadarBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Меню слотов радара (порт ContainerMachineRadarNT из 1.7.10, текстура gui_radar_link.png).
 *
 * Раскладка 176x184 (дословный порт):
 *   слоты 0..7 — верхний ряд линк-слотов (26 + i*18, 17);
 *   слот 8     — radar linker к Radar Screen (26, 44);
 *   слот 9     — батарея (152, 44);
 *   инвентарь игрока 3x9 (8 + j*18, 103 + i*18);
 *   хотбар (8 + i*18, 161).
 */
public class MachineRadarSlotsMenu extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 10;

    private final MachineRadarBlockEntity blockEntity;

    public MachineRadarSlotsMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, MachineRadarMenu.getRadarBlockEntity(inventory, extraData));
    }

    public MachineRadarSlotsMenu(int id, Inventory inventory, MachineRadarBlockEntity blockEntity) {
        super(ModMenuTypes.RADAR_SLOTS_MENU.get(), id);
        this.blockEntity = blockEntity;

        Container machineContainer = new MachineRadarMenu.HandlerContainer(blockEntity.getInventory());

        // 8 линк-слотов (0..7).
        for (int i = 0; i < 8; i++) {
            addMachineSlot(machineContainer, i, 26 + i * 18, 17);
        }
        // Слот 8 — radar linker к экрану; слот 9 — батарея.
        addMachineSlot(machineContainer, 8, 26, 44);
        addMachineSlot(machineContainer, 9, 152, 44);

        // Инвентарь игрока.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18));
            }
        }
        // Хотбар.
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 161));
        }
    }

    private void addMachineSlot(Container container, int index, int x, int y) {
        this.addSlot(new Slot(container, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity.canPlaceItemInSlot(index, stack);
            }
        });
    }

    public MachineRadarBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        // Без проверки дистанции (как в MachineRadarMenu, порт 1.7.10):
        // саб-меню слотов может открываться с Radar Screen, который находится
        // далеко от самого радара. distanceToSqr <= 64 закрывал GUI через секунду.
        if (blockEntity == null || blockEntity.getLevel() != player.level()) {
            return false;
        }
        return !blockEntity.isRemoved();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            originalStack = stack.copy();

            if (index < MACHINE_SLOTS) {
                // Из машины в инвентарь игрока.
                if (!this.moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Из инвентаря — батарея → слот 9, иначе линк-слоты 0..8.
                if (blockEntity.canPlaceItemInSlot(9, stack)) {
                    if (!this.moveItemStackTo(stack, 9, 10, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(stack, 0, 9, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return originalStack;
    }
}
