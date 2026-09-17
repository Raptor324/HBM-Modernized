package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineWoodBurnerBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.module.ModuleBurnTime;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Порт {@code ContainerMachineWoodBurner} (1.7.10 Original): Fuel (26,18),
 * Ashes = take-only (26,54), Fluid ID (98,54), контейнер жидкости (98,18),
 * контейнер наружу = take-only (98,36), Battery (143,54); инвентарь игрока
 * (8,104) / хотбар (8,162). Shift-клик: батарея → 5, fluid identifier → 2,
 * топливо → 0, остальное → 3.
 *
 * <p>Слоты ручной установки unrestricted (как plain Slot 1.7.10), ограничение
 * топлива живёт на уровне BE ({@code isItemValidForSlot}) и воронок.
 */
public class MachineWoodBurnerMenu extends AbstractContainerMenu {

    public static final int SLOT_FUEL = MachineWoodBurnerBlockEntity.SLOT_FUEL;
    public static final int SLOT_ASH = MachineWoodBurnerBlockEntity.SLOT_ASH;
    public static final int SLOT_FLUID_ID = MachineWoodBurnerBlockEntity.SLOT_FLUID_ID;
    public static final int SLOT_FLUID_IN = MachineWoodBurnerBlockEntity.SLOT_FLUID_IN;
    public static final int SLOT_FLUID_OUT = MachineWoodBurnerBlockEntity.SLOT_FLUID_OUT;
    public static final int SLOT_BATTERY = MachineWoodBurnerBlockEntity.SLOT_BATTERY;
    public static final int MACHINE_SLOT_COUNT = MachineWoodBurnerBlockEntity.INVENTORY_SIZE;

    public final MachineWoodBurnerBlockEntity blockEntity;

    public MachineWoodBurnerMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineWoodBurnerMenu(int id, Inventory inventory, MachineWoodBurnerBlockEntity blockEntity) {
        super(ModMenuTypes.WOOD_BURNER_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        //Fuel
        this.addSlot(new Slot(container, SLOT_FUEL, 26, 18) {
            @Override public boolean mayPlace(ItemStack stack) { return true; }
        });
        //Ashes — SlotTakeOnly: класть нельзя, забирать можно
        this.addSlot(new SlotTakeOnly(container, SLOT_ASH, 26, 54));
        //Fluid ID
        this.addSlot(new Slot(container, SLOT_FLUID_ID, 98, 54) {
            @Override public boolean mayPlace(ItemStack stack) { return true; }
        });
        //Fluid Container
        this.addSlot(new Slot(container, SLOT_FLUID_IN, 98, 18) {
            @Override public boolean mayPlace(ItemStack stack) { return true; }
        });
        this.addSlot(new SlotTakeOnly(container, SLOT_FLUID_OUT, 98, 36));
        //Battery
        this.addSlot(new Slot(container, SLOT_BATTERY, 143, 54) {
            @Override public boolean mayPlace(ItemStack stack) { return true; }
        });

        // Player inventory, 1:1 с ContainerMachineWoodBurner
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 104 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 162));
        }
    }

    private static MachineWoodBurnerBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineWoodBurnerBlockEntity woodBurnerBlockEntity) {
            return woodBurnerBlockEntity;
        }
        throw new IllegalStateException("No MachineWoodBurnerBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":wood_burner_menu");
    }

    public MachineWoodBurnerBlockEntity getBlockEntity() {
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

    /**
     * Порт {@code transferStackInSlot}: слоты машины → инвентарь игрока;
     * батарея → 5, fluid identifier → 2, топливо → 0, остальное → 3.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (index <= 5) {
                if (!this.moveItemStackTo(slotStack, MACHINE_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {

                if (isChargeableItem(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, SLOT_BATTERY, SLOT_BATTERY + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotStack.getItem() instanceof IItemFluidIdentifier) {
                    if (!this.moveItemStackTo(slotStack, SLOT_FLUID_ID, SLOT_FLUID_ID + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (ModuleBurnTime.getBaseBurnTime(slotStack) > 0) {
                    if (!this.moveItemStackTo(slotStack, SLOT_FUEL, SLOT_FUEL + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(slotStack, SLOT_FLUID_IN, SLOT_FLUID_IN + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }

        return itemstack;
    }

    /** Аналог {@code stack.getItem() instanceof IBatteryItem}: предметы, принимающие заряд. */
    private static boolean isChargeableItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof com.hbm_m.powerarmor.ModArmorFSBPowered) return true;
        if (com.hbm_m.api.energy.ItemEnergyAccess.getHbmReceiver(stack).isPresent()) return true;
        return com.hbm_m.api.energy.ItemEnergyAccess.canForgeReceive(stack);
    }

    /** Порт {@code SlotTakeOnly}: mayPlace = false, извлечение разрешено. */
    private static class SlotTakeOnly extends Slot {
        public SlotTakeOnly(net.minecraft.world.Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
