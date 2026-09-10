package com.hbm_m.inventory.menu;

import com.hbm_m.api.energy.ItemEnergyAccess;
import com.hbm_m.blockentity.machines.MachineMiningLaserBlockEntity;
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

public class MachineMiningLaserMenu extends AbstractContainerMenu {

    private final MachineMiningLaserBlockEntity blockEntity;

    private static final int OUTPUT_START = MachineMiningLaserBlockEntity.OUTPUT_START;
    private static final int OUTPUT_COUNT = MachineMiningLaserBlockEntity.OUTPUT_COUNT;
    private static final int UPGRADE_START = MachineMiningLaserBlockEntity.UPGRADE_START;
    private static final int SLOT_BATTERY = MachineMiningLaserBlockEntity.SLOT_BATTERY;
    private static final int MACHINE_SLOT_COUNT = OUTPUT_START + OUTPUT_COUNT;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    public MachineMiningLaserMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineMiningLaserMenu(int id, Inventory inventory, MachineMiningLaserBlockEntity blockEntity) {
        super(ModMenuTypes.MINING_LASER_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        // 1:1 aus {@code ContainerMiningLaser}: acht Upgrades rechts oben, 7x3 Ausgabe darunter.
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                this.addSlot(new Slot(container, UPGRADE_START + row * 4 + col, 98 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade;
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }
                });
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 7; col++) {
                this.addSlot(new Slot(container, OUTPUT_START + row * 7 + col, 44 + col * 18, 72 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false; // Nur Entnahme - wird von der Maschine befuellt.
                    }
                });
            }
        }

        this.addSlot(new Slot(container, SLOT_BATTERY, 8, 108) {
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
        int playerInvY = 140;
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

    private static MachineMiningLaserBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineMiningLaserBlockEntity miningLaser) {
            return miningLaser;
        }
        throw new IllegalStateException("No MachineMiningLaserBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":mining_laser_menu");
    }

    public MachineMiningLaserBlockEntity getBlockEntity() {
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
                if (!this.moveItemStackTo(slotStack, SLOT_BATTERY, SLOT_BATTERY + 1, false)) {
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
