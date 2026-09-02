package com.hbm_m.inventory.menu;

import com.hbm_m.api.energy.ItemEnergyAccess;
import com.hbm_m.blockentity.machines.MachineAutocrafterBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}

public class MachineAutocrafterMenu extends AbstractContainerMenu {

    private final MachineAutocrafterBlockEntity blockEntity;

    private static final int SLOT_OUTPUT = MachineAutocrafterBlockEntity.SLOT_OUTPUT;
    private static final int SLOT_BATTERY = MachineAutocrafterBlockEntity.SLOT_BATTERY;
    private static final int MACHINE_SLOT_COUNT = 11;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    public MachineAutocrafterMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineAutocrafterMenu(int id, Inventory inventory, MachineAutocrafterBlockEntity blockEntity) {
        super(ModMenuTypes.AUTOCRAFTER_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(container, row * 3 + col, 30 + col * 18, 17 + row * 18));
            }
        }

        this.addSlot(new Slot(container, SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Nur Entnahme - wird von der Maschine befuellt.
            }
        });

        this.addSlot(new Slot(container, SLOT_BATTERY, 17, 71) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isEnergyItem(stack);
            }
        });

        int playerInvX = 8;
        int playerInvY = 108;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, playerInvX + col * 18, playerInvY + row * 18));
            }
        }
        int hotbarY = playerInvY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, playerInvX + col * 18, hotbarY));
        }
    }

    private static boolean isEnergyItem(ItemStack stack) {
        if (ItemEnergyAccess.getHbmProvider(stack).isPresent()) return true;
        //? if forge {
        return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
        //?} elif neoforge {
        /*return stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM) != null;
        *///?} else {
        /*return false;
        *///?}
    }

    private static MachineAutocrafterBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineAutocrafterBlockEntity autocrafter) {
            return autocrafter;
        }
        throw new IllegalStateException("No MachineAutocrafterBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":autocrafter_menu");
    }

    public MachineAutocrafterBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() != player.level()) {
            return false;
        }
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                boolean isEnergySource = isEnergyItem(slotStack);
                if (isEnergySource) {
                    if (!this.moveItemStackTo(slotStack, SLOT_BATTERY, SLOT_BATTERY + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(slotStack, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, slotStack);
        }
        return result;
    }
}
