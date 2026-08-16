package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The RBMK Console is a pure status/control panel - no inventory of its own, matching the
 * original 1.7.10 {@code ContainerRBMKConsole}. Previously had a leftover copy-pasted 27+9 player
 * inventory grid that didn't match the GUI texture (hotbar row rendered below the visible panel
 * entirely) and served no purpose since the BlockEntity has no item storage.
 */
public class MachineRbmkConsoleMenu extends AbstractContainerMenu {

    private final MachineRbmkConsoleBlockEntity blockEntity;

    public MachineRbmkConsoleMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineRbmkConsoleMenu(int id, Inventory inventory, MachineRbmkConsoleBlockEntity blockEntity) {
        super(ModMenuTypes.RBMK_CONSOLE_MENU.get(), id);
        this.blockEntity = blockEntity;
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
