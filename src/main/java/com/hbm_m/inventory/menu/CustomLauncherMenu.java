package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.machines.CustomLauncherBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.platform.DummyItemStackHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code ContainerCompactLauncher} / {@code ContainerLaunchTable} (identische Belegung): Rakete, Zielgeber,
 * Treibstoff- und Oxidatorkanister samt Ausgaben, Feststoff, Batterie.
 */
public class CustomLauncherMenu extends AbstractContainerMenu {

    private static final int TE_SLOTS = CustomLauncherBlockEntity.SLOT_COUNT;

    public final CustomLauncherBlockEntity blockEntity;

    public CustomLauncherMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public CustomLauncherMenu(int id, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.CUSTOM_LAUNCHER_MENU.get(), id);
        this.blockEntity = entity instanceof CustomLauncherBlockEntity l ? l : null;

        // тайл может отсутствовать на клиенте (реплей Flashback) — подставляем пустую заглушку
        var container = this.blockEntity != null
                ? new ModItemStackHandlerContainer(this.blockEntity.getInventory(), this.blockEntity::setChanged)
                : new ModItemStackHandlerContainer(new DummyItemStackHandler(TE_SLOTS), () -> {});

        addSlot(new Slot(container, 0, 26, 36));
        addSlot(new Slot(container, 1, 26, 72));
        addSlot(new Slot(container, 2, 116, 90 - 18));
        addSlot(new Slot(container, 3, 134, 90 - 18));
        addSlot(new Slot(container, 4, 152, 90));
        addSlot(new Slot(container, 5, 116, 108));
        addSlot(new Slot(container, 6, 116, 90));
        addSlot(new Slot(container, 7, 134, 90));

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18 + 56));

        for (int i = 0; i < 9; i++)
            addSlot(new Slot(inv, i, 8 + i * 18, 142 + 56));
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index <= 7) {
            if (!moveItemStackTo(stack, TE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, TE_SLOTS, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (blockEntity == null || blockEntity.isRemoved()) return false;
        return player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5, blockEntity.getBlockPos().getY() + 0.5, blockEntity.getBlockPos().getZ() + 0.5) <= 64;
    }
}
