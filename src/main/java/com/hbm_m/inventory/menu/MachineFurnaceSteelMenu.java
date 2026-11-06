package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineFurnaceSteelBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

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
import com.hbm_m.platform.ModItemStackHandler;

/** Slot-Koordinaten (176x166 Textur) 1:1 aus {@code ContainerFurnaceSteel} (1.7.10 Original)
 *  uebernommen: drei Eingaenge (35,17+18i), drei Ausgaenge (125,17+18i). Das Original hatte
 *  keine Brennstoff-Slots (reine Waermequelle) - der Port folgt dem Original und dem
 *  {@link MachineFurnaceSteelBlockEntity} (6 Slots, kein Brennstoff). Die Anzeigewerte
 *  (Hitze bis 100000, Fortschritt bis 40000) liest die GUI direkt aus dem synchronisierten
 *  BE - ContainerData uebertraegt nur 16 Bit. */
public class MachineFurnaceSteelMenu extends AbstractContainerMenu {

    public final MachineFurnaceSteelBlockEntity blockEntity;
    private final ContainerData data;

    private static final int MACHINE_SLOT_COUNT = 6;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    public MachineFurnaceSteelMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData),
                new SimpleContainerData(MachineFurnaceSteelBlockEntity.DATA_COUNT));
    }

    public MachineFurnaceSteelMenu(int id, Inventory inventory, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.FURNACE_STEEL_MENU.get(), id);
        checkContainerDataCount(data, MachineFurnaceSteelBlockEntity.DATA_COUNT);
        this.blockEntity = (MachineFurnaceSteelBlockEntity) entity;
        this.data = data;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);
        for (int lane = 0; lane < 3; lane++) {
            this.addSlot(new Slot(container, MachineFurnaceSteelBlockEntity.SLOT_INPUT_0 + lane, 35, 17 + lane * 18));
            this.addSlot(new OutputSlot(container, MachineFurnaceSteelBlockEntity.SLOT_OUTPUT_0 + lane, 125, 17 + lane * 18));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }

        addDataSlots(data);
    }

    private static MachineFurnaceSteelBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineFurnaceSteelBlockEntity be) {
            return be;
        }
        throw new MenuBlockEntityMissingException("No MachineFurnaceSteelBlockEntity found at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return MenuReach.stillValid(player, blockEntity, ModBlocks.FURNACE_STEEL.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 0, 3, false)) { // Original: mergeItemStack in die Eingaenge
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, slotStack);
        }
        return result;
    }

    private static class OutputSlot extends Slot {
        public OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
