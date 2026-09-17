package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineStrandCasterBlockEntity;
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
 * Порт {@code ContainerMachineStrandCaster} (1.7.10): изложница на (57,62),
 * шесть слотов выхода 3×2 на (125,26) с шагом 18 (take-only), инвентарь
 * игрока (8,132)/хотбар (8,190). Shift-клик: машина → игрок, игрок → изложница.
 */
public class MachineStrandCasterMenu extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = MachineStrandCasterBlockEntity.SLOT_COUNT; // 7
    private static final int PLAYER_INV_START = MACHINE_SLOTS;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_END = PLAYER_INV_END + 9;

    public final MachineStrandCasterBlockEntity blockEntity;

    public MachineStrandCasterMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineStrandCasterMenu(int id, Inventory inventory, BlockEntity entity) {
        super(ModMenuTypes.STRAND_CASTER_MENU.get(), id);
        this.blockEntity = (MachineStrandCasterBlockEntity) entity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        // the wretched mold
        this.addSlot(new Slot(container, MachineStrandCasterBlockEntity.SLOT_MOLD, 57, 62));

        // выход: 3 ряда × 2 колонки, индекс j + i*2 + 1
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                this.addSlot(new OutputSlot(container, j + i * 2 + 1, 125 + j * 18, 26 + i * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 132 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 190));
        }
    }

    private static MachineStrandCasterBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineStrandCasterBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No MachineStrandCasterBlockEntity found at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null
                && blockEntity.getLevel() == player.level()
                && player.distanceToSqr(
                        blockEntity.getBlockPos().getX() + 0.5D,
                        blockEntity.getBlockPos().getY() + 0.5D,
                        blockEntity.getBlockPos().getZ() + 0.5D) <= 128.0D;
    }

    /** Порт transferStackInSlot: машина → игрок; игрок → слот изложницы. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return copy;

        ItemStack stack = slot.getItem();
        copy = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return copy;
    }

    /** Порт SlotCraftingOutput: только извлечение. */
    private static class OutputSlot extends Slot {
        public OutputSlot(net.minecraft.world.Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
