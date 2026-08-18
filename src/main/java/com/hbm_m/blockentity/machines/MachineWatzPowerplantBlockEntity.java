package com.hbm_m.blockentity.machines;

import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.interfaces.IMultiblockSidedIO;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineWatzPowerplantMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.nuclear.WatzPelletItem;
import com.hbm_m.item.nuclear.WatzPelletType;
import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

/**
 * Watz Powerplant reactor core. Ported from {@code com.hbm.tileentity.machine.TileEntityWatz}
 * (1.7.10, 674 lines) onto this port's multiblock/fluid-tank framework, using
 * {@code MachineZirnoxBlockEntity} as the structural template for tanks/heat/fluid I/O.
 * <p>
 * <b>SCOPE (single-segment):</b> the original stacked multiple 3-tall {@code TileEntityWatz}
 * segments vertically into a tower, with pellets "falling" between segments and shared tanks
 * merged across the whole stack each tick. This port implements exactly ONE segment (one
 * controller, one 24-slot inventory, one set of 3 tanks) per multiblock structure - no
 * inter-segment stacking or pellet-falling. This is a deliberate scope reduction; the reaction/
 * coolant/waste-production math below is otherwise a faithful per-tick port.
 * <p>
 * <b>Fuel/absorber pellets:</b> the original's {@code ItemWatzPellet} was a single NBT-enum-meta
 * item with 12 variants driven by a small function-object algebra library. This port uses
 * {@link WatzPelletType} (5 representative archetypes covering self-igniting fuel, flux-fed
 * fuel, and two absorber response curves) as separate items (see {@link WatzPelletItem}),
 * matching this port's existing {@code ZirnoxRodItem} convention.
 * <p>
 * <b>Coolant heating:</b> the original used an {@code FT_Heatable} fluid trait
 * (heatReq/amountReq/amountProduced) looked up from the coolant fluid's registered traits. No
 * such generic heating-trait system exists in this port, so a fixed conversion ratio is used
 * instead: {@link #COOLANT_HEAT_PER_MB} heat is consumed to convert 1 mB of cold coolant into
 * 1 mB of hot coolant, bounded by the tick's {@link #COOLING_FACTOR} heat budget exactly as in
 * the original ({@code heatToUse = heat * 0.2}).
 * <p>
 * <b>Meltdown:</b> the original's mud/waste overflow triggered a violent structural
 * disassembly (rubble blocks, ~100 shrapnel entities, achievement). This port implements a
 * simplified meltdown: production stops, half the waste tank is vented, a radiation spike is
 * applied (reusing {@link ExplosionNukeGeneric#incrementRad}), a modest block-damage explosion
 * is triggered, and simple particles are spawned - no shrapnel entities or structure
 * destruction, following the same simplification already used by
 * {@code MachineZirnoxBlockEntity#meltdown()} in this port for a comparable large reactor.
 * <p>
 * <b>On/off control:</b> unlike the original (pump block + redstone above pump), this port
 * follows the {@code MachineZirnoxBlockEntity} convention: a GUI toggle button is the primary
 * control, and an external redstone signal into the structure forces the reactor on (mirroring
 * {@code MachineZirnoxBlock#neighborChanged}).
 * <p>
 * <b>Output:</b> faithful to the original - Watz produces heat and hot coolant/waste fluid
 * only, no direct FE output. Downstream consumers (turbines) are out of scope for this task;
 * the hot coolant and waste tanks are exposed via the standard fluid pipe network exactly like
 * every other fluid-producing machine in this port, so they are usable once such consumers
 * exist.
 */
@SuppressWarnings("UnstableApiUsage")
public class MachineWatzPowerplantBlockEntity extends BaseMachineBlockEntity
        implements IFluidStandardTransceiverMK2, IMultiblockSidedIO {

    public static final int PELLET_SLOTS = 24;

    public static final int COOLANT_MAX = 32_000;
    public static final int COOLANT_HOT_MAX = 32_000;
    public static final int WASTE_MAX = 16_000;

    /** Fraction of stored heat usable as the coolant-conversion budget each tick (faithful to original). */
    private static final double COOLING_FACTOR = 0.2D;
    /** Heat consumed per mB of cold coolant converted to hot coolant (simplified fixed ratio, see class doc). */
    private static final double COOLANT_HEAT_PER_MB = 2.0D;
    /** Passive heat loss per tick (faithful to original's {@code heat *= 0.99}). */
    private static final double PASSIVE_COOLING = 0.99D;
    /** Heat level at/above which the reactor is considered dangerously overheated for GUI warnings. */
    public static final double MAX_SAFE_HEAT = 1_000_000D;

    public final FluidTank coolantTank;
    public final FluidTank coolantHotTank;
    public final FluidTank wasteTank;

    public double heat = 0D;
    public double fluxLastBase = 0D;
    public double fluxLastReaction = 0D;
    public boolean isOn = false;
    public boolean redstonePowered = false;

    private Set<Direction> allowedFluidSides = EnumSet.noneOf(Direction.class);
    private boolean fluidSidesFromMultiblockStructure = false;

    //? if forge {
    private final LazyOptional<IFluidHandler> lazyFluidHandler;
    //?}

    public MachineWatzPowerplantBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATZ_POWERPLANT_BE.get(), pos, state, PELLET_SLOTS, 0L, 0L);
        this.coolantTank = new FluidTank(ModFluids.COOLANT.getSource(), COOLANT_MAX);
        this.coolantHotTank = new FluidTank(ModFluids.COOLANT_HOT.getSource(), COOLANT_HOT_MAX);
        this.wasteTank = new FluidTank(ModFluids.WATZ.getSource(), WASTE_MAX);
        //? if forge {
        this.lazyFluidHandler = LazyOptional.of(() -> new UnifiedFluidHandler(this));
        //?}
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineWatzPowerplantBlockEntity be) {
        if (level.isClientSide()) return;
        be.ensureNetworkInitialized();
        be.update(level, pos);
    }

    private void update(Level level, BlockPos pos) {
        int oldCoolant = coolantTank.getFill();
        int oldHot = coolantHotTank.getFill();
        int oldWaste = wasteTank.getFill();
        double oldHeat = heat;
        boolean oldOn = isOn;

        if (redstonePowered) isOn = true;

        updateReaction();
        updateCoolant();
        heat *= PASSIVE_COOLING;

        // push hot coolant + waste out / pull cold coolant in via pipes
        for (Direction dir : Direction.values()) {
            if (fluidSidesFromMultiblockStructure) {
                if (!allowedFluidSides.contains(dir)) continue;
            } else if (!allowedFluidSides.isEmpty() && !allowedFluidSides.contains(dir)) {
                continue;
            }
            BlockPos pipePos = pos.relative(dir);
            BlockEntity pipeBe = level.getBlockEntity(pipePos);
            if (!(pipeBe instanceof com.hbm_m.api.fluids.IFluidConnectorMK2)) continue;

            if (coolantHotTank.getFill() > 0) tryProvide(coolantHotTank, level, pipePos, dir);
            if (wasteTank.getFill() > 0) tryProvide(wasteTank, level, pipePos, dir);
            if (coolantTank.getFill() < coolantTank.getMaxFill()) trySubscribe(coolantTank.getTankType(), level, pipePos, dir);
        }

        checkWasteOverflow(level);

        // small passive radiation leak proportional to stored waste (this port's radiation manager hook)
        if (wasteTank.getFill() > 0 && level.getGameTime() % 20 == 0) {
            float leak = (wasteTank.getFill() / (float) WASTE_MAX) * 2.0F;
            if (leak > 0) {
                ChunkRadiationManager.incrementRad(level, pos.getX(), pos.getY() + 1, pos.getZ(), leak);
            }
        }

        if (oldCoolant != coolantTank.getFill() || oldHot != coolantHotTank.getFill() || oldWaste != wasteTank.getFill()
                || oldHeat != heat || oldOn != isOn) {
            setChanged();
            sendUpdateToClient();
        }
    }

    /** Faithful port of {@code TileEntityWatz.updateReaction} for a single segment (no `above`/pellet-falling). */
    private void updateReaction() {
        if (!isOn) {
            fluxLastBase = 0D;
            fluxLastReaction = 0D;
            return;
        }

        double baseFlux = 0D;
        for (int i = 0; i < PELLET_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem() instanceof WatzPelletItem pellet) {
                baseFlux += pellet.getType().passiveFlux;
            }
        }

        double inputFlux = baseFlux + fluxLastReaction;
        double addedFlux = 0D;
        double addedHeat = 0D;

        for (int i = 0; i < PELLET_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!(stack.getItem() instanceof WatzPelletItem pellet)) continue;
            WatzPelletType type = pellet.getType();
            if (type.burnFunc == null) continue;

            double div = type.heatDiv != null ? type.heatDiv.applyAsDouble(heat) : 1D;
            double burn = type.burnFunc.applyAsDouble(inputFlux) / Math.max(1e-6, div);
            if (burn <= 0) continue;

            WatzPelletItem.setYield(stack, WatzPelletItem.getYield(stack) - burn);
            addedFlux += burn;
            addedHeat += type.heatEmission * burn;
            wasteTank.fillMb(wasteTank.getTankType(), (int) Math.round(type.mudContent * burn));
        }

        for (int i = 0; i < PELLET_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!(stack.getItem() instanceof WatzPelletItem pellet)) continue;
            WatzPelletType type = pellet.getType();
            if (type.absorbFunc == null) continue;

            double absorb = type.absorbFunc.applyAsDouble(baseFlux + fluxLastReaction);
            if (absorb <= 0) continue;

            addedHeat += absorb;
            WatzPelletItem.setYield(stack, WatzPelletItem.getYield(stack) - absorb);
            wasteTank.fillMb(wasteTank.getTankType(), (int) Math.round(type.mudContent * absorb));
        }

        heat += addedHeat;
        fluxLastBase = baseFlux;
        fluxLastReaction = addedFlux;

        // deplete
        for (int i = 0; i < PELLET_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem() instanceof WatzPelletItem pellet && WatzPelletItem.getEnrichment(stack) <= 0D) {
                inventory.setStackInSlot(i, new ItemStack(depletedVariantOf(pellet.getType())));
            }
        }
    }

    /** Faithful port of {@code TileEntityWatz.updateCoolant}, using a fixed-ratio heating step (see class doc). */
    private void updateCoolant() {
        double heatToUse = heat * COOLING_FACTOR;

        int heatCycles = (int) (heatToUse / COOLANT_HEAT_PER_MB);
        int coolCycles = coolantTank.getFill();
        int hotCycles = coolantHotTank.getMaxFill() - coolantHotTank.getFill();

        int cycles = Math.max(0, Math.min(heatCycles, Math.min(coolCycles, hotCycles)));
        if (cycles <= 0) return;

        heat -= cycles * COOLANT_HEAT_PER_MB;
        coolantTank.drainMb(cycles);
        coolantHotTank.fillMb(coolantHotTank.getTankType(), cycles);
    }

    /** Simplified meltdown - see class doc for what was dropped relative to the original. */
    private void checkWasteOverflow(Level level) {
        if (wasteTank.getFill() < wasteTank.getMaxFill()) return;
        isOn = false;
        wasteTank.drainMb(wasteTank.getFill() / 2);
        heat = Math.max(0D, heat * 0.5D);

        if (level instanceof ServerLevel serverLevel && !level.isClientSide()) {
            double x = worldPosition.getX() + 0.5D;
            double y = worldPosition.getY() + 1.0D;
            double z = worldPosition.getZ() + 0.5D;
            ExplosionNukeGeneric.incrementRad(level, x, y, z, 15F);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 40, 1.5D, 1.0D, 1.5D, 0.03D);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 4, 0.8D, 0.5D, 0.8D, 0.01D);
            level.explode(null, x, y, z, 3.0F, Level.ExplosionInteraction.BLOCK);
        }
    }

    private Item depletedVariantOf(WatzPelletType type) {
        return switch (type) {
            case SCHRABIDIUM_OXIDE -> ModItems.WATZ_PELLET_SCHRABIDIUM_OXIDE_DEPLETED.get();
            case LES_OXIDE -> ModItems.WATZ_PELLET_LES_OXIDE_DEPLETED.get();
            case NATURAL_URANIUM -> ModItems.WATZ_PELLET_NATURAL_URANIUM_DEPLETED.get();
            case BORON_CARBIDE -> ModItems.WATZ_PELLET_BORON_CARBIDE_DEPLETED.get();
            case LEAD_SHIELD -> ModItems.WATZ_PELLET_LEAD_SHIELD_DEPLETED.get();
        };
    }

    // ── GUI gauges ──────────────────────────────────────────────────────────

    public int getGaugeScaled(int scale, int type) {
        return switch (type) {
            case 0 -> (int) Math.min((long) coolantTank.getFill() * scale / COOLANT_MAX, scale);
            case 1 -> (int) Math.min((long) coolantHotTank.getFill() * scale / COOLANT_HOT_MAX, scale);
            case 2 -> (int) Math.min((long) wasteTank.getFill() * scale / WASTE_MAX, scale);
            case 3 -> (int) Math.min((long) (heat * scale / MAX_SAFE_HEAT), scale);
            default -> 0;
        };
    }

    // ── Button / redstone control ─────────────────────────────────────────

    public void handleButtonPress(int action) {
        if (action == 0 && !redstonePowered) isOn = !isOn;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setRedstonePowered(boolean powered) {
        if (!powered && redstonePowered) isOn = false;
        redstonePowered = powered;
        setChanged();
        sendUpdateToClient();
    }

    // ── IFluidStandardTransceiverMK2 ───────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return new FluidTank[]{ coolantTank, coolantHotTank, wasteTank }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[]{ coolantTank }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[]{ coolantHotTank, wasteTank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        if (fromDir != null) {
            if (fluidSidesFromMultiblockStructure && !allowedFluidSides.contains(fromDir)) return false;
            if (!fluidSidesFromMultiblockStructure && !allowedFluidSides.isEmpty() && !allowedFluidSides.contains(fromDir)) {
                return false;
            }
        }
        if (fluid == null || fluid == Fluids.EMPTY) return false;
        if (VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.COOLANT.getSource())) {
            return coolantTank.getFill() < coolantTank.getMaxFill();
        }
        if (VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.COOLANT_HOT.getSource())) {
            return coolantHotTank.getFill() > 0;
        }
        if (VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.WATZ.getSource())) {
            return wasteTank.getFill() > 0;
        }
        return false;
    }

    @Override
    public void setAllowedFluidSidesFromMultiblockStructure(Set<Direction> sides) {
        this.allowedFluidSides = EnumSet.copyOf(sides);
        this.fluidSidesFromMultiblockStructure = true;
        setChanged();
        sendUpdateToClient();
    }

    @Override
    public void setAllowedFluidSides(Set<Direction> sides) {
        this.allowedFluidSides = EnumSet.copyOf(sides);
        this.fluidSidesFromMultiblockStructure = false;
        setChanged();
        sendUpdateToClient();
    }

    @Override
    public Set<Direction> getAllowedFluidSides() {
        return this.allowedFluidSides;
    }

    // ── NBT ───────────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    @Override
    public void saveAdditional(CompoundTag tag) {
        CompoundTag ct = new CompoundTag(); coolantTank.writeToNBT(ct, "coolant"); tag.put("CoolantTank", ct);
        CompoundTag ht = new CompoundTag(); coolantHotTank.writeToNBT(ht, "coolantHot"); tag.put("CoolantHotTank", ht);
        CompoundTag wt = new CompoundTag(); wasteTank.writeToNBT(wt, "waste"); tag.put("WasteTank", wt);
        tag.putDouble("heat", heat);
        tag.putDouble("fluxLastBase", fluxLastBase);
        tag.putDouble("fluxLastReaction", fluxLastReaction);
        tag.putBoolean("isOn", isOn);
        tag.putBoolean("redstonePowered", redstonePowered);
    }
    //?} else {
    /*@Override
    public void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    CompoundTag ct = new CompoundTag(); coolantTank.writeToNBT(ct, "coolant"); tag.put("CoolantTank", ct);
    CompoundTag ht = new CompoundTag(); coolantHotTank.writeToNBT(ht, "coolantHot"); tag.put("CoolantHotTank", ht);
    CompoundTag wt = new CompoundTag(); wasteTank.writeToNBT(wt, "waste"); tag.put("WasteTank", wt);
    tag.putDouble("heat", heat);
    tag.putDouble("fluxLastBase", fluxLastBase);
    tag.putDouble("fluxLastReaction", fluxLastReaction);
    tag.putBoolean("isOn", isOn);
    tag.putBoolean("redstonePowered", redstonePowered);
    }
    *///?}

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        if (tag.contains("CoolantTank")) coolantTank.readFromNBT(tag.getCompound("CoolantTank"), "coolant");
        if (tag.contains("CoolantHotTank")) coolantHotTank.readFromNBT(tag.getCompound("CoolantHotTank"), "coolantHot");
        if (tag.contains("WasteTank")) wasteTank.readFromNBT(tag.getCompound("WasteTank"), "waste");
        heat = tag.getDouble("heat");
        fluxLastBase = tag.getDouble("fluxLastBase");
        fluxLastReaction = tag.getDouble("fluxLastReaction");
        isOn = tag.getBoolean("isOn");
        redstonePowered = tag.getBoolean("redstonePowered");
    }

    // ── Misc ──────────────────────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot >= 0 && slot < PELLET_SLOTS && stack.getItem() instanceof WatzPelletItem;
    }

    @Override protected Component getDefaultName() { return Component.translatable("container.hbm_m.watz_powerplant"); }
    @Override public Component getDisplayName() { return getDefaultName(); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineWatzPowerplantMenu.create(id, inv, this);
    }

    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (side != null) {
                if (fluidSidesFromMultiblockStructure && !allowedFluidSides.contains(side)) return LazyOptional.empty();
                if (!fluidSidesFromMultiblockStructure && !allowedFluidSides.isEmpty() && !allowedFluidSides.contains(side)) {
                    return LazyOptional.empty();
                }
            }
            return lazyFluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyFluidHandler.invalidate();
    }

    private static class UnifiedFluidHandler implements IFluidHandler {
        private final MachineWatzPowerplantBlockEntity be;

        UnifiedFluidHandler(MachineWatzPowerplantBlockEntity be) {
            this.be = be;
        }

        @Override public int getTanks() { return 3; }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> new net.minecraftforge.fluids.FluidStack(be.coolantTank.getTankType(), be.coolantTank.getFill());
                case 1 -> new net.minecraftforge.fluids.FluidStack(be.coolantHotTank.getTankType(), be.coolantHotTank.getFill());
                case 2 -> new net.minecraftforge.fluids.FluidStack(be.wasteTank.getTankType(), be.wasteTank.getFill());
                default -> net.minecraftforge.fluids.FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> be.coolantTank.getMaxFill();
                case 1 -> be.coolantHotTank.getMaxFill();
                case 2 -> be.wasteTank.getMaxFill();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack stack) {
            return tank == 0 && VanillaFluidEquivalence.sameSubstance(stack.getFluid(), ModFluids.COOLANT.getSource());
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;
            if (!VanillaFluidEquivalence.sameSubstance(resource.getFluid(), ModFluids.COOLANT.getSource())) return 0;
            int space = be.coolantTank.getMaxFill() - be.coolantTank.getFill();
            int toFill = Math.min(space, resource.getAmount());
            if (toFill <= 0) return 0;
            if (action.execute()) be.coolantTank.fillMb(ModFluids.COOLANT.getSource(), toFill);
            return toFill;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return net.minecraftforge.fluids.FluidStack.EMPTY;
            FluidTank source = pickDrainTank(resource.getFluid());
            if (source == null || source.getFill() <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            int toDrain = Math.min(resource.getAmount(), source.getFill());
            net.minecraftforge.fluids.FluidStack drained = new net.minecraftforge.fluids.FluidStack(source.getTankType(), toDrain);
            if (action.execute()) source.drainMb(toDrain);
            return drained;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            FluidTank source = be.coolantHotTank.getFill() > 0 ? be.coolantHotTank : be.wasteTank;
            if (maxDrain <= 0 || source.getFill() <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            int toDrain = Math.min(maxDrain, source.getFill());
            net.minecraftforge.fluids.FluidStack drained = new net.minecraftforge.fluids.FluidStack(source.getTankType(), toDrain);
            if (action.execute()) source.drainMb(toDrain);
            return drained;
        }

        private FluidTank pickDrainTank(Fluid fluid) {
            if (VanillaFluidEquivalence.sameSubstance(fluid, be.coolantHotTank.getTankType())) return be.coolantHotTank;
            if (VanillaFluidEquivalence.sameSubstance(fluid, be.wasteTank.getTankType())) return be.wasteTank;
            return null;
        }
    }
    //?}
}
