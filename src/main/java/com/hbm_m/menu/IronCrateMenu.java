package com.hbm_m.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.IronCrateBlockEntity;
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
 */
public class IronCrateMenu extends AbstractContainerMenu {

    public final IronCrateBlockEntity blockEntity;
    private final Level level;

    // ===== ИЗМЕНЕНО: Константы для 36 слотов =====
    private static final int CRATE_SLOTS = 36;        // 4 ряда × 9 колонок
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

        // Добавляем слоты крейта (4 ряда × 9 колонок)
        addCrateSlots();

        // Добавляем инвентарь игрока
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    // ==================== SLOT SETUP ====================

    /**
     * Добавляет 36 слотов крейта (4 ряда × 9 колонок)
     */
    private void addCrateSlots() {
        int startX = 8;   // Отступ слева
        int startY = 18;  // Отступ сверху

        // ===== ИЗМЕНЕНО: 4 ряда вместо 6 =====
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

    /**
     * Добавляет основной инвентарь игрока (3 ряда × 9 колонок)
     */
    private void addPlayerInventory(Inventory playerInventory) {
        int startX = 8;
        // ===== ИЗМЕНЕНО: Позиция инвентаря ближе (4 ряда крейта вместо 6) =====
        int startY = 104; // 18 (начало) + 4*18 (4 ряда) + 14 (отступ) = 104

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

    /**
     * Добавляет хотбар игрока (1 ряд × 9 колонок)
     */
    private void addPlayerHotbar(Inventory playerInventory) {
        int startX = 8;
        // ===== ИЗМЕНЕНО: Позиция хотбара ниже =====
        int startY = 162; // 104 + 3*18 (инвентарь) + 4 (отступ) = 162

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(
                    playerInventory,
                    col,
                    startX + col * 18,
                    startY
            ));
        }
    }

    // ==================== SHIFT-CLICK HANDLING ====================

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Из крейта в инвентарь игрока
        if (index < CRATE_SLOTS) {
            if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_START + 9, true)) {
                return ItemStack.EMPTY;
            }
        }
        // Из инвентаря игрока в крейт
        else {
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

    // ==================== VALIDATION ====================

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(
                ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player,
                ModBlocks.CRATE_IRON.get()
        );
    }
}