package com.hbm_m.inventory.menu;

import com.hbm_m.block.entity.machines.rbmk.RBMKRodBlockEntity;
import com.hbm_m.item.rbmk.RBMKRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RBMKRodMenu extends AbstractContainerMenu {

    private final RBMKRodBlockEntity blockEntity;

    public RBMKRodMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf));
    }

    public RBMKRodMenu(int id, Inventory inv, RBMKRodBlockEntity be) {
        super(ModMenuTypes.RBMK_ROD_MENU.get(), id);
        this.blockEntity = be;

        SimpleContainer container = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                be.fuelSlot = getItem(0).copy();
                be.setChanged();
            }
        };
        container.setItem(0, be.fuelSlot.copy());

        // Fuel rod slot — centered in the info panel (slot 0)
        addSlot(new Slot(container, 0, 8, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof RBMKRodItem;
            }
        });
        // No player inventory: rod insertion happens via right-click in world or drag onto the slot above
    }

    private static RBMKRodBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof RBMKRodBlockEntity rbmk) return rbmk;
        throw new IllegalStateException("No RBMKRodBlockEntity at " + pos);
    }

    public RBMKRodBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() == player.level()
            && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64;
    }

    // Prevent any manual interaction with the fuel slot when the rod is too hot to handle.
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == 0 && !player.isCreative() && !blockEntity.coldEnoughForManual()) return;
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // no player inventory in this menu
    }
}
