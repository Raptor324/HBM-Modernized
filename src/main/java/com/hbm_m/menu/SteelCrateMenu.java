package com.hbm_m.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.custom.crates.SteelCrateBlockEntity;
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
 * Menu для Steel Crate (54 слота: 6 рядов × 9 колонок)
 * Повторяет функционал IronCrateMenu, но с 54 слотами и текущим расположением
 */
public class SteelCrateMenu extends AbstractContainerMenu {

    public final SteelCrateBlockEntity blockEntity;
    private final Level level;

    private static final int CRATE_SLOTS = 54;
    private static final int PLAYER_INVENTORY_START = 54;
    private static final int PLAYER_HOTBAR_START = 81;

    public SteelCrateMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, extraData.readBlockPos());
    }

    private SteelCrateMenu(int containerId, Inventory inv, BlockPos pos) {
        this(containerId, inv, inv.player.level().getBlockEntity(pos));
    }

    public SteelCrateMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.STEEL_CRATE_MENU.get(), containerId);

        if (entity instanceof SteelCrateBlockEntity steelEntity) {
            blockEntity = steelEntity;
        } else {
            blockEntity = new SteelCrateBlockEntity(BlockPos.ZERO,
                    ModBlocks.CRATE_STEEL.get().defaultBlockState());
        }

        this.level = inv.player.level();

        addCrateSlots();
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    private void addCrateSlots() {
        int startX = 8;
        int startY = 18;

        for (int row = 0; row < 6; row++) {
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
        int startY = 140;

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
        int startX = 8;
        int startY = 198;

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

        // Логика как в IronCrateMenu:
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
                ModBlocks.CRATE_STEEL.get()
        );
    }
}
