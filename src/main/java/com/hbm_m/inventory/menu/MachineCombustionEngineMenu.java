package com.hbm_m.inventory.menu;

import com.hbm_m.api.energy.ItemEnergyAccess;
import com.hbm_m.blockentity.machines.MachineCombustionEngineBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}

/**
 * Порт {@code ContainerCombustionEngine} (1.7.10): Fluid in (17,17), Fluid out =
 * take-only (17,53), Pistons (88,71), Battery (143,71), Fluid-ID (35,71);
 * инвентарь игрока на 121/179.
 */
public class MachineCombustionEngineMenu extends AbstractContainerMenu {

    private final MachineCombustionEngineBlockEntity blockEntity;

    private static final int SLOT_FLUID_IN = MachineCombustionEngineBlockEntity.SLOT_FLUID_IN;
    private static final int SLOT_FLUID_OUT = MachineCombustionEngineBlockEntity.SLOT_FLUID_OUT;
    private static final int SLOT_PISTON = MachineCombustionEngineBlockEntity.SLOT_PISTON;
    private static final int SLOT_BATTERY = MachineCombustionEngineBlockEntity.SLOT_BATTERY;
    private static final int SLOT_FLUID_ID = MachineCombustionEngineBlockEntity.SLOT_FLUID_ID;
    private static final int MACHINE_SLOT_COUNT = MachineCombustionEngineBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    public MachineCombustionEngineMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineCombustionEngineMenu(int id, Inventory inventory, MachineCombustionEngineBlockEntity blockEntity) {
        super(ModMenuTypes.COMBUSTION_ENGINE_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        //Fluid in
        this.addSlot(new Slot(container, SLOT_FLUID_IN, 17, 17));
        //Fluid out — SlotTakeOnly: класть нельзя, забирать можно
        this.addSlot(new SlotTakeOnly(container, SLOT_FLUID_OUT, 17, 53));
        //Pistons
        this.addSlot(new Slot(container, SLOT_PISTON, 88, 71));
        //Battery
        this.addSlot(new Slot(container, SLOT_BATTERY, 143, 71) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isEnergyItem(stack);
            }
        });
        //Fluid ID
        this.addSlot(new Slot(container, SLOT_FLUID_ID, 35, 71));

        // Player inventory, портирован 1:1 с ContainerCombustionEngine (121/179).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 121 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 179));
        }
    }

    private static MachineCombustionEngineBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineCombustionEngineBlockEntity engine) {
            return engine;
        }
        throw new IllegalStateException("No MachineCombustionEngineBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":combustion_engine_menu");
    }

    public MachineCombustionEngineBlockEntity getBlockEntity() {
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
     * Порт {@code ContainerCombustionEngine.transferStackInSlot}: слоты машины → инвентарь;
     * из инвентаря: батарея → 3, fluid identifier → 4, поршни → 2, остальное → 0 (Fluid in).
     */
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
                if (isEnergyItem(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, SLOT_BATTERY, SLOT_BATTERY + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotStack.getItem() instanceof FluidIdentifierItem) {
                    if (!this.moveItemStackTo(slotStack, SLOT_FLUID_ID, SLOT_FLUID_ID + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (MachineCombustionEngineBlockEntity.isPistonSet(slotStack.getItem())) {
                    if (!this.moveItemStackTo(slotStack, SLOT_PISTON, SLOT_PISTON + 1, false)) {
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

            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, slotStack);
        }
        return result;
    }

    private static boolean isEnergyItem(ItemStack stack) {
        if (ItemEnergyAccess.getHbmProvider(stack).isPresent()) return true;
        //? if forge {
        return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
        //?} elif neoforge {
        /*return stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM) != null;
        *///?} else {
        /*return false;
        *///?}
    }

    /** Порт {@code SlotTakeOnly}: mayPlace = false, извлечение разрешено. */
    private static class SlotTakeOnly extends Slot {
        public SlotTakeOnly(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
