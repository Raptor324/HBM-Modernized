package com.hbm_m.inventory.menu;

import com.hbm_m.block.bomb.LargeNukeType;
import com.hbm_m.blockentity.bomb.LargeNukeBlockEntity;
import com.hbm_m.inventory.menu.ModMenuTypes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Общее меню больших ядерных бомб; раскладка слотов зависит от LargeNukeType.
 */
public class LargeNukeMenu extends AbstractContainerMenu {

    public final LargeNukeBlockEntity be;
    public final LargeNukeType type;

    public LargeNukeMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, (extraData == null ? null : (LargeNukeBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos())));
    }

    public LargeNukeMenu(int id, Inventory inventory, LargeNukeBlockEntity blockEntity) {
        super(ModMenuTypes.LARGE_NUKE_MENU.get(), id);
        this.be = blockEntity;
        this.type = blockEntity.getNukeType();

        for (int slot = 0; slot < type.slots(); slot++) {
            final int index = slot;
            addSlot(new Slot(be, index, type.slotX(index), type.slotY(index)) {
                @Override
                public boolean mayPlace(ItemStack stack) { return be.canPlaceItem(index, stack); }
            });
        }

        int invX = type.inventoryX();
        int invY = type.inventoryY();
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(inventory, x + y * 9 + 9, invX + x * 18, invY + y * 18));
            }
        }
        for (int x = 0; x < 9; x++) {
            addSlot(new Slot(inventory, x, invX + x * 18, invY + 58));
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

