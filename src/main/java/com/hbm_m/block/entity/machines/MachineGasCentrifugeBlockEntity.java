package com.hbm_m.block.entity.machines;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.machines.MachineGasCentrifugeBlock;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineGasCentrifugeMenu;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.item.ModItems;
import com.hbm_m.recipe.PseudoFluidTank;
import com.hbm_m.recipe.PseudoFluidType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * Gas Centrifuge BlockEntity.
 * <p>
 * Faithful port of the 1.7.10 cascade-enrichment mechanic: a real piped fluid (UF6/PUF6/WATZ) is
 * converted 1:1 into an internal "pseudo-fluid" stage, which is enriched over time (consuming
 * energy) and periodically handed off to an adjacent Gas Centrifuge for the next enrichment stage,
 * with tangible item byproducts produced at each cycle and at the final stage.
 */
public class MachineGasCentrifugeBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2 {

    private static final int SLOT_COUNT = 7;
    private static final int SLOT_OUTPUT_0 = 0;
    private static final int SLOT_OUTPUT_3 = 3;
    private static final int SLOT_BATTERY = 4;
    private static final int SLOT_FLUID_ID = 5;
    private static final int SLOT_UPGRADE = 6;

    private static final long MAX_POWER = 100_000;
    private static final int PROCESSING_SPEED = 150;
    private static final int PROCESSING_SPEED_HIGH = 80;
    private static final int ENERGY_PER_TICK = 200;
    private static final int ENERGY_PER_TICK_HIGH = 300;

    /** Cascade-chain search range: an adjacent centrifuge's controller may be a few blocks away
     *  once the multiblock footprint is taken into account. */
    private static final int CASCADE_SEARCH_RANGE = 4;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = Map.of(UpgradeType.SPEED, 1);

    private final FluidTank tank;
    private final PseudoFluidTank inputTank;
    private final PseudoFluidTank outputTank;
    private final UpgradeManager upgradeManager = new UpgradeManager();

    private int progress = 0;
    private boolean isProgressing = false;

    private float anim = 0.0F;
    private float prevAnim = 0.0F;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> getProcessingSpeed();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 2;
        }
    };

    public MachineGasCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_CENTRIFUGE_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);
        this.tank = new FluidTank(2000) {
            @Override
            public boolean isFluidValid(Fluid fluid) {
                return PseudoFluidType.FLUID_CONVERSIONS.containsKey(fluid);
            }
        };
        this.inputTank = new PseudoFluidTank(PseudoFluidType.NONE, 8000);
        this.outputTank = new PseudoFluidTank(PseudoFluidType.NONE, 8000);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineGasCentrifugeBlockEntity entity) {
        entity.prevAnim = entity.anim;

        if (level.isClientSide) {
            if (entity.isProgressing) {
                entity.anim += 0.05F;
            }
            if (entity.anim > (float) (Math.PI * 2.0)) {
                entity.anim -= (float) (Math.PI * 2.0);
            }
            return;
        }

        entity.ensureNetworkInitialized();
        entity.chargeFromBatterySlot(SLOT_BATTERY);
        entity.upgradeManager.checkSlots(entity.inventory, SLOT_UPGRADE, SLOT_UPGRADE, VALID_UPGRADES);

        if (level.getGameTime() % 10L == 0L) {
            entity.updateEnergyDelta(entity.getEnergyStored());
        }

        boolean dirty = false;

        entity.refreshTankTypeFromIdentifier();
        dirty |= entity.attemptConversion();

        if (entity.canEnrich()) {
            entity.isProgressing = true;
            entity.progress++;

            long cost = entity.isHighSpeed() ? ENERGY_PER_TICK_HIGH : ENERGY_PER_TICK;
            entity.setEnergyStored(entity.getEnergyStored() - cost);
            if (entity.getEnergyStored() < 0) {
                entity.setEnergyStored(0);
                entity.progress = 0;
            }

            if (entity.progress >= entity.getProcessingSpeed()) {
                entity.enrich();
            }
            dirty = true;
        } else {
            if (entity.progress != 0) {
                entity.progress = 0;
                dirty = true;
            }
            if (entity.isProgressing) {
                entity.isProgressing = false;
                dirty = true;
            }
        }

        if (level.getGameTime() % 10L == 0L) {
            BlockState blockState = entity.getBlockState();
            if (blockState.hasProperty(MachineGasCentrifugeBlock.FACING)) {
                Direction facing = blockState.getValue(MachineGasCentrifugeBlock.FACING);
                MachineGasCentrifugeBlockEntity neighbor = entity.findNeighborCentrifuge(level, pos, facing);
                boolean transferred = neighbor != null && entity.attemptTransferTo(neighbor);

                if (!transferred && entity.outputTank.getTankType() == PseudoFluidType.LEUF6) {
                    entity.convertFinalUraniumOutput();
                }
            }
        }

        if (dirty) {
            entity.setChanged();
            entity.sendUpdateToClient();
        }
    }

    private boolean isHighSpeed() {
        return upgradeManager.getLevel(UpgradeType.SPEED) > 0;
    }

    public int getProcessingSpeed() {
        return isHighSpeed() ? PROCESSING_SPEED_HIGH : PROCESSING_SPEED;
    }

    /** Reads the fluid-identifier item in the ID slot and (re)conforms {@link #tank} to it. */
    private void refreshTankTypeFromIdentifier() {
        ItemStack idStack = inventory.getStackInSlot(SLOT_FLUID_ID);
        Fluid newType = FluidIdentifierItem.resolvePrimaryForTank(idStack);
        if (newType == null) return;

        if (tank.getTankType() != newType) {
            PseudoFluidType pseudo = PseudoFluidType.FLUID_CONVERSIONS.get(newType);
            if (pseudo != null) {
                inputTank.setTankType(pseudo);
                outputTank.setTankType(pseudo.getOutputType());
                tank.conform(newType);
            }
        }
    }

    /** Converts real piped fluid into the internal pseudo-fluid 1:1, up to the input tank's capacity. */
    private boolean attemptConversion() {
        if (inputTank.getFill() >= inputTank.getMaxFill() || tank.getFill() <= 0) {
            return false;
        }
        int fill = Math.min(inputTank.getMaxFill() - inputTank.getFill(), tank.getFill());
        if (fill <= 0) return false;

        tank.setFill(tank.getFill() - fill);
        inputTank.setFill(inputTank.getFill() + fill);
        return true;
    }

    private boolean canEnrich() {
        PseudoFluidType type = inputTank.getTankType();
        if (getEnergyStored() <= 0) return false;
        if (inputTank.getFill() < type.getFluidConsumed()) return false;
        if (outputTank.getFill() + type.getFluidProduced() > outputTank.getMaxFill()) return false;
        if (type.isHighSpeed() && !isHighSpeed()) return false;
        if (type.getOutput().isEmpty()) return false;
        return hasRoomForOutputs(type);
    }

    private boolean hasRoomForOutputs(PseudoFluidType type) {
        for (ItemStack stack : type.getOutput()) {
            if (!canAcceptOutput(stack)) return false;
        }
        return true;
    }

    private boolean canAcceptOutput(ItemStack stack) {
        for (int slot = SLOT_OUTPUT_0; slot <= SLOT_OUTPUT_3; slot++) {
            ItemStack existing = inventory.getStackInSlot(slot);
            if (existing.isEmpty()) return true;
            if (ItemStack.isSameItemSameTags(existing, stack)
                    && existing.getCount() + stack.getCount() <= existing.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private void enrich() {
        PseudoFluidType type = inputTank.getTankType();
        this.progress = 0;
        inputTank.setFill(inputTank.getFill() - type.getFluidConsumed());
        outputTank.setFill(outputTank.getFill() + type.getFluidProduced());

        for (ItemStack stack : type.getOutput()) {
            placeOutputAnySlot(stack.copy());
        }
    }

    private void placeOutputAnySlot(ItemStack stack) {
        if (stack.isEmpty()) return;
        for (int slot = SLOT_OUTPUT_0; slot <= SLOT_OUTPUT_3; slot++) {
            ItemStack existing = inventory.getStackInSlot(slot);
            if (existing.isEmpty()) {
                inventory.setStackInSlot(slot, stack);
                return;
            }
            if (ItemStack.isSameItemSameTags(existing, stack)
                    && existing.getCount() + stack.getCount() <= existing.getMaxStackSize()) {
                existing.grow(stack.getCount());
                return;
            }
        }
    }

    /** Hands this centrifuge's outputTank pseudo-fluid to the next centrifuge in the cascade. */
    private boolean attemptTransferTo(MachineGasCentrifugeBlockEntity neighbor) {
        if (neighbor.tank.getTankType() != this.tank.getTankType()) return false;

        PseudoFluidType outType = this.outputTank.getTankType();
        if (neighbor.inputTank.getTankType() != outType && outType != PseudoFluidType.NONE) {
            neighbor.inputTank.setTankType(outType);
            PseudoFluidType next = outType.getOutputType();
            neighbor.outputTank.setTankType(next != null ? next : PseudoFluidType.NONE);
        }

        if (neighbor.inputTank.getFill() >= neighbor.inputTank.getMaxFill() || this.outputTank.getFill() <= 0) {
            return true;
        }

        int fill = Math.min(neighbor.inputTank.getMaxFill() - neighbor.inputTank.getFill(), this.outputTank.getFill());
        this.outputTank.setFill(this.outputTank.getFill() - fill);
        neighbor.inputTank.setFill(neighbor.inputTank.getFill() + fill);
        neighbor.setChanged();
        return true;
    }

    @Nullable
    private MachineGasCentrifugeBlockEntity findNeighborCentrifuge(Level level, BlockPos controllerPos, Direction facing) {
        for (int offset = 1; offset <= CASCADE_SEARCH_RANGE; offset++) {
            BlockEntity be = level.getBlockEntity(controllerPos.relative(facing.getOpposite(), offset));
            if (be instanceof MachineGasCentrifugeBlockEntity other && other != this) {
                return other;
            }
        }
        return null;
    }

    /** Legacy special case: fully-enriched LEUF6 that has nowhere further to cascade to is
     *  converted directly into usable fuel nuggets once enough has accumulated. */
    private void convertFinalUraniumOutput() {
        if (outputTank.getFill() < 600) return;

        ItemStack nugget = new ItemStack(ModItems.NUGGET_URANIUM_FUEL.get(), 6);
        ItemStack fluorite = new ItemStack(ModItems.FLUORITE.get(), 1);
        if (!canAcceptOutput(nugget) || !canAcceptOutput(fluorite)) return;

        outputTank.setFill(outputTank.getFill() - 600);
        placeOutputAnySlot(nugget);
        placeOutputAnySlot(fluorite);
    }

    public void drops() {
        if (level == null) return;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.gas_centrifuge");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= SLOT_OUTPUT_0 && slot <= SLOT_OUTPUT_3) {
            return false;
        }
        if (slot == SLOT_BATTERY) {
            return isEnergyProviderItem(stack);
        }
        if (slot == SLOT_FLUID_ID) {
            return stack.getItem() instanceof FluidIdentifierItem;
        }
        if (slot == SLOT_UPGRADE) {
            return stack.getItem() instanceof ItemMachineUpgrade upgrade && upgrade.getUpgradeType() == UpgradeType.SPEED;
        }
        return false;
    }

    @Override
    protected void setupFluidCapability() {
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineGasCentrifugeMenu(containerId, playerInventory, this, data);
    }

    public ContainerData getContainerData() {
        return data;
    }

    public FluidTank getTank() { return tank; }
    public PseudoFluidTank getInputTank() { return inputTank; }
    public PseudoFluidTank getOutputTank() { return outputTank; }

    public boolean isProgressing() { return isProgressing; }
    public int getProgress() { return progress; }

    public float getAnim(float partialTicks) {
        return prevAnim + (anim - prevAnim) * partialTicks;
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tank };
    }

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { tank };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && PseudoFluidType.FLUID_CONVERSIONS.containsKey(fluid);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progress", progress);
        tag.putBoolean("isProgressing", isProgressing);
        tag.putFloat("anim", anim);
        tag.putFloat("prevAnim", prevAnim);
        tank.writeToNBT(tag, "tank");
        inputTank.writeToNBT(tag, "inputTank");
        outputTank.writeToNBT(tag, "outputTank");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("progress");
        isProgressing = tag.getBoolean("isProgressing");
        anim = tag.getFloat("anim");
        prevAnim = tag.getFloat("prevAnim");
        tank.readFromNBT(tag, "tank");
        inputTank.readFromNBT(tag, "inputTank");
        outputTank.readFromNBT(tag, "outputTank");
    }
}
