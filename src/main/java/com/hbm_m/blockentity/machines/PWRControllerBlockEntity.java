package com.hbm_m.blockentity.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.redstoneoverradio.IRORInteractive;
import com.hbm_m.api.redstoneoverradio.IRORValueProvider;
import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.block.ModBlocks;
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
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import com.hbm_m.platform.BlockHooks;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
//?}

/**
 * 1:1 {@code TileEntityPWRController}. Der Aufbau wird von {@code MachinePWRControllerBlock} geflutet und in Traeger
 * ({@code pwr_block}) verwandelt; {@link #setup} zaehlt Bauteile und Brennstab-Verbindungen. Ueberhitzt der Kern,
 * werden alle Brennstaebe zu Corium und der Reaktor explodiert.
 */
public class PWRControllerBlockEntity extends BaseMachineBlockEntity
        implements IFluidStandardTransceiverMK2, IControlReceiver, IRORValueProvider, IRORInteractive {

    public static final int SLOT_FUEL_IN = 0;
    public static final int SLOT_FUEL_OUT = 1;
    public static final int SLOT_FLUID_ID = 2;
    public static final int SLOT_COUNT = 3;

    public static final long CORE_HEAT_CAPACITY_BASE = 10_000_000L;
    public static final long HULL_HEAT_CAPACITY_BASE = 10_000_000L;
    public static final long coreHeatCapacityBase = CORE_HEAT_CAPACITY_BASE;
    public static final long hullHeatCapacityBase = HULL_HEAT_CAPACITY_BASE;

    public static final int COOLANT_MAX = 128_000;
    public static final int COOLANT_HOT_MAX = 128_000;

    public final FluidTank[] tanks;
    public final FluidTank coolantTank;
    public final FluidTank coolantHotTank;

    public boolean assembled = false;

    public long coreHeat;
    public long hullHeat;
    public long coreHeatCapacity = CORE_HEAT_CAPACITY_BASE;
    public double flux;
    public double rodLevel = 100D;
    public double rodTarget = 100D;

    public int typeLoaded = -1;
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

    public int unloadDelay = 0;

    protected final List<BlockPos> ports = new ArrayList<>();
    protected final List<BlockPos> rods = new ArrayList<>();

    /** Last signature whose state delta triggered setChanged() (anti-sync-spam gate). */
    private long lastSyncedStateSig = Long.MIN_VALUE;

    //? if forge {
    private final LazyOptional<IItemHandler> automationHandler;
    //?} elif neoforge {
    /*private final com.hbm_m.platform.LazyCap<net.minecraftforge.items.IItemHandler> automationHandler;
    *///?}

    public PWRControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PWR_CONTROLLER_BE.get(), pos, state, SLOT_COUNT, 0L, 0L);
        this.tanks = new FluidTank[] {
                new FluidTank(ModFluids.COOLANT.getSource(), COOLANT_MAX),
                new FluidTank(ModFluids.COOLANT_HOT.getSource(), COOLANT_HOT_MAX)
        };
        this.coolantTank = this.tanks[0];
        this.coolantHotTank = this.tanks[1];
        //? if forge {
        this.automationHandler = LazyOptional.of(() -> new AutomationHandler(inventory));
        //?} elif neoforge {
        /*this.automationHandler = com.hbm_m.platform.LazyCap.of(() -> new AutomationHandler(inventory));
        *///?}
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

    /** Einrichtung des Reaktors beim Zusammenbau: zaehlt Bauteile und berechnet die Brennstab-Verbindungen. */
    public void setup(Map<BlockPos, Kind> partMap) {
        rodCount = 0;
        connections = 0;
        connectionsControlled = 0;
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
                    Kind atKind = partMap.get(checkPos);
                    if (atKind == null || atKind == Kind.CASING || atKind == Kind.PORT) break;
                    if (atKind == Kind.CONTROL) controlled = true;
                    if (atKind == Kind.FUEL) {
                        if (controlled) connectionsControlledDouble++; else connectionsDouble++;
                        break;
                    }
                    if (atKind == Kind.REFLECTOR) {
                        if (controlled) connectionsControlledDouble += 2; else connectionsDouble += 2;
                        break;
                    }
                }
            }
        }

        connections = connectionsDouble / 2;
        connectionsControlled = connectionsControlledDouble / 2;
        heatsinkCount = Math.min(heatsinkCount, 80);

        this.coreHeatCapacity = CORE_HEAT_CAPACITY_BASE + this.heatsinkCount * (CORE_HEAT_CAPACITY_BASE / 20);
        setChanged();
    }

    // ── Tick ────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, PWRControllerBlockEntity be) {
        if (!level.isClientSide) {
            if (level instanceof ServerLevel serverLevel) {
                be.serverTick(serverLevel, pos);
            }
        } else {
            com.hbm_m.sound.ClientSoundBootstrap.updateSound(be, be.amountLoaded > 0, be::createAudioLoop);
        }
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        ItemStack[] slots = { inventory.getStackInSlot(0), inventory.getStackInSlot(1), inventory.getStackInSlot(2) };
        this.tanks[0].setType(2, slots);
        setupTanks();

        if (this.assembled) {
            boolean subscribeDue = level.getGameTime() % 20 == 0;
            for (BlockPos portPos : ports) {
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = portPos.relative(dir);
                    if (tanks[1].getFill() > 0) tryProvide(tanks[1], level, neighbor, dir);
                    if (subscribeDue) trySubscribe(tanks[0].getTankType(), level, neighbor, dir);
                }
            }

            loadFuel();

            double diff = this.rodLevel - this.rodTarget;
            if (diff < 1 && diff > -1) this.rodLevel = this.rodTarget;
            else if (this.rodTarget > this.rodLevel) this.rodLevel++;
            else if (this.rodTarget < this.rodLevel) this.rodLevel--;

            double moderatorMultiplier = 1D;
            FT_PWRModerator moderator = FluidType.getTrait(tanks[0].getTankType(), FT_PWRModerator.class);
            if (moderator != null) {
                moderatorMultiplier = moderator.getMultiplier();
            }

            int newFlux = this.sourceCount * 20;

            if (typeLoaded >= 0 && amountLoaded > 0) {
                PWRFuelType fuel = fuelType(typeLoaded);
                double usedRods = getTotalProcessMultiplier();
                double fluxPerRod = this.rodCount > 0 ? this.flux / this.rodCount : 0D;
                double outputPerRod = fuel.function != null ? fuel.function.effonix(fluxPerRod) : 0D;
                double totalOutput = outputPerRod * amountLoaded * usedRods;
                double totalHeatOutput = totalOutput * fuel.heatEmission;
                if (tanks[0].getFill() > 0) totalHeatOutput *= moderatorMultiplier;

                this.coreHeat += Math.round(totalHeatOutput);
                newFlux += totalOutput;

                this.processTime = fuel.yield;
                this.progress += totalOutput;

                if (this.progress >= this.processTime) {
                    this.progress -= this.processTime;
                    produceHotFuel(fuel);
                    this.amountLoaded--;
                    setChanged();
                }

                if (level.getGameTime() % 100 == 0) {
                    com.hbm_m.satellite.RayScanEvents.reportEvent(level, worldPosition, com.hbm_m.satellite.RayScanEvents.INFO_NUCLEAR, 200);
                }

                if (this.amountLoaded <= 0) this.typeLoaded = -1;
                if (this.amountLoaded > rodCount) this.amountLoaded = rodCount;
            }

            double coreCoolingApproachNum = getXOverE((double) this.heatexCount * 5D / getRodCountForCoolant(), 2D) / 2D;
            long averageHeat = (this.coreHeat + this.hullHeat) / 2;
            this.coreHeat -= Math.round((this.coreHeat - averageHeat) * coreCoolingApproachNum);
            this.hullHeat -= Math.round((this.hullHeat - averageHeat) * coreCoolingApproachNum);

            updateCoolant();

            this.coreHeat = Math.round(this.coreHeat * 0.999D);
            this.hullHeat = Math.round(this.hullHeat * 0.999D);

            this.flux = tanks[0].getFill() > 0 ? newFlux * moderatorMultiplier : newFlux;

            if (this.coreHeat > this.coreHeatCapacity) {
                meltDown(level);
                return;
            }
        }

        long stateSig = com.hbm_m.recipe.index.CachedRecipeResolver.sign(1L,
                Double.doubleToRawLongBits(rodLevel),
                Double.doubleToRawLongBits(rodTarget),
                Double.doubleToRawLongBits(flux),
                coreHeat >>> 10, hullHeat >>> 10,
                tanks[0].getFill(), tanks[1].getFill(),
                typeLoaded,
                amountLoaded, Double.doubleToRawLongBits(progress));
        if (stateSig != lastSyncedStateSig) {
            lastSyncedStateSig = stateSig;
            setChanged();
        }
        if (level.getGameTime() % 5 == 0) {
            sendUpdateToClient();
        }
    }

    private void loadFuel() {
        ItemStack slot0 = inventory.getStackInSlot(SLOT_FUEL_IN);
        if (!(slot0.getItem() instanceof PWRFuelItem fuel)) return;

        if (typeLoaded == -1 || amountLoaded <= 0) {
            typeLoaded = fuel.getType().ordinal();
            amountLoaded++;
            slot0.shrink(1);
            setChanged();
        } else if (fuel.getType().ordinal() == typeLoaded && amountLoaded < rodCount) {
            amountLoaded++;
            slot0.shrink(1);
            setChanged();
        }
    }

    private void produceHotFuel(PWRFuelType type) {
        Item hot = hotFuelItemFor(type);
        ItemStack slot1 = inventory.getStackInSlot(SLOT_FUEL_OUT);
        if (slot1.isEmpty()) {
            inventory.setStackInSlot(SLOT_FUEL_OUT, new ItemStack(hot));
        } else if (slot1.getItem() == hot && slot1.getCount() < slot1.getMaxStackSize()) {
            slot1.grow(1);
        }
    }

    protected void meltDown(Level level) {
        level.destroyBlock(worldPosition, false);

        double x = 0, y = 0, z = 0;
        int n = Math.max(1, rods.size());
        for (BlockPos pos : this.rods) {
            level.setBlock(pos, ModBlocks.CORIUM_BLOCK.get().defaultBlockState()
                    .setValue(com.hbm_m.block.fluid.FiniteFluidBlock.LEVEL, 5), 3);
            x += pos.getX() + 0.5D;
            y += pos.getY() + 0.5D;
            z += pos.getZ() + 0.5D;
        }

        if (rods.isEmpty()) {
            x = worldPosition.getX() + 0.5D;
            y = worldPosition.getY() + 0.5D;
            z = worldPosition.getZ() + 0.5D;
        } else {
            x /= n; y /= n; z /= n;
        }

        if (level instanceof ServerLevel serverLevel) {
            ExplosionNukeGeneric.incrementRad(serverLevel, x, y, z, 15F);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 40, 1.5D, 1.0D, 1.5D, 0.03D);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 4, 0.8D, 0.5D, 0.8D, 0.01D);
        }
        level.explode(null, x, y, z, 15.0F, Level.ExplosionInteraction.BLOCK);

        typeLoaded = -1;
        amountLoaded = 0;
        progress = 0;
        coreHeat = 0;
        hullHeat = 0;
        assembled = false;
    }

    /** Original: {@code createAudioLoop} - Geigerschleife, Lautstaerke 1, Reichweite 10. */
    private Object createAudioLoop() {
        try {
            return Class.forName("com.hbm_m.client.sound.PWRLoopSoundFactory")
                    .getMethod("create", PWRControllerBlockEntity.class)
                    .invoke(null, this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    protected void updateCoolant() {
        FT_Heatable trait = FluidType.getTrait(tanks[0].getTankType(), FT_Heatable.class);
        if (trait == null || trait.getEfficiency(HeatingType.PWR) <= 0) return;

        double coolingEff = (double) this.channelCount / (double) getRodCountForCoolant() * 0.1D;
        if (coolingEff > 1D) coolingEff = 1D;

        long heatToUse = Math.min(Math.min(this.hullHeat, (long) (this.hullHeat * coolingEff * trait.getEfficiency(HeatingType.PWR))), 2_000_000_000L);
        HeatingStep step = trait.getFirstStep();
        if (step == null) return;

        int coolCycles = tanks[0].getFill() / step.amountReq;
        int hotCycles = (tanks[1].getMaxFill() - tanks[1].getFill()) / step.amountProduced;
        long heatCycles = step.heatReq > 0 ? heatToUse / step.heatReq : 0;
        long cycles = Math.max(0, Math.min(coolCycles, Math.min(hotCycles, heatCycles)));
        if (cycles <= 0) return;

        this.hullHeat -= step.heatReq * cycles;
        this.tanks[0].drainMb((int) (step.amountReq * cycles));
        this.tanks[1].fillMb(step.typeProduced, (int) (step.amountProduced * cycles));
    }

    protected int getRodCountForCoolant() {
        return this.rodCount + (int) Math.ceil(this.heatsinkCount / 4D);
    }

    protected void setupTanks() {
        FT_Heatable trait = FluidType.getTrait(tanks[0].getTankType(), FT_Heatable.class);
        if (trait == null || trait.getEfficiency(HeatingType.PWR) <= 0) {
            tanks[0].setTankType(ModFluids.NONE.getSource());
            tanks[1].setTankType(ModFluids.NONE.getSource());
            return;
        }
        tanks[1].setTankType(trait.getFirstStep().typeProduced);
    }

    public double getTotalProcessMultiplier() {
        double totalConnections = this.connections + this.connectionsControlled * (1D - (this.rodLevel / 100D));
        return connectinFunc(totalConnections);
    }

    public double connectinFunc(double connections) {
        return connections / 10D * (1D - getXOverE(connections, 300D)) + connections / 150D * getXOverE(connections, 300D);
    }

    public double getXOverE(double x, double d) {
        return 1D - Math.pow(Math.E, -x / d);
    }

    public static PWRFuelType fuelType(int ordinal) {
        PWRFuelType[] v = PWRFuelType.values();
        return v[Math.abs(ordinal) % v.length];
    }

    /** {@code pwr_fuel} mit Metadaten {@code ordinal}. */
    public static Item freshFuelItemFor(PWRFuelType type) {
        Item it = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, "pwr_fuel_" + type.name().toLowerCase(java.util.Locale.ROOT)));
        return it == null ? Items.AIR : it;
    }

    /** {@code pwr_fuel_hot} mit Metadaten {@code ordinal}. */
    public static Item hotFuelItemFor(PWRFuelType type) {
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
        if (Double.isNaN(target)) return;
        rodTarget = Math.max(0D, Math.min(100D, target));
        setChanged();
        sendUpdateToClient();
    }

    public FluidTank getCoolantTank() { return coolantTank; }
    public FluidTank getCoolantHotTank() { return coolantHotTank; }

    // ── Comparator ──────────────────────────────────────────────────────────

    public int getComparatorPower() {
        if (coolantHotTank.getFill() == 0) return 0;
        double frac = (double) coolantHotTank.getFill() / coolantHotTank.getCapacityMb() * 15D;
        return (int) Math.max(0, Math.min(15, Math.round(frac) + 1));
    }

    // ── Inventar ────────────────────────────────────────────────────────────

    @Override
    protected ModItemStackHandler createInventoryHandler(int size) {
        return new ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) { setChanged(); }
        };
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_FUEL_IN) return stack.getItem() instanceof PWRFuelItem;
        return false;
    }

    // ── Steuerung / ROR ─────────────────────────────────────────────────────

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("control")) {
            this.rodTarget = Mth.clamp(data.getInt("control"), 0, 100);
            setChanged();
        }
    }

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
    public String[] getFunctionInfo() { return ROR; }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "rods").equals(name)) return "" + (int) (100 - this.rodLevel);
        if ((PREFIX_VALUE + "coreheat").equals(name)) return "" + this.coreHeat;
        if ((PREFIX_VALUE + "hullheat").equals(name)) return "" + this.hullHeat;
        if ((PREFIX_VALUE + "coldbuf").equals(name)) return "" + this.tanks[0].getFill();
        if ((PREFIX_VALUE + "hotbuf").equals(name)) return "" + this.tanks[1].getFill();
        if ((PREFIX_VALUE + "flux").equals(name)) return "" + (int) this.flux;
        if ((PREFIX_VALUE + "depletion").equals(name)) return "" + (int) (this.processTime > 0 ? this.progress * 100 / this.processTime : 0);
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setrods").equals(name) && params.length > 0) {
            int percent = IRORInteractive.parseInt(params[0], 0, 100);
            setRodTarget(100 - percent);
            return null;
        }

        if ((PREFIX_FUNCTION + "jettison").equals(name)) {
            this.typeLoaded = -1;
            this.amountLoaded = 0;
            this.progress = 0;
            setChanged();
            sendUpdateToClient();
            return null;
        }

        return null;
    }

    // ── IFluidStandardTransceiverMK2 ───────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ── NBT ───────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);

        tanks[0].writeToNBT(nbt, "t0");
        tanks[1].writeToNBT(nbt, "t1");

        nbt.putBoolean("assembled", assembled);
        nbt.putLong("coreHeat", coreHeat);
        nbt.putLong("coreHeatL", coreHeat);
        nbt.putLong("hullHeat", hullHeat);
        nbt.putLong("hullHeatL", hullHeat);
        nbt.putDouble("flux", flux);
        nbt.putDouble("rodLevel", rodLevel);
        nbt.putDouble("rodTarget", rodTarget);
        nbt.putInt("typeLoaded", typeLoaded);
        nbt.putInt("amountLoaded", amountLoaded);
        nbt.putDouble("progress", progress);
        nbt.putDouble("processTime", processTime);
        nbt.putLong("coreHeatCapacity", coreHeatCapacity);
        nbt.putLong("coreHeatCapacityL", coreHeatCapacity);

        nbt.putInt("rodCount", rodCount);
        nbt.putInt("connections", connections);
        nbt.putInt("connectionsControlled", connectionsControlled);
        nbt.putInt("heatexCount", heatexCount);
        nbt.putInt("channelCount", channelCount);
        nbt.putInt("sourceCount", sourceCount);
        nbt.putInt("heatsinkCount", heatsinkCount);

        nbt.putInt("portCount", ports.size());
        for (int i = 0; i < ports.size(); i++) {
            com.hbm_m.platform.BlockHooks.writeBlockPos(nbt, "port" + i, ports.get(i));
        }

        nbt.putInt("rodPosCount", rods.size());
        for (int i = 0; i < rods.size(); i++) {
            com.hbm_m.platform.BlockHooks.writeBlockPos(nbt, "rodPos" + i, rods.get(i));
        }
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);

        if (nbt.contains("t0")) tanks[0].readFromNBT(nbt, "t0");
        else if (nbt.contains("tank_coolant")) tanks[0].readFromNBT(nbt, "tank_coolant");

        if (nbt.contains("t1")) tanks[1].readFromNBT(nbt, "t1");
        else if (nbt.contains("tank_coolant_hot")) tanks[1].readFromNBT(nbt, "tank_coolant_hot");

        this.assembled = nbt.getBoolean("assembled");
        this.coreHeat = Math.max(nbt.getInt("coreHeat"), nbt.getLong("coreHeatL"));
        this.hullHeat = Math.max(nbt.getInt("hullHeat"), nbt.getLong("hullHeatL"));
        this.flux = nbt.getDouble("flux");
        this.rodLevel = nbt.getDouble("rodLevel");
        this.rodTarget = nbt.getDouble("rodTarget");
        if (nbt.contains("typeLoaded")) {
            if (nbt.getTagType("typeLoaded") == CompoundTag.TAG_STRING) {
                try {
                    this.typeLoaded = PWRFuelType.valueOf(nbt.getString("typeLoaded")).ordinal();
                } catch (Exception e) {
                    this.typeLoaded = -1;
                }
            } else {
                this.typeLoaded = nbt.getInt("typeLoaded");
            }
        } else {
            this.typeLoaded = -1;
        }
        this.amountLoaded = nbt.getInt("amountLoaded");
        this.progress = nbt.getDouble("progress");
        this.processTime = nbt.getDouble("processTime");
        this.coreHeatCapacity = Math.max(nbt.getInt("coreHeatCapacity"), nbt.getLong("coreHeatCapacityL"));
        if (this.coreHeatCapacity < CORE_HEAT_CAPACITY_BASE) this.coreHeatCapacity = CORE_HEAT_CAPACITY_BASE;

        this.rodCount = nbt.getInt("rodCount");
        this.connections = nbt.getInt("connections");
        this.connectionsControlled = nbt.getInt("connectionsControlled");
        this.heatexCount = nbt.getInt("heatexCount");
        this.channelCount = nbt.getInt("channelCount");
        this.sourceCount = nbt.getInt("sourceCount");
        this.heatsinkCount = nbt.getInt("heatsinkCount");

        ports.clear();
        int portCount = nbt.getInt("portCount");
        for (int i = 0; i < portCount; i++) {
            if (nbt.contains("port" + i)) {
                BlockPos p = com.hbm_m.platform.BlockHooks.readBlockPos(nbt, "port" + i);
                if (p != null) ports.add(p);
            } else if (nbt.contains("p" + i)) {
                int[] p = nbt.getIntArray("p" + i);
                if (p.length == 3) ports.add(new BlockPos(p[0], p[1], p[2]));
            }
        }

        rods.clear();
        int rodPosCount = nbt.contains("rodPosCount") ? nbt.getInt("rodPosCount") : nbt.getInt("rodCount");
        for (int i = 0; i < rodPosCount; i++) {
            if (nbt.contains("rodPos" + i)) {
                BlockPos p = com.hbm_m.platform.BlockHooks.readBlockPos(nbt, "rodPos" + i);
                if (p != null) rods.add(p);
            } else if (nbt.contains("r" + i)) {
                int[] p = nbt.getIntArray("r" + i);
                if (p.length == 3) rods.add(new BlockPos(p[0], p[1], p[2]));
            }
        }
    }

    // The port and rod position lists are server-only bookkeeping, rebuilt by the structure scan.
    // They have no business in the chunk packet, where they were the bulk of the payload.
    //? if < 1.21.1 {
    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return stripStructureLists(super.getUpdateTag());
    }
    //?} else {
    /*@Override
    public @NotNull CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return stripStructureLists(super.getUpdateTag(registries));
    }
    *///?}

    private static CompoundTag stripStructureLists(CompoundTag tag) {
        int portCount = tag.getInt("portCount");
        for (int i = 0; i < portCount; i++) tag.remove("port" + i);
        tag.remove("portCount");

        int rodPosCount = tag.getInt("rodPosCount");
        for (int i = 0; i < rodPosCount; i++) tag.remove("rodPos" + i);
        tag.remove("rodPosCount");
        return tag;
    }

    // ── Sonstiges ───────────────────────────────────────────────────────────

    @Override protected Component getDefaultName() { return Component.translatable("container.hbm_m.pwr_controller"); }
    @Override public Component getDisplayName() { return getDefaultName(); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return PWRControllerMenu.create(id, inv, this);
    }

    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return automationHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public @Nullable Object getItemHandler(@Nullable Direction side) {
        return automationHandler.orElse(null);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        automationHandler.invalidate();
    }

    /** Original: {@code getAccessibleSlotsFromSide = {0, 1}}, Einfuegen nur gueltig in 0, Entnahme nur aus 1. */
    private class AutomationHandler implements IItemHandler {
        private final ModItemStackHandler inv;
        AutomationHandler(ModItemStackHandler inv) { this.inv = inv; }
        @Override public int getSlots() { return 2; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inv.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!isItemValidForSlot(slot, stack)) return stack;
            return inv.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 1) return ItemStack.EMPTY;
            return inv.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return inv.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return isItemValidForSlot(slot, stack); }
    }
    //?} elif neoforge {
    /*@Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER) return automationHandler.cast();
        return super.getHbmCapability(cap, side);
    }

    @Override
    public @Nullable Object getItemHandler(@Nullable Direction side) {
        return automationHandler.orElse(null);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        automationHandler.invalidate();
    }

    /^* Original: {@code getAccessibleSlotsFromSide = {0, 1}}, Einfuegen nur gueltig in 0, Entnahme nur aus 1. ^/
    private class AutomationHandler implements net.minecraftforge.items.IItemHandler {
        private final ModItemStackHandler inv;
        AutomationHandler(ModItemStackHandler inv) { this.inv = inv; }
        @Override public int getSlots() { return 2; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inv.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!isItemValidForSlot(slot, stack)) return stack;
            return inv.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 1) return ItemStack.EMPTY;
            return inv.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return inv.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return isItemValidForSlot(slot, stack); }
    }
    *///?}
}
