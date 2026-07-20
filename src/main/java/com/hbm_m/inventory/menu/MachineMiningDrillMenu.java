package com.hbm_m.inventory.menu;

import com.hbm_m.api.energy.ItemEnergyAccess;
import com.hbm_m.blockentity.machines.MachineMiningDrillBlockEntity;
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

public class MachineMiningDrillMenu extends AbstractContainerMenu {

    private final MachineMiningDrillBlockEntity blockEntity;

    private static final int OUTPUT_START = 1;
    private static final int OUTPUT_COUNT = 9;
    private static final int MACHINE_SLOT_COUNT = 11;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    public MachineMiningDrillMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineMiningDrillMenu(int id, Inventory inventory, MachineMiningDrillBlockEntity blockEntity) {
        super(ModMenuTypes.MINING_DRILL_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(container, MachineMiningDrillBlockEntity.SLOT_DRILLBIT, 26, 20));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(container, OUTPUT_START + row * 3 + col, 110 + col * 18, 16 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false; // Nur Entnahme - wird von der Maschine befuellt.
                    }
                });
            }
        }

        this.addSlot(new Slot(container, MachineMiningDrillBlockEntity.SLOT_BATTERY, 134, 78) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (ItemEnergyAccess.getHbmProvider(stack).isPresent()) return true;
                //? if forge {
                return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
                //?}
                //? if fabric {
                /*return false;
                *///?}
            }
        });

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 180));
        }
    }

    public static MachineMiningDrillMenu create(int id, Inventory inventory, MachineMiningDrillBlockEntity blockEntity) {
        return new MachineMiningDrillMenu(id, inventory, blockEntity);
    }

    private static MachineMiningDrillBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineMiningDrillBlockEntity miningDrillBlockEntity) {
            return miningDrillBlockEntity;
        }
        throw new IllegalStateException("No MachineMiningDrillBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":mining_drill_menu");
    }

    public MachineMiningDrillBlockEntity getBlockEntity() {
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
                boolean isEnergySource = ItemEnergyAccess.getHbmProvider(slotStack).isPresent()
                        //? if forge {
                        || slotStack.getCapability(ForgeCapabilities.ENERGY).isPresent();
                        //?}
                if (isEnergySource) {
                    if (!this.moveItemStackTo(slotStack, MachineMiningDrillBlockEntity.SLOT_BATTERY, MachineMiningDrillBlockEntity.SLOT_BATTERY + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(slotStack, MachineMiningDrillBlockEntity.SLOT_DRILLBIT, MachineMiningDrillBlockEntity.SLOT_DRILLBIT + 1, false)) {
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
