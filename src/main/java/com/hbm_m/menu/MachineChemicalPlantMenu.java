package com.hbm_m.menu;

<<<<<<< HEAD
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.custom.machines.MachineChemicalPlantBlockEntity;
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

public class MachineChemicalPlantMenu extends AbstractContainerMenu {

    public final MachineChemicalPlantBlockEntity blockEntity;
    private final ContainerData data;

    // Slot layout (22 machine slots total):
    // 0-3: Item Input (2x2 top left)
    // 4-7: Fluid Container Input (2x2)
    // 8-11: Item Output (2x2 top right)
    // 12-15: Fluid Container Output (2x2)
    // 16-19: Template slots (4 bottom middle)
    // 20: Battery slot
    // 21: Template/Recipe slot
    private static final int MACHINE_SLOTS = 22;
    
    // Player inventory positions
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOTS;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    // Client constructor
    public MachineChemicalPlantMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    // Server constructor
=======
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

>>>>>>> origin/main
    public MachineChemicalPlantMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.CHEMICAL_PLANT_MENU.get(), id);
        this.blockEntity = (MachineChemicalPlantBlockEntity) entity;
        this.data = data;

<<<<<<< HEAD
        addDataSlots(data);

        // Add machine slots based on gui_chemplant.png layout
        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            // Item Input slots (2x2, top left area)
            this.addSlot(new SlotItemHandler(handler, 0, 8, 18));
            this.addSlot(new SlotItemHandler(handler, 1, 26, 18));
            this.addSlot(new SlotItemHandler(handler, 2, 8, 36));
            this.addSlot(new SlotItemHandler(handler, 3, 26, 36));

            // Fluid Container Input slots (2x2, below item inputs)
            this.addSlot(new SlotItemHandler(handler, 4, 8, 72));
            this.addSlot(new SlotItemHandler(handler, 5, 26, 72));
            this.addSlot(new SlotItemHandler(handler, 6, 8, 90));
            this.addSlot(new SlotItemHandler(handler, 7, 26, 90));

            // Item Output slots (2x2, top right area)
            this.addSlot(new SlotItemHandler(handler, 8, 116, 18));
            this.addSlot(new SlotItemHandler(handler, 9, 134, 18));
            this.addSlot(new SlotItemHandler(handler, 10, 116, 36));
            this.addSlot(new SlotItemHandler(handler, 11, 134, 36));

            // Fluid Container Output slots (2x2, below item outputs)
            this.addSlot(new SlotItemHandler(handler, 12, 116, 72));
            this.addSlot(new SlotItemHandler(handler, 13, 134, 72));
            this.addSlot(new SlotItemHandler(handler, 14, 116, 90));
            this.addSlot(new SlotItemHandler(handler, 15, 134, 90));

            // Template slots (4, bottom middle area)
            this.addSlot(new SlotItemHandler(handler, 16, 44, 126));
            this.addSlot(new SlotItemHandler(handler, 17, 62, 126));
            this.addSlot(new SlotItemHandler(handler, 18, 80, 126));
            this.addSlot(new SlotItemHandler(handler, 19, 98, 126));

            // Battery slot (bottom left)
            this.addSlot(new SlotItemHandler(handler, 20, 8, 144));

            // Template/Recipe slot (next to battery)
            this.addSlot(new SlotItemHandler(handler, 21, 44, 144));
        });

        // Player inventory (below machine GUI - starts at Y=160 to match texture)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 160 + row * 18));
            }
        }

        // Hotbar (at Y=218 to match texture)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 218));
        }
    }

    // Get fluid amount (synced via data slots)
    public int getFluidAmount() {
        return this.data.get(0);
    }

    // Get fluid ID (synced via data slots)
    public int getFluidId() {
        return this.data.get(1);
    }

    // Get progress (synced via data slots)
    public int getProgress() {
        return this.data.get(2);
    }

    // Get max progress (synced via data slots)
    public int getMaxProgress() {
        return this.data.get(3);
    }

    public FluidStack getFluid() {
        int amount = getFluidAmount();
        int fluidId = getFluidId();

        if (fluidId < 0 || amount <= 0) {
            return FluidStack.EMPTY;
        }

        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        return new FluidStack(fluid, amount);
    }

    public float getProgressScaled() {
        int progress = getProgress();
        int maxProgress = getMaxProgress();
        if (maxProgress == 0) return 0;
        return (float) progress / maxProgress;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack sourceStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            sourceStack = stackInSlot.copy();

            // From machine slots -> player inventory
            if (index < PLAYER_INVENTORY_START) {
                if (!moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From player inventory -> machine input slots
            else {
                // Try to put in item input slots first (0-3)
                if (!moveItemStackTo(stackInSlot, 0, 4, false)) {
                    // Try fluid container slots (4-7)
                    if (!moveItemStackTo(stackInSlot, 4, 8, false)) {
                        // If hotbar, try inventory
                        if (index >= HOTBAR_START) {
                            if (!moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else {
                            // From inventory to hotbar
                            if (!moveItemStackTo(stackInSlot, HOTBAR_START, HOTBAR_END, false)) {
                                return ItemStack.EMPTY;
                            }
                        }
                    }
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
=======
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
>>>>>>> origin/main
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.CHEMICAL_PLANT.get());
    }
}
