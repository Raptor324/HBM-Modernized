package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.decorations.FileCabinetBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ContainerFileCabinet}: 2x4 Faecher (53/18, Zeilenabstand 36), Spielerinventar ab 8/88. */
public class FileCabinetMenu extends AbstractContainerMenu {

    public final FileCabinetBlockEntity cabinet;

    public FileCabinetMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, resolve(inv, extraData.readBlockPos()));
    }

    private static FileCabinetBlockEntity resolve(Inventory inv, BlockPos pos) {
        return inv.player.level().getBlockEntity(pos) instanceof FileCabinetBlockEntity be ? be
                : new FileCabinetBlockEntity(pos, com.hbm_m.block.ModBlocks.FILE_CABINET.get().defaultBlockState());
    }

    public FileCabinetMenu(int containerId, Inventory inv, FileCabinetBlockEntity tile) {
        super(ModMenuTypes.FILE_CABINET_MENU.get(), containerId);
        this.cabinet = tile;
        this.cabinet.startOpen();
        var container = new ModItemStackHandlerContainer(tile.items, tile::setChanged);

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 4; j++) {
                this.addSlot(new Slot(container, j + i * 4, 53 + j * 18, 18 + i * 36));
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 88 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 146));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack returnStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            returnStack = originalStack.copy();

            if (index <= 7) {
                if (!this.moveItemStackTo(originalStack, 8, this.slots.size(), true)) return ItemStack.EMPTY;
                slot.onQuickCraft(originalStack, returnStack);
            } else if (!this.moveItemStackTo(originalStack, 0, 8, false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return returnStack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return cabinet.getLevel() != null && cabinet.getLevel().getBlockEntity(cabinet.getBlockPos()) == cabinet
                && player.distanceToSqr(cabinet.getBlockPos().getX() + 0.5D, cabinet.getBlockPos().getY() + 0.5D, cabinet.getBlockPos().getZ() + 0.5D) <= 64;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.cabinet.stopOpen();
    }
}
