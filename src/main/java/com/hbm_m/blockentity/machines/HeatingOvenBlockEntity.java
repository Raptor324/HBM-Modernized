package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.HeatingOvenMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Heating Oven block entity - a pure combustion/heat source with an animated door.
 * Burns fuel to generate TU. Based on original 1.7.10 TileEntityHeaterOven.
 */
public class HeatingOvenBlockEntity extends BaseMachineBlockEntity {

    // Slots
    private static final int SLOT_COUNT = 1;
    private static final int FUEL_SLOT = 0;

    // Constants
    private static final int MAX_BURN_TIME = 400;
    private static final long ENERGY_CAPACITY = 20_000L;
    private static final long TU_GENERATION_PER_TICK = 500L;

    // State variables
    private int burnTime = 0;
    private boolean isOn = false;

    // Door animation (0-135 degrees equivalent, stored as 0-135 ticks)
    private float doorAngle = 0;
    private float prevDoorAngle = 0;
    private boolean doorOpen = false;

    // Client-side state
    private boolean wasOn = false;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> MAX_BURN_TIME;
                case 2 -> isOn ? 1 : 0;
                case 3 -> doorOpen ? 1 : 0;
                case 4 -> (int) doorAngle;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 2 -> isOn = value != 0;
                case 3 -> doorOpen = value != 0;
                case 4 -> doorAngle = value;
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public HeatingOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATING_OVEN_BE.get(), pos, state, SLOT_COUNT,
                ENERGY_CAPACITY, 0L, TU_GENERATION_PER_TICK);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.heating_oven");
    }
    
    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == FUEL_SLOT) {
            return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0) > 0;
        }
        return false;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new HeatingOvenMenu(containerId, playerInventory, this, this.data);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HeatingOvenBlockEntity blockEntity) {
        blockEntity.ensureNetworkInitialized();
        boolean wasOnBefore = blockEntity.isOn;

        // Update door animation
        blockEntity.updateDoorAnimation();

        // Process fuel and cooking
        if (blockEntity.burnTime > 0) {
            blockEntity.burnTime--;
            blockEntity.isOn = true;
        } else {
            // Try to consume fuel
            ItemStack fuelStack = blockEntity.inventory.getStackInSlot(FUEL_SLOT);
            if (!fuelStack.isEmpty()) {
                int fuelValue = AbstractFurnaceBlockEntity.getFuel().getOrDefault(fuelStack.getItem(), 0);
                if (fuelValue > 0) {
                    blockEntity.burnTime = fuelValue;
                    fuelStack.shrink(1);
                    blockEntity.isOn = true;
                    blockEntity.setChanged();
                }
            } else {
                blockEntity.isOn = false;
            }
        }

        // Generate TU whenever the oven is burning fuel.
        if (blockEntity.isOn) {
            blockEntity.setEnergyStored(Math.min(blockEntity.getMaxEnergyStored(),
                    blockEntity.getEnergyStored() + TU_GENERATION_PER_TICK));
        }

        // Sync to client if burning state changed
        if (wasOnBefore != blockEntity.isOn) {
            blockEntity.sendUpdateToClient();
        }
    }

    private void updateDoorAnimation() {
        prevDoorAngle = doorAngle;

        if (doorOpen) {
            if (doorAngle < 135) {
                doorAngle = Math.min(135, doorAngle + 10);
            }
        } else {
            if (doorAngle > 0) {
                doorAngle = Math.max(0, doorAngle - 10);
            }
        }
    }

    public void toggleDoor() {
        doorOpen = !doorOpen;
        setChanged();
        sendUpdateToClient();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("burnTime", burnTime);
        tag.putBoolean("isOn", isOn);
        tag.putFloat("doorAngle", doorAngle);
        tag.putBoolean("doorOpen", doorOpen);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        burnTime = tag.getInt("burnTime");
        isOn = tag.getBoolean("isOn");
        doorAngle = tag.getFloat("doorAngle");
        prevDoorAngle = doorAngle;
        doorOpen = tag.getBoolean("doorOpen");
        wasOn = isOn;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("isOn", isOn);
        tag.putFloat("doorAngle", doorAngle);
        tag.putBoolean("doorOpen", doorOpen);
        return tag;
    }

    //? if forge {
    @Override
    public void handleUpdateTag(CompoundTag tag) {
        // Forge-only hook; на Fabric используется стандартная синхронизация через load()
        wasOn = tag.getBoolean("isOn");
        doorAngle = tag.getFloat("doorAngle");
        prevDoorAngle = doorAngle;
        doorOpen = tag.getBoolean("doorOpen");
    }
    //?}

    @Override
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof com.hbm_m.block.machines.HeatingOvenBlock block)) {
            return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 2, 2));
        }
        Direction facing = state.getValue(com.hbm_m.block.machines.HeatingOvenBlock.FACING);
        return block.getStructureHelper().getRenderBoundingBox(worldPosition, facing, 0.0);
    }

    // Client-side getters for renderer
    public float getDoorAngle() {
        return doorAngle;
    }

    public float getPrevDoorAngle() {
        return prevDoorAngle;
    }

    public float getInterpolatedDoorAngle(float partialTick) {
        return Mth.lerp(partialTick, prevDoorAngle, doorAngle);
    }

    public boolean isOvenOn() {
        return wasOn;
    }

    public boolean isDoorOpen() {
        return doorOpen;
    }

    // Energy methods - heating oven doesn't use energy
    @Override
    public boolean canConnectEnergy(Direction side) {
        return true;
    }
}
