package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachinePumpjackBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.DummyItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 1:1 {@code ContainerMachineOilWell} (1.7.10) - dasselbe Slotlayout wie der Bohrturm. */
public class MachinePumpjackMenu extends AbstractContainerMenu {

    private final MachinePumpjackBlockEntity blockEntity;

    public MachinePumpjackMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachinePumpjackMenu(int id, Inventory inventory, MachinePumpjackBlockEntity blockEntity) {
        super(ModMenuTypes.PUMPJACK_MENU.get(), id);
        this.blockEntity = blockEntity;

        // На клиенте тайл может отсутствовать (реплей Flashback) — подставляем пустую заглушку
        var container = new ModItemStackHandlerContainer(
                blockEntity != null ? blockEntity.getInventory() : new DummyItemStackHandler(8),
                blockEntity != null ? blockEntity::setChanged : null);

        // Battery
        this.addSlot(new Slot(container, 0, 8, 58));
        // Canister Input / Output
        this.addSlot(new Slot(container, 1, 94, 22));
        this.addSlot(new Slot(container, 2, 94, 58) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        // Gas Input / Output
        this.addSlot(new Slot(container, 3, 130, 22));
        this.addSlot(new Slot(container, 4, 130, 58) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        // Upgrades
        this.addSlot(new Slot(container, 5, 156, 36));
        this.addSlot(new Slot(container, 6, 156, 54));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 12 + col * 18, 108 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 12 + col * 18, 166));
        }
    }

    public static MachinePumpjackMenu create(int id, Inventory inventory, MachinePumpjackBlockEntity blockEntity) {
        return new MachinePumpjackMenu(id, inventory, blockEntity);
    }

    private static MachinePumpjackBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachinePumpjackBlockEntity pumpjackBlockEntity) {
            return pumpjackBlockEntity;
        }
        // На клиенте тайл может отсутствовать (реплей Flashback) — не крашим пакет, возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (inventory.player.level().isClientSide) {
            return null;
        }
        throw new IllegalStateException("No MachinePumpjackBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":pumpjack_menu");
    }

    public MachinePumpjackBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        // audit13: Original isUseableByPlayer (<= 128 zur Kernmitte) oder Huelle <= 64; Vanilla 64 schloss die GUI an grossen Maschinen
        return MultiblockMenuReach.stillValidCore(blockEntity, player, 128.0D);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index <= 6) {
            if (!this.moveItemStackTo(stack, 7, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof ItemMachineUpgrade) {
            if (!this.moveItemStackTo(stack, 5, 7, false)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, 0, 2, false)) {
            if (!this.moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
