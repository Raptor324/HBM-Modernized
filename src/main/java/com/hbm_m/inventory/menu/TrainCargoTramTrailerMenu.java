package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.entity.train.TrainCargoTramTrailer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code TrainCargoTramTrailer.ContainerTrainCargoTramTrailer}: 5x9 Ladeplaetze, Spielerinventar ab Y 140. */
public class TrainCargoTramTrailerMenu extends AbstractContainerMenu {

    @Nullable public final TrainCargoTramTrailer train;
    private final Container inventory;

    public TrainCargoTramTrailerMenu(int id, Inventory invPlayer, FriendlyByteBuf buf) {
        this(id, invPlayer, invPlayer.player.level().getEntity(buf.readVarInt()) instanceof TrainCargoTramTrailer t ? t : null);
    }

    public TrainCargoTramTrailerMenu(int id, Inventory invPlayer, @Nullable TrainCargoTramTrailer train) {
        super(ModMenuTypes.TRAIN_CARGO_TRAM_TRAILER_MENU.get(), id);
        this.train = train;
        this.inventory = train != null ? train : new SimpleContainer(45);

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inventory, i * 9 + j, 8 + j * 18, 18 + i * 18));
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 140 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(invPlayer, i, 8 + i * 18, 198));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        ItemStack stackCopy = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            stackCopy = stack.copy();
            if (slotIndex < inventory.getContainerSize()) {
                if (!this.moveItemStackTo(stack, inventory.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, 45, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return stackCopy;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return inventory.stillValid(player);
    }
}
