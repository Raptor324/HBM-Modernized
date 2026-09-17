package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.FireboxBaseBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.module.ModuleBurnTime;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * Общая логика контейнера печей — порт {@code ContainerFirebox} (1.7.10):
 * два слота топлива (44,27)/(62,27), инвентарь игрока (8,86), хотбар (8,144).
 * Открытие/закрытие меню крутит {@code playersUsing} печи (анимация дверцы).
 *
 * @param <BE> конкретный BlockEntity печи
 */
public abstract class FireboxBaseMenu<BE extends FireboxBaseBlockEntity> extends AbstractContainerMenu {

    public static final int MACHINE_SLOT_COUNT = FireboxBaseBlockEntity.FUEL_SLOTS;

    public static final int IDX_BURN_TIME     = 0;
    public static final int IDX_MAX_BURN_TIME = 1;
    public static final int IDX_BURN_HEAT     = 2;
    public static final int IDX_HEAT_ENERGY   = 3;
    public static final int IDX_MAX_HEAT      = 4;
    public static final int DATA_SLOTS = 5;

    public final BE blockEntity;
    protected final ContainerData data;

    protected FireboxBaseMenu(MenuType<?> type, int containerId, Inventory playerInventory, BE be, ContainerData data) {
        super(type, containerId);
        this.blockEntity = be;
        this.data = data;
        checkContainerDataCount(data, DATA_SLOTS);
        addDataSlots(data);

        // Порт ContainerFirebox: openInventory в конструкторе — дверца открывается
        if (!playerInventory.player.level().isClientSide()) {
            be.playerOpened();
        }

        var container = new ModItemStackHandlerContainer(be.getInventory(), be::setChanged);
        for (int i = 0; i < MACHINE_SLOT_COUNT; i++) {
            this.addSlot(new FuelSlot(container, i, 44 + i * 18, 27));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 86 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 144));
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            blockEntity.playerClosed();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null
                && blockEntity.getLevel() == player.level()
                && player.distanceToSqr(
                        blockEntity.getBlockPos().getX() + 0.5D,
                        blockEntity.getBlockPos().getY() + 0.5D,
                        blockEntity.getBlockPos().getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return copy;

        ItemStack stack = slot.getItem();
        copy = stack.copy();

        if (index < MACHINE_SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, MACHINE_SLOT_COUNT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, 0, MACHINE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return copy;
    }

    // ── Данные для GUI ───────────────────────────────────────────────────

    public int getBurnTime() { return data.get(IDX_BURN_TIME); }
    public int getMaxBurnTime() { return data.get(IDX_MAX_BURN_TIME); }
    public int getBurnHeat() { return data.get(IDX_BURN_HEAT); }
    public int getHeatEnergy() { return data.get(IDX_HEAT_ENERGY); }
    public int getMaxHeatEnergy() { return data.get(IDX_MAX_HEAT); }

    public boolean isBurning() { return getBurnTime() > 0; }

    public int getBurnTimeScaled(int scale) {
        int max = getMaxBurnTime();
        return max > 0 ? getBurnTime() * scale / max : 0;
    }

    public int getHeatScaled(int scale) {
        int max = getMaxHeatEnergy();
        return max > 0 ? getHeatEnergy() * scale / max : 0;
    }

    /** Длительность горения в секундах (для тултипа, как в оригинале burnTime / 20). */
    public int getBurnSeconds() {
        return getBurnTime() / 20;
    }

    /** Описание модификаторов топлива модуля (оригинал getModule().getDesc()). */
    public java.util.List<Component> getFuelDescription() {
        return blockEntity.getModule().getDesc();
    }

    /** Слот топлива — только то, что горит (порт isItemValidForSlot). */
    private static class FuelSlot extends Slot {
        public FuelSlot(net.minecraft.world.Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ModuleBurnTime.getBaseBurnTime(stack) > 0;
        }
    }
}
