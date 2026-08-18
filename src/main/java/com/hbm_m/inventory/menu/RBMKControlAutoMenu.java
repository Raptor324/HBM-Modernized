package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.rbmk.RBMKControlAutoBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Port of {@code ContainerRBMKControlAuto} (1.7.10 Original).
 * Like its {@link RBMKControlMenu} sibling, the automated control rod has no
 * machine-specific item slots — it's purely a settings/status screen, so this
 * menu only exposes the player's inventory.
 */
public class RBMKControlAutoMenu extends AbstractContainerMenu {

    private final RBMKControlAutoBlockEntity blockEntity;

    public RBMKControlAutoMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf));
    }

    public RBMKControlAutoMenu(int id, Inventory inv, RBMKControlAutoBlockEntity be) {
        super(ModMenuTypes.RBMK_CONTROL_AUTO_MENU.get(), id);
        this.blockEntity = be;

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }

    private static RBMKControlAutoBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof RBMKControlAutoBlockEntity c) return c;
        throw new IllegalStateException("No RBMKControlAutoBlockEntity at " + pos);
    }

    public RBMKControlAutoBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() == player.level()
            && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
