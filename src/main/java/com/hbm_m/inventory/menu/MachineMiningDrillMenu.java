package com.hbm_m.inventory.menu;

import com.hbm_m.block.entity.machines.MachineMiningDrillBlockEntity;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachineMiningDrillMenu extends AbstractContainerMenu {

    private final MachineMiningDrillBlockEntity blockEntity;

    public MachineMiningDrillMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineMiningDrillMenu(int id, Inventory inventory, MachineMiningDrillBlockEntity blockEntity) {
        super(ModMenuTypes.MINING_DRILL_MENU.get(), id);
        this.blockEntity = blockEntity;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
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
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}