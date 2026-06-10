package com.hbm_m.inventory.menu;

import com.hbm_m.block.entity.machines.MachineFlareStackBlockEntity;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachineFlareStackMenu extends AbstractContainerMenu {

    private final MachineFlareStackBlockEntity blockEntity;

    public MachineFlareStackMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineFlareStackMenu(int id, Inventory inventory, MachineFlareStackBlockEntity blockEntity) {
        super(ModMenuTypes.FLARE_STACK_MENU.get(), id);
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

    public static MachineFlareStackMenu create(int id, Inventory inventory, MachineFlareStackBlockEntity blockEntity) {
        return new MachineFlareStackMenu(id, inventory, blockEntity);
    }

    private static MachineFlareStackBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineFlareStackBlockEntity flareStackBlockEntity) {
            return flareStackBlockEntity;
        }
        throw new IllegalStateException("No MachineFlareStackBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":flare_stack_menu");
    }

    public MachineFlareStackBlockEntity getBlockEntity() {
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
