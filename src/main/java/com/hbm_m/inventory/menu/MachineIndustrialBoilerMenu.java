package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineIndustrialBoilerBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}

/**
 * Menu for the Industrial Boiler - two fluid-container slot pairs (water in/out, steam in/out)
 * feeding {@code waterTank}/{@code steamTank}, matching {@link MachineIndustrialBoilerBlockEntity}'s
 * inventory layout 1:1. No battery slot: the boiler is powered purely via the wired HBM/FE energy
 * network (see {@code BaseMachineBlockEntity}'s capability wiring), same as the basic Boiler.
 */
public class MachineIndustrialBoilerMenu extends AbstractContainerMenu {

    private final MachineIndustrialBoilerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;
    private final ModItemStackHandlerContainer machineInventory;

    private static final int MACHINE_SLOTS = MachineIndustrialBoilerBlockEntity.INVENTORY_SIZE;

    public MachineIndustrialBoilerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData), new net.minecraft.world.inventory.SimpleContainerData(8));
    }

    public MachineIndustrialBoilerMenu(int containerId, Inventory playerInventory,
                                        MachineIndustrialBoilerBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.INDUSTRIAL_BOILER_MENU.get(), containerId);

        checkContainerDataCount(data, 8);

        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();
        this.data = data;

        addDataSlots(data);

        this.machineInventory = new ModItemStackHandlerContainer(this.blockEntity.getInventory(), this.blockEntity::setChanged);

        // Water: fill-container-in (drains into tank) / empty-container-out
        this.addSlot(new Slot(machineInventory, MachineIndustrialBoilerBlockEntity.SLOT_WATER_IN, 44, 16) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return com.hbm_m.platform.PlatformHooks.isFluidContainer(stack);
            }
        });
        this.addSlot(new Slot(machineInventory, MachineIndustrialBoilerBlockEntity.SLOT_WATER_OUT, 44, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Steam: empty-container-in (gets filled from tank) / filled-container-out
        this.addSlot(new Slot(machineInventory, MachineIndustrialBoilerBlockEntity.SLOT_STEAM_IN, 151, 16) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return com.hbm_m.platform.PlatformHooks.isFluidContainer(stack);
            }
        });
        this.addSlot(new Slot(machineInventory, MachineIndustrialBoilerBlockEntity.SLOT_STEAM_OUT, 151, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        int playerInvX = 8;
        int playerInvY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        playerInvX + col * 18, playerInvY + row * 18));
            }
        }

        int hotbarY = 142;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, playerInvX + col * 18, hotbarY));
        }
    }

    private static MachineIndustrialBoilerBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (blockEntity instanceof MachineIndustrialBoilerBlockEntity boiler) {
            return boiler;
        }
        throw new IllegalStateException("BlockEntity is not an Industrial Boiler");
    }

    public MachineIndustrialBoilerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getWaterAmount() { return data.get(0); }
    public int getWaterCapacity() { return data.get(1); }
    public int getSteamAmount() { return data.get(2); }
    public int getSteamCapacity() { return data.get(3); }
    public int getHeat() { return data.get(4); }
    public boolean isOn() { return data.get(5) != 0; }

    public long getEnergyLong() {
        if (blockEntity != null && !level.isClientSide()) {
            return blockEntity.getEnergyStored();
        }
        return (data.get(6) & 0xFFFFFFFFL) | ((long) data.get(7) << 32);
    }

    public long getMaxEnergyLong() {
        if (blockEntity != null) {
            return blockEntity.getMaxEnergyStored();
        }
        return 0L;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getItem();
        ItemStack copy = slotStack.copy();

        int playerInventoryStart = MACHINE_SLOTS;
        int playerInventoryEnd = this.slots.size();

        if (index < MACHINE_SLOTS) {
            if (!this.moveItemStackTo(slotStack, playerInventoryStart, playerInventoryEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean isFluidContainer = com.hbm_m.platform.PlatformHooks.isFluidContainer(slotStack);
            if (isFluidContainer) {
                if (!this.moveItemStackTo(slotStack, MachineIndustrialBoilerBlockEntity.SLOT_WATER_IN,
                        MachineIndustrialBoilerBlockEntity.SLOT_WATER_IN + 1, false)
                        && !this.moveItemStackTo(slotStack, MachineIndustrialBoilerBlockEntity.SLOT_STEAM_IN,
                        MachineIndustrialBoilerBlockEntity.SLOT_STEAM_IN + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, slotStack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.INDUSTRIAL_BOILER.get());
    }
}
