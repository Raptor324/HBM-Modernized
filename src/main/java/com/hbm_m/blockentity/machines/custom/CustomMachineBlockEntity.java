package com.hbm_m.blockentity.machines.custom;

import com.hbm_m.platform.RenderBounds;

import com.hbm_m.platform.StackNbt;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.custom.CustomMachineBlock;
import com.hbm_m.blockentity.MachinePollutingBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineReactorResearchBlockEntity;
import com.hbm_m.blockentity.machines.UniversalMachinePartBlockEntity;
import com.hbm_m.config.CustomMachineConfigJSON;
import com.hbm_m.config.CustomMachineConfigJSON.MachineConfiguration;
import com.hbm_m.config.CustomMachineConfigJSON.MachineConfiguration.ComponentDefinition;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.interfaces.IHeatSource;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.inventory.menu.CustomMachineMenu;
import com.hbm_m.inventory.recipes.CustomMachineRecipes;
import com.hbm_m.inventory.recipes.CustomMachineRecipes.CustomMachineRecipe;
import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityCustomMachine}: Steuerung einer frei konfigurierten Maschine. Die Bauteile werden alle 300 Ticks
 * (und bei jedem Rechtsklick) gegen den Bauplan geprueft; {@code cm_port}-Bloecke reichen Inventar, Strom und Fluide
 * an die Steuerung, {@code cm_flux} liest den Neutronenfluss eines Forschungsreaktors, {@code cm_heat} zieht Waerme
 * aus Quellen darunter. Rezepte kommen aus {@link CustomMachineRecipes} (feste Eingabereihenfolge), im
 * Generatorbetrieb wird beim Start verbraucht und Strom erzeugt.
 *
 * <p>Slots: 0 Batterie, 1-3 Fluidkennungen, 4-9 Eingaben, 10-15 Filter, 16-21 Ausgaben.</p>
 */
public class CustomMachineBlockEntity extends MachinePollutingBlockEntity
        implements com.hbm_m.api.fluids.IFluidStandardTransceiverMK2 {

    public String machineType;
    @Nullable
    public MachineConfiguration config;

    public int flux;
    public int heat;
    public int maxHeat;
    public int progress;
    public int maxProgress = 1;
    public FluidTank[] inputTanks = new FluidTank[0];
    public FluidTank[] outputTanks = new FluidTank[0];
    public ModulePatternMatcher matcher = new ModulePatternMatcher(0);
    public int structureCheckDelay;
    public boolean structureOK = false;
    @Nullable
    public CustomMachineRecipe cachedRecipe;

    public final List<Object[]> connectionPos = new ArrayList<>();
    public final List<Object[]> fluxPos = new ArrayList<>();
    public final List<Object[]> heatPos = new ArrayList<>();

    private int pendingCachedIndex = -1;

    public CustomMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CUSTOM_MACHINE_BE.get(), pos, state, 22, 1, 0, 0, 100);
    }

    public void init() {
        MachineConfiguration config = CustomMachineConfigJSON.customMachines.get(this.machineType);

        if (config != null) {
            this.config = config;

            inputTanks = new FluidTank[config.fluidInCount];
            for (int i = 0; i < inputTanks.length; i++) inputTanks[i] = new FluidTank(ModFluids.NONE.getSource(), config.fluidInCap);
            outputTanks = new FluidTank[config.fluidOutCount];
            for (int i = 0; i < outputTanks.length; i++) outputTanks[i] = new FluidTank(ModFluids.NONE.getSource(), config.fluidOutCap);
            maxHeat = config.maxHeat;
            matcher = new ModulePatternMatcher(config.itemInCount);
            smoke.changeTankSize(config.maxPollutionCap);
            smokeLeaded.changeTankSize(config.maxPollutionCap);
            smokePoison.changeTankSize(config.maxPollutionCap);
            setEnergyCapacity(config.maxPower);
        } else {
            this.config = null;
        }
    }

    public String getName() {
        return config != null ? config.localizedName : "INVALID";
    }

    private ItemStack[] slots() {
        ItemStack[] s = new ItemStack[22];
        for (int i = 0; i < 22; i++) s[i] = inventory.getStackInSlot(i);
        return s;
    }

    private ItemStack slot(int i) {
        return inventory.getStackInSlot(i);
    }

    private Direction dir() {
        BlockState st = getBlockState();
        return st.hasProperty(CustomMachineBlock.FACING) ? st.getValue(CustomMachineBlock.FACING) : Direction.NORTH;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CustomMachineBlockEntity be) {
        if (level.isClientSide) return;
        be.update((ServerLevel) level);
    }

    private void update(ServerLevel level) {
        if (config == null) {
            level.destroyBlock(worldPosition, false);
            return;
        }

        chargeFromBatterySlot(0);

        ItemStack[] s = slots();
        if (this.inputTanks.length > 0) this.inputTanks[0].setType(1, s);
        if (this.inputTanks.length > 1) this.inputTanks[1].setType(2, s);
        if (this.inputTanks.length > 2) this.inputTanks[2].setType(3, s);

        this.structureCheckDelay--;
        if (this.structureCheckDelay <= 0) this.checkStructure();

        if (level.getGameTime() % 20 == 0) {
            for (Object[] p : this.connectionPos) {
                BlockPos at = (BlockPos) p[0];
                Direction d = (Direction) p[1];
                for (FluidTank tank : this.inputTanks) this.trySubscribe(tank.getTankType(), level, at, d);
                if (!config.generatorMode) this.trySubscribe(level, at.getX(), at.getY(), at.getZ(), d);
            }
            for (Direction dir : new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST }) {
                for (Object[] p : this.fluxPos) {
                    BlockPos at = ((BlockPos) p[0]).offset(dir.getStepX(), 0, dir.getStepZ());
                    MachineReactorResearchBlockEntity reactor = findResearchReactor(level, at);
                    if (reactor != null) this.flux = reactor.totalFlux;
                }
                if (config.maxHeat > 0) {
                    for (Object[] p : this.heatPos) {
                        BlockPos at = (BlockPos) p[0];
                        this.tryPullHeat(level, at.getX() + dir.getStepX(), at.getY() - 1, at.getZ() + dir.getStepZ());
                    }
                }
            }
        }

        for (Object[] p : this.connectionPos) {
            BlockPos at = (BlockPos) p[0];
            Direction d = (Direction) p[1];
            if (config.generatorMode && getEnergyStored() > 0) this.tryProvide(level, at.getX(), at.getY(), at.getZ(), d);
            for (FluidTank tank : this.outputTanks) if (tank.getFill() > 0) this.tryProvide(tank, level, at, d);
            this.sendSmoke(level, at, d);
        }

        if (this.structureOK) {

            if (config.generatorMode) {
                if (this.cachedRecipe == null) {
                    CustomMachineRecipe recipe = this.getMatchingRecipe();
                    if (recipe != null && this.hasRequiredQuantities(recipe) && this.hasSpace(recipe)) {
                        this.cachedRecipe = recipe;
                        this.useUpInput(recipe);
                    }
                }

                if (this.cachedRecipe != null) {
                    this.maxProgress = (int) Math.max(cachedRecipe.duration / this.config.recipeSpeedMult, 1);
                    int powerReq = (int) Math.max(cachedRecipe.consumptionPerTick * this.config.recipeConsumptionMult, 1);

                    this.progress++;
                    long power = getEnergyStored() + powerReq;
                    this.heat -= cachedRecipe.heat;
                    if (power > config.maxPower) power = config.maxPower;
                    setEnergyStored(power);
                    if (level.getGameTime() % 20 == 0) {
                        pollution(level, cachedRecipe);
                        radiation(level, cachedRecipe);
                    }
                    if (progress >= this.maxProgress) {
                        this.progress = 0;
                        this.processRecipe(level, cachedRecipe);
                        this.cachedRecipe = null;
                    }
                }

            } else {
                CustomMachineRecipe recipe = this.getMatchingRecipe();

                if (recipe != null) {
                    this.maxProgress = (int) Math.max(recipe.duration / this.config.recipeSpeedMult, 1);
                    int powerReq = (int) Math.max(recipe.consumptionPerTick * this.config.recipeConsumptionMult, 1);

                    if (getEnergyStored() >= powerReq && this.hasRequiredQuantities(recipe) && this.hasSpace(recipe)) {
                        this.progress++;
                        setEnergyStored(getEnergyStored() - powerReq);
                        this.heat -= recipe.heat;
                        if (level.getGameTime() % 20 == 0) {
                            pollution(level, recipe);
                            radiation(level, recipe);
                        }
                        if (progress >= this.maxProgress) {
                            this.progress = 0;
                            this.useUpInput(recipe);
                            this.processRecipe(level, recipe);
                        }
                    }
                } else {
                    this.progress = 0;
                }
            }
        } else {
            this.progress = 0;
        }
        setChanged();
        sendUpdateToClient();
    }

    @Nullable
    private static MachineReactorResearchBlockEntity findResearchReactor(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MachineReactorResearchBlockEntity r) return r;
        if (be instanceof UniversalMachinePartBlockEntity part && part.getControllerPos() != null
                && level.getBlockEntity(part.getControllerPos()) instanceof MachineReactorResearchBlockEntity r) return r;
        return null;
    }

    /** Only accepts inputs in a fixed order, saves a ton of performance because there's no permutations to check for */
    @Nullable
    public CustomMachineRecipe getMatchingRecipe() {
        List<CustomMachineRecipe> recipes = CustomMachineRecipes.recipes.get(this.config.recipeKey);
        if (recipes == null || recipes.isEmpty()) return null;

        outer:
        for (CustomMachineRecipe recipe : recipes) {
            for (int i = 0; i < recipe.inputFluids.length; i++) {
                if (i >= inputTanks.length) continue outer;
                if (this.inputTanks[i].getTankType() != recipe.inputFluids[i].type() || this.inputTanks[i].getPressure() != recipe.inputFluids[i].pressure()) continue outer;
            }

            for (int i = 0; i < recipe.inputItems.length; i++) {
                if (recipe.inputItems[i] != null && slot(i + 4).isEmpty()) continue outer;
                if (!recipe.inputItems[i].matches(slot(i + 4), true)) continue outer;
            }

            return recipe;
        }

        return null;
    }

    public void pollution(Level level, CustomMachineRecipe recipe) {
        if (recipe.pollutionType == null || recipe.pollutionType.isEmpty()) return;
        PollutionType type;
        try { type = PollutionType.valueOf(recipe.pollutionType); } catch (IllegalArgumentException e) { return; }
        if (recipe.pollutionAmount > 0) {
            this.pollute(type, recipe.pollutionAmount);
        } else if (recipe.pollutionAmount < 0 && PollutionHandler.getPollution(level, worldPosition, type) >= -recipe.pollutionAmount) {
            PollutionHandler.decrementPollution(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), type, -recipe.pollutionAmount);
        }
    }

    public void radiation(Level level, CustomMachineRecipe recipe) {
        if (recipe.radiationAmount > 0) {
            ChunkRadiationManager.incrementRad(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), recipe.radiationAmount);
        } else if (recipe.radiationAmount < 0) {
            ChunkRadiationManager.getProxy().decrementRad(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), -recipe.radiationAmount);
        }
    }

    protected void tryPullHeat(Level level, int x, int y, int z) {
        BlockEntity con = level.getBlockEntity(new BlockPos(x, y, z));

        if (con instanceof IHeatSource source) {
            int diff = source.getHeatStored() - this.heat;
            if (diff == 0) return;

            if (diff > 0) {
                source.useUpHeat(diff);
                this.heat += diff;
                if (this.heat > this.maxHeat) this.heat = this.maxHeat;
            }
        }
    }

    public boolean hasRequiredQuantities(CustomMachineRecipe recipe) {
        for (int i = 0; i < recipe.inputFluids.length; i++) {
            if (this.inputTanks[i].getFill() < recipe.inputFluids[i].fill()) return false;
        }
        for (int i = 0; i < recipe.inputItems.length; i++) {
            if (!slot(i + 4).isEmpty() && slot(i + 4).getCount() < recipe.inputItems[i].stacksize()) return false;
        }
        if (config.fluxMode && this.flux < recipe.flux) return false;
        if (config.maxHeat > 0 && recipe.heat > 0 && this.heat < recipe.heat) return false;
        return true;
    }

    public boolean hasSpace(CustomMachineRecipe recipe) {
        for (int i = 0; i < recipe.outputFluids.length; i++) {
            if (i >= outputTanks.length) return false;
            if (this.outputTanks[i].getTankType() == recipe.outputFluids[i].type() && this.outputTanks[i].getFill() + recipe.outputFluids[i].fill() > this.outputTanks[i].getMaxFill()) return false;
        }
        for (int i = 0; i < recipe.outputItems.length; i++) {
            ItemStack out = slot(i + 16);
            ItemStack key = recipe.outputItems[i].stack();
            if (!out.isEmpty() && !StackNbt.sameItemSameTags(out, key)) return false;
            if (!out.isEmpty() && out.getCount() + key.getCount() > out.getMaxStackSize()) return false;
        }
        return true;
    }

    public void useUpInput(CustomMachineRecipe recipe) {
        for (int i = 0; i < recipe.inputFluids.length; i++) {
            this.inputTanks[i].setFill(this.inputTanks[i].getFill() - recipe.inputFluids[i].fill());
        }
        for (int i = 0; i < recipe.inputItems.length; i++) {
            inventory.extractItem(i + 4, recipe.inputItems[i].stacksize(), false);
        }
    }

    public void processRecipe(Level level, CustomMachineRecipe recipe) {
        for (int i = 0; i < recipe.outputFluids.length; i++) {
            if (this.outputTanks[i].getTankType() != recipe.outputFluids[i].type()) this.outputTanks[i].setTankType(recipe.outputFluids[i].type());
            this.outputTanks[i].setFill(this.outputTanks[i].getFill() + recipe.outputFluids[i].fill());
        }
        for (int i = 0; i < recipe.outputItems.length; i++) {
            if (level.random.nextFloat() < recipe.outputItems[i].chance()) {
                ItemStack out = slot(i + 16);
                if (out.isEmpty()) {
                    inventory.setStackInSlot(i + 16, recipe.outputItems[i].stack().copy());
                } else {
                    out.grow(recipe.outputItems[i].stack().getCount());
                    inventory.setStackInSlot(i + 16, out);
                }
            }
        }
    }

    /** Original-Formel samt EW-Sonderfall ("i wholeheartedly believe it is the computer who is wrong here"). */
    public BlockPos componentPos(ComponentDefinition comp) {
        Direction dir = dir();
        Direction rot = dir.getClockWise();
        int x = worldPosition.getX() - dir.getStepX() * comp.x + rot.getStepX() * comp.x;
        int y = worldPosition.getY() + comp.y;
        int z = worldPosition.getZ() - dir.getStepZ() * comp.z + rot.getStepZ() * comp.z;
        if (dir == Direction.EAST || dir == Direction.WEST) {
            x = worldPosition.getX() + dir.getStepZ() * comp.z - rot.getStepZ() * comp.z;
            z = worldPosition.getZ() + dir.getStepX() * comp.x - rot.getStepX() * comp.x;
        }
        return new BlockPos(x, y, z);
    }

    public boolean checkStructure() {
        this.connectionPos.clear();
        this.fluxPos.clear();
        this.heatPos.clear();
        this.structureCheckDelay = 300;
        this.structureOK = false;
        if (this.config == null || level == null) return false;

        for (ComponentDefinition comp : config.components) {
            BlockPos p = componentPos(comp);
            Block b = level.getBlockState(p).getBlock();
            if (!comp.blocks.contains(b)) return false;

            if (level.getBlockEntity(p) instanceof CMPortBlockEntity proxy) {
                proxy.setCachedPosition(worldPosition);
                for (Direction facing : Direction.values()) this.connectionPos.add(new Object[] { p.relative(facing), facing });
            }
            if (b == ModBlocks.CM_FLUX.get()) {
                for (Direction facing : Direction.values()) this.fluxPos.add(new Object[] { p.relative(facing), facing });
            } else if (b == ModBlocks.CM_HEAT.get()) {
                for (Direction facing : Direction.values()) this.heatPos.add(new Object[] { p.relative(facing), facing });
            }
        }
        for (Direction facing : Direction.values()) this.connectionPos.add(new Object[] { worldPosition.relative(facing), facing });

        this.structureOK = true;
        return true;
    }

    public void buildStructure() {
        if (this.config == null || level == null) return;
        for (ComponentDefinition comp : config.components) {
            level.setBlock(componentPos(comp), comp.blocks.get(0).defaultBlockState(), 3);
        }
    }

    // ─── Inventar ───────────────────────────────────────────────────────────

    /** GUI-Plaetze: Batterie/Kennungen frei, Eingaben ueber Filter, Filter als Geist, Ausgaben nur entnehmen. */
    @Override
    protected boolean isItemValidForSlot(int slot, @NotNull ItemStack stack) {
        if (slot >= 16) return false;
        if (slot >= 4 && slot <= 9) return isValidInput(slot, stack);
        return true;
    }

    /** Original {@code isItemValidForSlot}: nur Eingaben, mit Filterpruefung. */
    public boolean isValidInput(int slot, ItemStack stack) {
        if (slot < 4 || slot > 9) return false;
        int index = slot - 4;
        ItemStack filter = slot(slot + 6);
        if (filter.isEmpty()) return true;
        if (index >= matcher.size()) return true;
        return matcher.isValidForFilter(filter, index, stack);
    }

    /** Original {@code getAccessibleSlotsFromSide}: die freigeschalteten Eingaben und alle Ausgaben. */
    public int[] getAccessibleSlots() {
        if (this.config == null) return new int[] { };
        int in = Math.min(6, Math.max(0, config.itemInCount));
        int[] out = new int[in + 6];
        for (int i = 0; i < in; i++) out[i] = 4 + i;
        for (int i = 0; i < 6; i++) out[in + i] = 16 + i;
        return out;
    }

    @Override
    public void dropInventoryContents() {
        if (level == null) return;
        for (int i = 0; i < 22; i++) {
            if (i >= 10 && i <= 15) continue; // do NOT drop the filters
            ItemStack stack = slot(i);
            if (!stack.isEmpty()) net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    //? if forge {
    private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> automationHandler =
            net.minecraftforge.common.util.LazyOptional.of(AutomationHandler::new);

    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) return automationHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        automationHandler.invalidate();
    }

    private class AutomationHandler implements net.minecraftforge.items.IItemHandler {
        private int real(int slot) {
            int[] acc = getAccessibleSlots();
            return slot >= 0 && slot < acc.length ? acc[slot] : -1;
        }
        @Override public int getSlots() { return getAccessibleSlots().length; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { int r = real(slot); return r < 0 ? ItemStack.EMPTY : inventory.getStackInSlot(r); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            int r = real(slot);
            if (r < 0 || !isValidInput(r, stack)) return stack;
            return inventory.insertItem(r, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            int r = real(slot);
            if (r < 16 || r > 21) return ItemStack.EMPTY;
            return inventory.extractItem(r, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { int r = real(slot); return r >= 0 && isValidInput(r, stack); }
    }
    //?} elif neoforge {
    /*private final com.hbm_m.platform.LazyCap<net.neoforged.neoforge.items.IItemHandler> automationHandler =
            com.hbm_m.platform.LazyCap.of(AutomationHandler::new);

    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER) return automationHandler.cast();
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        automationHandler.invalidate();
    }

    private class AutomationHandler implements net.neoforged.neoforge.items.IItemHandler {
        private int real(int slot) {
            int[] acc = getAccessibleSlots();
            return slot >= 0 && slot < acc.length ? acc[slot] : -1;
        }
        @Override public int getSlots() { return getAccessibleSlots().length; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { int r = real(slot); return r < 0 ? ItemStack.EMPTY : inventory.getStackInSlot(r); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            int r = real(slot);
            if (r < 0 || !isValidInput(r, stack)) return stack;
            return inventory.insertItem(r, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            int r = real(slot);
            if (r < 16 || r > 21) return ItemStack.EMPTY;
            return inventory.extractItem(r, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { int r = real(slot); return r >= 0 && isValidInput(r, stack); }
    }
    *///?}

    // ─── Strom ──────────────────────────────────────────────────────────────

    @Override
    public long getReceiveSpeed() {
        return config != null && !config.generatorMode ? config.maxPower : 0;
    }

    @Override
    public long getProvideSpeed() {
        return config != null && config.generatorMode ? config.maxPower : 0;
    }

    @Override
    public boolean canReceive() {
        return config != null && !config.generatorMode && getEnergyStored() < getMaxEnergyStored();
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        if (!canReceive()) return 0;
        long received = Math.min(getMaxEnergyStored() - getEnergyStored(), maxReceive);
        if (!simulate && received > 0) setEnergyStored(getEnergyStored() + received);
        return received;
    }

    @Override
    public boolean canExtract() {
        return config != null && config.generatorMode && getEnergyStored() > 0;
    }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        if (!canExtract()) return 0;
        long extracted = Math.min(getEnergyStored(), maxExtract);
        if (!simulate && extracted > 0) setEnergyStored(getEnergyStored() - extracted);
        return extracted;
    }

    // ─── Fluide ─────────────────────────────────────────────────────────────

    @Override
    public FluidTank[] getAllTanks() {
        FluidTank[] smokeT = getSmokeTanks();
        FluidTank[] all = new FluidTank[inputTanks.length + outputTanks.length + smokeT.length];
        int k = 0;
        for (FluidTank t : inputTanks) all[k++] = t;
        for (FluidTank t : outputTanks) all[k++] = t;
        for (FluidTank t : smokeT) all[k++] = t;
        return all;
    }

    @Override
    public FluidTank[] getSendingTanks() {
        FluidTank[] smokeT = getSmokeTanks();
        FluidTank[] all = new FluidTank[outputTanks.length + smokeT.length];
        for (int i = 0; i < outputTanks.length; i++) all[i] = outputTanks[i];
        for (int i = 0; i < smokeT.length; i++) all[outputTanks.length + i] = smokeT[i];
        return all;
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return inputTanks != null ? inputTanks : new FluidTank[0];
    }

    // ─── NBT / Sync ─────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        if (machineType == null || this.config == null) {
            super.writeNbtData(nbt, registries);
            return;
        }
        nbt.putString("machineType", machineType);
        super.writeNbtData(nbt, registries);

        for (int i = 0; i < inputTanks.length; i++) inputTanks[i].writeToNBT(nbt, "i" + i);
        for (int i = 0; i < outputTanks.length; i++) outputTanks[i].writeToNBT(nbt, "o" + i);
        this.matcher.writeToNBT(nbt);

        if (this.cachedRecipe != null) {
            nbt.putInt("cachedIndex", CustomMachineRecipes.recipes.get(this.config.recipeKey).indexOf(this.cachedRecipe));
        } else {
            nbt.putInt("cachedIndex", -1);
        }
        nbt.putInt("progress", progress);
        nbt.putInt("maxProgress", maxProgress);
        nbt.putInt("flux", flux);
        nbt.putInt("heat", heat);
        nbt.putBoolean("structureOK", structureOK);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        String type = nbt.getString("machineType");
        if (!type.equals(this.machineType) || this.config == null) {
            this.machineType = type;
            this.init();
        }
        super.readNbtData(nbt, registries);

        if (this.config != null) {
            for (int i = 0; i < inputTanks.length; i++) inputTanks[i].readFromNBT(nbt, "i" + i);
            for (int i = 0; i < outputTanks.length; i++) outputTanks[i].readFromNBT(nbt, "o" + i);
            this.matcher.readFromNBT(nbt);

            int index = nbt.contains("cachedIndex") ? nbt.getInt("cachedIndex") : -1;
            List<CustomMachineRecipe> list = CustomMachineRecipes.recipes.get(this.config.recipeKey);
            this.cachedRecipe = index != -1 && list != null && index < list.size() ? list.get(index) : null;
        }
        progress = nbt.getInt("progress");
        maxProgress = Math.max(1, nbt.getInt("maxProgress"));
        flux = nbt.getInt("flux");
        heat = nbt.getInt("heat");
        structureOK = nbt.getBoolean("structureOK");
    }

    // ─── Sonstiges ──────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.literal(getName());
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        if (this.config == null) return null;
        return new CustomMachineMenu(id, inv, this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return RenderBounds.INFINITE;
    }
}
