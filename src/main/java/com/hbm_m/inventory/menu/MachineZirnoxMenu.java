package com.hbm_m.inventory.menu;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.block.entity.machines.MachineZirnoxBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.FluidBarrelItem;
import com.hbm_m.item.tags_and_tiers.ModTags;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;

public class MachineZirnoxMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachineZirnoxBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_SLOT_START = MACHINE_SLOT_COUNT;

    private final MachineZirnoxBlockEntity blockEntity;
    private final ModItemStackHandlerContainer machineContainer;

    public MachineZirnoxMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineZirnoxMenu(int id, Inventory inventory, MachineZirnoxBlockEntity blockEntity) {
        super(ModMenuTypes.ZIRNOX_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.machineContainer = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        // Rods
        this.addSlot(new Slot(machineContainer, 0, 26, 16));
        this.addSlot(new Slot(machineContainer, 1, 62, 16));
        this.addSlot(new Slot(machineContainer, 2, 98, 16));
        this.addSlot(new Slot(machineContainer, 3, 8, 34));
        this.addSlot(new Slot(machineContainer, 4, 44, 34));
        this.addSlot(new Slot(machineContainer, 5, 80, 34));
        this.addSlot(new Slot(machineContainer, 6, 116, 34));
        this.addSlot(new Slot(machineContainer, 7, 26, 52));
        this.addSlot(new Slot(machineContainer, 8, 62, 52));
        this.addSlot(new Slot(machineContainer, 9, 98, 52));
        this.addSlot(new Slot(machineContainer, 10, 8, 70));
        this.addSlot(new Slot(machineContainer, 11, 44, 70));
        this.addSlot(new Slot(machineContainer, 12, 80, 70));
        this.addSlot(new Slot(machineContainer, 13, 116, 70));
        this.addSlot(new Slot(machineContainer, 14, 26, 88));
        this.addSlot(new Slot(machineContainer, 15, 62, 88));
        this.addSlot(new Slot(machineContainer, 16, 98, 88));
        this.addSlot(new Slot(machineContainer, 17, 8, 106));
        this.addSlot(new Slot(machineContainer, 18, 44, 106));
        this.addSlot(new Slot(machineContainer, 19, 80, 106));
        this.addSlot(new Slot(machineContainer, 20, 116, 106));
        this.addSlot(new Slot(machineContainer, 21, 26, 124));
        this.addSlot(new Slot(machineContainer, 22, 62, 124));
        this.addSlot(new Slot(machineContainer, 23, 98, 124));

        // Fluid IO
        this.addSlot(new Slot(machineContainer, MachineZirnoxBlockEntity.SLOT_CO2_IN, 143, 124));
        this.addSlot(new TakeOnlySlot(machineContainer, MachineZirnoxBlockEntity.SLOT_CO2_OUT, 143, 142));
        this.addSlot(new Slot(machineContainer, MachineZirnoxBlockEntity.SLOT_WATER_IN, 179, 124));
        this.addSlot(new TakeOnlySlot(machineContainer, MachineZirnoxBlockEntity.SLOT_WATER_OUT, 179, 142));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 174 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 232));
        }
    }

    public static MachineZirnoxMenu create(int id, Inventory inventory, MachineZirnoxBlockEntity blockEntity) {
        return new MachineZirnoxMenu(id, inventory, blockEntity);
    }

    private static MachineZirnoxBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineZirnoxBlockEntity zirnoxBlockEntity) {
            return zirnoxBlockEntity;
        }
        throw new IllegalStateException("No MachineZirnoxBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":zirnox_menu");
    }

    public MachineZirnoxBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() != player.level()) {
            return false;
        }
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(stack, PLAYER_SLOT_START, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (holdsFluid(stack, ModFluids.CARBONDIOXIDE.getSource())) {
                if (!this.moveItemStackTo(stack, MachineZirnoxBlockEntity.SLOT_CO2_IN,
                        MachineZirnoxBlockEntity.SLOT_CO2_IN + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (holdsFluid(stack, ModFluids.WATER.getSource()) || stack.is(Items.WATER_BUCKET)) {
                if (!this.moveItemStackTo(stack, MachineZirnoxBlockEntity.SLOT_WATER_IN,
                        MachineZirnoxBlockEntity.SLOT_WATER_IN + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(ModTags.Items.ZIRNOX_RODS)) {
                if (!this.moveItemStackTo(stack, 0, MachineZirnoxBlockEntity.ROD_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            slot.onTake(player, stack);
        }

        return result;
    }

    private static boolean holdsFluid(ItemStack stack, Fluid targetFluid) {
        if (stack.is(Items.WATER_BUCKET)) {
            return VanillaFluidEquivalence.sameSubstance(targetFluid, net.minecraft.world.level.material.Fluids.WATER);
        }

        if (!stack.is(ModItems.FLUID_BARREL.get())) {
            return false;
        }

        FluidStack fluid = FluidBarrelItem.getFluid(stack);
        return !fluid.isEmpty() && VanillaFluidEquivalence.sameSubstance(fluid.getFluid(), targetFluid);
    }

    private static final class TakeOnlySlot extends Slot {
        private TakeOnlySlot(net.minecraft.world.Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
