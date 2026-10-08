package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineElectrolyserBlockEntity;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.DummyItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code ContainerElectrolyserFluid}: Batterie (186,109), Upgrades (186,140/158), Fluid-ID, Ein-/Ausgabebehaelter,
 * Nebenprodukte (154,18-54), Spielerinventar ab (8,122).
 */
public class MachineElectrolyserFluidMenu extends AbstractContainerMenu {

    private final MachineElectrolyserBlockEntity electrolyser;

    public MachineElectrolyserFluidMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineElectrolyserFluidMenu(int id, Inventory invPlayer, MachineElectrolyserBlockEntity tedf) {
        super(ModMenuTypes.ELECTROLYSER_FLUID_MENU.get(), id);
        this.electrolyser = tedf;

        Container c = container(tedf);

        //Battery
        this.addSlot(new Slot(c, 0, 186, 109));
        //Upgrades
        this.addSlot(new Slot(c, 1, 186, 140));
        this.addSlot(new Slot(c, 2, 186, 158));
        //Fluid ID
        this.addSlot(new Slot(c, 3, 6, 18));
        this.addSlot(takeOnly(c, 4, 6, 54));
        //Input
        this.addSlot(new Slot(c, 5, 24, 18));
        this.addSlot(takeOnly(c, 6, 24, 54));
        //Output
        this.addSlot(new Slot(c, 7, 78, 18));
        this.addSlot(takeOnly(c, 8, 78, 54));
        this.addSlot(new Slot(c, 9, 134, 18));
        this.addSlot(takeOnly(c, 10, 134, 54));
        //Byproducts
        this.addSlot(takeOnly(c, 11, 154, 18));
        this.addSlot(takeOnly(c, 12, 154, 36));
        this.addSlot(takeOnly(c, 13, 154, 54));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 122 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(invPlayer, i, 8 + i * 18, 180));
        }
    }

    static Container container(MachineElectrolyserBlockEntity tedf) {
        return new ModItemStackHandlerContainer(
                tedf != null ? tedf.getInventory() : new DummyItemStackHandler(MachineElectrolyserBlockEntity.INVENTORY_SIZE),
                tedf != null ? tedf::setChanged : null);
    }

    /** {@code SlotTakeOnly} / {@code SlotCraftingOutput}: nur Entnahme. */
    static Slot takeOnly(Container c, int index, int x, int y) {
        return new Slot(c, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    static MachineElectrolyserBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        if (be instanceof MachineElectrolyserBlockEntity e) return e;
        if (inventory.player.level().isClientSide) return null;
        throw new IllegalStateException("No MachineElectrolyserBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":electrolyser_menu");
    }

    static boolean stillValid(MachineElectrolyserBlockEntity electrolyser, Player player) {
        if (electrolyser == null || electrolyser.getLevel() != player.level()) return false;
        BlockPos pos = electrolyser.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 128.0D;
    }

    public MachineElectrolyserBlockEntity getBlockEntity() {
        return electrolyser;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(electrolyser, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int par2) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = this.slots.get(par2);

        if (var4 != null && var4.hasItem()) {
            ItemStack var5 = var4.getItem();
            var3 = var5.copy();

            if (par2 <= 13) {
                if (!this.moveItemStackTo(var5, 14, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {

                if (var3.getItem() instanceof com.hbm_m.api.item.IBatteryItem) {
                    if (!this.moveItemStackTo(var5, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (var3.getItem() instanceof ItemMachineUpgrade) {
                    if (!this.moveItemStackTo(var5, 1, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (var3.getItem() instanceof IItemFluidIdentifier) {
                    if (!this.moveItemStackTo(var5, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (var5.isEmpty()) {
                var4.set(ItemStack.EMPTY);
            } else {
                var4.setChanged();
            }
        }

        return var3;
    }
}
