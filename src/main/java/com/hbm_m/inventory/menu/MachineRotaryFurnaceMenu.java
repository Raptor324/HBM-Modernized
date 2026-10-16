package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineRotaryFurnaceBlockEntity;
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

/** Slot-Koordinaten 1:1 aus {@code ContainerMachineRotaryFurnace} (1.7.10 Original) uebernommen:
 *  Inputs (8,18)/(26,18)/(44,18), Brennstoff (44,54). Der Fluid-ID-Slot des Originals (8,54) entfaellt,
 *  da dieser Port echte Forge-Fluid-Tanks statt Item-Fluid-Identifier nutzt. Der Output-Slot (98,36)
 *  ist neu (siehe {@link MachineRotaryFurnaceBlockEntity}). */
public class MachineRotaryFurnaceMenu extends AbstractContainerMenu {

    public final MachineRotaryFurnaceBlockEntity blockEntity;
    private final ContainerData data;

    private static final int SLOT_IN1 = MachineRotaryFurnaceBlockEntity.SLOT_IN1;
    private static final int SLOT_IN2 = MachineRotaryFurnaceBlockEntity.SLOT_IN2;
    private static final int SLOT_IN3 = MachineRotaryFurnaceBlockEntity.SLOT_IN3;
    private static final int SLOT_FUEL = MachineRotaryFurnaceBlockEntity.SLOT_FUEL;
    private static final int SLOT_OUTPUT = MachineRotaryFurnaceBlockEntity.SLOT_OUTPUT;
    private static final int MACHINE_SLOT_COUNT = 5;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    public MachineRotaryFurnaceMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData), new SimpleContainerData(3));
    }

    public MachineRotaryFurnaceMenu(int id, Inventory inventory, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ROTARY_FURNACE_MENU.get(), id);
        checkContainerDataCount(data, 3);
        this.blockEntity = (MachineRotaryFurnaceBlockEntity) entity;
        this.data = data;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);
        this.addSlot(new Slot(container, SLOT_IN1, 8, 18));
        this.addSlot(new Slot(container, SLOT_IN2, 26, 18));
        this.addSlot(new Slot(container, SLOT_IN3, 44, 18));
        this.addSlot(new FuelSlot(container, SLOT_FUEL, 44, 54));
        this.addSlot(new OutputSlot(container, SLOT_OUTPUT, 98, 36));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 162));
        }

        addDataSlots(data);
    }

    private static MachineRotaryFurnaceBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineRotaryFurnaceBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No MachineRotaryFurnaceBlockEntity found at " + pos);
    }

    public int getLitTime() { return data.get(0); }
    public int getLitDuration() { return data.get(1); }
    public int getProgressScaled(int scale) { return data.get(2) * scale / 1000; }

    public int getBurnProgressScaled(int scale) {
        int duration = getLitDuration();
        return duration > 0 ? getLitTime() * scale / duration : 0;
    }

    public boolean isLit() {
        return getLitTime() > 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.ROTARY_FURNACE.get());
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
                if (MachineRotaryFurnaceBlockEntity.isFuel(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, SLOT_FUEL, SLOT_FUEL + 1, false)) {
                        if (!this.moveItemStackTo(slotStack, SLOT_IN1, SLOT_IN3 + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else if (!this.moveItemStackTo(slotStack, SLOT_IN1, SLOT_IN3 + 1, false)) {
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
            return MachineRotaryFurnaceBlockEntity.isFuel(stack);
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
