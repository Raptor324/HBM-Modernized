package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineStrandCasterBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Slot-Koordinaten angelehnt an {@code ContainerMachineStrandCaster} (1.7.10 Original): Mold-Slot
 *  bei (8,18), Output bei (98,36) (Original hat 6 Output-Slots, hier auf 1 vereinfacht siehe
 *  {@link MachineStrandCasterBlockEntity}). */
public class MachineStrandCasterMenu extends AbstractContainerMenu {

    public final MachineStrandCasterBlockEntity blockEntity;

    private static final int SLOT_MOLD = MachineStrandCasterBlockEntity.SLOT_MOLD;
    private static final int SLOT_OUTPUT = MachineStrandCasterBlockEntity.SLOT_OUTPUT;
    private static final int MACHINE_SLOT_COUNT = 2;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    public MachineStrandCasterMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineStrandCasterMenu(int id, Inventory inventory, BlockEntity entity) {
        super(ModMenuTypes.STRAND_CASTER_MENU.get(), id);
        this.blockEntity = (MachineStrandCasterBlockEntity) entity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);
        this.addSlot(new Slot(container, SLOT_MOLD, 8, 18));
        this.addSlot(new OutputSlot(container, SLOT_OUTPUT, 98, 36));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 162));
        }
    }

    private static MachineStrandCasterBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineStrandCasterBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No MachineStrandCasterBlockEntity found at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.STRAND_CASTER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, SLOT_MOLD, SLOT_MOLD + 1, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, slotStack);
        }
        return result;
    }

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
