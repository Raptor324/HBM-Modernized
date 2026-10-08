package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.DummyItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code ContainerCrucible}: neun Schmelzplaetze (Platz 1-9 des Tiegels) im 3x3-Raster ab (107,18), je ein Stueck;
 * Spielerinventar ab (8,132). Shift-Klick verteilt in die Schmelzplaetze.
 */
public class MachineCrucibleMenu extends AbstractContainerMenu {

    private final MachineCrucibleBlockEntity crucible;

    public MachineCrucibleMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineCrucibleMenu(int id, Inventory invPlayer, MachineCrucibleBlockEntity crucible) {
        super(ModMenuTypes.CRUCIBLE_MENU.get(), id);
        this.crucible = crucible;

        var container = new ModItemStackHandlerContainer(
                crucible != null ? crucible.getInventory() : new DummyItemStackHandler(10),
                crucible != null ? crucible::setChanged : null);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new Slot(container, j + i * 3 + 1, 107 + j * 18, 18 + i * 18) {
                    @Override public int getMaxStackSize() { return 1; }
                });
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 132 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(invPlayer, i, 8 + i * 18, 190));
        }
    }

    private static MachineCrucibleBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        if (be instanceof MachineCrucibleBlockEntity crucible) return crucible;
        if (inventory.player.level().isClientSide) return null;
        throw new IllegalStateException("No MachineCrucibleBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":crucible_menu");
    }

    public MachineCrucibleBlockEntity getBlockEntity() {
        return crucible;
    }

    @Override
    public boolean stillValid(Player player) {
        // w16b: Original isUseableByPlayer (TileEntityMachineBase) = 128 vom Kern, dazu Huelle der Maschine (MultiblockMenuReach)
        return MultiblockMenuReach.stillValidCore(crucible, player, 128.0D);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack originalStack = slot.getItem();
        ItemStack stack = originalStack.copy();

        if (index <= 8) {
            if (!this.moveItemStackTo(originalStack, 9, this.slots.size(), true)) return ItemStack.EMPTY;
            slot.onQuickCraft(originalStack, stack);
        } else {
            // Original InventoryUtil.mergeItemStack: verteilt einzeln auf die Plaetze
            boolean moved = false;
            for (int i = 0; i < 9 && !originalStack.isEmpty(); i++) {
                Slot target = this.slots.get(i);
                if (!target.hasItem() && target.mayPlace(originalStack)) {
                    target.set(originalStack.split(1));
                    moved = true;
                }
            }
            if (!moved) return ItemStack.EMPTY;
        }

        if (originalStack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return stack;
    }
}
