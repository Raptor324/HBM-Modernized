package com.hbm_m.blockentity.machines;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.BlastFurnaceBlock;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineBlastFurnaceMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.recipe.BlastFurnaceRecipe;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

/**
 * Доменная печь (обновлённая версия из оригинала).
 * Мультиблок 3x7x3 с семью портами, буфер топлива (38400, расход 800 на операцию),
 * бак воздушного дутья (ускорение до 5x) и бак дымовых газов.
 */
public class MachineBlastFurnaceBlockEntity extends BaseHbmBlockEntity implements MenuProvider, IFluidStandardTransceiverMK2 {

    public static final int SLOT_COUNT = 5;
    public static final int FUEL_SLOT = 0;
    public static final int INPUT_SLOT_FIRST = 1;
    public static final int INPUT_SLOT_SECOND = 2;
    public static final int OUTPUT_SLOT_FIRST = 3;
    public static final int OUTPUT_SLOT_SECOND = 4;

    public static final int FUEL_RATE = 800;
    public static final int MAX_FUEL = 38_400;
    public static final int AIR_CAPACITY_MB = 4_000;
    public static final int FLUE_CAPACITY_MB = 1_000;
    public static final int FLUE_PER_OPERATION = 100;

    private final ModItemStackHandler itemHandler = new ModItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case FUEL_SLOT -> isFuel(stack);
                case OUTPUT_SLOT_FIRST, OUTPUT_SLOT_SECOND -> false;
                case INPUT_SLOT_FIRST -> {
                    ItemStack other = itemHandler.getStackInSlot(INPUT_SLOT_SECOND);
                    yield other.isEmpty() || !PlatformHooks.isSameItemSameTags(other, stack);
                }
                case INPUT_SLOT_SECOND -> {
                    ItemStack other = itemHandler.getStackInSlot(INPUT_SLOT_FIRST);
                    yield other.isEmpty() || !PlatformHooks.isSameItemSameTags(other, stack);
                }
                default -> false;
            };
        }
    };

    private final FluidTank airTank = new FluidTank(ModFluids.AIRBLAST.getSource(), AIR_CAPACITY_MB);
    private final FluidTank flueTank = new FluidTank(ModFluids.FLUE.getSource(), FLUE_CAPACITY_MB);

    private double progress;
    private double speed;
    private int fuel;
    private boolean progressing;

    // Кэш последнего синка — шлём обновление клиенту только при изменении.
    private int lastProgress = Integer.MIN_VALUE;
    private int lastSpeed = Integer.MIN_VALUE;
    private int lastFuel = Integer.MIN_VALUE;
    private int lastAir = Integer.MIN_VALUE;
    private int lastFlue = Integer.MIN_VALUE;
    private boolean lastProgressing;

    private static final int DATA_COUNT = 6;
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_SPEED = 1;
    private static final int DATA_FUEL = 2;
    private static final int DATA_AIR = 3;
    private static final int DATA_FLUE = 4;
    private static final int DATA_PROGRESSING = 5;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> (int) Math.round(progress * 1_000_000D);
                case DATA_SPEED -> (int) Math.round(speed * 1_000D);
                case DATA_FUEL -> fuel;
                case DATA_AIR -> airTank.getFluidAmountMb();
                case DATA_FLUE -> flueTank.getFluidAmountMb();
                case DATA_PROGRESSING -> progressing ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> progress = value / 1_000_000D;
                case DATA_SPEED -> speed = value / 1_000D;
                case DATA_FUEL -> fuel = value;
                case DATA_AIR -> airTank.setFluid(ModFluids.AIRBLAST.getSource(), Math.max(0, value));
                case DATA_FLUE -> flueTank.setFluid(ModFluids.FLUE.getSource(), Math.max(0, value));
                case DATA_PROGRESSING -> progressing = value != 0;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MachineBlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_BLAST_FURNACE_BE.get(), pos, state);
    }

    public ModItemStackHandler getInventory() {
        return itemHandler;
    }

    public FluidTank getAirTank() {
        return airTank;
    }

    public FluidTank getFlueTank() {
        return flueTank;
    }

    public double getProgress() {
        return progress;
    }

    public double getSpeed() {
        return speed;
    }

    public int getFuel() {
        return fuel;
    }

    public boolean isProgressing() {
        return progressing;
    }

    public ContainerData getData() {
        return data;
    }

    //? if forge {
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

    @Override
    public void onLoad() {
        super.onLoad();
        lazyFluidHandler = LazyOptional.of(() -> new CombinedFluidHandler());
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return lazyFluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyFluidHandler.invalidate();
    }

    /** Заполнение - только дутьё, слив - только дымовые газы (как в оригинале). */
    private class CombinedFluidHandler implements IFluidHandler {
        @Override public int getTanks() { return 2; }
        @Override public FluidStack getFluidInTank(int tank) {
            if (tank == 0 && !airTank.isEmpty()) return new FluidStack(airTank.getStoredFluid(), airTank.getFluidAmountMb());
            if (tank == 1 && !flueTank.isEmpty()) return new FluidStack(flueTank.getStoredFluid(), flueTank.getFluidAmountMb());
            return FluidStack.EMPTY;
        }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? AIR_CAPACITY_MB : tank == 1 ? FLUE_CAPACITY_MB : 0; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && !stack.isEmpty() && stack.getFluid() == ModFluids.AIRBLAST.getSource();
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != ModFluids.AIRBLAST.getSource()) return 0;
            return airTank.fillMb(resource.getFluid(), resource.getAmount(), action.simulate());
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != flueTank.getStoredFluid()) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            int drained = flueTank.drainMb(maxDrain, action.simulate());
            if (drained <= 0) return FluidStack.EMPTY;
            return new FluidStack(ModFluids.FLUE.getSource(), drained);
        }
    }
    //?}

    @Override
    public @Nullable Object getFluidHandler(@Nullable Direction side) {
        //? if forge {
        return lazyFluidHandler.resolve().orElse(null);
        //?} else {
        /*return new com.hbm_m.api.fluids.NeoForgeFluidHandlerMK2(this);
         *///?}
    }

    // ==================== IFluidUserMK2 ====================

    @Override
    public com.hbm_m.inventory.fluid.tank.FluidTank[] getAllTanks() {
        return new com.hbm_m.inventory.fluid.tank.FluidTank[] { airTank, flueTank };
    }

    @Override
    public com.hbm_m.inventory.fluid.tank.FluidTank[] getReceivingTanks() {
        return new com.hbm_m.inventory.fluid.tank.FluidTank[] { airTank };
    }

    @Override
    public com.hbm_m.inventory.fluid.tank.FluidTank[] getSendingTanks() {
        return new com.hbm_m.inventory.fluid.tank.FluidTank[] { flueTank };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        if (fromDir == null) return false;
        return fluid == ModFluids.AIRBLAST.getSource() || fluid == ModFluids.FLUE.getSource();
    }

    // ==================== TICK ====================

    public static void tick(Level level, BlockPos pos, BlockState state, MachineBlastFurnaceBlockEntity entity) {
        if (level.isClientSide()) {
            entity.clientTick(level, pos);
        } else {
            entity.serverTick((ServerLevel) level, pos, state);
        }
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        loadFuel();

        boolean wasProgressing = progressing;
        speed = 0D;
        BlastFurnaceRecipe recipe = findRecipe();
        if (recipe != null && canProcess(recipe)) {
            speed = Mth.clamp(0.5D + airTank.getFluidAmountMb() * 8D / AIR_CAPACITY_MB, 0.5D, 5D);
            progressing = true;
            progress += speed / recipe.getDuration();
            if (progress >= 1D) {
                process(recipe);
                progress = 0D;
            }
            if (level.random.nextInt(10) == 0) {
                PlatformHooks.playSound(level, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                        1.0F, 0.5F + level.random.nextFloat() * 0.25F);
            }
        } else {
            progressing = false;
            progress = 0D;
        }

        // Воздушное дутьё рассеивается (5% в тик)
        if (!airTank.isEmpty()) {
            int remaining = (int) (airTank.getFluidAmountMb() * 0.95D);
            if (remaining > 0) {
                airTank.setFluid(ModFluids.AIRBLAST.getSource(), remaining);
            } else {
                airTank.setFluid(null, 0);
            }
        }

        // Переполнение бака газов - выброс через трубу (аналог сброса в оригинале)
        int overflow = flueTank.getFluidAmountMb() - FLUE_CAPACITY_MB;
        if (overflow > 0) {
            flueTank.setFluid(ModFluids.FLUE.getSource(), FLUE_CAPACITY_MB);
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 7D, pos.getZ() + 0.5D,
                    Math.max(1, overflow / 20), 0.2D, 0.2D, 0.2D, 0.01D);
        }

        if (wasProgressing != progressing) {
            level.setBlock(pos, state.setValue(BlastFurnaceBlock.LIT, progressing), 3);
        }

        syncIfChanged();
        setChanged();
    }

    private void clientTick(Level level, BlockPos pos) {
        if (!progressing) return;
        if ((level.getGameTime() & 1L) == 0L) {
            level.addParticle(ParticleTypes.LAVA,
                    pos.getX() + 0.25D + level.random.nextDouble() * 0.5D,
                    pos.getY() + 7.25D,
                    pos.getZ() + 0.25D + level.random.nextDouble() * 0.5D,
                    0D, 0D, 0D);
            if (flueTank.getFluidAmountMb() >= 100) {
                level.addParticle(ParticleTypes.LARGE_SMOKE,
                        pos.getX() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.25D,
                        pos.getY() + 7D,
                        pos.getZ() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.25D,
                        0D, 0.1D, 0D);
            }
        }
    }

    private void syncIfChanged() {
        int progressValue = (int) Math.round(progress * 1_000_000D);
        int speedValue = (int) Math.round(speed * 1_000D);
        if (progressValue != lastProgress || speedValue != lastSpeed || fuel != lastFuel
                || airTank.getFluidAmountMb() != lastAir || flueTank.getFluidAmountMb() != lastFlue
                || progressing != lastProgressing) {
            lastProgress = progressValue;
            lastSpeed = speedValue;
            lastFuel = fuel;
            lastAir = airTank.getFluidAmountMb();
            lastFlue = flueTank.getFluidAmountMb();
            lastProgressing = progressing;
            sendUpdateToClient();
        }
    }

    /**
     * Синк BE на клиент через block update (аналог {@code BaseMachineBlockEntity#sendUpdateToClient}):
     * печать доменной печи не наследует энергомашину, поэтому helper дублируется локально.
     */
    protected void sendUpdateToClient() {
        if (level != null && !level.isClientSide && !isRemoved()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ==================== FUEL ====================

    private void loadFuel() {
        ItemStack stack = itemHandler.getStackInSlot(FUEL_SLOT);
        if (stack.isEmpty()) return;
        int value = getFuelValue(stack);
        if (value <= 0 || value > MAX_FUEL - fuel) return;
        fuel += value;

        Item remaining = stack.getItem().getCraftingRemainingItem();
        ItemStack remainder = remaining != null ? new ItemStack(remaining) : ItemStack.EMPTY;
        stack.shrink(1);
        if (stack.isEmpty()) {
            itemHandler.setStackInSlot(FUEL_SLOT, remainder);
        }
        setChanged();
    }

    public static boolean isFuel(ItemStack stack) {
        return getFuelValue(stack) > 0;
    }

    private static int getFuelValue(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.LAVA_BUCKET) {
            return 20_000;
        }
        if (item == Items.COAL || item == Items.CHARCOAL) {
            return 1_600;
        }
        if (item == Items.COAL_BLOCK) {
            return 16_000;
        }
        if (item == Items.BLAZE_ROD) {
            return 8_000;
        }
        if (item == Items.DRIED_KELP) {
            return 120;
        }
        if (item == Items.DRIED_KELP_BLOCK) {
            return 1_200;
        }
        if (item == Items.BLAZE_POWDER) {
            return 2_400;
        }
        if (item == ModItems.LIGNITE.get()) {
            return 1_200;
        }
        // Угольная пыль (оригинал: powder_coal 1600)
        if (item == ModMaterialItems.item(ModMaterials.COAL, MaterialShape.POWDER)) {
            return 1_600;
        }
        return 0;
    }

    // ==================== PROCESSING ====================

    @Nullable
    private BlastFurnaceRecipe findRecipe() {
        if (level == null) return null;
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        RecipeInputWrapper wrapper = new RecipeInputWrapper(inventory);
        for (BlastFurnaceRecipe recipe : RecipeHooks.getAllRecipes(level, BlastFurnaceRecipe.Type.INSTANCE)) {
            if (recipe.matchesRecipe(wrapper, level)) {
                return recipe;
            }
        }
        return null;
    }

    private boolean canProcess(BlastFurnaceRecipe recipe) {
        if (fuel < FUEL_RATE) return false;
        if (!canAcceptOutput(OUTPUT_SLOT_FIRST, recipe.getResultItemSafe())) return false;
        return canAcceptOutput(OUTPUT_SLOT_SECOND, recipe.getSecondaryOutputSafe());
    }

    private boolean canAcceptOutput(int slot, ItemStack output) {
        if (output.isEmpty()) return true;
        ItemStack existing = itemHandler.getStackInSlot(slot);
        if (existing.isEmpty()) return true;
        if (!PlatformHooks.isSameItemSameTags(existing, output)) return false;
        return existing.getCount() + output.getCount() <= existing.getMaxStackSize();
    }

    private void process(BlastFurnaceRecipe recipe) {
        ItemStack primary = recipe.getResultItemSafe();
        ItemStack secondary = recipe.getSecondaryOutputSafe();

        addOutput(OUTPUT_SLOT_FIRST, primary);
        addOutput(OUTPUT_SLOT_SECOND, secondary);

        itemHandler.extractItem(INPUT_SLOT_FIRST, 1, false);
        itemHandler.extractItem(INPUT_SLOT_SECOND, 1, false);

        fuel -= FUEL_RATE;

        int accepted = flueTank.fillMb(ModFluids.FLUE.getSource(), FLUE_PER_OPERATION, false);
        if (accepted < FLUE_PER_OPERATION && !level.isClientSide()) {
            // Бак полон - избыток газа выбрасывается через трубу
            ((ServerLevel) level).sendParticles(ParticleTypes.LARGE_SMOKE,
                    worldPosition.getX() + 0.5D, worldPosition.getY() + 7D, worldPosition.getZ() + 0.5D,
                    Math.max(1, (FLUE_PER_OPERATION - accepted) / 20), 0.2D, 0.2D, 0.2D, 0.01D);
        }

        setChanged();
    }

    private void addOutput(int slot, ItemStack output) {
        if (output.isEmpty()) return;
        ItemStack existing = itemHandler.getStackInSlot(slot);
        if (existing.isEmpty()) {
            itemHandler.setStackInSlot(slot, output.copy());
        } else {
            existing.grow(output.getCount());
            itemHandler.setStackInSlot(slot, existing);
        }
    }

    // ==================== INVENTORY / MENU ====================

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm_m.machine_blast_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineBlastFurnaceMenu(containerId, playerInventory, this, this.data);
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.put("inventory", com.hbm_m.platform.ItemStackSerialization.serialize(itemHandler, registries));
        tag.putDouble("bf_progress", progress);
        tag.putDouble("bf_speed", speed);
        tag.putInt("bf_fuel", fuel);
        tag.putBoolean("bf_progressing", progressing);
        airTank.writeToNBT(tag, "tank_air");
        flueTank.writeToNBT(tag, "tank_flue");
        super.writeNbtData(tag, registries);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        com.hbm_m.platform.ItemStackSerialization.deserialize(itemHandler, tag.getCompound("inventory"), registries);
        progress = Mth.clamp(tag.getDouble("bf_progress"), 0D, 1D);
        speed = tag.getDouble("bf_speed");
        fuel = Mth.clamp(tag.getInt("bf_fuel"), 0, MAX_FUEL);
        progressing = tag.getBoolean("bf_progressing");
        airTank.readFromNBT(tag, "tank_air");
        flueTank.readFromNBT(tag, "tank_flue");
    }
}
