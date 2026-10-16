package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineFurnaceBrickBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Slot-Koordinaten (176x166 Textur) aus {@code ContainerFurnaceBrick} (1.7.10 Original)
 *  uebernommen: Input (62,35), Brennstoff (35,17), Output (116,35). Der Aschen-Slot (Slot 3)
 *  des Originals entfaellt - siehe {@link MachineFurnaceBrickBlockEntity}. */
public class MachineFurnaceBrickMenu extends AbstractContainerMenu {

    public final MachineFurnaceBrickBlockEntity blockEntity;
    private final ContainerData data;

    private static final int SLOT_INPUT = MachineFurnaceBrickBlockEntity.SLOT_INPUT;
    private static final int SLOT_FUEL = MachineFurnaceBrickBlockEntity.SLOT_FUEL;
    private static final int SLOT_OUTPUT = MachineFurnaceBrickBlockEntity.SLOT_OUTPUT;
    private static final int MACHINE_SLOT_COUNT = 3;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    private static final int DATA_LIT_TIME = 0;
    private static final int DATA_LIT_DURATION = 1;
    private static final int DATA_PROGRESS = 2;

    public MachineFurnaceBrickMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData), new SimpleContainerData(3));
    }

    public MachineFurnaceBrickMenu(int id, Inventory inventory, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.FURNACE_BRICK_MENU.get(), id);
        checkContainerDataCount(data, 3);
        this.blockEntity = (MachineFurnaceBrickBlockEntity) entity;
        this.data = data;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);
        this.addSlot(new Slot(container, SLOT_INPUT, 62, 35));
        this.addSlot(new FuelSlot(container, SLOT_FUEL, 35, 17));
        this.addSlot(new OutputSlot(container, SLOT_OUTPUT, 116, 35));

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

    private static MachineFurnaceBrickBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineFurnaceBrickBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No MachineFurnaceBrickBlockEntity found at " + pos);
    }

    public int getLitTime() { return data.get(DATA_LIT_TIME); }
    public int getLitDuration() { return data.get(DATA_LIT_DURATION); }
    public int getProgress() { return data.get(DATA_PROGRESS); }

    public int getBurnProgressScaled(int scale) {
        int duration = getLitDuration();
        return duration > 0 ? getLitTime() * scale / duration : 0;
    }

    public int getCookProgressScaled(int scale) {
        return getProgress() * scale / 200;
    }

    public boolean isLit() {
        return getLitTime() > 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.FURNACE_BRICK.get());
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
            } else {
                if (MachineFurnaceBrickBlockEntity.isFuel(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, SLOT_FUEL, SLOT_FUEL + 1, false)) {
                        if (!this.moveItemStackTo(slotStack, SLOT_INPUT, SLOT_INPUT + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else if (!this.moveItemStackTo(slotStack, SLOT_INPUT, SLOT_INPUT + 1, false)) {
                    return ItemStack.EMPTY;
                }
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

    private static class FuelSlot extends Slot {
        public FuelSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return MachineFurnaceBrickBlockEntity.isFuel(stack);
        }
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
