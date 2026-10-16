package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.bomb.NukeN2BlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.inventory.menu.ModMenuTypes;
import com.hbm_m.platform.DummyItemStackHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

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
        throw new IllegalStateException("BlockEntity is not a NukeN2BlockEntity");
    }

    public NukeN2Menu(int id, Inventory inventory, NukeN2BlockEntity blockEntity) {
        super(ModMenuTypes.NUKE_N2_MENU.get(), id);
        this.be = blockEntity;

        // тайл может отсутствовать на клиенте (реплей Flashback) — подставляем пустую заглушку
        var container = this.be != null
                ? this.be
                : new ModItemStackHandlerContainer(new DummyItemStackHandler(NukeN2BlockEntity.SLOTS), () -> {});

        for (int slot = 0; slot < NukeN2BlockEntity.SLOTS; slot++) {
            final int index = slot;
            addSlot(new Slot(container, index, 98 + (index % 3) * 18, 36 + (index / 3) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) { return be != null && be.canPlaceItem(index, stack); }
            });
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
        return ItemStack.EMPTY;
    }
}

