package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Heatable;
import com.hbm_m.inventory.menu.RBMKHeaterMenu;
import com.hbm_m.platform.ModItemStackHandler;
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
 * 1:1 port of {@code TileEntityRBMKHeater} - the heat-exchanger column.
 *
 * <p>This is a real heat exchanger, not a fluid pump: the input tank's fluid must carry the
 * {@link FT_Heatable} trait, and its first heating step decides what the column produces, how much
 * feed one operation costs, and how much heat it takes. The conversion rate is
 * {@code 2000 * efficiency} thermal units per degree - CE's figure, derived from 1 mB of water
 * absorbing 200 TU and 0.1 degree from an RBMK column.</p>
 *
 * <p>The previous implementation was an invention: two untyped tanks moving fluid 1:1 at a fixed
 * rate, with no trait lookup, no fluid-identifier slot, and no connection to the fluid network at
 * all - so nothing could fill it, nothing could drain it, and the fluid it produced was whatever
 * went in.</p>
 */
public class RBMKHeaterBlockEntity extends RBMKColumnBlockEntity
        implements MenuProvider, com.hbm_m.api.fluids.IFluidStandardTransceiverMK2 {

    /** CE: {@code new FluidTankNTM(Fluids.COOLANT, 16_000)} / {@code COOLANT_HOT}. */
    public final FluidTank inputTank  = new FluidTank(ModFluids.COOLANT.getSource(), 16_000);
    public final FluidTank outputTank = new FluidTank(ModFluids.COOLANT_HOT.getSource(), 16_000);

    /** Slot 0 is the fluid identifier that retypes the feed tank, as in {@code ContainerRBMKHeater}. */
    public static final int SLOT_FLUID_ID = 0;
    public final ModItemStackHandler inventory = new ModItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public RBMKHeaterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_HEATER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKHeaterBlockEntity be) {
        baseTick(level, pos, state, be);
        if (level.isClientSide) return;

        be.applyFluidIdentifier();
        be.exchangeHeat();
        be.exchangeFluids(level);
    }

    /** Lets a fluid-identifier item in slot 0 retype the feed tank. */
    private void applyFluidIdentifier() {
        ItemStack[] slots = { inventory.getStackInSlot(SLOT_FLUID_ID) };
        if (inputTank.setType(SLOT_FLUID_ID, slots)) {
            inventory.setStackInSlot(SLOT_FLUID_ID, slots[0] == null ? ItemStack.EMPTY : slots[0]);
            setChanged();
        }
    }

    private void exchangeHeat() {
        FT_Heatable trait = FluidType.getTrait(inputTank.getTankType(), FT_Heatable.class);
        if (trait == null) {
            clearTankTypes();
            return;
        }

        FT_Heatable.HeatingStep step = trait.getFirstStep();
        double eff = trait.getEfficiency(FT_Heatable.HeatingType.HEATEXCHANGER);

        if (step == null || eff <= 0) {
            clearTankTypes();
            return;
        }

        outputTank.setTankType(step.typeProduced);

        double tempRange = heat - FluidType.getTemperatureCelsius(outputTank.getTankType());
        if (tempRange <= 0 || step.amountReq <= 0 || step.amountProduced <= 0) return;

        // 1 mB of water absorbs 200 TU and 0.1 degree from a column, hence 2000 TU per degree.
        double tuPerDegree = 2_000D * eff;

        int inputOps  = inputTank.getFill() / step.amountReq;
        int outputOps = (outputTank.getMaxFill() - outputTank.getFill()) / step.amountProduced;
        int tempOps   = step.heatReq > 0 ? (int) Math.floor((tempRange * tuPerDegree) / step.heatReq) : 0;
        int ops = Math.min(inputOps, Math.min(outputOps, tempOps));
        if (ops <= 0) return;

        inputTank.setFill(inputTank.getFill() - step.amountReq * ops);
        outputTank.setFill(outputTank.getFill() + step.amountProduced * ops);
        heat -= (step.heatReq * (double) ops / tuPerDegree) * eff;
        setChanged();
    }

    private void clearTankTypes() {
        Fluid none = ModFluids.NONE.getSource();
        if (inputTank.getTankType() != none)  inputTank.setTankType(none);
        if (outputTank.getTankType() != none) outputTank.setTankType(none);
    }

    /**
     * Feed comes in from directly underneath; the product leaves through the top of the column plus
     * the sides and bottom of a loader placed under it - the same geometry as the boiler channel.
     */
    private void exchangeFluids(Level level) {
        BlockPos pos = getBlockPos();
        trySubscribe(inputTank.getTankType(), level, pos.below(), Direction.DOWN);

        if (outputTank.getFill() <= 0) return;
        for (com.mojang.datafixers.util.Pair<BlockPos, Direction> target : getOutputPos(level)) {
            tryProvide(outputTank, level, target.getFirst(), target.getSecond());
        }
    }

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
        }
        return out;
    }

    /** {@code TileEntityRBMKHeater.onMelt}: 1-2 pieces of blank debris before the standard melt. */
    @Override
    public void onMelt(Level level, int reduce) {
        int count = 1 + level.random.nextInt(2);
        for (int i = 0; i < count; i++) spawnDebris(level, "blank");
        super.onMelt(level, reduce);
    }

    // ─── MK2 fluid network ───────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks()       { return new FluidTank[] { inputTank, outputTank }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { inputTank }; }
    @Override public FluidTank[] getSendingTanks()   { return new FluidTank[] { outputTank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    //? if forge {
    /** Bottom face takes the feed, every other face hands out the heated product. */
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap,
            @org.jetbrains.annotations.Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            if (side == Direction.DOWN || side == null) return inputTank.getForgeFluidCapability().cast();
            return outputTank.getForgeFluidCapability().cast();
        }
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            return net.minecraftforge.common.util.LazyOptional.of(() -> inventory).cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    @Override public RBMKType getRBMKType()      { return RBMKType.OTHER; }
    @Override public ColumnType getConsoleType() { return ColumnType.HEATER; }

    @Override
    public CompoundTag getNBTForConsole() {
        CompoundTag d = new CompoundTag();
        d.putInt("water",    inputTank.getFill());
        d.putInt("maxWater", inputTank.getMaxFill());
        d.putInt("steam",    outputTank.getFill());
        d.putInt("maxSteam", outputTank.getMaxFill());
        return d;
    }

    @Override public Component getDisplayName() { return Component.translatable("block.hbm_m.rbmk_heater"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new RBMKHeaterMenu(id, inv, this); }

    
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        inputTank.writeToNBT(tag, "input");
        outputTank.writeToNBT(tag, "output");
        tag.put("inventory", inventory.serializeNBT());
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        inputTank.readFromNBT(tag, "input");
        outputTank.readFromNBT(tag, "output");
        if (tag.contains("inventory")) inventory.deserializeNBT(tag.getCompound("inventory"));
    }
}
