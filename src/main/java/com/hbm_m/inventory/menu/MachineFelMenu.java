package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineFelBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.machine.ItemFELCrystal;
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

/** 1:1 {@code ContainerFEL}: Batterie (182,144), Laserkristall (141,23), Spielerinventar ab y 83. */
public class MachineFelMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 2;

    private final MachineFelBlockEntity blockEntity;

    public MachineFelMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineFelMenu(int id, Inventory inventory, MachineFelBlockEntity blockEntity) {
        super(ModMenuTypes.FEL_MENU.get(), id);
        this.blockEntity = blockEntity;

        var handler = blockEntity != null ? blockEntity.getInventory() : new DummyItemStackHandler(MACHINE_SLOTS);
        var container = new ModItemStackHandlerContainer(handler, blockEntity != null ? blockEntity::setChanged : () -> { });

        this.addSlot(new Slot(container, 0, 182, 144));
        this.addSlot(new Slot(container, 1, 141, 23));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 83 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 141));
        }
    }

    public static MachineFelMenu create(int id, Inventory inventory, MachineFelBlockEntity blockEntity) {
        return new MachineFelMenu(id, inventory, blockEntity);
    }

    private static MachineFelBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineFelBlockEntity felBlockEntity) {
            return felBlockEntity;
        }
        // На клиенте тайл может отсутствовать (реплей Flashback) — не крашим пакет, возвращаем null.
        if (inventory.player.level().isClientSide) {
            return null;
        }
        throw new IllegalStateException("No MachineFelBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":fel_menu");
    }

    public MachineFelBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        // w16b: Original isUseableByPlayer (TileEntityMachineBase) = 128 vom Kern, dazu Huelle der Maschine (MultiblockMenuReach)
        return MultiblockMenuReach.stillValidCore(blockEntity, player, 128.0D);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < MACHINE_SLOTS) {
                if (!this.moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemFELCrystal) {
                if (!this.moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();

            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }
}
