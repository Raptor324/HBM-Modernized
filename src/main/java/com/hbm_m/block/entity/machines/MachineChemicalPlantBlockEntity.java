package com.hbm_m.block.entity.machines;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.inventory.menu.MachineChemicalPlantMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.industrial.ItemBlueprintFolder;
import com.hbm_m.recipe.ChemicalPlantRecipes;
import com.hbm_m.recipe.ChemicalPlantRecipes.ChemicalRecipe;
import com.hbm_m.recipe.ChemicalPlantRecipes.RecipeInput;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Chemical Plant BlockEntity — порт с 1.7.10.
 * 22 слота, 6 FluidTank (3 input, 3 output), энергия.
 * Логика крафтов — заглушка.
 */
public class MachineChemicalPlantBlockEntity extends BaseMachineBlockEntity {

    private static final int SLOT_COUNT = 22;
    private static final int SLOT_BATTERY = 0;
    private static final int SLOT_SCHEMATIC = 1;
    private static final int SLOT_UPGRADE_START = 2;
    private static final int SLOT_UPGRADE_END = 3;
    private static final int SLOT_SOLID_INPUT_START = 4;
    private static final int SLOT_SOLID_INPUT_END = 6;
    private static final int SLOT_SOLID_OUTPUT_START = 7;
    private static final int SLOT_SOLID_OUTPUT_END = 9;
    private static final int SLOT_FLUID_INPUT_START = 10;
    private static final int SLOT_FLUID_INPUT_END = 12;
    private static final int SLOT_FLUID_INPUT_EMPTY_START = 13;
    private static final int SLOT_FLUID_INPUT_EMPTY_END = 15;
    private static final int SLOT_FLUID_OUTPUT_START = 16;
    private static final int SLOT_FLUID_OUTPUT_END = 18;
    private static final int SLOT_FLUID_OUTPUT_EMPTY_START = 19;
    private static final int SLOT_FLUID_OUTPUT_EMPTY_END = 21;

    private static final int TANK_CAPACITY = 24_000;
    private static final long MAX_POWER = 100_000;

    private final FluidTank[] inputTanks = new FluidTank[3];
    private final FluidTank[] outputTanks = new FluidTank[3];
    private final LazyOptional<IFluidHandler>[] inputTankHandlers = new LazyOptional[3];
    private final LazyOptional<IFluidHandler>[] outputTankHandlers = new LazyOptional[3];

    private boolean didProcess = false;
    @Nullable private String recipe = null;

    private int progress = 0;
    private int maxProgress = 100;

    private float anim = 0.0F;
    private float prevAnim = 0.0F;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
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

    public MachineChemicalPlantBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEMICAL_PLANT_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            inputTanks[i] = new FluidTank(TANK_CAPACITY) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                    if (level != null && !level.isClientSide) {
                        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    }
                }
            };
            outputTanks[i] = new FluidTank(TANK_CAPACITY) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                    if (level != null && !level.isClientSide) {
                        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    }
                }
            };
            inputTankHandlers[i] = LazyOptional.of(() -> inputTanks[idx]);
            outputTankHandlers[i] = LazyOptional.of(() -> outputTanks[idx]);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineChemicalPlantBlockEntity entity) {
        entity.prevAnim = entity.anim;

        if (level.isClientSide) {
            if (entity.didProcess) {
                entity.anim += 0.05F;
            }
            if (entity.anim > (float) (Math.PI * 2.0)) {
                entity.anim -= (float) (Math.PI * 2.0);
            }
            entity.clientTick();
            return;
        }

        entity.ensureNetworkInitialized();
        entity.chargeFromBattery();
        entity.transferFluidsFromItems();
        entity.transferFluidsToItems();

        if (level.getGameTime() % 10L == 0L) {
            entity.updateEnergyDelta(entity.getEnergyStored());
        }

        boolean dirty = false;

        ChemicalRecipe currentRecipe = entity.recipe != null ? ChemicalPlantRecipes.getRecipe(entity.recipe) : null;

        if (currentRecipe != null) {
            entity.maxProgress = currentRecipe.getDuration();
            long powerPerTick = currentRecipe.getPowerConsumption();

            if (entity.canProcess(currentRecipe) && entity.getEnergyStored() >= powerPerTick) {
                entity.setEnergyStored(entity.getEnergyStored() - powerPerTick);
                entity.progress++;
                entity.didProcess = true;
                dirty = true;

                if (entity.progress >= entity.maxProgress) {
                    entity.progress = 0;
                    entity.finishRecipe(currentRecipe);
                }
            } else {
                if (entity.progress != 0 || entity.didProcess) {
                    entity.progress = 0;
                    entity.didProcess = false;
                    dirty = true;
                }
            }
        } else {
            if (entity.progress != 0 || entity.didProcess) {
                entity.progress = 0;
                entity.didProcess = false;
                dirty = true;
            }
        }

        if (dirty) {
            entity.setChanged();
            entity.sendUpdateToClient();
        }
    }

    private boolean canProcess(ChemicalRecipe recipe) {
        // Check item inputs (positional: recipe input i → slot SLOT_SOLID_INPUT_START + i)
        List<RecipeInput> itemInputs = recipe.getItemInputs();
        for (int i = 0; i < itemInputs.size(); i++) {
            int slot = SLOT_SOLID_INPUT_START + i;
            ItemStack slotStack = inventory.getStackInSlot(slot);
            if (!itemInputs.get(i).matches(slotStack)) return false;
        }

        // Check fluid inputs (positional: recipe fluid i → inputTanks[i])
        List<FluidStack> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < fluidInputs.size(); i++) {
            FluidStack required = fluidInputs.get(i);
            FluidTank tank = inputTanks[i];
            if (tank.getFluid().isEmpty()
                || tank.getFluid().getFluid() != required.getFluid()
                || tank.getFluidAmount() < required.getAmount()) {
                return false;
            }
        }

        // Check item output space (positional: recipe output i → slot SLOT_SOLID_OUTPUT_START + i)
        List<ItemStack> itemOutputs = recipe.getItemOutputs();
        for (int i = 0; i < itemOutputs.size(); i++) {
            ItemStack output = itemOutputs.get(i);
            if (output.isEmpty()) continue;
            int slot = SLOT_SOLID_OUTPUT_START + i;
            ItemStack slotStack = inventory.getStackInSlot(slot);
            if (!slotStack.isEmpty()) {
                if (!ItemStack.isSameItemSameTags(slotStack, output)) return false;
                if (slotStack.getCount() + output.getCount() > slotStack.getMaxStackSize()) return false;
            }
        }

        // Check fluid output space (positional: recipe fluid output i → outputTanks[i])
        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        for (int i = 0; i < fluidOutputs.size(); i++) {
            FluidStack outputFluid = fluidOutputs.get(i);
            FluidTank tank = outputTanks[i];
            if (!tank.getFluid().isEmpty() && tank.getFluid().getFluid() != outputFluid.getFluid()) return false;
            if (tank.getFluidAmount() + outputFluid.getAmount() > tank.getCapacity()) return false;
        }

        return true;
    }

    private void finishRecipe(ChemicalRecipe recipe) {
        // Consume item inputs
        List<RecipeInput> itemInputs = recipe.getItemInputs();
        for (int i = 0; i < itemInputs.size(); i++) {
            int slot = SLOT_SOLID_INPUT_START + i;
            inventory.getStackInSlot(slot).shrink(itemInputs.get(i).getCount());
        }

        // Consume fluid inputs
        List<FluidStack> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < fluidInputs.size(); i++) {
            inputTanks[i].drain(fluidInputs.get(i).getAmount(), IFluidHandler.FluidAction.EXECUTE);
        }

        // Produce item outputs
        List<ItemStack> itemOutputs = recipe.getItemOutputs();
        for (int i = 0; i < itemOutputs.size(); i++) {
            ItemStack output = itemOutputs.get(i);
            if (output.isEmpty()) continue;
            int slot = SLOT_SOLID_OUTPUT_START + i;
            ItemStack slotStack = inventory.getStackInSlot(slot);
            if (slotStack.isEmpty()) {
                inventory.setStackInSlot(slot, output.copy());
            } else {
                slotStack.grow(output.getCount());
            }
        }

        // Produce fluid outputs
        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        for (int i = 0; i < fluidOutputs.size(); i++) {
            outputTanks[i].fill(fluidOutputs.get(i).copy(), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    private void clientTick() {
        com.hbm_m.sound.ClientSoundManager.updateSound(this, this.didProcess,
                () -> new com.hbm_m.sound.ChemicalPlantSoundInstance(this.getBlockPos()));
    }

    private void chargeFromBattery() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (stack.isEmpty()) return;

        if (stack.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
            return;
        }

        stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).ifPresent(provider -> {
            long needed = getMaxEnergyStored() - getEnergyStored();
            if (needed <= 0) return;
            long extracted = provider.extractEnergy(Math.min(needed, getReceiveSpeed()), false);
            if (extracted > 0) {
                setEnergyStored(getEnergyStored() + extracted);
                setChanged();
            }
        });

        if (!stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).isPresent()) {
            stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(provider -> {
                long needed = getMaxEnergyStored() - getEnergyStored();
                if (needed <= 0) return;
                int extracted = provider.extractEnergy((int) Math.min(needed, getReceiveSpeed()), false);
                if (extracted > 0) {
                    setEnergyStored(getEnergyStored() + extracted);
                    setChanged();
                }
            });
        }
    }

    private void transferFluidsFromItems() {
        for (int i = 0; i < 3; i++) {
            int fullContainerSlot = SLOT_FLUID_INPUT_START + i;
            int emptyContainerSlot = SLOT_FLUID_INPUT_EMPTY_START + i;
            ItemStack fullContainer = inventory.getStackInSlot(fullContainerSlot);
            if (fullContainer.isEmpty()) continue;
            if (!inventory.getStackInSlot(emptyContainerSlot).isEmpty()) continue;

            var result = FluidUtil.tryEmptyContainer(fullContainer, inputTanks[i], TANK_CAPACITY, null, true);
            if (result.isSuccess()) {
                fullContainer.shrink(1);
                inventory.setStackInSlot(emptyContainerSlot, result.getResult());
                setChanged();
            }
        }
    }

    private void transferFluidsToItems() {
        for (int i = 0; i < 3; i++) {
            int emptyContainerSlot = SLOT_FLUID_OUTPUT_START + i;
            int filledContainerSlot = SLOT_FLUID_OUTPUT_EMPTY_START + i;
            ItemStack emptyContainer = inventory.getStackInSlot(emptyContainerSlot);
            if (emptyContainer.isEmpty()) continue;
            if (!inventory.getStackInSlot(filledContainerSlot).isEmpty()) continue;

            var result = FluidUtil.tryFillContainer(emptyContainer, outputTanks[i], TANK_CAPACITY, null, true);
            if (result.isSuccess()) {
                emptyContainer.shrink(1);
                inventory.setStackInSlot(filledContainerSlot, result.getResult());
                setChanged();
            }
        }
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
        return Component.translatable("container.hbm_m.chemical_plant");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) {
            return stack.getCapability(ForgeCapabilities.ENERGY).isPresent()
                || stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).isPresent();
        }
        if (slot == SLOT_SCHEMATIC) {
            return stack.getItem() instanceof ItemBlueprintFolder;
        }
        if (slot >= SLOT_UPGRADE_START && slot <= SLOT_UPGRADE_END) {
            return true;
        }
        if (slot >= SLOT_SOLID_OUTPUT_START && slot <= SLOT_SOLID_OUTPUT_END) {
            return false;
        }
        if (slot >= SLOT_FLUID_INPUT_START && slot <= SLOT_FLUID_INPUT_END) {
            return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
        }
        if (slot >= SLOT_FLUID_OUTPUT_START && slot <= SLOT_FLUID_OUTPUT_END) {
            return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
        }
        if (slot >= SLOT_FLUID_INPUT_EMPTY_START && slot <= SLOT_FLUID_INPUT_EMPTY_END) {
            return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
        }
        if (slot >= SLOT_FLUID_OUTPUT_EMPTY_START && slot <= SLOT_FLUID_OUTPUT_EMPTY_END) {
            return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
        }
        return true;
    }

    @Override
    protected void setupFluidCapability() {
        // Экспонируем первый input tank по умолчанию
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineChemicalPlantMenu(containerId, playerInventory, this, data);
    }

    public FluidTank[] getInputTanks() {
        return inputTanks;
    }

    public FluidTank[] getOutputTanks() {
        return outputTanks;
    }

    public boolean getDidProcess() {
        return didProcess;
    }

    @Nullable
    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(@Nullable String recipe) {
        this.recipe = recipe;
        setChanged();
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public float getAnim(float partialTicks) {
        return prevAnim + (anim - prevAnim) * partialTicks;
    }

    public FluidStack getFluid() {
        return inputTanks[0].getFluid();
    }

    public float getFluidFillFraction() {
        if (inputTanks[0].getCapacity() <= 0) return 0.0F;
        return Math.min(1.0F, Math.max(0.0F, inputTanks[0].getFluidAmount() / (float) inputTanks[0].getCapacity()));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < 3; i++) {
            tag.put("inputTank" + i, inputTanks[i].writeToNBT(new CompoundTag()));
            tag.put("outputTank" + i, outputTanks[i].writeToNBT(new CompoundTag()));
        }
        tag.putBoolean("didProcess", didProcess);
        if (recipe != null) {
            tag.putString("recipe", recipe);
        }
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        tag.putFloat("anim", anim);
        tag.putFloat("prevAnim", prevAnim);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < 3; i++) {
            if (tag.contains("inputTank" + i)) {
                inputTanks[i].readFromNBT(tag.getCompound("inputTank" + i));
            }
            if (tag.contains("outputTank" + i)) {
                outputTanks[i].readFromNBT(tag.getCompound("outputTank" + i));
            }
        }
        didProcess = tag.getBoolean("didProcess");
        recipe = tag.contains("recipe") ? tag.getString("recipe") : null;
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
        if (maxProgress <= 0) maxProgress = 100;
        anim = tag.getFloat("anim");
        prevAnim = tag.getFloat("prevAnim");
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return inputTankHandlers[0].cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (int i = 0; i < 3; i++) {
            inputTankHandlers[i].invalidate();
            outputTankHandlers[i].invalidate();
        }
    }
}
