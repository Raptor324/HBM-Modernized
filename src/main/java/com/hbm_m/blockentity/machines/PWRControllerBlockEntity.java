package com.hbm_m.blockentity.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.redstoneoverradio.IRORInteractive;
import com.hbm_m.api.redstoneoverradio.IRORValueProvider;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.PWRPartBlockEntity.Kind;
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
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
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
 * PWR reactor core. 1:1 port of {@code com.hbm.tileentity.machine.TileEntityPWRController}
 * (1.7.10, 708 lines): a flood-fill-assembled structure of {@code pwr_fuel}/{@code pwr_control}/
 * {@code pwr_channel}/{@code pwr_heatex}/{@code pwr_heatsink}/{@code pwr_neutron_source} (core
 * blocks) bounded by {@code pwr_casing}/{@code pwr_reflector}/{@code pwr_port} (casing blocks).
 * Assembly, structure scanning, and part delegation live in
 * {@code MachinePWRControllerBlock}/{@link PWRPartBlockEntity} (via
 * {@link com.hbm_m.multiblock.IDummyCorePart} instead of the original's meta-carrier-block trick
 * - see {@link PWRPartBlockEntity}'s class doc for why that's the one non-behavioral deviation).
 * <p>
 * <b>Fuel:</b> {@link PWRFuelType}'s 15 archetypes are a 1:1 port of the original's
 * {@code EnumPWRFuel} curves/constants (see that class's doc).
 * <p>
 * <b>Dropped safeguard (not a mechanic):</b> the original's {@code unloadDelay} guard froze
 * production for 40 ticks after any of 5 chunks up to 2 chunks away (searching for distant fluid
 * sources) loaded, to avoid a startup glitch. Ports here are always physically adjacent to the
 * controller/parts (loaded together with this block entity by definition in 1.20), so that
 * specific race condition can't occur and the guard has no equivalent to port.
 * <p>
 * <b>Meltdown:</b> the original replaced every fuel rod position with a corium block and spawned
 * ~100 shrapnel entities. This port has no corium block/debris system yet (a separate, currently
 * unimplemented feature - the same TODO already exists on {@code RBMKRodBlockEntity}'s own
 * meltdown, so this isn't a PWR-specific gap) - meltdown here destroys the rod blocks, plus the
 * same radiation-spike + explosion treatment already used by {@code MachineZirnoxBlockEntity}/
 * {@code MachineWatzPowerplantBlockEntity} for comparable large reactors in this port.
 * <p>
 * <b>Redstone-over-Radio / comparator:</b> ported 1:1 ({@link #provideRORValue}/
 * {@link #runRORFunction}, {@link #getComparatorPower()} - see that method's doc for why it
 * implements the original's evident intent rather than its literal, dead-code condition).
 * <p>
 * <b>OpenComputers:</b> not ported, matching this port's project-wide convention of skipping OC
 * integration (OpenComputers isn't a dependency of this port at all - see e.g.
 * {@code MachineCapacitorBlockEntity}'s class doc for the same documented omission elsewhere).
 */
public class PWRControllerBlockEntity extends BaseMachineBlockEntity
        implements IFluidStandardTransceiverMK2, IRORValueProvider, IRORInteractive {

    public static final int SLOT_FUEL_IN = 0;
    public static final int SLOT_FUEL_OUT = 1;

    public static final long CORE_HEAT_CAPACITY_BASE = 10_000_000L;
    public static final long HULL_HEAT_CAPACITY_BASE = 10_000_000L;

    public static final int COOLANT_MAX = 128_000;
    public static final int COOLANT_HOT_MAX = 128_000;

    private final FluidTank coolantTank = new FluidTank(ModFluids.COOLANT.getSource(), COOLANT_MAX);
    private final FluidTank coolantHotTank = new FluidTank(ModFluids.COOLANT_HOT.getSource(), COOLANT_HOT_MAX);

    public boolean assembled = false;

    public long coreHeat;
    public long hullHeat;
    public long coreHeatCapacity = CORE_HEAT_CAPACITY_BASE;
    public double flux;
    public double rodLevel = 100D;
    public double rodTarget = 100D;

    @Nullable public PWRFuelType typeLoaded;
    public int amountLoaded;
    public double progress;
    public double processTime;

    public int rodCount;
    public int connections;
    public int connectionsControlled;
    public int heatexCount;
    public int channelCount;
    public int heatsinkCount;
    public int sourceCount;

    private final List<BlockPos> ports = new ArrayList<>();
    private final List<BlockPos> rods = new ArrayList<>();

    public PWRControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PWR_CONTROLLER_BE.get(), pos, state, 2, 0L, 0L);
    }

    // ── Assembly ────────────────────────────────────────────────────────────

    public void setAssembled(boolean assembled) {
        if (this.assembled != assembled) {
            this.assembled = assembled;
            setChanged();
            sendUpdateToClient();
        }
    }

    public List<BlockPos> getPorts() { return ports; }
    public List<BlockPos> getRods() { return rods; }

    /** 1:1 port of {@code TileEntityPWRController.setup}. */
    public void setup(Map<BlockPos, Kind> partMap) {
        rodCount = 0;
        heatexCount = 0;
        channelCount = 0;
        heatsinkCount = 0;
        sourceCount = 0;
        ports.clear();
        rods.clear();

        for (Map.Entry<BlockPos, Kind> entry : partMap.entrySet()) {
            switch (entry.getValue()) {
                case FUEL -> { rodCount++; rods.add(entry.getKey()); }
                case HEATEX -> heatexCount++;
                case CHANNEL -> channelCount++;
                case HEATSINK -> heatsinkCount++;
                case NEUTRON_SOURCE -> sourceCount++;
                case PORT -> ports.add(entry.getKey());
                default -> { /* control/casing/reflector: no counter, but do affect connection scan below */ }
            }
        }

        int connectionsDouble = 0;
        int connectionsControlledDouble = 0;

        for (BlockPos fuelPos : rods) {
            for (Direction dir : Direction.values()) {
                boolean controlled = false;
                for (int i = 1; i < 16; i++) {
                    BlockPos checkPos = fuelPos.relative(dir, i);
                    Kind atPos = partMap.get(checkPos);
                    if (atPos == null || atPos == Kind.CASING) break;
                    if (atPos == Kind.CONTROL) controlled = true;
                    if (atPos == Kind.FUEL) {
                        if (controlled) connectionsControlledDouble++; else connectionsDouble++;
                        break;
                    }
                    if (atPos == Kind.REFLECTOR) {
                        if (controlled) connectionsControlledDouble += 2; else connectionsDouble += 2;
                        break;
                    }
                }
            }
        }

        connections = connectionsDouble / 2;
        connectionsControlled = connectionsControlledDouble / 2;
        heatsinkCount = Math.min(heatsinkCount, 80);

        coreHeatCapacity = CORE_HEAT_CAPACITY_BASE + heatsinkCount * (CORE_HEAT_CAPACITY_BASE / 20);
    }

    // ── Tick ────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, PWRControllerBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        be.serverTick(serverLevel, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        if (!assembled) return;

        if (level.getGameTime() % 20 == 0) {
            for (BlockPos portPos : ports) {
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = portPos.relative(dir);
                    if (coolantHotTank.getFill() > 0) tryProvide(coolantHotTank, level, neighbor, dir);
                    trySubscribe(coolantTank.getTankType(), level, neighbor, dir);
                }
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

        int newFlux = sourceCount * 20;

        if (typeLoaded != null && amountLoaded > 0) {
            double usedRods = getTotalProcessMultiplier();
            double fluxPerRod = flux / rodCount;
            double outputPerRod = typeLoaded.burnFunc.applyAsDouble(fluxPerRod);
            double totalOutput = outputPerRod * amountLoaded * usedRods;
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
            }

            if (amountLoaded <= 0) typeLoaded = null;
            if (amountLoaded > rodCount) amountLoaded = rodCount;
        }

        double coreCoolingApproachNum = getXOverE((double) heatexCount * 5D / getRodCountForCoolant(), 2D) / 2D;
        long averageHeat = (coreHeat + hullHeat) / 2;
        coreHeat -= Math.round((coreHeat - averageHeat) * coreCoolingApproachNum);
        hullHeat -= Math.round((hullHeat - averageHeat) * coreCoolingApproachNum);

        updateCoolant();

        coreHeat = Math.round(coreHeat * 0.999D);
        hullHeat = Math.round(hullHeat * 0.999D);

        flux = coolantTank.getFill() > 0 ? newFlux * moderatorMultiplier : newFlux;

        if (coreHeat > coreHeatCapacity) {
            meltDown(level);
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
        } else if (fuel.getType() == typeLoaded && amountLoaded < rodCount) {
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
            case HEU233 -> ModItems.PWR_FUEL_HEU233_HOT.get();
            case HEU235 -> ModItems.PWR_FUEL_HEU235_HOT.get();
            case MEN -> ModItems.PWR_FUEL_MEN_HOT.get();
            case HEN237 -> ModItems.PWR_FUEL_HEN237_HOT.get();
            case MOX -> ModItems.PWR_FUEL_MOX_HOT.get();
            case MEP -> ModItems.PWR_FUEL_MEP_HOT.get();
            case HEP239 -> ModItems.PWR_FUEL_HEP239_HOT.get();
            case HEP241 -> ModItems.PWR_FUEL_HEP241_HOT.get();
            case MEA -> ModItems.PWR_FUEL_MEA_HOT.get();
            case HEA242 -> ModItems.PWR_FUEL_HEA242_HOT.get();
            case HES326 -> ModItems.PWR_FUEL_HES326_HOT.get();
            case HES327 -> ModItems.PWR_FUEL_HES327_HOT.get();
            case BFB_AM_MIX -> ModItems.PWR_FUEL_BFB_AM_MIX_HOT.get();
            case BFB_PU241 -> ModItems.PWR_FUEL_BFB_PU241_HOT.get();
        };
    }

    /** 1:1 port of {@code TileEntityPWRController.getTotalProcessMultiplier}. */
    public double getTotalProcessMultiplier() {
        double totalConnections = connections + connectionsControlled * (1D - (rodLevel / 100D));
        return connectinFunc(totalConnections);
    }

    private double connectinFunc(double c) {
        double x = getXOverE(c, 300D);
        return c / 10D * (1D - x) + c / 150D * x;
    }

    private double getXOverE(double x, double d) {
        return 1D - Math.pow(Math.E, -x / d);
    }

    private int getRodCountForCoolant() {
        return rodCount + (int) Math.ceil(heatsinkCount / 4D);
    }

    private void updateCoolant() {
        FT_Heatable trait = FluidType.getTrait(coolantTank.getStoredFluid(), FT_Heatable.class);
        if (trait == null || trait.getEfficiency(HeatingType.PWR) <= 0) return;

        double coolingEff = Math.min(1D, (double) channelCount / getRodCountForCoolant() * 0.1D);

        HeatingStep step = trait.getFirstStep();
        if (step == null) return;

        long heatToUse = Math.min(Math.min(hullHeat, (long) (hullHeat * coolingEff * trait.getEfficiency(HeatingType.PWR))), 2_000_000_000L);
        int coolCycles = coolantTank.getFill() / step.amountReq;
        int hotCycles = (coolantHotTank.getCapacityMb() - coolantHotTank.getFluidAmountMb()) / step.amountProduced;
        long heatCycles = step.heatReq > 0 ? heatToUse / step.heatReq : 0;

        long cycles = Math.max(0, Math.min(coolCycles, Math.min(hotCycles, heatCycles)));
        if (cycles <= 0) return;

        hullHeat -= step.heatReq * cycles;
        coolantTank.drainMb((int) (step.amountReq * cycles));
        coolantHotTank.fillMb(step.typeProduced, (int) (step.amountProduced * cycles));
    }

    private void meltDown(ServerLevel level) {
        for (BlockPos rodPos : rods) {
            level.destroyBlock(rodPos, false);
        }

        typeLoaded = null;
        amountLoaded = 0;
        progress = 0;
        coreHeat = 0;
        hullHeat = 0;
        assembled = false;

        double x = 0, y = 0, z = 0;
        int n = Math.max(1, rods.size());
        for (BlockPos rodPos : rods) {
            x += rodPos.getX() + 0.5D;
            y += rodPos.getY() + 0.5D;
            z += rodPos.getZ() + 0.5D;
        }
        if (rods.isEmpty()) {
            x = worldPosition.getX() + 0.5D;
            y = worldPosition.getY() + 0.5D;
            z = worldPosition.getZ() + 0.5D;
        } else {
            x /= n; y /= n; z /= n;
        }

        ExplosionNukeGeneric.incrementRad(level, x, y, z, 15F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 40, 1.5D, 1.0D, 1.5D, 0.03D);
        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 4, 0.8D, 0.5D, 0.8D, 0.01D);
        level.explode(null, x, y, z, 15.0F, Level.ExplosionInteraction.BLOCK);
    }

    // ── GUI gauges ──────────────────────────────────────────────────────────

    public int getGaugeScaled(int scale, int type) {
        return switch (type) {
            case 0 -> (int) Math.min((long) coolantTank.getFill() * scale / COOLANT_MAX, scale);
            case 1 -> (int) Math.min((long) coolantHotTank.getFill() * scale / COOLANT_HOT_MAX, scale);
            case 2 -> (int) Math.min(coreHeat * scale / coreHeatCapacity, scale);
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

    // ── Comparator ──────────────────────────────────────────────────────────

    /**
     * The original's {@code getComparatorInputOverride} gated this behind a metadata check
     * ({@code meta >= 6}) that can never be true for the controller's own block position (its
     * metadata only ever holds its facing, 2-5) - i.e. the override was effectively dead code in
     * the original. This port implements the evident intent instead (a comparator readout of the
     * reactor), using the hot-coolant tank fill the same way {@code TileEntityBarrel} exposes its
     * own fill percentage.
     */
    public int getComparatorPower() {
        if (coolantHotTank.getFill() == 0) return 0;
        double frac = (double) coolantHotTank.getFill() / coolantHotTank.getCapacityMb() * 15D;
        return (int) Math.max(0, Math.min(15, Math.round(frac) + 1));
    }

    // ── Redstone-over-Radio ─────────────────────────────────────────────────

    public static final String[] ROR = new String[] {
        PREFIX_VALUE + "rods",
        PREFIX_VALUE + "coreheat",
        PREFIX_VALUE + "hullheat",
        PREFIX_VALUE + "coldbuf",
        PREFIX_VALUE + "hotbuf",
        PREFIX_VALUE + "flux",
        PREFIX_VALUE + "depletion",
        PREFIX_FUNCTION + "setrods" + NAME_SEPARATOR + "percent",
        PREFIX_FUNCTION + "jettison",
    };

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "rods").equals(name)) return "" + (int) (100 - rodLevel);
        if ((PREFIX_VALUE + "coreheat").equals(name)) return "" + coreHeat;
        if ((PREFIX_VALUE + "hullheat").equals(name)) return "" + hullHeat;
        if ((PREFIX_VALUE + "coldbuf").equals(name)) return "" + coolantTank.getFill();
        if ((PREFIX_VALUE + "hotbuf").equals(name)) return "" + coolantHotTank.getFill();
        if ((PREFIX_VALUE + "flux").equals(name)) return "" + (int) flux;
        if ((PREFIX_VALUE + "depletion").equals(name)) return "" + (int) (processTime > 0 ? progress * 100 / processTime : 0);
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setrods").equals(name) && params.length > 0) {
            int percent = IRORInteractive.parseInt(params[0], 0, 100);
            setRodTarget(percent);
            return null;
        }
        if ((PREFIX_FUNCTION + "jettison").equals(name)) {
            typeLoaded = null;
            amountLoaded = 0;
            progress = 0;
            setChanged();
            sendUpdateToClient();
            return null;
        }
        return null;
    }

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
        tag.putBoolean("assembled", assembled);
        tag.putLong("coreHeat", coreHeat);
        tag.putLong("hullHeat", hullHeat);
        tag.putLong("coreHeatCapacity", coreHeatCapacity);
        tag.putDouble("flux", flux);
        tag.putDouble("rodLevel", rodLevel);
        tag.putDouble("rodTarget", rodTarget);
        if (typeLoaded != null) tag.putString("typeLoaded", typeLoaded.name());
        tag.putInt("amountLoaded", amountLoaded);
        tag.putDouble("progress", progress);
        tag.putDouble("processTime", processTime);

        tag.putInt("rodCount", rodCount);
        tag.putInt("connections", connections);
        tag.putInt("connectionsControlled", connectionsControlled);
        tag.putInt("heatexCount", heatexCount);
        tag.putInt("channelCount", channelCount);
        tag.putInt("heatsinkCount", heatsinkCount);
        tag.putInt("sourceCount", sourceCount);

        tag.putInt("portCount", ports.size());
        for (int i = 0; i < ports.size(); i++) {
            tag.put("port" + i, NbtUtils.writeBlockPos(ports.get(i)));
        }
        tag.putInt("rodPosCount", rods.size());
        for (int i = 0; i < rods.size(); i++) {
            tag.put("rodPos" + i, NbtUtils.writeBlockPos(rods.get(i)));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        coolantTank.readFromNBT(tag, "tank_coolant");
        coolantHotTank.readFromNBT(tag, "tank_coolant_hot");
        assembled = tag.getBoolean("assembled");
        coreHeat = tag.getLong("coreHeat");
        hullHeat = tag.getLong("hullHeat");
        coreHeatCapacity = tag.contains("coreHeatCapacity") ? tag.getLong("coreHeatCapacity") : CORE_HEAT_CAPACITY_BASE;
        flux = tag.getDouble("flux");
        rodLevel = tag.getDouble("rodLevel");
        rodTarget = tag.getDouble("rodTarget");
        typeLoaded = tag.contains("typeLoaded") ? PWRFuelType.valueOf(tag.getString("typeLoaded")) : null;
        amountLoaded = tag.getInt("amountLoaded");
        progress = tag.getDouble("progress");
        processTime = tag.getDouble("processTime");

        rodCount = tag.getInt("rodCount");
        connections = tag.getInt("connections");
        connectionsControlled = tag.getInt("connectionsControlled");
        heatexCount = tag.getInt("heatexCount");
        channelCount = tag.getInt("channelCount");
        heatsinkCount = tag.getInt("heatsinkCount");
        sourceCount = tag.getInt("sourceCount");

        ports.clear();
        int portCount = tag.getInt("portCount");
        for (int i = 0; i < portCount; i++) {
            if (tag.contains("port" + i)) ports.add(NbtUtils.readBlockPos(tag.getCompound("port" + i)));
        }
        rods.clear();
        int rodPosCount = tag.getInt("rodPosCount");
        for (int i = 0; i < rodPosCount; i++) {
            if (tag.contains("rodPos" + i)) rods.add(NbtUtils.readBlockPos(tag.getCompound("rodPos" + i)));
        }
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
