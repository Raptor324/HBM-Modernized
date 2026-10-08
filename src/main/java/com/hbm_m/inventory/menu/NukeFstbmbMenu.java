package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.bomb.NukeFstbmbBlockEntity;
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
 * 1:1 {@code ContainerNukeFstbmb}: Ei (17,36), Batterie (53,36), Spielerinventar bei y=140.
 */
public class NukeFstbmbMenu extends AbstractContainerMenu {

    public final NukeFstbmbBlockEntity be;

    public NukeFstbmbMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, getBlockEntity(playerInv, extraData));
    }

    private static NukeFstbmbBlockEntity getBlockEntity(Inventory playerInv, FriendlyByteBuf extraData) {
        if (extraData == null) return null;
        BlockEntity blockEntity = playerInv.player.level().getBlockEntity(extraData.readBlockPos());
        if (blockEntity instanceof NukeFstbmbBlockEntity tile) return tile;
        // На клиенте тайл может отсутствовать (реплей Flashback) — возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (playerInv.player.level().isClientSide) return null;
        throw new IllegalStateException("BlockEntity is not a NukeFstbmbBlockEntity");
    }

    public NukeFstbmbMenu(int id, Inventory inventory, NukeFstbmbBlockEntity blockEntity) {
        super(ModMenuTypes.NUKE_FSTBMB_MENU.get(), id);
        this.be = blockEntity;

        // тайл может отсутствовать на клиенте (реплей Flashback) — подставляем пустую заглушку
        var container = this.be != null
                ? this.be
                : new ModItemStackHandlerContainer(new DummyItemStackHandler(2), () -> {});

        // Original: einfache Slots, jeder Gegenstand passt
        addSlot(new Slot(container, 0, 17, 36));
        addSlot(new Slot(container, 1, 53, 36));

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(inventory, x + y * 9 + 9, 8 + x * 18, 84 + y * 18 + 56));
            }
        }
        for (int x = 0; x < 9; x++) {
            addSlot(new Slot(inventory, x, 8 + x * 18, 142 + 56));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        // тайл может отсутствовать на клиенте (реплей Flashback)
        return be != null && be.stillValid(player);
    }

    /** Original {@code transferStackInSlot}: nur aus der Bombe heraus ({@code par2 <= 2}, Ziel ab Index 2). */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = this.slots.get(index);

        if (var4 != null && var4.hasItem()) {
            ItemStack var5 = var4.getItem();
            var3 = var5.copy();

            if (index <= 2) {
                if (!this.moveItemStackTo(var5, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (var5.isEmpty()) {
                var4.set(ItemStack.EMPTY);
            } else {
                var4.setChanged();
            }
        }

        return var3;
    }
}
