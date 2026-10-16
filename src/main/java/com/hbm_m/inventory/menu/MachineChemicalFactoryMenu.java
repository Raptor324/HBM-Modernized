package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.energy.ItemEnergyAccess;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineChemicalFactoryBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.industrial.ItemBlueprintFolder;
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
 * Menu для Chemical Factory — порт 1.7.10 {@code ContainerMachineChemicalFactory}.
 *
 * <p>Раскладка слотов 1:1 с оригиналом (GUI 248×216):
 * <ul>
 *   <li>0 — батарея (224, 88);</li>
 *   <li>1..3 — апгрейды вертикально от (206, 125) с шагом 18;</li>
 *   <li>4 линии с шагом 7 слотов и 22 пикселей: шаблон (93, 20+lane*22),
 *       3 входа от (10, 20+lane*22) с шагом 16, 3 выхода от (139, 20+lane*22) с шагом 16;</li>
 *   <li>инвентарь игрока от (26, 134), хотбар на 58 ниже.</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public class MachineChemicalFactoryMenu extends AbstractContainerMenu {

    private static final int LANE_COUNT = 4;
    private static final int TE_SLOT_COUNT = 4 + LANE_COUNT * 7;
    private static final int VANILLA_SLOT_COUNT = 36;
    private static final int VANILLA_FIRST_SLOT_INDEX = TE_SLOT_COUNT;

    private final MachineChemicalFactoryBlockEntity blockEntity;
    private final ContainerData data;

    public MachineChemicalFactoryMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(LANE_COUNT * 3 + 5));
    }

    public MachineChemicalFactoryMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.CHEMICAL_FACTORY_MENU.get(), id);
        this.blockEntity = (MachineChemicalFactoryBlockEntity) entity;
        this.data = data;

        var handler = blockEntity.getInventory();
        var container = new ModItemStackHandlerContainer(handler, blockEntity::setChanged);

        // Battery — как в оригинале (224, 88)
        addSlot(new Slot(container, 0, 224, 88) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                if (ItemEnergyAccess.getHbmProvider(stack).isPresent() || ItemEnergyAccess.getHbmReceiver(stack).isPresent()) return true;
                //? if neoforge {
                /*if (stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM) != null) return true;
                *///?}
                return false;
            }
        });
        // Upgrades — вертикальная колонка от (206, 125)
        addSlot(new UpgradeSlot(container, 1, 206, 125));
        addSlot(new UpgradeSlot(container, 2, 206, 143));
        addSlot(new UpgradeSlot(container, 3, 206, 161));

        for (int lane = 0; lane < LANE_COUNT; lane++) {
            int base = 4 + lane * 7;
            int rowY = 20 + lane * 22;
            // Template (папка чертежей)
            addSlot(new Slot(container, base, 93, rowY) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return stack.getItem() instanceof ItemBlueprintFolder;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
            // Solid input
            addSlot(new Slot(container, base + 1, 10, rowY));
            addSlot(new Slot(container, base + 2, 26, rowY));
            addSlot(new Slot(container, base + 3, 42, rowY));
            // Solid output (только забирать)
            addSlot(new Slot(container, base + 4, 139, rowY) {
                @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
            });
            addSlot(new Slot(container, base + 5, 155, rowY) {
                @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
            });
            addSlot(new Slot(container, base + 6, 171, rowY) {
                @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
            });
        }

        // Player inventory — как в оригинале playerInv(invPlayer, 26, 134)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 26 + col * 18, 134 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 26 + col * 18, 134 + 58));
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

    // === ContainerData layout: 0..3 progress, 4..7 max, 8..11 didProcess, 12 canCool, 13/14 energy, 15/16 max ===

    public int getLaneProgress(int lane) {
        return data.get(lane);
    }

    public int getLaneMaxProgress(int lane) {
        return data.get(LANE_COUNT + lane);
    }

    public boolean getLaneDidProcess(int lane) {
        return data.get(LANE_COUNT * 2 + lane) != 0;
    }

    public boolean getCanCool() {
        return data.get(LANE_COUNT * 3) != 0;
    }

    public long getEnergyStored() {
        return ((long) data.get(LANE_COUNT * 3 + 2) << 32) | (data.get(LANE_COUNT * 3 + 1) & 0xFFFFFFFFL);
    }

    public long getMaxEnergyStored() {
        return ((long) data.get(LANE_COUNT * 3 + 4) << 32) | (data.get(LANE_COUNT * 3 + 3) & 0xFFFFFFFFL);
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
                    //? if neoforge {
                    /*|| stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM) != null
                    *///?}
            ) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemBlueprintFolder) {
                // Папка чертежей — в слот шаблона первой свободной линии (как в оригинале)
                boolean placed = false;
                for (int lane = 0; lane < LANE_COUNT && !placed; lane++) {
                    int tpl = 4 + lane * 7;
                    placed = moveItemStackTo(stack, tpl, tpl + 1, false);
                }
                if (!placed) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemMachineUpgrade) {
                if (!moveItemStackTo(stack, 1, 4, false)) return ItemStack.EMPTY;
            } else {
                boolean placed = moveItemStackTo(stack, 5, 8, false)
                        || moveItemStackTo(stack, 12, 15, false)
                        || moveItemStackTo(stack, 19, 22, false)
                        || moveItemStackTo(stack, 26, 29, false);
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
