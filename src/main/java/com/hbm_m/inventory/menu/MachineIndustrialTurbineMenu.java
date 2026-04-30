package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.machines.MachineIndustrialTurbineBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

public class MachineIndustrialTurbineMenu extends AbstractContainerMenu {

    private final MachineIndustrialTurbineBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    private static final int SLOT_STEAM_IN = MachineIndustrialTurbineBlockEntity.SLOT_STEAM_IN;
    private static final int SLOT_STEAM_OUT = MachineIndustrialTurbineBlockEntity.SLOT_STEAM_OUT;
    private static final int SLOT_SPENT_IN = MachineIndustrialTurbineBlockEntity.SLOT_SPENT_IN;
    private static final int SLOT_SPENT_OUT = MachineIndustrialTurbineBlockEntity.SLOT_SPENT_OUT;
    private static final int MACHINE_SLOTS = MachineIndustrialTurbineBlockEntity.INVENTORY_SIZE;

    public MachineIndustrialTurbineMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, getBlockEntity(playerInventory, extraData), new SimpleContainerData(7));
    }

    public MachineIndustrialTurbineMenu(int id, Inventory playerInventory, MachineIndustrialTurbineBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.INDUSTRIAL_TURBINE_MENU.get(), id);
        checkContainerDataCount(data, 7);

        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();
        this.data = data;

        addDataSlots(data);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            this.addSlot(new SlotItemHandler(handler, SLOT_STEAM_IN, 9, 17));
            this.addSlot(new SlotItemHandler(handler, SLOT_STEAM_OUT, 9, 53) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });

            this.addSlot(new SlotItemHandler(handler, SLOT_SPENT_IN, 153, 17));
            this.addSlot(new SlotItemHandler(handler, SLOT_SPENT_OUT, 153, 53) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    private static MachineIndustrialTurbineBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity entity = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (entity instanceof MachineIndustrialTurbineBlockEntity turbine) {
            return turbine;
        }
        throw new IllegalStateException("BlockEntity is not an Industrial Turbine");
    }

    public int getSteamAmount() {
        return data.get(0);
    }

    public int getSteamCapacity() {
        return data.get(1);
    }

    public int getSpentAmount() {
        return data.get(2);
    }

    public int getSpentCapacity() {
        return data.get(3);
    }

    public boolean isActive() {
        return data.get(4) > 0;
    }

    public long getEnergyStoredLong() {
        long low = data.get(5) & 0xFFFFFFFFL;
        long high = data.get(6) & 0xFFFFFFFFL;
        return (high << 32) | low;
    }

    public long getMaxEnergyStoredLong() {
        return blockEntity.getMaxEnergyStored();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int playerStart = MACHINE_SLOTS;
        int playerEnd = this.slots.size();

        if (index < MACHINE_SLOTS) {
            if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, SLOT_STEAM_IN, SLOT_STEAM_IN + 1, false)
                    && !this.moveItemStackTo(stack, SLOT_SPENT_IN, SLOT_SPENT_IN + 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.INDUSTRIAL_TURBINE.get());
    }
}