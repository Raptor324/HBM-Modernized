package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineCompressorBaseBlockEntity;
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

/** 1:1 {@code ContainerCompressor}: Fluidkennung, Batterie, zwei Upgrades - fuer beide Kompressoren. */
public class MachineCompressorMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 4;

    private final MachineCompressorBaseBlockEntity blockEntity;

    public MachineCompressorMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf));
    }

    public MachineCompressorMenu(int id, Inventory inv, MachineCompressorBaseBlockEntity be) {
        super(ModMenuTypes.COMPRESSOR_MENU.get(), id);
        this.blockEntity = be;

        var tile = new ModItemStackHandlerContainer(
                be != null ? be.getInventory() : new DummyItemStackHandler(MACHINE_SLOTS),
                be != null ? be::setChanged : null);

        // Fluidkennung
        addSlot(new Slot(tile, 0, 17, 72));
        // Batterie
        addSlot(new Slot(tile, 1, 152, 72));
        // Upgrades
        addSlot(new Slot(tile, 2, 52, 72));
        addSlot(new Slot(tile, 3, 70, 72));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 122 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inv, i, 8 + i * 18, 180));
        }
    }

    public static MachineCompressorMenu create(int id, Inventory inv, MachineCompressorBaseBlockEntity be) {
        return new MachineCompressorMenu(id, inv, be);
    }

    private static MachineCompressorBaseBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof MachineCompressorBaseBlockEntity c) return c;
        if (inv.player.level().isClientSide) return null;
        throw new IllegalStateException("No compressor at " + pos);
    }

    public MachineCompressorBaseBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && blockEntity.getLevel() == player.level()
            && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = slots.get(index);

        if (var4 != null && var4.hasItem()) {
            ItemStack var5 = var4.getItem();
            var3 = var5.copy();

            if (index <= 3) {
                if (!moveItemStackTo(var5, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (var3.getItem() instanceof com.hbm_m.api.item.IBatteryItem) {
                    if (!moveItemStackTo(var5, 1, 2, false)) return ItemStack.EMPTY;
                } else if (var3.getItem() instanceof com.hbm_m.interfaces.IItemFluidIdentifier) {
                    if (!moveItemStackTo(var5, 0, 1, false)) return ItemStack.EMPTY;
                } else {
                    if (!moveItemStackTo(var5, 2, 4, false)) return ItemStack.EMPTY;
                }
            }

            if (var5.isEmpty()) var4.set(ItemStack.EMPTY);
            else var4.setChanged();
        }
        return var3;
    }
}
