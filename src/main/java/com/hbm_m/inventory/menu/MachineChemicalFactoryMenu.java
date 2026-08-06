package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.energy.ItemEnergyAccess;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineChemicalFactoryBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.industrial.ItemMachineUpgrade;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if fabric {
/*import team.reborn.energy.api.EnergyStorage;
*///?}

/**
 * Menu для Chemical Factory — 4 параллельные линии Chemical Plant.
 * Слоты: 0 battery, 1-3 upgrades, затем 4 линии по 6 слотов (3 solid in + 3 solid out).
 */
@SuppressWarnings("UnstableApiUsage")
public class MachineChemicalFactoryMenu extends AbstractContainerMenu {

    private static final int LANE_COUNT = 4;
    private static final int TE_SLOT_COUNT = 4 + LANE_COUNT * 6;
    private static final int VANILLA_SLOT_COUNT = 36;
    private static final int VANILLA_FIRST_SLOT_INDEX = TE_SLOT_COUNT;

    private final MachineChemicalFactoryBlockEntity blockEntity;
    private final ContainerData data;

    public MachineChemicalFactoryMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(LANE_COUNT * 2 + 5));
    }

    public MachineChemicalFactoryMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.CHEMICAL_FACTORY_MENU.get(), id);
        this.blockEntity = (MachineChemicalFactoryBlockEntity) entity;
        this.data = data;

        var handler = blockEntity.getInventory();
        var container = new ModItemStackHandlerContainer(handler, blockEntity::setChanged);

        addSlot(new Slot(container, 0, 224, 18) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                if (ItemEnergyAccess.getHbmProvider(stack).isPresent() || ItemEnergyAccess.getHbmReceiver(stack).isPresent()) return true;
                //? if fabric {
                /*return EnergyStorage.ITEM.find(stack, null) != null;
                *///?} else {
                return false;
                //?}
            }
        });
        addSlot(new UpgradeSlot(container, 1, 224, 100));
        addSlot(new UpgradeSlot(container, 2, 224, 118));
        addSlot(new UpgradeSlot(container, 3, 224, 136));

        for (int lane = 0; lane < LANE_COUNT; lane++) {
            int base = 4 + lane * 6;
            int rowY = 10 + lane * 40;
            addSlot(new Slot(container, base, 10, rowY));
            addSlot(new Slot(container, base + 1, 28, rowY));
            addSlot(new Slot(container, base + 2, 46, rowY));
            addSlot(new Slot(container, base + 3, 100, rowY) {
                @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
            });
            addSlot(new Slot(container, base + 4, 118, rowY) {
                @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
            });
            addSlot(new Slot(container, base + 5, 136, rowY) {
                @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 178 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 236));
        }

        addDataSlots(data);
    }

    private static final class UpgradeSlot extends Slot {
        UpgradeSlot(ModItemStackHandlerContainer container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return stack.getItem() instanceof ItemMachineUpgrade;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    public MachineChemicalFactoryBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getLaneProgress(int lane) {
        return data.get(lane);
    }

    public int getLaneMaxProgress(int lane) {
        return data.get(LANE_COUNT + lane);
    }

    public long getEnergyStored() {
        return ((long) data.get(LANE_COUNT * 2 + 1) << 32) | (data.get(LANE_COUNT * 2) & 0xFFFFFFFFL);
    }

    public long getMaxEnergyStored() {
        return ((long) data.get(LANE_COUNT * 2 + 3) << 32) | (data.get(LANE_COUNT * 2 + 2) & 0xFFFFFFFFL);
    }

    public boolean getAnyDidProcess() {
        return data.get(LANE_COUNT * 2 + 4) != 0;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < TE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (ItemEnergyAccess.getHbmProvider(stack).isPresent()
                    || ItemEnergyAccess.getHbmReceiver(stack).isPresent()
                    //? if fabric {
                    /*|| EnergyStorage.ITEM.find(stack, null) != null
                    *///?}
            ) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemMachineUpgrade) {
                if (!moveItemStackTo(stack, 1, 4, false)) return ItemStack.EMPTY;
            } else {
                boolean placed = false;
                for (int lane = 0; lane < LANE_COUNT && !placed; lane++) {
                    int base = 4 + lane * 6;
                    placed = moveItemStackTo(stack, base, base + 3, false);
                }
                if (!placed) return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.CHEMICAL_FACTORY.get());
    }
}
