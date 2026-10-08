package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineFurnaceSteelBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.platform.DummyItemStackHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code ContainerFurnaceSteel}: drei Eingaenge (35,17+18i), drei Ausgaenge (125,17+18i). Die Anzeigewerte (Hitze
 * bis 100000, Fortschritt bis 40000) liest die GUI direkt aus der synchronisierten BE - ContainerData ueberträgt nur
 * 16 Bit.
 */
public class MachineFurnaceSteelMenu extends AbstractContainerMenu {

    public final MachineFurnaceSteelBlockEntity blockEntity;

    public MachineFurnaceSteelMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData), new SimpleContainerData(MachineFurnaceSteelBlockEntity.DATA_COUNT));
    }

    public MachineFurnaceSteelMenu(int id, Inventory inventory, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.FURNACE_STEEL_MENU.get(), id);
        this.blockEntity = entity instanceof MachineFurnaceSteelBlockEntity f ? f : null;

        var furnace = new ModItemStackHandlerContainer(
                blockEntity != null ? blockEntity.getInventory() : new DummyItemStackHandler(6),
                blockEntity != null ? blockEntity::setChanged : null);

        this.addSlot(new Slot(furnace, 0, 35, 17));
        this.addSlot(new Slot(furnace, 1, 35, 35));
        this.addSlot(new Slot(furnace, 2, 35, 53));

        this.addSlot(new OutputSlot(furnace, 3, 125, 17));
        this.addSlot(new OutputSlot(furnace, 4, 125, 35));
        this.addSlot(new OutputSlot(furnace, 5, 125, 53));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 142));
        }
    }

    private static BlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos();
        return inventory.player.level().getBlockEntity(pos);
    }

    @Override
    public boolean stillValid(Player player) {
        // w16b: Original isUseableByPlayer (TileEntityMachineBase) = 128 vom Kern, dazu Huelle der Maschine (MultiblockMenuReach)
        return MultiblockMenuReach.stillValidCore(blockEntity, player, 128.0D);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();

            if (index <= 5) {
                if (!this.moveItemStackTo(stack, 6, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, original);
            } else if (!this.moveItemStackTo(stack, 0, 3, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            slot.onTake(player, stack);
        }
        return original;
    }

    private static class OutputSlot extends Slot {
        OutputSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
}
