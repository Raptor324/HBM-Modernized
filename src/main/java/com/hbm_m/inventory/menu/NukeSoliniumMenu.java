package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.bomb.NukeSoliniumBlockEntity;
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
 * Меню солиниевой бомбы: X-паттерн из 9 слотов.
 */
public class NukeSoliniumMenu extends AbstractContainerMenu {

    public final NukeSoliniumBlockEntity be;

    private static final int[][] SLOT_POS = {
            {26, 18}, {53, 18}, {107, 18}, {134, 18},
            {80, 36},
            {26, 54}, {53, 54}, {107, 54}, {134, 54}
    };

    public NukeSoliniumMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, getBlockEntity(playerInv, extraData));
    }

    private static NukeSoliniumBlockEntity getBlockEntity(Inventory playerInv, FriendlyByteBuf extraData) {
        if (extraData == null) return null;
        BlockEntity blockEntity = playerInv.player.level().getBlockEntity(extraData.readBlockPos());
        if (blockEntity instanceof NukeSoliniumBlockEntity tile) return tile;
        // На клиенте тайл может отсутствовать (реплей Flashback) — возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (playerInv.player.level().isClientSide) return null;
        throw new MenuBlockEntityMissingException("BlockEntity is not a NukeSoliniumBlockEntity");
    }

    public NukeSoliniumMenu(int id, Inventory inventory, NukeSoliniumBlockEntity blockEntity) {
        super(ModMenuTypes.NUKE_SOLINIUM_MENU.get(), id);
        this.be = blockEntity;

        // тайл может отсутствовать на клиенте (реплей Flashback) — подставляем пустую заглушку
        var container = this.be != null
                ? this.be
                : new ModItemStackHandlerContainer(new ModItemStackHandler.Dummy(NukeSoliniumBlockEntity.SLOTS), () -> {});

        // 1:1 legacy ContainerNukeSolinium (ориг. строки 19-27): простые Slot — любой предмет
        // можно положить в любой слот; корректность набора проверяет только isReady().
        for (int slot = 0; slot < NukeSoliniumBlockEntity.SLOTS; slot++) {
            addSlot(new Slot(container, slot, SLOT_POS[slot][0], SLOT_POS[slot][1]));
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
        // 1:1 legacy ContainerNukeSolinium.transferStackInSlot (ориг. строки 44-74):
        // shift-click переносит только из слотов бомбы в инвентарь игрока;
        // из инвентаря в бомбу — запрещено (ориг. return null).
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            ItemStack copy = stackInSlot.copy();

            if (index < NukeSoliniumBlockEntity.SLOTS) {
                if (!this.moveItemStackTo(stackInSlot, NukeSoliniumBlockEntity.SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return copy;
        }
        return ItemStack.EMPTY;
    }
}

