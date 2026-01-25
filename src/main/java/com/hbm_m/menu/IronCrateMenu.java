package com.hbm_m.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.custom.crates.IronCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Menu для Iron Crate (36 слотов: 4 ряда × 9 колонок)
 * 🔒 НЕ МОЖНО ПЕРЕМЕСТИТЬ ЯЩИК пока меню открыто!
 */
public class IronCrateMenu extends AbstractContainerMenu {

    public final IronCrateBlockEntity blockEntity;
    private final Level level;
    private final ItemStack protectedCrate;

    private static final int CRATE_SLOTS = 36;
    private static final int PLAYER_INVENTORY_START = 36;
    private static final int PLAYER_HOTBAR_START = 63;

    // ==================== CONSTRUCTORS ====================
    public IronCrateMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, extraData.readBlockPos());
    }

    private IronCrateMenu(int containerId, Inventory inv, BlockPos pos) {
        this(containerId, inv, inv.player.level().getBlockEntity(pos));
    }

    public IronCrateMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.IRON_CRATE_MENU.get(), containerId);

        if (entity instanceof IronCrateBlockEntity) {
            blockEntity = (IronCrateBlockEntity) entity;
        } else {
            blockEntity = new IronCrateBlockEntity(BlockPos.ZERO,
                    ModBlocks.CRATE_IRON.get().defaultBlockState());
        }

        this.level = inv.player.level();
        // 🔒 Получаем ItemStack ящика из BlockEntity для защиты
        this.protectedCrate = new ItemStack(ModBlocks.CRATE_IRON.get());

        addCrateSlots();
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    // ==================== SLOT SETUP ====================

    private void addCrateSlots() {
        int startX = 8;
        int startY = 18;

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                this.addSlot(new SlotItemHandler(
                        blockEntity.getItemHandler(),
                        index,
                        startX + col * 18,
                        startY + row * 18
                ));
            }
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        int startX = 8;
        int startY = 104;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                // 🔒 ЗАЩИЩЕННЫЕ СЛОТЫ
                this.addSlot(new ProtectedSlot(playerInventory,
                        col + row * 9 + 9,
                        startX + col * 18,
                        startY + row * 18,
                        protectedCrate));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        int startX = 8;
        int startY = 162;

        for (int col = 0; col < 9; col++) {
            // 🔒 ЗАЩИЩЕННЫЙ ХОТБАР
            this.addSlot(new ProtectedSlot(playerInventory,
                    col,
                    startX + col * 18,
                    startY,
                    protectedCrate));
        }
    }

    // ==================== PROTECTED SLOT ====================
    private static class ProtectedSlot extends Slot {
        private final ItemStack protectedCrate;

        public ProtectedSlot(Inventory inv, int index, int x, int y, ItemStack protectedCrate) {
            super(inv, index, x, y);
            this.protectedCrate = protectedCrate;
        }

        @Override
        public boolean mayPickup(Player player) {
            // 🔒 ЗАПРЕТ: нельзя взять ящик из инвентаря пока меню открыто
            ItemStack slotItem = this.getItem();
            return !ItemStack.isSameItemSameTags(slotItem, protectedCrate);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // Дополнительная защита: нельзя положить ящик в слот
            return !ItemStack.isSameItemSameTags(stack, protectedCrate);
        }
    }

    // ==================== SHIFT-CLICK ====================

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < CRATE_SLOTS) {
            if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_START + 9, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(sourceStack, 0, CRATE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(
                ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player,
                ModBlocks.CRATE_IRON.get()
        );
    }
}
