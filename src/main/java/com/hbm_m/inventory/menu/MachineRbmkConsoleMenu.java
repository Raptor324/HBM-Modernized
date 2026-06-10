package com.hbm_m.inventory.menu;

import com.hbm_m.block.entity.machines.MachineRbmkConsoleBlockEntity;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachineRbmkConsoleMenu extends AbstractContainerMenu {

    private final MachineRbmkConsoleBlockEntity blockEntity;

    public MachineRbmkConsoleMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineRbmkConsoleMenu(int id, Inventory inventory, MachineRbmkConsoleBlockEntity blockEntity) {
        super(ModMenuTypes.RBMK_CONSOLE_MENU.get(), id);
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

    public static MachineRbmkConsoleMenu create(int id, Inventory inventory, MachineRbmkConsoleBlockEntity blockEntity) {
        return new MachineRbmkConsoleMenu(id, inventory, blockEntity);
    }

    private static MachineRbmkConsoleBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineRbmkConsoleBlockEntity rbmkConsoleBlockEntity) {
            return rbmkConsoleBlockEntity;
        }
        throw new IllegalStateException("No MachineRbmkConsoleBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":rbmk_console_menu");
    }

    public MachineRbmkConsoleBlockEntity getBlockEntity() {
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
