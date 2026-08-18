package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineCompressorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if forge {
import net.minecraftforge.items.SlotItemHandler;
//?} elif neoforge {
/*import net.neoforged.neoforge.items.SlotItemHandler;
*///?}

/** Slot-Koordinaten 1:1 aus {@code ContainerCompressor} (1.7.10 Original): Fluid-ID (17,72),
 *  Batterie (152,72). Upgrade-Slots des Originals entfallen (siehe
 *  {@link MachineCompressorBlockEntity}). */
public class MachineCompressorMenu extends AbstractContainerMenu {

    private final MachineCompressorBlockEntity blockEntity;
    private static final int MACHINE_SLOTS = 2;

    public MachineCompressorMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf));
    }

    public MachineCompressorMenu(int id, Inventory inv, MachineCompressorBlockEntity be) {
        super(ModMenuTypes.COMPRESSOR_MENU.get(), id);
        this.blockEntity = be;

        var handler = be.getInventory();
        addSlot(new SlotItemHandler(handler, MachineCompressorBlockEntity.SLOT_FLUID_ID, 17, 72));
        addSlot(new SlotItemHandler(handler, MachineCompressorBlockEntity.SLOT_BATTERY, 152, 72));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 180));
        }
    }

    public static MachineCompressorMenu create(int id, Inventory inv, MachineCompressorBlockEntity be) {
        return new MachineCompressorMenu(id, inv, be);
    }

    private static MachineCompressorBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof MachineCompressorBlockEntity c) return c;
        throw new IllegalStateException("No MachineCompressorBlockEntity at " + pos);
    }

    public MachineCompressorBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() == player.level()
            && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, MachineCompressorBlockEntity.SLOT_BATTERY, MachineCompressorBlockEntity.SLOT_BATTERY + 1, false)
             && !moveItemStackTo(stack, MachineCompressorBlockEntity.SLOT_FLUID_ID, MachineCompressorBlockEntity.SLOT_FLUID_ID + 1, false))
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
