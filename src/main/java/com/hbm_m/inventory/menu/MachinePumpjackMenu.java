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

/** Upstream shares ContainerMachineOilWell between derrick and pumpjack, so the layouts are identical. */
public class MachinePumpjackMenu extends AbstractContainerMenu {

    private final MachinePumpjackBlockEntity blockEntity;

    public MachinePumpjackMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachinePumpjackMenu(int id, Inventory inventory, MachinePumpjackBlockEntity blockEntity) {
        super(ModMenuTypes.PUMPJACK_MENU.get(), id);
        this.blockEntity = blockEntity;

        // На клиенте тайл может отсутствовать (реплей Flashback) — подставляем пустую заглушку,
        // чтобы конструктор дошёл до конца и пакет открытия меню не уронил клиент
        var container = new ModItemStackHandlerContainer(
                blockEntity != null ? blockEntity.getInventory() : new DummyItemStackHandler(8),
                blockEntity != null ? blockEntity::setChanged : null);

        // slot 0: battery
        this.addSlot(new Slot(container, 0, 8, 53));
        // slot 1: oil canister input
        this.addSlot(new Slot(container, 1, 80, 17));
        // slot 2: oil canister output (take-only)
        this.addSlot(new Slot(container, 2, 80, 53) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        // slot 3: gas canister input
        this.addSlot(new Slot(container, 3, 125, 17));
        // slot 4: gas canister output (take-only)
        this.addSlot(new Slot(container, 4, 125, 53) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        // slots 5-7: upgrades
        this.addSlot(new Slot(container, 5, 152, 17));
        this.addSlot(new Slot(container, 6, 152, 35));
        this.addSlot(new Slot(container, 7, 152, 53));

        // player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
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
        throw new MenuBlockEntityMissingException("No MachinePumpjackBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":pumpjack_menu");
    }

    public MachinePumpjackBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        return MenuReach.stillValid(player, blockEntity);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index <= 7) {
            // machine slot → move to player inventory
            if (!this.moveItemStackTo(stack, 8, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // player slot → move to machine
            if (stack.getItem() instanceof ItemMachineUpgrade) {
                if (!this.moveItemStackTo(stack, 5, 8, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Slots 2 and 4 are take-only outputs and must stay out of the insert ranges: vanilla
                // moveItemStackTo skips mayPlace when it merges onto an existing stack.
                if (!this.moveItemStackTo(stack, 0, 2, false)) {
                    if (!this.moveItemStackTo(stack, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }
}
