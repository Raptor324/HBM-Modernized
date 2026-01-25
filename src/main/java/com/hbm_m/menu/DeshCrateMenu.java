package com.hbm_m.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.custom.crates.DeshCrateBlockEntity;
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

public class DeshCrateMenu extends AbstractContainerMenu {

    public final DeshCrateBlockEntity blockEntity;
    private final Level level;

    private static final int CRATE_SLOTS = 104;
    private static final int PLAYER_INVENTORY_START = 104;
    private static final int PLAYER_HOTBAR_START = 131;

    public DeshCrateMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, extraData.readBlockPos());
    }

    private DeshCrateMenu(int containerId, Inventory inv, BlockPos pos) {
        this(containerId, inv, inv.player.level().getBlockEntity(pos));
    }

    public DeshCrateMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.DESH_CRATE_MENU.get(), containerId);

        if (entity instanceof DeshCrateBlockEntity) {
            blockEntity = (DeshCrateBlockEntity) entity;
        } else {
            blockEntity = new DeshCrateBlockEntity(BlockPos.ZERO,
                    ModBlocks.CRATE_DESH.get().defaultBlockState());
        }

        this.level = inv.player.level();

        addCrateSlots();
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    /**
     * Слоты крейта (8 рядов × 13 колонок)
     */
    private void addCrateSlots() {
        int startX = 6;  // 5 (GUI отступ) + 1 = 6
        int startY = 21; // 20 (GUI) + 1 = 21

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 13; col++) {
                int index = row * 13 + col;
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
     * Инвентарь игрока (3 ряда × 9 колонок)
     */
    private void addPlayerInventory(Inventory playerInventory) {
        int startX = 43; // Центрируем
        int startY = 183;

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
     * Хотбар (1 ряд × 9 колонок)
     */
    private void addPlayerHotbar(Inventory playerInventory) {
        int startX = 43;
        int startY = 183 + 3 * 18 + 4; // 237

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(
                    playerInventory,
                    col,
                    startX + col * 18,
                    startY
            ));
        }
    }

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
                ModBlocks.CRATE_DESH.get()
        );
    }
}