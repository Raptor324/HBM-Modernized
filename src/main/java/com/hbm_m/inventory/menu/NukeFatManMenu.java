package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.bomb.NukeFatManBlockEntity;

import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.platform.DummyItemStackHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class NukeFatManMenu extends AbstractContainerMenu {

    public final NukeFatManBlockEntity be;

    public NukeFatManMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, getBlockEntity(playerInv, extraData));
    }

    private static NukeFatManBlockEntity getBlockEntity(Inventory playerInv, FriendlyByteBuf extraData) {
        if (extraData == null) return null;
        BlockEntity blockEntity = playerInv.player.level().getBlockEntity(extraData.readBlockPos());
        if (blockEntity instanceof NukeFatManBlockEntity tile) return tile;
        // На клиенте тайл может отсутствовать (реплей Flashback) — возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (playerInv.player.level().isClientSide) return null;
        throw new IllegalStateException("BlockEntity is not a NukeFatManBlockEntity");
    }

    public NukeFatManMenu(int id, Inventory inventory, NukeFatManBlockEntity blockEntity) {
        super(ModMenuTypes.NUKE_FAT_MAN_MENU.get(), id);
        this.be = blockEntity;

        // тайл может отсутствовать на клиенте (реплей Flashback) — подставляем пустую заглушку
        var container = this.be != null
                ? this.be
                : new ModItemStackHandlerContainer(new DummyItemStackHandler(6), () -> {});

        addSlot(new Slot(container, 0, 8, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) { return be != null && be.canPlaceItem(0, stack); }
        });
        addSlot(new Slot(container, 1, 44, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) { return be != null && be.canPlaceItem(1, stack); }
        });
        addSlot(new Slot(container, 2, 8, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) { return be != null && be.canPlaceItem(2, stack); }
        });
        addSlot(new Slot(container, 3, 44, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) { return be != null && be.canPlaceItem(3, stack); }
        });
        addSlot(new Slot(container, 4, 26, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return be != null && be.canPlaceItem(4, stack); }
        });
        addSlot(new Slot(container, 5, 98, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return be != null && be.canPlaceItem(5, stack); }
        });

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(inventory, x + y * 9 + 9, 8 + x * 18, 84 + y * 18));
            }
        }
        for (int x = 0; x < 9; x++) {
            addSlot(new Slot(inventory, x, 8 + x * 18, 142));
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
