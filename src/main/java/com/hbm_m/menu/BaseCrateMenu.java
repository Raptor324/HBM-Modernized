package com.hbm_m.menu;

import com.hbm_m.block.custom.machines.crates.CrateSlot;
import com.hbm_m.block.custom.machines.crates.CrateType;
import com.hbm_m.block.custom.machines.crates.CrateValidation;
import com.hbm_m.block.entity.custom.crates.BaseCrateBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Базовое меню для всех ящиков HBM.
 * Содержит общую логику: CrateSlot с валидацией, ProtectedSlot для хотбара,
 * quickMoveStack и стандартное расположение слотов.
 */
public abstract class BaseCrateMenu extends AbstractContainerMenu {

    protected final BaseCrateBlockEntity blockEntity;
    protected final Level level;
    protected final CrateType crateType;

    private final int crateSlots;
    private final int playerInvStart;
    private final int totalSlots;

    protected BaseCrateMenu(MenuType<?> menuType, int containerId, Inventory inv,
                            BaseCrateBlockEntity blockEntity, CrateType crateType) {
        super(menuType, containerId);
        this.blockEntity = blockEntity;
        this.level = inv.player.level();
        this.crateType = crateType;
        this.crateSlots = crateType.getSlotCount();
        this.playerInvStart = crateSlots;
        this.totalSlots = crateSlots + 36;

        addCrateSlots();
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    private void addCrateSlots() {
        int startX = crateType.getCrateSlotStartX();
        int startY = crateType.getCrateSlotStartY();

        for (int row = 0; row < crateType.getRows(); row++) {
            for (int col = 0; col < crateType.getCols(); col++) {
                int index = row * crateType.getCols() + col;
                this.addSlot(new CrateSlot(
                        blockEntity.getItemHandler(),
                        index,
                        startX + col * 18,
                        startY + row * 18
                ));
            }
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        int startX = crateType.getPlayerInvStartX();
        int startY = crateType.getPlayerInvStartY();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        startX + col * 18,
                        startY + row * 18
                ));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        int startX = crateType.getPlayerInvStartX();
        int startY = crateType.getHotbarStartY();

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(
                    playerInventory,
                    col,
                    startX + col * 18,
                    startY
            ));
        }
    }

    /** Блок, к которому привязано меню (для stillValid). */
    protected abstract Block getBlock();

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index < crateSlots) {
            if (!this.moveItemStackTo(sourceStack, playerInvStart, totalSlots, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (CrateValidation.isCrateItem(sourceStack)) {
                return ItemStack.EMPTY;
            }
            if (!this.moveItemStackTo(sourceStack, 0, crateSlots, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, sourceStack);
        return copy;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(
                ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player,
                getBlock()
        );
    }

    public BaseCrateBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
