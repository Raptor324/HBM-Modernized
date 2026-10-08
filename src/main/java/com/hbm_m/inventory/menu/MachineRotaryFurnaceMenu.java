package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineRotaryFurnaceBlockEntity;
import com.hbm_m.interfaces.IItemFluidIdentifier;
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
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code ContainerMachineRotaryFurnace}: Eingaben (8,18)/(26,18)/(44,18), Fluid-ID (8,54), Brennstoff (44,54),
 * Spielerinventar ab (8,104). Shift-Klick: Brennstoff, Identifikator, sonst Eingaben.
 */
public class MachineRotaryFurnaceMenu extends AbstractContainerMenu {

    private final MachineRotaryFurnaceBlockEntity furnace;

    public MachineRotaryFurnaceMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineRotaryFurnaceMenu(int id, Inventory invPlayer, MachineRotaryFurnaceBlockEntity tile) {
        super(ModMenuTypes.ROTARY_FURNACE_MENU.get(), id);
        this.furnace = tile;

        var container = new ModItemStackHandlerContainer(
                tile != null ? tile.getInventory() : new DummyItemStackHandler(MachineRotaryFurnaceBlockEntity.INVENTORY_SIZE),
                tile != null ? tile::setChanged : null);

        //Inputs
        this.addSlot(new Slot(container, 0, 8, 18));
        this.addSlot(new Slot(container, 1, 26, 18));
        this.addSlot(new Slot(container, 2, 44, 18));
        //Fluid ID
        this.addSlot(new Slot(container, 3, 8, 54));
        //Solid fuel
        this.addSlot(new Slot(container, 4, 44, 54));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 104 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(invPlayer, i, 8 + i * 18, 162));
        }
    }

    private static MachineRotaryFurnaceBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        if (be instanceof MachineRotaryFurnaceBlockEntity furnace) return furnace;
        if (inventory.player.level().isClientSide) return null;
        throw new IllegalStateException("No MachineRotaryFurnaceBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":rotary_furnace_menu");
    }

    public MachineRotaryFurnaceBlockEntity getBlockEntity() {
        return furnace;
    }

    @Override
    public boolean stillValid(Player player) {
        if (furnace == null || furnace.getLevel() != player.level()) return false;
        BlockPos pos = furnace.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 128.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack rStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            rStack = stack.copy();

            if (index <= 4) {
                if (!this.moveItemStackTo(stack, 5, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (AbstractFurnaceBlockEntity.isFuel(rStack)) {
                    if (!this.moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
                } else if (rStack.getItem() instanceof IItemFluidIdentifier) {
                    if (!this.moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
                } else {
                    if (!this.moveItemStackTo(stack, 0, 3, false)) return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return rStack;
    }
}
