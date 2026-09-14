package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.network.pneumatic.PneumoStorageImporterBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1-Port von {@code ContainerPneumoStorageImporter} (1.7.10): drei mal drei Plaetze bei (62, 17),
 * das Spielerinventar auf Hoehe 103.
 */
public class PneumoStorageImporterMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = PneumoStorageImporterBlockEntity.INVENTORY_SIZE;

    private final PneumoStorageImporterBlockEntity blockEntity;

    public PneumoStorageImporterMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public PneumoStorageImporterMenu(int id, Inventory inv, PneumoStorageImporterBlockEntity blockEntity) {
        super(ModMenuTypes.PNEUMO_STORAGE_IMPORTER_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                this.addSlot(new Slot(container, index, 62 + col * 18, 17 + row * 18) {
                    @Override
                    public void setChanged() {
                        super.setChanged();
                        // Original: Einlegen weckt den Platz sofort aus seiner Sperre.
                        blockEntity.wakeSlot(index);
                    }
                });
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 103 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 161));
        }
    }

    private static PneumoStorageImporterBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof PneumoStorageImporterBlockEntity importer) return importer;
        throw new IllegalStateException("No PneumoStorageImporterBlockEntity at " + pos);
    }

    public PneumoStorageImporterBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() != player.level()) return false;
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }
}
