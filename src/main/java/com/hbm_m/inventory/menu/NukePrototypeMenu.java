package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.bomb.NukePrototypeBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.platform.DummyItemStackHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class NukePrototypeMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 14;

    public final NukePrototypeBlockEntity be;

    public NukePrototypeMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, getBlockEntity(playerInv, extraData));
    }

    private static NukePrototypeBlockEntity getBlockEntity(Inventory playerInv, FriendlyByteBuf extraData) {
        if (extraData == null) return null;
        BlockEntity blockEntity = playerInv.player.level().getBlockEntity(extraData.readBlockPos());
        if (blockEntity instanceof NukePrototypeBlockEntity tile) return tile;
        // На клиенте тайл может отсутствовать (реплей Flashback) — возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (playerInv.player.level().isClientSide) return null;
        throw new IllegalStateException("BlockEntity is not a NukePrototypeBlockEntity");
    }

    public NukePrototypeMenu(int id, Inventory inventory, NukePrototypeBlockEntity blockEntity) {
        super(ModMenuTypes.NUKE_PROTOTYPE_MENU.get(), id);
        this.be = blockEntity;

        // тайл может отсутствовать на клиенте (реплей Flashback) — подставляем пустую заглушку
        var container = this.be != null
                ? this.be
                : new ModItemStackHandlerContainer(new DummyItemStackHandler(MACHINE_SLOTS), () -> {});

        addSlot(new Slot(container,  0,   8, 35));
        addSlot(new Slot(container,  1,  26, 35));
        addSlot(new Slot(container,  2,  44, 26));
        addSlot(new Slot(container,  3,  44, 44));
        addSlot(new Slot(container,  4,  62, 26));
        addSlot(new Slot(container,  5,  62, 44));
        addSlot(new Slot(container,  6,  80, 26));
        addSlot(new Slot(container,  7,  80, 44));
        addSlot(new Slot(container,  8,  98, 26));
        addSlot(new Slot(container,  9,  98, 44));
        addSlot(new Slot(container, 10, 116, 26));
        addSlot(new Slot(container, 11, 116, 44));
        addSlot(new Slot(container, 12, 134, 35));
        addSlot(new Slot(container, 13, 152, 35));

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
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        if (index <= 13) {
            if (!this.moveItemStackTo(stack, 14, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }
}
