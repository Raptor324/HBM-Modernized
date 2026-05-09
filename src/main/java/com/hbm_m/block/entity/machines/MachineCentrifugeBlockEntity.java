package com.hbm_m.block.entity.machines;

import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.api.energy.ItemEnergyAccess;
import com.hbm_m.inventory.menu.MachineCentrifugeMenu;
import com.hbm_m.recipe.CentrifugeRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Centrifuge machine.
 * Implements energy storage + simple progress cycle used by the GUI.
 */
public class MachineCentrifugeBlockEntity extends BaseMachineBlockEntity {

    private static final int BATTERY_SLOT = 0;
    private static final int INPUT_SLOT = 1;
    private static final int OUTPUT_SLOT_START = 2;
    private static final int OUTPUT_SLOTS = 4;
    private static final int TOTAL_SLOTS = OUTPUT_SLOT_START + OUTPUT_SLOTS;

    public static final long MAX_POWER = 50_000L;
    private static final long MAX_RECEIVE = 1_000L;
    private static final long ENERGY_PER_TICK = 25L;

    private static final int MAX_PROGRESS = 145;

    private int progress = 0;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> MAX_PROGRESS;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // read-only
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public MachineCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CENTRIFUGE_BE.get(), pos, state, TOTAL_SLOTS, MAX_POWER, MAX_RECEIVE);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.centrifuge");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= OUTPUT_SLOT_START && slot < OUTPUT_SLOT_START + OUTPUT_SLOTS) {
            return false;
        }
        if (slot == BATTERY_SLOT) {
            return isEnergyProviderItem(stack);
        }
        return true;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineCentrifugeMenu(containerId, playerInventory, this, containerData);
    }

    public ContainerData getContainerData() {
        return containerData;
    }

    public void drops() {
        SimpleContainer container = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, container);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCentrifugeBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        blockEntity.ensureNetworkInitialized();
        blockEntity.chargeFromBattery();

        if (level.getGameTime() % 10L == 0L) {
            blockEntity.updateEnergyDelta(blockEntity.getEnergyStored());
        }

        boolean dirty = false;

        if (blockEntity.canProcess() && blockEntity.getEnergyStored() >= ENERGY_PER_TICK) {
            blockEntity.setEnergyStored(blockEntity.getEnergyStored() - ENERGY_PER_TICK);
            blockEntity.progress++;
            dirty = true;

            if (blockEntity.progress >= MAX_PROGRESS) {
                blockEntity.progress = 0;
                blockEntity.finishCycle();
            }
        } else {
            if (blockEntity.progress != 0) {
                blockEntity.progress = 0;
                dirty = true;
            }
        }

        if (dirty) {
            blockEntity.setChanged();
            blockEntity.sendUpdateToClient();
        }
    }

    private boolean canProcess() {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }

        if (level == null) {
            return false;
        }

        ItemStack[] outputs = CentrifugeRecipes.getOutput(input);
        if (outputs == null) {
            // No recipe found - cannot process
            return false;
        }

        for (int i = 0; i < OUTPUT_SLOTS && i < outputs.length; i++) {
            ItemStack result = outputs[i];
            if (result.isEmpty()) {
                continue;
            }

            ItemStack outputSlot = inventory.getStackInSlot(OUTPUT_SLOT_START + i);
            if (outputSlot.isEmpty()) {
                continue;
            }

            if (!ItemStack.isSameItemSameTags(outputSlot, result)) {
                return false;
            }

            if (outputSlot.getCount() + result.getCount() > outputSlot.getMaxStackSize()) {
                return false;
            }
        }

        return true;
    }

    private void finishCycle() {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }

        ItemStack[] outputs = CentrifugeRecipes.getOutput(input);
        if (outputs == null) {
            // No recipe - should not happen if canProcess() was called first
            return;
        }

        for (int i = 0; i < OUTPUT_SLOTS && i < outputs.length; i++) {
            ItemStack result = outputs[i];
            if (result.isEmpty()) continue;

            int slot = OUTPUT_SLOT_START + i;
            ItemStack outputSlot = inventory.getStackInSlot(slot);
            if (outputSlot.isEmpty()) {
                inventory.setStackInSlot(slot, result.copy());
            } else if (ItemStack.isSameItemSameTags(outputSlot, result)) {
                outputSlot.grow(result.getCount());
            }
        }

        input.shrink(1);
    }

    private void chargeFromBattery() {
        chargeFromBatterySlot(BATTERY_SLOT);
    }
}
