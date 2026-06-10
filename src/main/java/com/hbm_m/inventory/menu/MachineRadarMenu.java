package com.hbm_m.inventory.menu;

import com.hbm_m.block.entity.machines.MachineRadarBlockEntity;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachineRadarMenu extends AbstractContainerMenu {

    private final MachineRadarBlockEntity blockEntity;

    public MachineRadarMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineRadarMenu(int id, Inventory inventory, MachineRadarBlockEntity blockEntity) {
        super(ModMenuTypes.RADAR_MENU.get(), id);
        this.blockEntity = blockEntity;
    }

    public static MachineRadarMenu create(int id, Inventory inventory, MachineRadarBlockEntity blockEntity) {
        return new MachineRadarMenu(id, inventory, blockEntity);
    }

    private static MachineRadarBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineRadarBlockEntity radarBlockEntity) {
            return radarBlockEntity;
        }
        throw new IllegalStateException("No MachineRadarBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":radar_menu");
    }

    public MachineRadarBlockEntity getBlockEntity() {
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
