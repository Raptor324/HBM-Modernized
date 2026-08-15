package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.interfaces.IItemFluidIdentifier;

import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import dev.architectury.fluid.FluidStack;

//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;
//?}

/**
 * Direct clone of {@link BarrelSteelMenu}, registered/validated against the corroded barrel. The
 * load/unload item slots are present for layout consistency but functionally inert while the
 * barrel is in its permanent "exploded"/leaking state (see {@code MachineFluidTankBlockEntity}'s
 * {@code tick}, which skips all item-slot processing once {@code hasExploded} is set).
 */
@SuppressWarnings("UnstableApiUsage")
public class BarrelCorrodedMenu extends AbstractContainerMenu {

    public final MachineFluidTankBlockEntity blockEntity;
    private final ContainerData data;

    private static final int MACHINE_SLOTS = 6;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOTS;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    public BarrelCorrodedMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(7));
    }

    public BarrelCorrodedMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.BARREL_CORRODED_MENU.get(), id);
        this.blockEntity = (MachineFluidTankBlockEntity) entity;
        this.data = data;

        checkContainerDataCount(data, 7);

        //? if forge {
        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            this.addSlot(new SlotItemHandler(handler, MachineFluidTankBlockEntity.SLOT_ID_IN, 8, 17) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return stack.getItem() instanceof IItemFluidIdentifier;
                }
            });

            this.addSlot(new SlotItemHandler(handler, MachineFluidTankBlockEntity.SLOT_ID_OUT, 8, 53) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });

            this.addSlot(new SlotItemHandler(handler, MachineFluidTankBlockEntity.SLOT_LOAD_IN, 35, 17) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
                }
            });

            this.addSlot(new SlotItemHandler(handler, MachineFluidTankBlockEntity.SLOT_LOAD_OUT, 35, 53) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });

            this.addSlot(new SlotItemHandler(handler, MachineFluidTankBlockEntity.SLOT_UNLOAD_IN, 125, 17) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
                }
            });

            this.addSlot(new SlotItemHandler(handler, MachineFluidTankBlockEntity.SLOT_UNLOAD_OUT, 125, 53) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
        });
        //?}

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
            return FluidStack.empty();
        }

        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        return FluidStack.create(fluid, (long) amount);
    }

    public int getTankTypeFluidId() {
        return this.data.get(1);
    }

    public Fluid getTankTypeFluid() {
        int id = getTankTypeFluidId();
        if (id < 0) {
            return Fluids.EMPTY;
        }
        Fluid f = BuiltInRegistries.FLUID.byId(id);
        return f != null ? f : Fluids.EMPTY;
    }

    public int getMode() {
        return this.data.get(2);
    }

    public int getPressure() {
        return this.data.get(5);
    }

    public int getFilterFluidId() {
        return this.data.get(6);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack sourceStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            sourceStack = stackInSlot.copy();

            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (stackInSlot.getItem() instanceof IItemFluidIdentifier) {
                    if (!moveItemStackTo(stackInSlot, MACHINE_SLOTS + 0, MACHINE_SLOTS + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
                    if (!moveItemStackTo(stackInSlot, MachineFluidTankBlockEntity.SLOT_LOAD_IN, MachineFluidTankBlockEntity.SLOT_LOAD_IN + 1, false)) {
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
                player, ModBlocks.BARREL_CORRODED.get());
    }
}
