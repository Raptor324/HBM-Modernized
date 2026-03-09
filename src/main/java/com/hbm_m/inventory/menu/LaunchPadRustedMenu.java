package com.hbm_m.inventory.menu;

import com.hbm_m.api.item.IDesignatorItem;
import com.hbm_m.block.entity.machines.LaunchPadRustedBlockEntity;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Меню для ржавой пусковой площадки.
 *
 * Порт старого LaunchPadRustedMenu:
 * - слот 0 — выход (ракета)
 * - слот 1 — launch codes
 * - слот 2 — launch key
 * - слот 3 — дизайнатор
 * - инвентарь игрока (3*9 + хотбар).
 */
public class LaunchPadRustedMenu extends AbstractContainerMenu {

    private static final int SLOT_OUTPUT = 0;
    private static final int SLOT_CODES = 1;
    private static final int SLOT_KEY = 2;
    private static final int SLOT_DESIGNATOR = 3;
    private static final int MACHINE_SLOTS = 4;

    private final LaunchPadRustedBlockEntity blockEntity;
    private final Level level;
    private final Player player;

    public LaunchPadRustedMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public LaunchPadRustedMenu(int id, Inventory inv, LaunchPadRustedBlockEntity blockEntity) {
        super(getMenuType(), id);
        this.blockEntity = blockEntity;
        this.level = inv.player.level();
        this.player = inv.player;

        var handler = blockEntity.getInventory();

        // Выходной слот (нельзя класть предметы)
        this.addSlot(new SlotItemHandler(handler, SLOT_OUTPUT, 26, 72) {
            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return false;
            }
        });
        // Launch codes
        this.addSlot(new SlotItemHandler(handler, SLOT_CODES, 116, 45));
        // Launch key
        this.addSlot(new SlotItemHandler(handler, SLOT_KEY, 134, 45));
        // Designator (only IDesignatorItem)
        this.addSlot(new SlotItemHandler(handler, SLOT_DESIGNATOR, 26, 99) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof IDesignatorItem;
            }
        });

        // Инвентарь игрока
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9,
                        8 + col * 18,
                        154 + row * 18));
            }
        }

        // Хотбар
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 212));
        }
    }

    private static LaunchPadRustedBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof LaunchPadRustedBlockEntity rusted) {
            return rusted;
        }
        throw new IllegalStateException("No LaunchPadRustedBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":launch_pad_rusted_menu");
    }

    private static MenuType<?> getMenuType() {
        return ModMenuTypes.LAUNCH_PAD_RUSTED_MENU.get();
    }

    public LaunchPadRustedBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() != player.level()) {
            return false;
        }
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        net.minecraft.world.item.ItemStack originalStack = net.minecraft.world.item.ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            net.minecraft.world.item.ItemStack stack = slot.getItem();
            originalStack = stack.copy();

            if (index < MACHINE_SLOTS) {
                if (!this.moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) {
                    return net.minecraft.world.item.ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                    return net.minecraft.world.item.ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(net.minecraft.world.item.ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return originalStack;
    }
}
