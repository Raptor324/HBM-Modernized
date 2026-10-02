package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineExposureChamberBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.platform.DummyItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 1:1 {@code ContainerMachineExposureChamber}: Kapsel, leerer Behaelter, Zutat, Ausgabe, Batterie, zwei Upgrades. */
public class MachineExposureChamberMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 7;

    private final MachineExposureChamberBlockEntity blockEntity;

    public MachineExposureChamberMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf));
    }

    public MachineExposureChamberMenu(int id, Inventory invPlayer, MachineExposureChamberBlockEntity be) {
        super(ModMenuTypes.EXPOSURE_CHAMBER_MENU.get(), id);
        this.blockEntity = be;

        var tedf = new ModItemStackHandlerContainer(
                be != null ? be.getInventory() : new DummyItemStackHandler(8),
                be != null ? be::setChanged : null);

        this.addSlot(new Slot(tedf, 0, 8, 18));
        this.addSlot(new TakeOnly(tedf, 2, 8, 54));
        this.addSlot(new Slot(tedf, 3, 80, 36));
        this.addSlot(new TakeOnly(tedf, 4, 116, 36));
        this.addSlot(new Slot(tedf, 5, 152, 54));
        this.addSlot(new Slot(tedf, 6, 44, 54));
        this.addSlot(new Slot(tedf, 7, 62, 54));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 104 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(invPlayer, i, 8 + i * 18, 162));
        }
    }

    private static final class TakeOnly extends Slot {
        TakeOnly(net.minecraft.world.Container c, int slot, int x, int y) { super(c, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }

    public static MachineExposureChamberMenu create(int id, Inventory inv, MachineExposureChamberBlockEntity be) {
        return new MachineExposureChamberMenu(id, inv, be);
    }

    private static MachineExposureChamberBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof MachineExposureChamberBlockEntity c) return c;
        if (inv.player.level().isClientSide) return null;
        throw new IllegalStateException("No MachineExposureChamberBlockEntity at " + pos);
    }

    public MachineExposureChamberBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && blockEntity.getLevel() == player.level()
            && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = this.slots.get(index);

        if (var4 != null && var4.hasItem()) {
            ItemStack var5 = var4.getItem();
            var3 = var5.copy();

            if (index <= 6) {
                if (!this.moveItemStackTo(var5, MACHINE_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {

                if (var3.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade) {
                    if (!this.moveItemStackTo(var5, 5, 7, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (var3.getItem() instanceof com.hbm_m.api.item.IBatteryItem
                        || var3.getItem() instanceof com.hbm_m.item.fekal_electric.ItemCreativeBattery) {
                    if (!this.moveItemStackTo(var5, 4, 5, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(var5, 0, 3, false)) {
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
