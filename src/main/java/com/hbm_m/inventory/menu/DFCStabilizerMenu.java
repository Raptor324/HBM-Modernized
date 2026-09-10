package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.dfc.DFCStabilizerBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1-Port von {@code ContainerCoreStabilizer} (1.7.10): der Linsenplatz bei (80, 17),
 * Spielerinventar bei (8, 84).
 */
public class DFCStabilizerMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = DFCStabilizerBlockEntity.INVENTORY_SIZE;

    private final DFCStabilizerBlockEntity blockEntity;
    private final ContainerData data;

    public DFCStabilizerMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public DFCStabilizerMenu(int id, Inventory inv, DFCStabilizerBlockEntity blockEntity) {
        super(ModMenuTypes.DFC_STABILIZER_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.data = blockEntity.getContainerData();

        checkContainerDataCount(data, 3);
        addDataSlots(data);

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(container, DFCStabilizerBlockEntity.SLOT_LENS, 80, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.AMS_LENS.get());
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    private static DFCStabilizerBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof DFCStabilizerBlockEntity stabilizer) return stabilizer;
        throw new IllegalStateException("No DFCStabilizerBlockEntity at " + pos);
    }

    public DFCStabilizerBlockEntity getBlockEntity() { return blockEntity; }

    public int getWatts()         { return data.get(0); }
    public int getBeam()          { return data.get(1); }
    /** Energiestand in Promille - der Rohwert sprengt den int-Bereich. */
    public int getPowerPermille() { return data.get(2); }

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
