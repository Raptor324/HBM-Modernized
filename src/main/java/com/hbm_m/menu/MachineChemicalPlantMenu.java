package com.hbm_m.menu;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.custom.machines.MachineChemicalPlantBlockEntity;
import com.hbm_m.item.custom.industrial.ItemBlueprintFolder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Menu для Chemical Plant — порт с 1.7.10.
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

        blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            addSlot(new SlotItemHandler(handler, 0, 152, 81));
            addSlot(new SlotItemHandler(handler, 1, 35, 126));
            addSlot(new SlotItemHandler(handler, 2, 152, 108));
            addSlot(new SlotItemHandler(handler, 3, 170, 108));
            addSlot(new SlotItemHandler(handler, 4, 8, 99));
            addSlot(new SlotItemHandler(handler, 5, 26, 99));
            addSlot(new SlotItemHandler(handler, 6, 44, 99));
            addSlot(new SlotItemHandler(handler, 7, 80, 99) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
            addSlot(new SlotItemHandler(handler, 8, 98, 99) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
            addSlot(new SlotItemHandler(handler, 9, 116, 99) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
            addSlot(new SlotItemHandler(handler, 10, 8, 54));
            addSlot(new SlotItemHandler(handler, 11, 26, 54));
            addSlot(new SlotItemHandler(handler, 12, 44, 54));
            addSlot(new SlotItemHandler(handler, 13, 8, 72) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
            addSlot(new SlotItemHandler(handler, 14, 26, 72) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
            addSlot(new SlotItemHandler(handler, 15, 44, 72) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
            addSlot(new SlotItemHandler(handler, 16, 80, 54));
            addSlot(new SlotItemHandler(handler, 17, 98, 54));
            addSlot(new SlotItemHandler(handler, 18, 116, 54));
            addSlot(new SlotItemHandler(handler, 19, 80, 72) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
            addSlot(new SlotItemHandler(handler, 20, 98, 72) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
            addSlot(new SlotItemHandler(handler, 21, 116, 72) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
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
    public @NotNull ItemStack quickMoveStack(@Nonnull Player player, int index) {
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
            if (stack.getCapability(ForgeCapabilities.ENERGY).isPresent()
                    || stack.getCapability(com.hbm_m.capability.ModCapabilities.HBM_ENERGY_PROVIDER).isPresent()) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemBlueprintFolder) {
                if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else if (stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
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
    public boolean stillValid(@Nonnull Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.CHEMICAL_PLANT.get());
    }
}
