package com.hbm_m.inventory.menu;

import com.hbm_m.block.entity.bomb.NukeFatManBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class NukeFatManMenu extends AbstractContainerMenu {

    public final NukeFatManBlockEntity be;

    public NukeFatManMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, (NukeFatManBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public NukeFatManMenu(int id, Inventory inventory, NukeFatManBlockEntity blockEntity) {
        super(ModMenuTypes.NUKE_FAT_MAN_MENU.get(), id);
        this.be = blockEntity;

        addSlot(new Slot(be, 0, 8, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) { return be.canPlaceItem(0, stack); }
        });
        addSlot(new Slot(be, 1, 44, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) { return be.canPlaceItem(1, stack); }
        });
        addSlot(new Slot(be, 2, 8, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) { return be.canPlaceItem(2, stack); }
        });
        addSlot(new Slot(be, 3, 44, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) { return be.canPlaceItem(3, stack); }
        });
        addSlot(new Slot(be, 4, 26, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return be.canPlaceItem(4, stack); }
        });
        addSlot(new Slot(be, 5, 98, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return be.canPlaceItem(5, stack); }
        });

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(inventory, x + y * 9 + 9, 8 + x * 18, 84 + y * 18));
            }
        }
        for (int x = 0; x < 9; x++) {
            addSlot(new Slot(inventory, x, 8 + x * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return be.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
