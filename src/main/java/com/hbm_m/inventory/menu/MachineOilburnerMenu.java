package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineOilburnerBlockEntity;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 1:1 {@code ContainerOilburner}: Behaelter rein (26,17), leer raus (26,53, nur entnehmen), Fluid-ID (44,71). */
public class MachineOilburnerMenu extends AbstractContainerMenu {

    private final MachineOilburnerBlockEntity blockEntity;

    public MachineOilburnerMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineOilburnerMenu(int id, Inventory invPlayer, MachineOilburnerBlockEntity tedf) {
        super(ModMenuTypes.OILBURNER_MENU.get(), id);
        this.blockEntity = tedf;

        var container = new ModItemStackHandlerContainer(tedf.getInventory(), tedf::setChanged);
        // In
        this.addSlot(new Slot(container, 0, 26, 17));
        // Out
        this.addSlot(new Slot(container, 1, 26, 53) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        // Fluid ID
        this.addSlot(new Slot(container, 2, 44, 71));

        int offset = 37;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18 + offset));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(invPlayer, i, 8 + i * 18, 142 + offset));
        }
    }

    public static MachineOilburnerMenu create(int id, Inventory inventory, MachineOilburnerBlockEntity blockEntity) {
        return new MachineOilburnerMenu(id, inventory, blockEntity);
    }

    private static MachineOilburnerBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineOilburnerBlockEntity oilburnerBlockEntity) {
            return oilburnerBlockEntity;
        }
        throw new IllegalStateException("No MachineOilburnerBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":oilburner_menu");
    }

    public MachineOilburnerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        // audit13: Original isUseableByPlayer (<= 128 zur Kernmitte) oder Huelle <= 64; Vanilla 64 schloss die GUI an grossen Maschinen
        return MultiblockMenuReach.stillValidCore(blockEntity, player, 128.0D);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int par2) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = this.slots.get(par2);

        if (var4 != null && var4.hasItem()) {
            ItemStack var5 = var4.getItem();
            var3 = var5.copy();

            if (par2 <= 2) {
                if (!this.moveItemStackTo(var5, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (var3.getItem() instanceof IItemFluidIdentifier) {
                    if (!this.moveItemStackTo(var5, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(var5, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
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
