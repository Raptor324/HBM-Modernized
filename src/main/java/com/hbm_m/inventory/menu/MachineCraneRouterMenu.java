package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.network.MachineCraneRouterBlockEntity;
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

public class MachineCraneRouterMenu extends AbstractContainerMenu {

    private final MachineCraneRouterBlockEntity blockEntity;

    public MachineCraneRouterMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineCraneRouterMenu(int id, Inventory inventory, MachineCraneRouterBlockEntity blockEntity) {
        super(ModMenuTypes.CRANE_ROUTER_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        // 6 groups of 5 (one per side), arranged 2 columns x 3 rows of groups.
        for (int col = 0; col < 2; col++) {
            for (int row = 0; row < 3; row++) {
                int side = col * 3 + row;
                for (int k = 0; k < 5; k++) {
                    int slotIndex = side * 5 + k;
                    this.addSlot(new Slot(container, slotIndex, 34 + k * 18 + col * 98, 17 + row * 26) {
                        @Override
                        public void set(ItemStack stack) {
                            super.set(stack);
                            blockEntity.initPattern(slotIndex, stack);
                        }
                    });
                }
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 47 + col * 18, 119 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 47 + col * 18, 177));
        }
    }

    public static MachineCraneRouterMenu create(int id, Inventory inventory, MachineCraneRouterBlockEntity blockEntity) {
        return new MachineCraneRouterMenu(id, inventory, blockEntity);
    }

    private static MachineCraneRouterBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineCraneRouterBlockEntity routerBlockEntity) {
            return routerBlockEntity;
        }
        throw new IllegalStateException("No MachineCraneRouterBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":crane_router_menu");
    }

    public MachineCraneRouterBlockEntity getBlockEntity() {
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

    /** Filter slots are never shift-click transferable, matching the original. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
