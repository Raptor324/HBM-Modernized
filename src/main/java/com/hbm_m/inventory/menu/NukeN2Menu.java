package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.bomb.NukeN2BlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.inventory.menu.ModMenuTypes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.hbm_m.platform.ModItemStackHandler;

/**
 * Меню мины N2: сетка 3x4 из одинаковых зарядов.
 */
public class NukeN2Menu extends AbstractContainerMenu {

    public final NukeN2BlockEntity be;

    public NukeN2Menu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, getBlockEntity(playerInv, extraData));
    }

    private static NukeN2BlockEntity getBlockEntity(Inventory playerInv, FriendlyByteBuf extraData) {
        if (extraData == null) return null;
        BlockEntity blockEntity = playerInv.player.level().getBlockEntity(extraData.readBlockPos());
        if (blockEntity instanceof NukeN2BlockEntity tile) return tile;
        // На клиенте тайл может отсутствовать (реплей Flashback) — возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (playerInv.player.level().isClientSide) return null;
        throw new MenuBlockEntityMissingException("BlockEntity is not a NukeN2BlockEntity");
    }

    public NukeN2Menu(int id, Inventory inventory, NukeN2BlockEntity blockEntity) {
        super(ModMenuTypes.NUKE_N2_MENU.get(), id);
        this.be = blockEntity;

        // тайл может отсутствовать на клиенте (реплей Flashback) — подставляем пустую заглушку
        var container = this.be != null
                ? this.be
                : new ModItemStackHandlerContainer(new ModItemStackHandler.Dummy(NukeN2BlockEntity.SLOTS), () -> {});

        // Оригинал (1.7.10): обычные Slot без ограничения - в слоты мины через GUI
        // можно положить любой предмет; isReady() требует 12 зарядов N2, поэтому
        // посторонние предметы просто не дают детонации.
        for (int slot = 0; slot < NukeN2BlockEntity.SLOTS; slot++) {
            final int index = slot;
            addSlot(new Slot(container, index, 98 + (index % 3) * 18, 36 + (index / 3) * 18));
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(inventory, x + y * 9 + 9, 8 + x * 18, 140 + y * 18));
            }
        }
        for (int x = 0; x < 9; x++) {
            addSlot(new Slot(inventory, x, 8 + x * 18, 198));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        // тайл может отсутствовать на клиенте (реплей Flashback)
        return be != null && be.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Оригинал (1.7.10) ContainerNukeN2.transferStackInSlot: shift-click переносит
        // только содержимое слотов мины (индексы 0-11) в инвентарь игрока;
        // из инвентаря игрока предметы не переносятся (return null).
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = slot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index < NukeN2BlockEntity.SLOTS) {
            if (!this.moveItemStackTo(sourceStack, NukeN2BlockEntity.SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}

