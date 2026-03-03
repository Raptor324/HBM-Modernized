package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.item.IItemFluidIdentifier;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class MachineFluidTankMenu extends AbstractContainerMenu {

    public final MachineFluidTankBlockEntity blockEntity;
    private final ContainerData data;

    // Слоты машины (6 штук, как в оригинале)
    private static final int MACHINE_SLOTS = 6;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOTS;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    public MachineFluidTankMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(6));
    }

    public MachineFluidTankMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.FLUID_TANK_MENU.get(), id);
        this.blockEntity = (MachineFluidTankBlockEntity) entity;
        this.data = data;

        checkContainerDataCount(data, 4);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            // Fluid identifier input: (8, 17) - только IItemFluidIdentifier
            this.addSlot(new SlotItemHandler(handler, MachineFluidTankBlockEntity.SLOT_ID_IN, 8, 17) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return stack.getItem() instanceof IItemFluidIdentifier;
                }
            });
            
            // Fluid identifier output: (8, 53) - только для вывода
            this.addSlot(new SlotItemHandler(handler, MachineFluidTankBlockEntity.SLOT_ID_OUT, 8, 53) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false; // Только для вывода
                }
            });

            // Load input: (35, 17) - только предметы с FLUID_HANDLER_ITEM
            this.addSlot(new SlotItemHandler(handler, MachineFluidTankBlockEntity.SLOT_LOAD_IN, 35, 17) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
                }
            });
            
            // Load output: (35, 53) - только для вывода
            this.addSlot(new SlotItemHandler(handler, MachineFluidTankBlockEntity.SLOT_LOAD_OUT, 35, 53) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false; // Только для вывода
                }
            });

            // Unload input: (125, 17) - только предметы с FLUID_HANDLER_ITEM
            this.addSlot(new SlotItemHandler(handler, MachineFluidTankBlockEntity.SLOT_UNLOAD_IN, 125, 17) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
                }
            });
            
            // Unload output: (125, 53) - только для вывода
            this.addSlot(new SlotItemHandler(handler, MachineFluidTankBlockEntity.SLOT_UNLOAD_OUT, 125, 53) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false; // Только для вывода
                }
            });
        });

        addDataSlots(data);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    public FluidStack getFluid() {
        int amount = this.data.get(0);
        int fluidId = this.data.get(1);

        if (fluidId < 0 || amount <= 0) {
            return FluidStack.EMPTY;
        }

        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        return new FluidStack(fluid, amount);
    }

    public int getMode() {
        return this.data.get(2);
    }

    /** Filter fluid ID for tooltip when tank is empty (-1 if no filter) */
    public int getFilterFluidId() {
        return this.data.get(3);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack sourceStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            sourceStack = stackInSlot.copy();

            // Move from machine slots to player inventory
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player inventory to machine slots
                if (stackInSlot.getItem() instanceof IItemFluidIdentifier) {
                    // Put into the input fluid identifier slot
                    if (!moveItemStackTo(stackInSlot, MACHINE_SLOTS + 0, MACHINE_SLOTS + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
                    // Try to put into LOAD_IN slot first
                    if (!moveItemStackTo(stackInSlot, MachineFluidTankBlockEntity.SLOT_LOAD_IN, MachineFluidTankBlockEntity.SLOT_LOAD_IN + 1, false)) {
                        // If that fails, try UNLOAD_IN slot
                        if (!moveItemStackTo(stackInSlot, MachineFluidTankBlockEntity.SLOT_UNLOAD_IN, MachineFluidTankBlockEntity.SLOT_UNLOAD_IN + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else {
                    return ItemStack.EMPTY;
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
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.FLUID_TANK.get());
    }
}
