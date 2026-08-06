package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineLiquefactorBlockEntity;
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

/** Slot-Koordinaten 1:1 aus {@code ContainerLiquefactor} (1.7.10 Original) uebernommen. Die
 *  Upgrade-Slots des Originals wurden nicht uebernommen (siehe Klassenkommentar in
 *  {@link MachineLiquefactorBlockEntity}). */
public class MachineLiquefactorMenu extends AbstractContainerMenu {

    private final MachineLiquefactorBlockEntity blockEntity;
    private static final int SLOT_INPUT = MachineLiquefactorBlockEntity.SLOT_INPUT;
    private static final int SLOT_BATTERY = MachineLiquefactorBlockEntity.SLOT_BATTERY;
    private static final int MACHINE_SLOT_COUNT = 2;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    public MachineLiquefactorMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineLiquefactorMenu(int id, Inventory inventory, MachineLiquefactorBlockEntity blockEntity) {
        super(ModMenuTypes.LIQUEFACTOR_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);
        this.addSlot(new Slot(container, SLOT_INPUT, 35, 54));
        this.addSlot(new Slot(container, SLOT_BATTERY, 134, 72));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 180));
        }
    }

    public static MachineLiquefactorMenu create(int id, Inventory inventory, MachineLiquefactorBlockEntity blockEntity) {
        return new MachineLiquefactorMenu(id, inventory, blockEntity);
    }

    private static MachineLiquefactorBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineLiquefactorBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No MachineLiquefactorBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":liquefactor_menu");
    }

    public MachineLiquefactorBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() != player.level()) {
            return false;
        }
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
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
            } else {
                if (!this.moveItemStackTo(slotStack, SLOT_INPUT, SLOT_INPUT + 1, false)) {
                    return ItemStack.EMPTY;
                }
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
}
