package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineCoreInjectorBlockEntity;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Port von {@code ContainerCoreInjector} (1.7.10 Original): Fluidkennung Deuterium ein/aus (26/17, 26/53),
 * Tritium ein/aus (134/17, 134/53); die Ausgaenge sind nur entnehmbar (Original SlotCraftingOutput).
 */
public class MachineCoreInjectorMenu extends AbstractContainerMenu {

    private final MachineCoreInjectorBlockEntity blockEntity;

    public MachineCoreInjectorMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineCoreInjectorMenu(int id, Inventory inventory, MachineCoreInjectorBlockEntity blockEntity) {
        super(ModMenuTypes.CORE_INJECTOR_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new com.hbm_m.inventory.ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);
        this.addSlot(new Slot(container, 0, 26, 17));
        this.addSlot(new Slot(container, 1, 26, 53));
        this.addSlot(new Slot(container, 2, 134, 17));
        this.addSlot(new Slot(container, 3, 134, 53));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public static MachineCoreInjectorMenu create(int id, Inventory inventory, MachineCoreInjectorBlockEntity blockEntity) {
        return new MachineCoreInjectorMenu(id, inventory, blockEntity);
    }

    private static MachineCoreInjectorBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineCoreInjectorBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No MachineCoreInjectorBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":core_injector_menu");
    }

    public MachineCoreInjectorBlockEntity getBlockEntity() {
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
        // Original transferStackInSlot: nur aus den Maschinenslots (0-3) ins Spielerinventar, sonst nichts
        Slot slot = this.slots.get(index);
        if (index > 3 || slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (!this.moveItemStackTo(stack, 4, this.slots.size(), true)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
