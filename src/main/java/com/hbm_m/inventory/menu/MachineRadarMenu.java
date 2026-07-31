package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineRadarBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Главное меню радара (порт ContainerMachineRadarNT из 1.7.10).
 *
 * Как и в 1.7.10, основной GUI радара — это чистый экран БЕЗ слотов и БЕЗ
 * инвентаря игрока: только радарная развёртка, кнопки-тумблеры и клики по целям.
 * Слоты (батареи, дизайнатор, radar linker) вынесены в отдельный GUI —
 * {@link MachineRadarSlotsMenu} (порт GUIMachineRadarNTSlots, текстура gui_radar_link),
 * который открывается кнопкой из главного GUI.
 */
public class MachineRadarMenu extends AbstractContainerMenu {

    private final MachineRadarBlockEntity blockEntity;

    public MachineRadarMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineRadarMenu(int id, Inventory inventory, MachineRadarBlockEntity blockEntity) {
        super(ModMenuTypes.RADAR_MENU.get(), id);
        this.blockEntity = blockEntity;
        // Слотов нет — как ContainerMachineRadarNT в 1.7.10.
    }

    public static MachineRadarMenu create(int id, Inventory inventory, MachineRadarBlockEntity blockEntity) {
        return new MachineRadarMenu(id, inventory, blockEntity);
    }

    static MachineRadarBlockEntity getRadarBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        return getBlockEntity(inventory, buffer);
    }

    private static MachineRadarBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineRadarBlockEntity radarBlockEntity) {
            return radarBlockEntity;
        }
        throw new IllegalStateException("No MachineRadarBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":radar_menu");
    }

    public MachineRadarBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        // Без проверки дистанции (порт 1.7.10 Container не имел таковой):
        // GUI радара может открываться с Radar Screen, который находится далеко
        // от самого радара. distanceToSqr <= 64 закрывал GUI через секунду.
        if (blockEntity == null || blockEntity.getLevel() != player.level()) {
            return false;
        }
        return !blockEntity.isRemoved();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /**
     * Ванильный Container-адаптер поверх {@link ModItemStackHandler}
     * (как {@code LaunchPadLargeMenu.HandlerContainer}).
     * Общий для {@link MachineRadarMenu} и {@link MachineRadarSlotsMenu}.
     */
    static final class HandlerContainer implements Container {
        private final ModItemStackHandler handler;

        HandlerContainer(ModItemStackHandler handler) {
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
        public ItemStack getItem(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return handler.extractItem(slot, amount, false);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack cur = handler.getStackInSlot(slot);
            if (cur.isEmpty()) return ItemStack.EMPTY;
            handler.setStackInSlot(slot, ItemStack.EMPTY);
            return cur;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            handler.setStackInSlot(slot, stack);
        }

        @Override
        public void setChanged() {
            // изменения трекаются в ModItemStackHandler.onContentsChanged()
        }

        @Override
        public boolean stillValid(Player player) {
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
