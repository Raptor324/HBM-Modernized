package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.fusion.FusionKlystronBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1-Port von {@code ContainerFusionKlystron} (1.7.10): ein Batterieslot bei (8/72),
 * Spielerinventar ab (17/118).
 */
public class MachineFusionKlystronMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = FusionKlystronBlockEntity.INVENTORY_SIZE;

    private final FusionKlystronBlockEntity blockEntity;
    private final Level level;

    public MachineFusionKlystronMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public MachineFusionKlystronMenu(int id, Inventory inv, FusionKlystronBlockEntity blockEntity) {
        super(ModMenuTypes.FUSION_KLYSTRON_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.level = inv.player.level();

        ModItemStackHandlerContainer machineInventory =
                new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(machineInventory, FusionKlystronBlockEntity.SLOT_BATTERY, 8, 72));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 17 + j * 18, 118 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 17 + i * 18, 176));
        }
    }

    public static MachineFusionKlystronMenu create(int id, Inventory inv, FusionKlystronBlockEntity be) {
        return new MachineFusionKlystronMenu(id, inv, be);
    }

    private static FusionKlystronBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockEntity be = inv.player.level().getBlockEntity(data.readBlockPos());
        if (be instanceof FusionKlystronBlockEntity klystron) return klystron;
        throw new IllegalStateException("BlockEntity is not a klystron");
    }

    public FusionKlystronBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, FusionKlystronBlockEntity.SLOT_BATTERY,
                    FusionKlystronBlockEntity.SLOT_BATTERY + 1, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.KLYSTRON.get());
    }
}
