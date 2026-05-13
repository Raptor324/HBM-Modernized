package com.hbm_m.inventory.menu;

import com.hbm_m.block.entity.machines.MachineZirnoxBlockEntity;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachineZirnoxMenu extends AbstractContainerMenu {

    private final MachineZirnoxBlockEntity blockEntity;

    public MachineZirnoxMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineZirnoxMenu(int id, Inventory inventory, MachineZirnoxBlockEntity blockEntity) {
        super(ModMenuTypes.ZIRNOX_MENU.get(), id);
        this.blockEntity = blockEntity;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public static MachineZirnoxMenu create(int id, Inventory inventory, MachineZirnoxBlockEntity blockEntity) {
        return new MachineZirnoxMenu(id, inventory, blockEntity);
    }

    private static MachineZirnoxBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineZirnoxBlockEntity zirnoxBlockEntity) {
            return zirnoxBlockEntity;
        }
        throw new IllegalStateException("No MachineZirnoxBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":zirnox_menu");
    }

    public MachineZirnoxBlockEntity getBlockEntity() {
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
