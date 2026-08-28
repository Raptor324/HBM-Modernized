package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.handler.rbmk.RBMKOutgasserRecipes;
import com.hbm_m.handler.rbmk.RBMKOutgasserRecipes.OutgasserRecipe;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKNeutronStream;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.RBMKOutgasserMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 port of {@code TileEntityRBMKOutgasser} - the irradiation channel.
 *
 * <p>An item sits in the input slot and soaks up neutron flux; once the accumulated flux passes
 * {@link #duration} the item transmutes according to {@link RBMKOutgasserRecipes}, producing a
 * solid output (into the take-only slot), a gas, or both.</p>
 *
 * <p>What was here before was an invention - a single-slot "xenon scrubber" that spent flux
 * removing poison from a fuel rod. It had no recipes, no gas tank, no output slot, no progress and
 * no connection to the fluid network, so the channel produced nothing and none of the outgasser's
 * real products (tritium, gold-198, thorium fuel) were reachable in-game at all.</p>
 */
public class RBMKOutgasserBlockEntity extends RBMKColumnBlockEntity
        implements IRBMKFluxReceiver, IRBMKLoadable, MenuProvider,
                   com.hbm_m.api.fluids.IFluidStandardSenderMK2 {

    public static final int SLOT_INPUT  = 0;
    public static final int SLOT_OUTPUT = 1;

    public ItemStack inputSlot  = ItemStack.EMPTY;
    public ItemStack outputSlot = ItemStack.EMPTY;

    /** CE: {@code new FluidTankNTM(Fluids.TRITIUM, 64000)}. Retyped per recipe. */
    public final FluidTank gasTank = new FluidTank(ModFluids.TRITIUM.getSource(), 64_000);

    public double progress = 0;
    public int duration = 10_000;

    /** Flux consumed during the current tick - what the console's outgasser readout shows. */
    public double lastUsedFlux = 0;
    private long lastFluxTick = -1;

    /** Guards against the progress bar carrying over when the operator swaps the input item. */
    private ItemStack previousStack = ItemStack.EMPTY;

    public RBMKOutgasserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_OUTGASSER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKOutgasserBlockEntity be) {
        baseTick(level, pos, state, be);
        if (level.isClientSide) return;

        if (level.getGameTime() != be.lastFluxTick) be.lastUsedFlux = 0;

        // CE resets the timer when the item changes - without it, half-irradiating one item and
        // then swapping in another finishes the second one instantly.
        if (!be.canProcess() || !ItemStack.isSameItem(be.previousStack, be.inputSlot)) {
            be.progress = 0;
        }

        if (be.gasTank.getFill() > 0) {
            for (com.mojang.datafixers.util.Pair<BlockPos, Direction> target : be.getOutputPos(level)) {
                be.tryProvide(be.gasTank, level, target.getFirst(), target.getSecond());
            }
        }

        be.previousStack = be.inputSlot.copy();
    }

    // ─── Flux ────────────────────────────────────────────────────────────────

    @Override
    public void receiveFlux(RBMKNeutronStream stream) {
        if (level == null) return;

        // Fast neutrons are worth much less here: efficiency falls to 20% for a purely fast stream.
        double efficiency = Math.min(1 - stream.fluxRatio * 0.8, 1);

        if (canProcess()) {
            double usedFlux = stream.fluxQuantity * efficiency * RBMKDials.getOutgasserMod(level);
            progress += usedFlux;

            long now = level.getGameTime();
            if (now != lastFluxTick) {
                lastFluxTick = now;
                lastUsedFlux = 0;
            }
            lastUsedFlux += usedFlux;

            if (progress > duration) {
                process();
                setChanged();
            }
        } else if (!inputSlot.isEmpty()) {
            // No recipe for this item: CE bombards it anyway and the stack picks up induced
            // radioactivity, which then doses whoever carries it out.
            com.hbm_m.util.ContaminationUtil.neutronActivateItem(
                    inputSlot, (float) (stream.fluxQuantity * efficiency * 0.001), 1F);
            setChanged();
        }
    }

    /**
     * True while there is an item with a recipe whose outputs both fit. Also used by the neutron
     * handler to decide whether a stream terminates on this column.
     */
    public boolean canProcess() {
        if (inputSlot.isEmpty()) return false;

        OutgasserRecipe recipe = RBMKOutgasserRecipes.getRecipe(inputSlot);
        if (recipe == null) return false;

        if (recipe.hasFluid()) {
            if (gasTank.getTankType() != recipe.fluidType() && gasTank.getFill() > 0) return false;
            gasTank.setTankType(recipe.fluidType());
            if (gasTank.getFill() + recipe.fluidAmount() > gasTank.getMaxFill()) return false;
        }

        if (!recipe.hasSolid()) return true;
        if (outputSlot.isEmpty()) return true;

        ItemStack out = recipe.solidOutput();
        return ItemStack.isSameItemSameTags(outputSlot, out)
                && outputSlot.getCount() + out.getCount() <= outputSlot.getMaxStackSize();
    }

    private void process() {
        OutgasserRecipe recipe = RBMKOutgasserRecipes.getRecipe(inputSlot);
        inputSlot.shrink(1);
        progress = 0;
        if (recipe == null) return;

        if (recipe.hasFluid()) {
            gasTank.setFill(Math.min(gasTank.getFill() + recipe.fluidAmount(), gasTank.getMaxFill()));
        }
        if (recipe.hasSolid()) {
            ItemStack out = recipe.solidOutput();
            if (outputSlot.isEmpty()) outputSlot = out.copy();
            else outputSlot.grow(out.getCount());
        }
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_INPUT && RBMKOutgasserRecipes.getRecipe(stack) != null;
    }

    /** {@code TileEntityRBMKOutgasser.onMelt}: 4-5 pieces of blank debris before the standard melt. */
    @Override
    public void onMelt(Level level, int reduce) {
        int count = 4 + level.random.nextInt(2);
        for (int i = 0; i < count; i++) spawnDebris(level, "blank");
        super.onMelt(level, reduce);
    }

    // ─── Fluid output ────────────────────────────────────────────────────────

    /**
     * Gas leaves through the top of the column, and - unlike the boiler - also straight down when
     * there is no loader underneath, which is CE's third {@code getConPos} branch.
     */
    private java.util.List<com.mojang.datafixers.util.Pair<BlockPos, Direction>> getOutputPos(Level level) {
        BlockPos pos = getBlockPos();
        int h = RBMKDials.getColumnHeight(level);
        java.util.List<com.mojang.datafixers.util.Pair<BlockPos, Direction>> out = new java.util.ArrayList<>();
        out.add(com.mojang.datafixers.util.Pair.of(pos.above(h + 1), Direction.UP));

        BlockPos loader = null;
        if (level.getBlockState(pos.below()).is(com.hbm_m.block.ModBlocks.RBMK_LOADER.get())) {
            loader = pos.below();
        } else if (level.getBlockState(pos.below(2)).is(com.hbm_m.block.ModBlocks.RBMK_LOADER.get())) {
            loader = pos.below(2);
        }

        if (loader != null) {
            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP) continue;
                out.add(com.mojang.datafixers.util.Pair.of(loader.relative(dir), dir));
            }
        } else {
            out.add(com.mojang.datafixers.util.Pair.of(pos.below(), Direction.DOWN));
        }
        return out;
    }

    @Override public FluidTank[] getAllTanks()     { return new FluidTank[] { gasTank }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { gasTank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap,
            @org.jetbrains.annotations.Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return gasTank.getCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    // ─── IRBMKLoadable ───────────────────────────────────────────────────────

    @Override public boolean canLoad(ItemStack s)  { return !s.isEmpty() && inputSlot.isEmpty(); }
    @Override public void    load(ItemStack s)     { inputSlot = s.copy(); setChanged(); }
    @Override public boolean canUnload()           { return !outputSlot.isEmpty(); }
    @Override public ItemStack provideNext()       { return outputSlot; }
    @Override public void    unload()              { outputSlot = ItemStack.EMPTY; setChanged(); }

    @Override public Component getDisplayName() { return Component.translatable("block.hbm_m.rbmk_outgasser"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new RBMKOutgasserMenu(id, inv, this); }
    @Override public RBMKType getRBMKType()      { return RBMKType.OUTGASSER; }
    @Override public ColumnType getConsoleType() { return ColumnType.OUTGASSER; }

    @Override
    public CompoundTag getNBTForConsole() {
        CompoundTag d = new CompoundTag();
        d.putInt("gas",           gasTank.getFill());
        d.putInt("maxGas",        gasTank.getMaxFill());
        d.putDouble("progress",    progress);
        d.putDouble("maxProgress", duration);
        d.putDouble("usedFlux",    lastUsedFlux);
        return d;
    }

    // ─── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!inputSlot.isEmpty())  tag.put("inputSlot",  safeItemSave(inputSlot));
        if (!outputSlot.isEmpty()) tag.put("outputSlot", safeItemSave(outputSlot));
        gasTank.writeToNBT(tag, "gas");
        tag.putDouble("progress", progress);
        tag.putDouble("lastUsedFlux", lastUsedFlux);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inputSlot  = tag.contains("inputSlot")  ? ItemStack.of(tag.getCompound("inputSlot"))  : ItemStack.EMPTY;
        outputSlot = tag.contains("outputSlot") ? ItemStack.of(tag.getCompound("outputSlot")) : ItemStack.EMPTY;
        gasTank.readFromNBT(tag, "gas");
        progress = tag.getDouble("progress");
        lastUsedFlux = tag.getDouble("lastUsedFlux");
    }
}
