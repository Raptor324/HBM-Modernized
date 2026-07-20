package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineSatLinkerBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the "Satellite ID Manager" - slot layout is a direct port of legacy
 * {@code ContainerMachineSatLinker} (same pixel coordinates).
 */
public class MachineSatLinkerMenu extends AbstractContainerMenu {

    private static final int TE_SLOT_COUNT = MachineSatLinkerBlockEntity.SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = TE_SLOT_COUNT;
    private static final int VANILLA_SLOT_COUNT = 36;

    private final MachineSatLinkerBlockEntity blockEntity;

    public MachineSatLinkerMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, (MachineSatLinkerBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public MachineSatLinkerMenu(int id, Inventory inv, MachineSatLinkerBlockEntity entity) {
        super(ModMenuTypes.MACHINE_SATLINKER_MENU.get(), id);
        this.blockEntity = entity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        addSlot(new Slot(container, MachineSatLinkerBlockEntity.SLOT_COPY_SOURCE, 44, 35));
        addSlot(new Slot(container, MachineSatLinkerBlockEntity.SLOT_COPY_TARGET, 80, 35));
        addSlot(new Slot(container, MachineSatLinkerBlockEntity.SLOT_RANDOMIZE, 116, 35));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    public MachineSatLinkerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < TE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, TE_SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.MACHINE_SATLINKER.get());
    }
}
