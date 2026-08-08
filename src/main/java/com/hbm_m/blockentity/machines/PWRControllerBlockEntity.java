package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Heatable;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm_m.inventory.fluid.trait.FT_PWRModerator;
import com.hbm_m.inventory.menu.PWRControllerMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.nuclear.PWRFuelItem;
import com.hbm_m.item.nuclear.PWRFuelType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * PWR reactor core. Ported from {@code com.hbm.tileentity.machine.TileEntityPWRController}
 * (1.7.10, 708 lines), following this port's established simplification for the big reactor
 * multiblocks (see {@code MachineWatzPowerplantBlockEntity}'s class doc): implemented as a single
 * block instead of a flood-fill-assembled structure of {@code pwr_fuel}/{@code pwr_control}/
 * {@code pwr_heatex}/etc. blocks with a shared {@code TileEntityBlockPWR} part delegate.
 * <p>
 * <b>SCOPE:</b> the original scaled fuel capacity, flux amplification, and cooling rate from the
 * physically-assembled structure (rod count, reflector/control-rod "connections", heat exchanger/
 * heat sink counts). Since there is no structure to measure here, those are replaced with fixed
 * constants ({@link #ROD_CAPACITY}, {@link #CORE_TO_HULL_RATE}) and {@link #rodLevel} directly
 * scales output ({@link #getControlMultiplier()}) instead of the original's connection-based
 * {@code connectinFunc}. The other 8 {@code pwr_*} blocks (casing/fuel/control/channel/heatex/
 * heatsink/neutron_source/reflector/port) remain purely decorative, matching how this port's
 * Watz reactor leaves its non-controller blocks decorative too.
 * <p>
 * <b>Fuel:</b> the original's 15-variant {@code EnumPWRFuel} (driven by the {@code Function}
 * algebra library dropped for {@code WatzPelletType}) is replaced by {@link PWRFuelType}'s 5
 * representative archetypes, matching this port's established convention.
 * <p>
 * <b>Coolant:</b> unlike Watz (fixed-ratio simplification), this uses the real
 * {@code FT_Heatable}/{@code HeatingType.PWR} fluid trait exactly like the original, since that
 * trait is already fully populated for the coolant fluid family in this port.
 * <p>
 * <b>Meltdown:</b> simplified the same way as {@code MachineZirnoxBlockEntity}/
 * {@code MachineWatzPowerplantBlockEntity} - production stops, a radiation spike and a modest
 * block-damage explosion are triggered, no corium blocks or shrapnel entities.
 */
public class PWRControllerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int SLOT_FUEL_IN = 0;
    public static final int SLOT_FUEL_OUT = 1;

    /** Fixed cap on loaded fuel rods, replacing the original's structurally-measured rod count. */
    public static final int ROD_CAPACITY = 12;
    public static final long CORE_HEAT_CAPACITY = 10_000_000L;
    /** Fixed core<->hull heat exchange rate, replacing the original's heat-exchanger-count-based ratio. */
    private static final double CORE_TO_HULL_RATE = 0.5D;
    /** Baseline neutron flux, replacing the original's structurally-counted neutron source blocks. */
    private static final double BASELINE_FLUX = 20D;

    public static final int COOLANT_MAX = 128_000;
    public static final int COOLANT_HOT_MAX = 128_000;

    private final FluidTank coolantTank = new FluidTank(ModFluids.COOLANT.getSource(), COOLANT_MAX);
    private final FluidTank coolantHotTank = new FluidTank(ModFluids.COOLANT_HOT.getSource(), COOLANT_HOT_MAX);

    public long coreHeat;
    public long hullHeat;
    public double flux;
    public double rodLevel = 100D;
    public double rodTarget = 100D;

    @Nullable public PWRFuelType typeLoaded;
    public int amountLoaded;
    public double progress;
    public double processTime;

    public PWRControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PWR_CONTROLLER_BE.get(), pos, state, 2, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PWRControllerBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        be.serverTick(serverLevel, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (coolantHotTank.getFill() > 0) tryProvide(coolantHotTank, level, neighbor, dir);
                trySubscribe(coolantTank.getTankType(), level, neighbor, dir);
            }
        }

        loadFuel();

        double diff = rodLevel - rodTarget;
        if (diff < 1D && diff > -1D) rodLevel = rodTarget;
        else if (rodTarget > rodLevel) rodLevel++;
        else if (rodTarget < rodLevel) rodLevel--;

        double moderatorMultiplier = 1D;
        FT_PWRModerator moderator = FluidType.getTrait(coolantTank.getStoredFluid(), FT_PWRModerator.class);
        if (moderator != null) {
            moderatorMultiplier = moderator.getMultiplier();
        }

        double newFlux = BASELINE_FLUX;

        if (typeLoaded != null && amountLoaded > 0) {
            double fluxPerRod = flux / ROD_CAPACITY;
            double outputPerRod = typeLoaded.burnFunc.applyAsDouble(fluxPerRod);
            double totalOutput = outputPerRod * amountLoaded * getControlMultiplier();
            double totalHeatOutput = totalOutput * typeLoaded.heatEmission;
            if (coolantTank.getFill() > 0) totalHeatOutput *= moderatorMultiplier;

            coreHeat += Math.round(totalHeatOutput);
            newFlux += totalOutput;

            processTime = typeLoaded.yield;
            progress += totalOutput;

            if (progress >= processTime) {
                progress -= processTime;
                produceHotFuel(typeLoaded);
                amountLoaded--;
                if (amountLoaded <= 0) typeLoaded = null;
            }
        }

        // core<->hull heat exchange
        long averageHeat = (coreHeat + hullHeat) / 2;
        coreHeat -= Math.round((coreHeat - averageHeat) * CORE_TO_HULL_RATE);
        hullHeat -= Math.round((hullHeat - averageHeat) * CORE_TO_HULL_RATE);

        updateCoolant();

        coreHeat = Math.round(coreHeat * 0.999D);
        hullHeat = Math.round(hullHeat * 0.999D);

        flux = coolantTank.getFill() > 0 ? newFlux * moderatorMultiplier : newFlux;

        if (coreHeat > CORE_HEAT_CAPACITY) {
            meltDown(level, pos);
        }

        setChanged();
        sendUpdateToClient();
    }

    private void loadFuel() {
        ItemStack slot0 = getInventory().getStackInSlot(SLOT_FUEL_IN);
        if (!(slot0.getItem() instanceof PWRFuelItem fuel)) return;

        if (typeLoaded == null && amountLoaded <= 0) {
            typeLoaded = fuel.getType();
            amountLoaded++;
            slot0.shrink(1);
            setChanged();
        } else if (fuel.getType() == typeLoaded && amountLoaded < ROD_CAPACITY) {
            amountLoaded++;
            slot0.shrink(1);
            setChanged();
        }
    }

    private void produceHotFuel(PWRFuelType type) {
        Item hot = hotFuelItemFor(type);
        ItemStack slot1 = getInventory().getStackInSlot(SLOT_FUEL_OUT);
        if (slot1.isEmpty()) {
            getInventory().setStackInSlot(SLOT_FUEL_OUT, new ItemStack(hot));
        } else if (slot1.getItem() == hot && slot1.getCount() < slot1.getMaxStackSize()) {
            slot1.grow(1);
        }
    }

    private static Item hotFuelItemFor(PWRFuelType type) {
        return switch (type) {
            case MEU -> ModItems.PWR_FUEL_MEU_HOT.get();
            case HEU -> ModItems.PWR_FUEL_HEU_HOT.get();
            case MOX -> ModItems.PWR_FUEL_MOX_HOT.get();
            case HEP -> ModItems.PWR_FUEL_HEP_HOT.get();
            case SCHRABIDIUM -> ModItems.PWR_FUEL_SCHRABIDIUM_HOT.get();
        };
    }

    /** Replaces the original's connection-based {@code connectinFunc}: rods fully inserted suppress 95% of output. */
    public double getControlMultiplier() {
        return 1D - (rodLevel / 100D) * 0.95D;
    }

    private void updateCoolant() {
        FT_Heatable trait = FluidType.getTrait(coolantTank.getStoredFluid(), FT_Heatable.class);
        if (trait == null || trait.getEfficiency(HeatingType.PWR) <= 0) return;

        HeatingStep step = trait.getFirstStep();
        if (step == null) return;

        long heatToUse = Math.min(hullHeat, (long) (hullHeat * trait.getEfficiency(HeatingType.PWR)));
        int coolCycles = coolantTank.getFill() / step.amountReq;
        int hotCycles = (coolantHotTank.getCapacityMb() - coolantHotTank.getFluidAmountMb()) / step.amountProduced;
        long heatCycles = step.heatReq > 0 ? heatToUse / step.heatReq : 0;

        long cycles = Math.max(0, Math.min(coolCycles, Math.min(hotCycles, heatCycles)));
        if (cycles <= 0) return;

        hullHeat -= step.heatReq * cycles;
        coolantTank.drainMb((int) (step.amountReq * cycles));
        coolantHotTank.fillMb(step.typeProduced, (int) (step.amountProduced * cycles));
    }

    private void meltDown(ServerLevel level, BlockPos pos) {
        typeLoaded = null;
        amountLoaded = 0;
        progress = 0;
        coreHeat = 0;
        hullHeat = 0;

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        ExplosionNukeGeneric.incrementRad(level, x, y, z, 15F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 40, 1.5D, 1.0D, 1.5D, 0.03D);
        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 4, 0.8D, 0.5D, 0.8D, 0.01D);
        level.explode(null, x, y, z, 4.0F, Level.ExplosionInteraction.BLOCK);
    }

    // ── GUI gauges ──────────────────────────────────────────────────────────

    public int getGaugeScaled(int scale, int type) {
        return switch (type) {
            case 0 -> (int) Math.min((long) coolantTank.getFill() * scale / COOLANT_MAX, scale);
            case 1 -> (int) Math.min((long) coolantHotTank.getFill() * scale / COOLANT_HOT_MAX, scale);
            case 2 -> (int) Math.min(coreHeat * scale / CORE_HEAT_CAPACITY, scale);
            default -> 0;
        };
    }

    public void setRodTarget(double target) {
        rodTarget = Math.max(0D, Math.min(100D, target));
        setChanged();
        sendUpdateToClient();
    }

    public FluidTank getCoolantTank() { return coolantTank; }
    public FluidTank getCoolantHotTank() { return coolantHotTank; }

    // ── IFluidStandardTransceiverMK2 ───────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return new FluidTank[]{ coolantTank, coolantHotTank }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[]{ coolantTank }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[]{ coolantHotTank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    // ── NBT ───────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        coolantTank.writeToNBT(tag, "tank_coolant");
        coolantHotTank.writeToNBT(tag, "tank_coolant_hot");
        tag.putLong("coreHeat", coreHeat);
        tag.putLong("hullHeat", hullHeat);
        tag.putDouble("flux", flux);
        tag.putDouble("rodLevel", rodLevel);
        tag.putDouble("rodTarget", rodTarget);
        if (typeLoaded != null) tag.putString("typeLoaded", typeLoaded.name());
        tag.putInt("amountLoaded", amountLoaded);
        tag.putDouble("progress", progress);
        tag.putDouble("processTime", processTime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        coolantTank.readFromNBT(tag, "tank_coolant");
        coolantHotTank.readFromNBT(tag, "tank_coolant_hot");
        coreHeat = tag.getLong("coreHeat");
        hullHeat = tag.getLong("hullHeat");
        flux = tag.getDouble("flux");
        rodLevel = tag.getDouble("rodLevel");
        rodTarget = tag.getDouble("rodTarget");
        typeLoaded = tag.contains("typeLoaded") ? PWRFuelType.valueOf(tag.getString("typeLoaded")) : null;
        amountLoaded = tag.getInt("amountLoaded");
        progress = tag.getDouble("progress");
        processTime = tag.getDouble("processTime");
    }

    // ── Misc ──────────────────────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_FUEL_IN && stack.getItem() instanceof PWRFuelItem;
    }

    @Override protected Component getDefaultName() { return Component.translatable("container.hbm_m.pwr_controller"); }
    @Override public Component getDisplayName() { return getDefaultName(); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return PWRControllerMenu.create(id, inv, this);
    }
}
