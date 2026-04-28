package com.hbm_m.inventory.menu;



import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.energy.ItemEnergyAccess;
import com.hbm_m.api.fluids.FluidItemAccess;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.machines.MachineChemicalPlantBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.industrial.ItemBlueprintFolder;

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
import team.reborn.energy.api.EnergyStorage;
//?}

/**
 * Menu для Chemical Plant - порт с 1.7.10.
 * Layout слотов соответствует оригинальному ContainerMachineChemicalPlant.
 */
public class MachineChemicalPlantMenu extends AbstractContainerMenu {

    private static final int TE_SLOT_COUNT = 22;
    private static final int VANILLA_SLOT_COUNT = 36;
    private static final int TE_FIRST_SLOT_INDEX = 0;
    private static final int VANILLA_FIRST_SLOT_INDEX = TE_SLOT_COUNT;

    private final MachineChemicalPlantBlockEntity blockEntity;
    private final ContainerData data;

    public MachineChemicalPlantMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(2));
    }

    public MachineChemicalPlantMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.CHEMICAL_PLANT_MENU.get(), id);
        this.blockEntity = (MachineChemicalPlantBlockEntity) entity;
        this.data = data;

        var handler = blockEntity.getInventory();
        var container = new ModItemStackHandlerContainer(handler, blockEntity::setChanged);

            addSlot(new Slot(container, 0, 152, 81) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    if (stack.getItem() instanceof ItemBlueprintFolder) return true;
                    if (ItemEnergyAccess.getHbmProvider(stack).isPresent() || ItemEnergyAccess.getHbmReceiver(stack).isPresent()) return true;
                    //? if fabric {
                    return EnergyStorage.ITEM.find(stack, null) != null;
                    //?} else {
                    /*return false;
                    *///?}
                }
            });
            addSlot(new Slot(container, 1, 35, 126));  // ??? (оставляем без ограничений как было)
            addSlot(new Slot(container, 2, 152, 108));
            addSlot(new Slot(container, 3, 170, 108));
            addSlot(new Slot(container, 4, 8, 99));
            addSlot(new Slot(container, 5, 26, 99));
            addSlot(new Slot(container, 6, 44, 99));
            addSlot(new Slot(container, 7, 80, 99) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
            addSlot(new Slot(container, 8, 98, 99) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
            addSlot(new Slot(container, 9, 116, 99) {
                @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
            });
            addSlot(new Slot(container, 10, 8, 54));
            addSlot(new Slot(container, 11, 26, 54));
            addSlot(new Slot(container, 12, 44, 54));
            addSlot(new Slot(container, 13, 8, 72) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
            addSlot(new Slot(container, 14, 26, 72) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
            addSlot(new Slot(container, 15, 44, 72) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
            addSlot(new Slot(container, 16, 80, 54));
            addSlot(new Slot(container, 17, 98, 54));
            addSlot(new Slot(container, 18, 116, 54));
            addSlot(new Slot(container, 19, 80, 72) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
            addSlot(new Slot(container, 20, 98, 72) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
            addSlot(new Slot(container, 21, 116, 72) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 174 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 232));
        }

        addDataSlots(data);
    }

    public MachineChemicalPlantBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getMaxProgress() {
        return data.get(1);
    }

    public int getProgressScaled(int width) {
        int max = getMaxProgress();
        return max > 0 ? getProgress() * width / max : 0;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < TE_FIRST_SLOT_INDEX + TE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (ItemEnergyAccess.getHbmProvider(stack).isPresent()
                    || ItemEnergyAccess.getHbmReceiver(stack).isPresent()
                    //? if fabric {
                    || EnergyStorage.ITEM.find(stack, null) != null
                    //?}
            ) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemBlueprintFolder) {
                if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else if (FluidItemAccess.hasFluidHandler(stack)) {
                if (!moveItemStackTo(stack, 10, 13, false) && !moveItemStackTo(stack, 16, 19, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!moveItemStackTo(stack, 4, 7, false)) return ItemStack.EMPTY;
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
                player, ModBlocks.CHEMICAL_PLANT.get());
    }
}
