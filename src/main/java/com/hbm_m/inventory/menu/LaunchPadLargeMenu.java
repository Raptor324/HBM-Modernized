package com.hbm_m.inventory.menu;

import com.hbm_m.api.item.IDesignatorItem;
import com.hbm_m.block.entity.machines.LaunchPadBaseBlockEntity;
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
 * Меню для обычной (и большой) пусковой площадки.
 *
 * Это упрощённый порт старого ContainerLaunchPadLarge:
 * - 7 слотов машины (ракета, дизайнатор, батарея, топливо/окислитель in/out)
 * - 36 слотов инвентаря игрока.
 *
 * Логика сортировки/переноса предметов (shift‑клик) упрощена и не
 * повторяет в точности поведение 1.7.10, но достаточно для базового UX.
 */
public class LaunchPadLargeMenu extends AbstractContainerMenu {

    private static final int SLOT_MISSILE = 0;
    private static final int SLOT_DESIGNATOR = 1;
    private static final int SLOT_BATTERY = 2;
    private static final int SLOT_FUEL_IN = 3;
    private static final int SLOT_FUEL_OUT = 4;
    private static final int SLOT_OXIDIZER_IN = 5;
    private static final int SLOT_OXIDIZER_OUT = 6;
    private static final int MACHINE_SLOTS = 7;

    private final LaunchPadBaseBlockEntity blockEntity;
    private final Level level;
    private final Player player;

    public LaunchPadLargeMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public LaunchPadLargeMenu(int id, Inventory inv, LaunchPadBaseBlockEntity blockEntity) {
        super(getMenuType(), id);
        this.blockEntity = blockEntity;
        this.level = inv.player.level();
        this.player = inv.player;

        var handler = blockEntity.getInventory();

        // Слоты машины — координаты соответствуют старому GUI
        // Missile
        this.addSlot(new SlotItemHandler(handler, SLOT_MISSILE, 26, 36));
        // Designator (only IDesignatorItem)
        this.addSlot(new SlotItemHandler(handler, SLOT_DESIGNATOR, 26, 72) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof IDesignatorItem;
            }
        });
        // Battery
        this.addSlot(new SlotItemHandler(handler, SLOT_BATTERY, 107, 90));
        // Fuel in
        this.addSlot(new SlotItemHandler(handler, SLOT_FUEL_IN, 125, 90));
        // Fuel out (только вывод)
        this.addSlot(new SlotItemHandler(handler, SLOT_FUEL_OUT, 125, 108) {
            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return false;
            }
        });
        // Oxidizer in
        this.addSlot(new SlotItemHandler(handler, SLOT_OXIDIZER_IN, 143, 90));
        // Oxidizer out (только вывод)
        this.addSlot(new SlotItemHandler(handler, SLOT_OXIDIZER_OUT, 143, 108) {
            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return false;
            }
        });

        // Инвентарь игрока (3 ряда по 9 слотов)
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

    private static LaunchPadBaseBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof LaunchPadBaseBlockEntity launchPad) {
            return launchPad;
        }
        throw new IllegalStateException("No LaunchPadBaseBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":launch_pad_large_menu");
    }

    private static MenuType<?> getMenuType() {
        // Отложенная ссылка, чтобы не тянуть ModMenuTypes в клиент‑агностичный код создания
        return ModMenuTypes.LAUNCH_PAD_LARGE_MENU.get();
    }

    public LaunchPadBaseBlockEntity getBlockEntity() {
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
                // Из машины в инвентарь игрока
                if (!this.moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) {
                    return net.minecraft.world.item.ItemStack.EMPTY;
                }
            } else {
                // Из инвентаря игрока в слоты машины (просто первая подходящая позиция)
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
