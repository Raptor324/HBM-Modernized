package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineMiningDrillBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.industrial.ItemDrillbit;
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

/**
 * 1:1 {@code ContainerMachineExcavator}: Batterie (220,72), Fluidkennung (202,72), Aufwertungen und Bohrkopf in einer
 * Reihe ab (136,75), 3x3-Puffer ab (136,5) nur zum Entnehmen, Spielerinventar ab (41,122).
 */
public class MachineMiningDrillMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachineMiningDrillBlockEntity.INVENTORY_SIZE;

    private final MachineMiningDrillBlockEntity blockEntity;

    public MachineMiningDrillMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineMiningDrillMenu(int id, Inventory inventory, MachineMiningDrillBlockEntity blockEntity) {
        super(ModMenuTypes.MINING_DRILL_MENU.get(), id);
        this.blockEntity = blockEntity;

        // На клиенте тайл может отсутствовать (реплей Flashback) — подставляем пустую заглушку
        var container = new ModItemStackHandlerContainer(
                blockEntity != null ? blockEntity.getInventory() : new DummyItemStackHandler(MACHINE_SLOT_COUNT),
                blockEntity != null ? blockEntity::setChanged : null);

        this.addSlot(new Slot(container, 0, 220, 72));
        this.addSlot(new Slot(container, 1, 202, 72));
        for (int i = 0; i < 3; i++) {
            this.addSlot(new Slot(container, 2 + i, 136 + i * 18, 75));
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new Slot(container, 5 + j + i * 3, 136 + j * 18, 5 + i * 18) {
                    @Override public boolean mayPlace(ItemStack stack) { return false; }
                });
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 41 + j * 18, 122 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inventory, i, 41 + i * 18, 180));
        }
    }

    public static MachineMiningDrillMenu create(int id, Inventory inventory, MachineMiningDrillBlockEntity blockEntity) {
        return new MachineMiningDrillMenu(id, inventory, blockEntity);
    }

    private static MachineMiningDrillBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineMiningDrillBlockEntity drill) return drill;
        if (inventory.player.level().isClientSide) return null;
        throw new IllegalStateException("No MachineMiningDrillBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":mining_drill_menu");
    }

    public MachineMiningDrillBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        // w16b: Original isUseableByPlayer (TileEntityMachineBase) = 128 vom Kern, dazu Huelle der Maschine (MultiblockMenuReach)
        return MultiblockMenuReach.stillValidCore(blockEntity, player, 128.0D);
    }

    /** Original {@code transferStackInSlot}: Batterie, Fluidkennung, Aufwertungen, Bohrkopf in ihre Plaetze. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        if (index <= 13) {
            if (!this.moveItemStackTo(stack, 14, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(result).isPresent()
                || com.hbm_m.api.energy.ItemEnergyAccess.getHbmReceiver(result).isPresent()) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (result.getItem() instanceof com.hbm_m.interfaces.IItemFluidIdentifier) {
            if (!this.moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else if (result.getItem() instanceof ItemMachineUpgrade) {
            if (!this.moveItemStackTo(stack, 2, 4, false)) return ItemStack.EMPTY;
        } else if (result.getItem() instanceof ItemDrillbit) {
            if (!this.moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
