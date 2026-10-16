package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineElectrolyserBlockEntity;

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

/** Vereinigt beide Original-GUIs (Fluid + Metall) in einem Menu (siehe
 *  {@link MachineElectrolyserBlockEntity}) statt zwei getrennter Container/Screens wie im
 *  Original {@code ContainerElectrolyserFluid}/{@code ContainerElectrolyserMetal}. */
public class MachineElectrolyserMenu extends AbstractContainerMenu {

    private final MachineElectrolyserBlockEntity blockEntity;
    private static final int MACHINE_SLOTS = 10;

    public MachineElectrolyserMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf));
    }

    public MachineElectrolyserMenu(int id, Inventory inv, MachineElectrolyserBlockEntity be) {
        super(ModMenuTypes.ELECTROLYSER_MENU.get(), id);
        this.blockEntity = be;

        var handler = be.getInventory();
        addSlot(new SlotItemHandler(handler, MachineElectrolyserBlockEntity.SLOT_BATTERY, 8, 17));
        addSlot(new SlotItemHandler(handler, MachineElectrolyserBlockEntity.SLOT_FLUID_ID, 8, 53));
        addSlot(new SlotItemHandler(handler, MachineElectrolyserBlockEntity.SLOT_BYPRODUCT_1, 62, 17));
        addSlot(new SlotItemHandler(handler, MachineElectrolyserBlockEntity.SLOT_BYPRODUCT_2, 62, 35));
        addSlot(new SlotItemHandler(handler, MachineElectrolyserBlockEntity.SLOT_BYPRODUCT_3, 62, 53));
        addSlot(new SlotItemHandler(handler, MachineElectrolyserBlockEntity.SLOT_CRYSTAL, 116, 17));
        addSlot(new SlotItemHandler(handler, MachineElectrolyserBlockEntity.SLOT_METAL_OUT_1, 152, 17));
        addSlot(new SlotItemHandler(handler, MachineElectrolyserBlockEntity.SLOT_METAL_OUT_2, 152, 35));
        addSlot(new SlotItemHandler(handler, MachineElectrolyserBlockEntity.SLOT_METAL_BYPRODUCT_1, 116, 53));
        addSlot(new SlotItemHandler(handler, MachineElectrolyserBlockEntity.SLOT_METAL_BYPRODUCT_2, 134, 53));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 162));
        }
    }

    public static MachineElectrolyserMenu create(int id, Inventory inv, MachineElectrolyserBlockEntity be) {
        return new MachineElectrolyserMenu(id, inv, be);
    }

    private static MachineElectrolyserBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof MachineElectrolyserBlockEntity e) return e;
        throw new IllegalStateException("No MachineElectrolyserBlockEntity at " + pos);
    }

    public MachineElectrolyserBlockEntity getBlockEntity() { return blockEntity; }

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
            if (!moveItemStackTo(stack, MachineElectrolyserBlockEntity.SLOT_BATTERY, MachineElectrolyserBlockEntity.SLOT_BATTERY + 1, false)
             && !moveItemStackTo(stack, MachineElectrolyserBlockEntity.SLOT_CRYSTAL, MachineElectrolyserBlockEntity.SLOT_CRYSTAL + 1, false)
             && !moveItemStackTo(stack, MachineElectrolyserBlockEntity.SLOT_FLUID_ID, MachineElectrolyserBlockEntity.SLOT_FLUID_ID + 1, false))
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
