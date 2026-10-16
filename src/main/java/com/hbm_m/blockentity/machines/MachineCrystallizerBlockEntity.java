package com.hbm_m.blockentity.machines;

import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.platform.FluidHooks;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.sound.ClientSoundBootstrap;
import com.hbm_m.block.machines.MachineCrystallizerBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineCrystallizerMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.CrystallizerRecipe;

import dev.architectury.fluid.FluidStack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

//? if fabric {
/*import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
*///?}


public class MachineCrystallizerBlockEntity extends BaseMachineBlockEntity
        implements com.hbm_m.api.fluids.IFluidStandardReceiverMK2 {

    private static final String CRYSTALLIZER_SOUND_INSTANCE = "com.hbm_m.sound.CrystallizerSoundInstance";

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_BATTERY = 1;
    private static final int SLOT_OUTPUT = 2;
    private static final int SLOT_FLUID_INPUT = 3;
    private static final int SLOT_FLUID_OUTPUT = 4;
    private static final int SLOT_UPGRADE_1 = 5;
    private static final int SLOT_UPGRADE_2 = 6;
    private static final int SLOT_FLUID_ID = 7;

    private static final int SLOT_COUNT = 8;
    private static final long MAX_POWER = 1_000_000;
    private static final long MAX_RECEIVE = 1_000;
    private static final int TANK_CAPACITY = 8_000;
    private static final int DEFAULT_DURATION = 600;
    private static final int BASE_POWER_PER_TICK = 1_000;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        public void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    private int progress = 0;
    private int duration = DEFAULT_DURATION;
    private boolean isOn = false;

    // Client-side visual state for the rotating center part.
    public float angle = 0.0F;
    public float prevAngle = 0.0F;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> getDuration();
                case 2 -> (int) (getEnergyStored() & 0xFFFFFFFFL);          // energy low 32 bits
                case 3 -> (int) ((getEnergyStored() >>> 32) & 0xFFFFFFFFL); // energy high 32 bits
                case 4 -> (int) (getMaxEnergyStored() & 0xFFFFFFFFL);          // maxEnergy low 32 bits
                case 5 -> (int) ((getMaxEnergyStored() >>> 32) & 0xFFFFFFFFL); // maxEnergy high 32 bits
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 6;
        }
    };

    public MachineCrystallizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTALLIZER.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_RECEIVE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCrystallizerBlockEntity entity) {
        if (level.isClientSide) {
            entity.clientTick(level, pos);
            return;
        }

        entity.ensureNetworkInitialized();
        entity.chargeFromBattery();
        entity.applyFluidIdentifier();
        entity.transferFluidsFromItems();

        ItemStack inputStack = entity.inventory.getStackInSlot(SLOT_INPUT);
        FluidStack tankFluid = entity.getTankFluidStack();
        // Рецепты теперь data-driven (JSON) — поиск через RecipeManager (кросс-версионный RecipeHooks).
        CrystallizerRecipe recipe = RecipeHooks.getAllRecipes(level, CrystallizerRecipe.Type.INSTANCE).stream()
                .filter(r -> r.matchesInput(inputStack) && r.matchesAcid(tankFluid))
                .findFirst()
                .orElse(null);

        boolean wasOn = entity.isOn;
        entity.isOn = false;

        if (recipe != null) {
            entity.duration = entity.calcDuration(recipe);

            if (entity.canProcess(recipe)) {
                int powerCost = entity.getPowerRequired();
                entity.setEnergyStored(entity.getEnergyStored() - powerCost);
                entity.progress++;
                entity.isOn = true;

                if (entity.progress >= entity.duration) {
                    entity.progress = 0;
                    entity.processItem(recipe);
                }
                entity.setChanged();
                entity.sendUpdateToClient();
            } else {
                if (entity.progress != 0) {
                    entity.progress = 0;
                    entity.setChanged();
                }
            }
        } else {
            if (entity.progress != 0) {
                entity.progress = 0;
                entity.setChanged();
            }
        }

        if (wasOn != entity.isOn) {
            entity.sendUpdateToClient();
        }
    }

    /**
     * Client-side visuals: rotate the center part while the machine works and spawn
     * small old-style white steam particles from the roof.
     */
    private void clientTick(Level level, BlockPos pos) {
        ClientSoundBootstrap.updateSound(this, isOn, () -> newCrystallizerSoundInstance());

        prevAngle = angle;

        if (isOn) {
            angle += 5.0F;
            if (angle >= 360.0F) {
                angle -= 360.0F;
                prevAngle -= 360.0F;
            }

            // The original 1.7.10 machine used small white smoke/steam puffs, not the
            // large campfire smoke that exists in newer Minecraft versions. CLOUD is
            // visually closer: small white squares that drift upward and fade out.
            if (level.random.nextInt(4) == 0) {
                double x = pos.getX() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.85D;
                double y = pos.getY() + 6.5D;
                double z = pos.getZ() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.85D;

                double vx = (level.random.nextDouble() - 0.5D) * 0.010D;
                double vy = 0.025D + level.random.nextDouble() * 0.015D;
                double vz = (level.random.nextDouble() - 0.5D) * 0.010D;

                level.addParticle(ParticleTypes.CLOUD, x, y, z, vx, vy, vz);
            }
        }
    }

    private Object newCrystallizerSoundInstance() {
        try {
            return Class.forName(CRYSTALLIZER_SOUND_INSTANCE)
                    .getConstructor(BlockPos.class)
                    .newInstance(this.getBlockPos());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean canProcess(CrystallizerRecipe recipe) {
        ItemStack inputStack = inventory.getStackInSlot(SLOT_INPUT);

        if (inputStack.getCount() < recipe.getInputCount()) return false;

        if (getEnergyStored() < getPowerRequired()) return false;

        if (recipe.getAcid() != null && tank.getFluidAmountMb() < recipe.getAcidAmount()) {
            return false;
        }

        ItemStack outSlot = inventory.getStackInSlot(SLOT_OUTPUT);
        ItemStack out = recipe.getOutput();
        if (!outSlot.isEmpty()) {
            if (!PlatformHooks.isSameItemSameTags(outSlot, out)) return false;
            if (outSlot.getCount() + out.getCount() > outSlot.getMaxStackSize()) return false;
        }

        return true;
    }

    private void processItem(CrystallizerRecipe recipe) {
        ItemStack out = recipe.getOutput().copy();
        ItemStack outSlot = inventory.getStackInSlot(SLOT_OUTPUT);
        if (outSlot.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, out);
        } else {
            outSlot.grow(out.getCount());
        }

        if (recipe.getAcid() != null && recipe.getAcidAmount() > 0) {
            tank.drainMb(recipe.getAcidAmount());
        }

        float freeChance = recipe.getProductivity();
        if (freeChance <= 0f || level.random.nextFloat() >= freeChance) {
            inventory.getStackInSlot(SLOT_INPUT).shrink(recipe.getInputCount());
        }

        setChanged();
    }

    private void applyFluidIdentifier() {
        ItemStack idStack = inventory.getStackInSlot(SLOT_FLUID_ID);
        if (idStack.isEmpty()) return;

        Fluid resolved = resolveIdentifierFluid(idStack);
        if (resolved == null) return;

        Fluid currentType = tank.getTankType();

        if (VanillaFluidEquivalence.sameSubstance(resolved, currentType)) {
            return;
        }

        tank.assignTypeAndZeroFluid(resolved);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    private Fluid resolveIdentifierFluid(ItemStack stack) {
        if (stack.getItem() instanceof FluidIdentifierItem) {
            return FluidIdentifierItem.resolvePrimaryForTank(stack);
        }
        if (stack.getItem() instanceof IItemFluidIdentifier idItem) {
            Fluid f = idItem.getType(level, worldPosition, stack);
            if (f == null || f == Fluids.EMPTY) return null;
            return f;
        }
        return null;
    }

    private void transferFluidsFromItems() {
        ItemStack fillStack = inventory.getStackInSlot(SLOT_FLUID_INPUT);
        if (fillStack.isEmpty()) return;

        // Кросс-платформенная реализация через FluidHooks + tank.fillMb.
        // Заменяет прежние раздельные ветки Forge/Fabric/NeoForge.
        Fluid currentType = tank.getTankType();
        if (currentType == Fluids.EMPTY || currentType == ModFluids.NONE.getSource()) {
            return;
        }

        // Сначала пробуем извлечь жидкость из контейнера (simulate), чтобы узнать тип/объём.
        int tankSpace = tank.getSpaceMb();
        if (tankSpace <= 0) return;

        FluidHooks.FluidExtraction sim = FluidHooks.extractFluidFromItem(fillStack, tankSpace, true);
        if (sim.amount() <= 0) {
            // Контейнер пуст — отправляем его в выходной слот (воспроизводим forge-поведение).
            FluidHooks.FluidExtraction probe = FluidHooks.extractFluidFromItem(fillStack, 1, true);
            if (probe.amount() == 0) {
                ItemStack singleEmpty = fillStack.copy();
                singleEmpty.setCount(1);
                tryMoveContainerToOutput(singleEmpty, fillStack);
            }
            return;
        }

        // Согласовываем тип жидкости с баком — заливаем только совместимую.
        int toFill = sim.amount();
        int accepted = tank.fillMb(sim.fluid(), toFill, true);
        if (accepted <= 0) return;

        // Реально извлекаем (execute) и заливаем в бак.
        FluidHooks.FluidExtraction real = FluidHooks.extractFluidFromItem(fillStack, accepted, false);
        if (real.amount() <= 0) return;
        tank.fillMb(real.fluid(), real.amount(), false);

        ItemStack updatedContainer = real.remainder();
        if (updatedContainer.isEmpty() || FluidHooks.extractFluidFromItem(updatedContainer, 1, true).amount() == 0) {
            // Контейнер опустел после перелива — отправляем в выходной слот.
            tryMoveContainerToOutput(updatedContainer, fillStack);
        } else {
            inventory.setStackInSlot(SLOT_FLUID_INPUT, updatedContainer);
        }
        setChanged();
    }

    private void tryMoveContainerToOutput(ItemStack emptyContainer, ItemStack originalFillStack) {
        if (emptyContainer.isEmpty()) {
            ItemStack remaining = originalFillStack.copy();
            remaining.shrink(1);
            inventory.setStackInSlot(SLOT_FLUID_INPUT, remaining);
            return;
        }

        ItemStack outSlot = inventory.getStackInSlot(SLOT_FLUID_OUTPUT);

        if (outSlot.isEmpty()) {
            ItemStack remaining = originalFillStack.copy();
            remaining.shrink(1);
            inventory.setStackInSlot(SLOT_FLUID_INPUT, remaining);
            inventory.setStackInSlot(SLOT_FLUID_OUTPUT, emptyContainer);
            return;
        }

        if (PlatformHooks.isSameItemSameTags(outSlot, emptyContainer)) {
            int max = outSlot.getMaxStackSize();
            int totalAfter = outSlot.getCount() + emptyContainer.getCount();
            if (totalAfter <= max) {
                ItemStack newOut = outSlot.copy();
                newOut.setCount(totalAfter);
                inventory.setStackInSlot(SLOT_FLUID_OUTPUT, newOut);

                ItemStack remaining = originalFillStack.copy();
                remaining.shrink(1);
                inventory.setStackInSlot(SLOT_FLUID_INPUT, remaining);
            }
            return;
        }
    }

    private void chargeFromBattery() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
            return;
        }
        chargeFromBatterySlot(SLOT_BATTERY);
    }

    private int calcDuration(CrystallizerRecipe recipe) {
        return recipe.getDuration();
    }

    private FluidStack getTankFluidStack() {
        var fluid = tank.getStoredFluid();
        int amount = tank.getFluidAmountMb();
        if (fluid == null || fluid == Fluids.EMPTY || amount <= 0) {
            return FluidStack.empty();
        }
        return FluidStack.create(fluid, amount);
    }

    public int getPowerRequired() {
        return BASE_POWER_PER_TICK;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isOn() {
        return isOn;
    }

    public long getPowerScaled(int scale) {
        long max = getMaxEnergyStored();
        return max <= 0 ? 0 : (getEnergyStored() * scale) / max;
    }

    public int getProgressScaled(int scale) {
        int dur = getDuration();
        return dur <= 0 ? 0 : (progress * scale) / dur;
    }

    public FluidTank getTank() {
        return tank;
    }

    public ContainerData getContainerData() {
        return data;
    }

    private final FluidTank[] receivingTanksArr = new FluidTank[] { tank };

    @Override
    public FluidTank[] getReceivingTanks() {
        return receivingTanksArr;
    }

    @Override
    public FluidTank[] getAllTanks() {
        return receivingTanksArr;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }
   
    /**
     * The controller BlockEntity is only one block, but the animated spinner/fluid BER is
     * rendered across the whole 3x3x6 multiblock. Without the expanded render bounds,
     * Minecraft frustum-culls the BER when the controller block itself leaves the camera
     * frustum, which makes the spinner and fluid disappear at steep viewing angles.
     */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof MachineCrystallizerBlock block
                && state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return block.getStructureHelper().getRenderBoundingBox(
                    worldPosition,
                    state.getValue(HorizontalDirectionalBlock.FACING),
                    1.25D
            );
        }
        return super.getRenderBoundingBox().inflate(3.0D, 6.0D, 3.0D);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.crystallizer");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) {
            for (CrystallizerRecipe r : RecipeHooks.getAllRecipes(level, CrystallizerRecipe.Type.INSTANCE)) {
                if (r.matchesInput(stack) && r.matchesAcid(getTankFluidStack())) {
                    return true;
                }
            }
            return false;
        }
        if (slot == SLOT_BATTERY) {
            if (stack.getItem() instanceof ItemCreativeBattery) return true;
            return isEnergyProviderItem(stack);
        }
        if (slot == SLOT_OUTPUT || slot == SLOT_FLUID_OUTPUT) {
            return false;
        }
        if (slot == SLOT_FLUID_INPUT) {
            return PlatformHooks.isFluidContainer(stack);
        }
        if (slot == SLOT_FLUID_ID) {
            return stack.getItem() instanceof IItemFluidIdentifier;
        }
        return true;
    }

    public boolean stillValid(Player player) {
        return !this.isRemoved() && player.distanceToSqr(this.getBlockPos().getCenter()) <= 64.0D;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineCrystallizerMenu(containerId, playerInventory, this, data);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.put("tank", tank.writeNBT(new CompoundTag()));
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        tag.putBoolean("isOn", isOn);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("tank")) {
            tank.readNBT(tag.getCompound("tank"));
        }
        progress = tag.getInt("progress");
        duration = tag.contains("duration") ? tag.getInt("duration") : DEFAULT_DURATION;
        isOn = tag.getBoolean("isOn");
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            ClientSoundBootstrap.updateSound(this, false, null);
        }
    }

    //? if forge {
    @Override
    protected void setupFluidCapability() {
        setFluidHandler(tank);
    }
    //?}
}
