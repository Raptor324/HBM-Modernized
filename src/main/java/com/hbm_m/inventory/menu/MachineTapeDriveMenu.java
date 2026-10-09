package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineTapeDriveBlockEntity;
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

/** 1:1 {@code ContainerTapeDrive}: 2x6 Laufwerksplaetze ab (35,27), Spielerinventar ab y 104. */
public class MachineTapeDriveMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachineTapeDriveBlockEntity.SLOT_COUNT;

    private final MachineTapeDriveBlockEntity blockEntity;

    public MachineTapeDriveMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineTapeDriveMenu(int id, Inventory inventory, MachineTapeDriveBlockEntity blockEntity) {
        super(ModMenuTypes.TAPE_DRIVE_MENU.get(), id);
        this.blockEntity = blockEntity;

        var handler = blockEntity.getInventory();
        var container = new ModItemStackHandlerContainer(handler, blockEntity::setChanged);

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 6; col++) {
                final int s = col + row * 6;
                this.addSlot(new Slot(container, s, 35 + col * 18, 27 + row * 18) {
                    @Override public boolean mayPlace(ItemStack stack) { return handler.isItemValid(s, stack); }
                    @Override public int getMaxStackSize() { return 1; }
                });
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 104 + 58));
        }
    }

    private static MachineTapeDriveBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineTapeDriveBlockEntity drive) return drive;
        throw new IllegalStateException("No MachineTapeDriveBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":tape_drive_menu");
    }

    public MachineTapeDriveBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        return !blockEntity.isRemoved() && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(slotStack, MACHINE_SLOT_COUNT, MACHINE_SLOT_COUNT + 36, true)) return ItemStack.EMPTY;
            } else {
                if (!MachineTapeDriveBlockEntity.isDrive(slotStack)) return ItemStack.EMPTY;
                boolean moved = false;
                for (int i = 0; i < MACHINE_SLOT_COUNT && !slotStack.isEmpty(); i++) {
                    Slot target = this.slots.get(i);
                    if (!target.hasItem()) {
                        target.set(slotStack.split(1));
                        moved = true;
                    }
                }
                if (!moved) return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();

            if (slotStack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, slotStack);
        }
        return result;
    }
}
