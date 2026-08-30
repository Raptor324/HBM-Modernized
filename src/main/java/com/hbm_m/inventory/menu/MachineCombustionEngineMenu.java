package com.hbm_m.inventory.menu;

import com.hbm_m.api.energy.ItemEnergyAccess;
import com.hbm_m.blockentity.machines.MachineCombustionEngineBlockEntity;
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

public class MachineCombustionEngineMenu extends AbstractContainerMenu {

    private final MachineCombustionEngineBlockEntity blockEntity;

    private static final int SLOT_BATTERY = MachineCombustionEngineBlockEntity.SLOT_BATTERY;
    private static final int SLOT_PISTON = MachineCombustionEngineBlockEntity.SLOT_PISTON;
    private static final int MACHINE_SLOT_COUNT = 2;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    public MachineCombustionEngineMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineCombustionEngineMenu(int id, Inventory inventory, MachineCombustionEngineBlockEntity blockEntity) {
        super(ModMenuTypes.COMBUSTION_ENGINE_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(container, SLOT_PISTON, 88, 71));

        this.addSlot(new Slot(container, SLOT_BATTERY, 143, 71) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (ItemEnergyAccess.getHbmProvider(stack).isPresent()) return true;
                //? if forge {
                return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
                //?} elif neoforge {
                /*return stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM) != null;
                *///?} else {
                /*return false;
                *///?}
            }
        });

        int playerInvX = 8;
        int playerInvY = 104;
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

    private static MachineCombustionEngineBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineCombustionEngineBlockEntity engine) {
            return engine;
        }
        throw new IllegalStateException("No MachineCombustionEngineBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":combustion_engine_menu");
    }

    public MachineCombustionEngineBlockEntity getBlockEntity() {
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
                boolean isEnergySource = ItemEnergyAccess.getHbmProvider(slotStack).isPresent();
                //? if forge {
                if (!isEnergySource) {
                    isEnergySource = slotStack.getCapability(ForgeCapabilities.ENERGY).isPresent();
                }
                //?} elif neoforge {
                /*if (!isEnergySource) {
                    isEnergySource = slotStack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM) != null;
                }
                *///?}
                if (isEnergySource) {
                    if (!this.moveItemStackTo(slotStack, SLOT_BATTERY, SLOT_BATTERY + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(slotStack, SLOT_PISTON, SLOT_PISTON + 1, false)) {
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
