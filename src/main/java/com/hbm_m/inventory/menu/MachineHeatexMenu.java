package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineHeatexBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.NotNull;

/**
 * Port of {@code ContainerHeaterHeatex} (1.7.10 Original):
 * 1 machine slot at (80, 72) + player inventory (8, 122) / hotbar (8, 180).
 * Shift-click: machine slot -> player inventory, player inventory -> machine slot (original transferStackInSlot).
 */
public class MachineHeatexMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 1;
    private static final int SLOT_HEATEX_X = 80;
    private static final int SLOT_HEATEX_Y = 72;

    private final MachineHeatexBlockEntity blockEntity;

    public MachineHeatexMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineHeatexMenu(int id, Inventory inventory, MachineHeatexBlockEntity blockEntity) {
        super(ModMenuTypes.HEATEX_MENU.get(), id);
        this.blockEntity = blockEntity;

        // На клиенте тайл может отсутствовать (реплей Flashback) — подставляем заглушку
        ModItemStackHandler handler = this.blockEntity != null
                ? this.blockEntity.getInventory()
                : new com.hbm_m.platform.DummyItemStackHandler(MACHINE_SLOTS);
        HandlerContainer machineInventory = new HandlerContainer(handler);

        // Оригинал: new Slot(tedf, 0, 80, 72)
        this.addSlot(new Slot(machineInventory, 0, SLOT_HEATEX_X, SLOT_HEATEX_Y));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 180));
        }
    }

    public static MachineHeatexMenu create(int id, Inventory inventory, MachineHeatexBlockEntity blockEntity) {
        return new MachineHeatexMenu(id, inventory, blockEntity);
    }

    private static MachineHeatexBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineHeatexBlockEntity heatexBlockEntity) {
            return heatexBlockEntity;
        }
        throw new IllegalStateException("No MachineHeatexBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":heatex_menu");
    }

    public MachineHeatexBlockEntity getBlockEntity() {
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
        // Оригинал transferStackInSlot: индекс 0 -> игрок, иначе -> слот 0
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index == 0) {
                if (!this.moveItemStackTo(slotStack, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, slotStack);
        }
        return result;
    }

    /**
     * Ванильный Container-адаптер поверх {@link ModItemStackHandler}.
     * Нужен, чтобы меню не зависело от Forge `IItemHandler`/`SlotItemHandler`.
     */
    private static final class HandlerContainer implements net.minecraft.world.Container {
        private final ModItemStackHandler handler;

        private HandlerContainer(ModItemStackHandler handler) {
            this.handler = handler;
        }

        @Override
        public int getContainerSize() {
            return handler.getSlots();
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) return false;
            }
            return true;
        }

        @Override
        public @NotNull ItemStack getItem(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack removeItem(int slot, int amount) {
            return handler.extractItem(slot, amount, false);
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int slot) {
            ItemStack cur = handler.getStackInSlot(slot);
            if (cur.isEmpty()) return ItemStack.EMPTY;
            handler.setStackInSlot(slot, ItemStack.EMPTY);
            return cur;
        }

        @Override
        public void setItem(int slot, @NotNull ItemStack stack) {
            handler.setStackInSlot(slot, stack);
        }

        @Override
        public void setChanged() {
            // изменения трекаются в ModItemStackHandler.onContentsChanged()
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < handler.getSlots(); i++) {
                handler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }
}
