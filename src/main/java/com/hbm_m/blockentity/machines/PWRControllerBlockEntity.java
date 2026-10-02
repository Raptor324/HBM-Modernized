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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
//?}

/**
 * 1:1 {@code TileEntityPWRController}. Der Aufbau wird von {@code MachinePWRControllerBlock} geflutet und in Traeger
 * ({@code pwr_block}) verwandelt; {@link #setup} zaehlt Bauteile und Brennstab-Verbindungen. Gespalten wird nur, wenn die
 * Umgebung (+-2 Chunks) geladen ist ({@code unloadDelay}), sonst werden Kern- und Huellenhitze genullt. Ueberhitzt der Kern,
 * werden alle Brennstaebe zu Corium und der Reaktor explodiert.
 */
public class PWRControllerBlockEntity extends BaseMachineBlockEntity
        implements IFluidStandardTransceiverMK2, IControlReceiver, IRORValueProvider, IRORInteractive {

    public FluidTank[] tanks;
    public long coreHeat;
    public static final long coreHeatCapacityBase = 10_000_000;
    public long coreHeatCapacity = 10_000_000;
    public long hullHeat;
    public static final long hullHeatCapacityBase = 10_000_000;
    public double flux;

    public double rodLevel = 100;
    public double rodTarget = 100;

    public int typeLoaded;
    public int amountLoaded;
    public double progress;
    public double processTime;

    public int rodCount;
    public int connections;
    public int connectionsControlled;
    public int heatexCount;
    public int heatsinkCount;
    public int channelCount;
    public int sourceCount;

    public int unloadDelay = 0;
    public boolean assembled;

    protected List<BlockPos> ports = new ArrayList<>();
    protected List<BlockPos> rods = new ArrayList<>();

    //? if forge {
    private final LazyOptional<IItemHandler> automationHandler;
    //?}

    public PWRControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PWR_CONTROLLER_BE.get(), pos, state, 3, 0L, 0L);
        this.tanks = new FluidTank[2];
        this.tanks[0] = new FluidTank(ModFluids.COOLANT.getSource(), 128_000);
        this.tanks[1] = new FluidTank(ModFluids.COOLANT_HOT.getSource(), 128_000);
        //? if forge {
        this.automationHandler = LazyOptional.of(() -> new AutomationHandler(inventory));
        //?}
    }

    /** Einrichtung des Reaktors beim Zusammenbau: zaehlt Bauteile und berechnet die Brennstab-Verbindungen. */
    public void setup(Map<BlockPos, Block> partMap, Map<BlockPos, Block> rodMap) {

        rodCount = 0;
        connections = 0;
        connectionsControlled = 0;
        heatexCount = 0;
        channelCount = 0;
        heatsinkCount = 0;
        sourceCount = 0;
        ports.clear();
        rods.clear();

        int connectionsDouble = 0;
        int connectionsControlledDouble = 0;

        for (Map.Entry<BlockPos, Block> entry : partMap.entrySet()) {
            Block block = entry.getValue();

            if (block == ModBlocks.PWR_FUEL.get()) rodCount++;
            if (block == ModBlocks.PWR_HEATEX.get()) heatexCount++;
            if (block == ModBlocks.PWR_CHANNEL.get()) channelCount++;
            if (block == ModBlocks.PWR_HEATSINK.get()) heatsinkCount++;
            if (block == ModBlocks.PWR_NEUTRON_SOURCE.get()) sourceCount++;
            if (block == ModBlocks.PWR_PORT.get()) ports.add(entry.getKey());
        }

        for (Map.Entry<BlockPos, Block> entry : rodMap.entrySet()) {
            BlockPos fuelPos = entry.getKey();

            rods.add(fuelPos);

            for (Direction dir : Direction.values()) {

                boolean controlled = false;

                for (int i = 1; i < 16; i++) {
                    BlockPos checkPos = fuelPos.relative(dir, i);
                    Block atPos = partMap.get(checkPos);
                    if (atPos == null || atPos == ModBlocks.PWR_CASING.get()) break;
                    if (atPos == ModBlocks.PWR_CONTROL.get()) controlled = true;
                    if (atPos == ModBlocks.PWR_FUEL.get()) {
                        if (controlled) connectionsControlledDouble++; else connectionsDouble++;
                        break;
                    }
                    if (atPos == ModBlocks.PWR_REFLECTOR.get()) {
                        if (controlled) connectionsControlledDouble += 2; else connectionsDouble += 2;
                        break;
                    }
                }
            }
        }

        connections = connectionsDouble / 2;
        connectionsControlled = connectionsControlledDouble / 2;
        heatsinkCount = Math.min(heatsinkCount, 80);

        // int64, weil die Kapazitaet ab 2127 Kuehlkoerpern int32 sprengt
        this.coreHeatCapacity = coreHeatCapacityBase + this.heatsinkCount * (coreHeatCapacityBase / 20);
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PWRControllerBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick(level, pos);
        } else {
            com.hbm_m.sound.ClientSoundBootstrap.updateSound(be, be.amountLoaded > 0, be::createAudioLoop);
        }
    }

    private void serverTick(Level level, BlockPos pos) {

        ItemStack[] slots = { inventory.getStackInSlot(0), inventory.getStackInSlot(1), inventory.getStackInSlot(2) };
        this.tanks[0].setType(2, slots);
        setupTanks();

        if (unloadDelay > 0) unloadDelay--;

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        // Fluessigkeitsquellen liegen oft nicht im selben Chunk, also zwei Chunks Abstand
        if (!level.hasChunk(chunkX, chunkZ) ||
                !level.hasChunk(chunkX + 2, chunkZ + 2) ||
                !level.hasChunk(chunkX + 2, chunkZ - 2) ||
                !level.hasChunk(chunkX - 2, chunkZ + 2) ||
                !level.hasChunk(chunkX - 2, chunkZ - 2)) {
            this.unloadDelay = 60;
        }

        if (this.assembled) {
            for (BlockPos p : ports) {
                for (Direction dir : Direction.values()) {
                    BlockPos portPos = p.relative(dir);

                    if (tanks[1].getFill() > 0) this.tryProvide(tanks[1], level, portPos, dir);
                    this.trySubscribe(tanks[0].getTankType(), level, portPos, dir);
                }
            }

            // Spaltung erst, wenn die Umgebung 40 Ticks oder laenger geladen ist
            if (this.unloadDelay <= 0) {

                ItemStack fuelIn = inventory.getStackInSlot(0);
                if ((typeLoaded == -1 || amountLoaded <= 0) && fuelIn.getItem() instanceof PWRFuelItem fresh) {
                    typeLoaded = fresh.getType().ordinal();
                    amountLoaded++;
                    inventory.extractItem(0, 1, false);
                    setChanged();
                } else if (fuelIn.getItem() instanceof PWRFuelItem fresh && fresh.getType().ordinal() == typeLoaded && amountLoaded < rodCount) {
                    amountLoaded++;
                    inventory.extractItem(0, 1, false);
                    setChanged();
                }
                double diff = this.rodLevel - this.rodTarget;
                if (diff < 1 && diff > -1) this.rodLevel = this.rodTarget;
                if (this.rodTarget > this.rodLevel) this.rodLevel++;
                if (this.rodTarget < this.rodLevel) this.rodLevel--;

                double multiplier = 1D;

                FT_PWRModerator moderator = FluidType.getTrait(tanks[0].getTankType(), FT_PWRModerator.class);
                if (moderator != null) {
                    multiplier = moderator.getMultiplier();
                }

                int newFlux = this.sourceCount * 20;

                if (typeLoaded != -1 && amountLoaded > 0) {

                    PWRFuelType fuel = fuelType(typeLoaded);
                    double usedRods = getTotalProcessMultiplier();
                    double fluxPerRod = this.flux / this.rodCount;
                    double outputPerRod = fuel.function.effonix(fluxPerRod);
                    double totalOutput = outputPerRod * amountLoaded * usedRods;
                    double totalHeatOutput = totalOutput * fuel.heatEmission;

                    if (tanks[0].getFill() > 0) {
                        totalHeatOutput *= multiplier;
                    }

                    this.coreHeat += totalHeatOutput;
                    newFlux += totalOutput;

                    this.processTime = (int) fuel.yield;
                    this.progress += totalOutput;

                    if (this.progress >= this.processTime) {
                        this.progress -= this.processTime;

                        ItemStack out = inventory.getStackInSlot(1);
                        Item hot = hotFuelItemFor(fuel);
                        if (out.isEmpty()) {
                            inventory.setStackInSlot(1, new ItemStack(hot));
                        } else if (out.getItem() == hot && out.getCount() < out.getMaxStackSize()) {
                            out.grow(1);
                        }

                        this.amountLoaded--;
                        setChanged();
                    }

                    if (level.getGameTime() % 100 == 0)
                        com.hbm_m.satellite.RayScanEvents.reportEvent(level, worldPosition, com.hbm_m.satellite.RayScanEvents.INFO_NUCLEAR, 200);
                }

                if (this.amountLoaded <= 0) {
                    this.typeLoaded = -1;
                }

                if (amountLoaded > rodCount) amountLoaded = rodCount;

                /* KERNKUEHLUNG */
                double coreCoolingApproachNum = getXOverE((double) this.heatexCount * 5 / (double) getRodCountForCoolant(), 2) / 2D;
                long averageCoreHeat = (this.coreHeat + this.hullHeat) / 2;
                this.coreHeat -= (coreHeat - averageCoreHeat) * coreCoolingApproachNum;
                this.hullHeat -= (hullHeat - averageCoreHeat) * coreCoolingApproachNum;

                updateCoolant();

                this.coreHeat *= 0.999D;
                this.hullHeat *= 0.999D;

                this.flux = newFlux;

                if (tanks[0].getFill() > 0) {
                    this.flux *= multiplier;
                }

                if (this.coreHeat > this.coreHeatCapacity) {
                    meltDown(level);
                    return;
                }
            } else {
                this.hullHeat = 0;
                this.coreHeat = 0;
            }
        }

        sendUpdateToClient();
    }

    protected void meltDown(Level level) {

        level.destroyBlock(worldPosition, false);

        double x = 0;
        double y = 0;
        double z = 0;

        for (BlockPos pos : this.rods) {
            // Original: breakBlock des Traegers (Bauteil zurueck) und sofort Corium darueber
            level.setBlock(pos, ModBlocks.CORIUM_BLOCK.get().defaultBlockState()
                    .setValue(com.hbm_m.block.fluid.FiniteFluidBlock.LEVEL, 5), 3);

            x += pos.getX() + 0.5;
            y += pos.getY() + 0.5;
            z += pos.getZ() + 0.5;
        }

        x /= rods.size();
        y /= rods.size();
        z /= rods.size();

        level.explode(null, x, y, z, 15F, true, Level.ExplosionInteraction.BLOCK);
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

        double coolingEff = (double) this.channelCount / (double) getRodCountForCoolant() * 0.1D; // 10% Kuehlung bei gleicher Anzahl
        if (coolingEff > 1D) coolingEff = 1D;

        // Zyklenzahl deckeln, damit die Rechnung nicht ueberlaeuft
        int heatToUse = (int) Math.min(Math.min(this.hullHeat, (long) (this.hullHeat * coolingEff * trait.getEfficiency(HeatingType.PWR))), 2_000_000_000);
        HeatingStep step = trait.getFirstStep();
        int coolCycles = tanks[0].getFill() / step.amountReq;
        int hotCycles = (tanks[1].getMaxFill() - tanks[1].getFill()) / step.amountProduced;
        int heatCycles = heatToUse / step.heatReq;
        int cycles = Math.min(coolCycles, Math.min(hotCycles, heatCycles));

        this.hullHeat -= step.heatReq * cycles;
        this.tanks[0].setFill(tanks[0].getFill() - step.amountReq * cycles);
        this.tanks[1].setFill(tanks[1].getFill() + step.amountProduced * cycles);
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
        return 1 - Math.pow(Math.E, -x / d);
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
        if (slot == 0) return stack.getItem() instanceof PWRFuelItem;
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

    public static final String[] ROR = new String[] { // nicht mit RUR verwechseln
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
        if ((PREFIX_VALUE + "depletion").equals(name)) return "" + (int) (this.progress * 100 / this.processTime);
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {

        if ((PREFIX_FUNCTION + "setrods").equals(name) && params.length > 0) {
            int percent = IRORInteractive.parseInt(params[0], 0, 100);
            this.rodTarget = percent;
            setChanged();
            return null;
        }

        if ((PREFIX_FUNCTION + "jettison").equals(name)) {
            this.typeLoaded = -1;
            this.amountLoaded = 0;
            this.progress = 0;
            setChanged();
            return null;
        }

        return null;
    }

    // ── Fluessigkeit ────────────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);

        tanks[0].readFromNBT(nbt, "t0");
        tanks[1].readFromNBT(nbt, "t1");

        this.assembled = nbt.getBoolean("assembled");
        this.coreHeat = Math.max(nbt.getInt("coreHeat"), nbt.getLong("coreHeatL"));
        this.hullHeat = Math.max(nbt.getInt("hullHeat"), nbt.getLong("hullHeatL"));
        this.flux = nbt.getDouble("flux");
        this.rodLevel = nbt.getDouble("rodLevel");
        this.rodTarget = nbt.getDouble("rodTarget");
        this.typeLoaded = nbt.getInt("typeLoaded");
        this.amountLoaded = nbt.getInt("amountLoaded");
        this.progress = nbt.getDouble("progress");
        this.processTime = nbt.getDouble("processTime");
        this.coreHeatCapacity = Math.max(nbt.getInt("coreHeatCapacity"), nbt.getLong("coreHeatCapacityL"));
        if (this.coreHeatCapacity < coreHeatCapacityBase) this.coreHeatCapacity = coreHeatCapacityBase;

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
            int[] port = nbt.getIntArray("p" + i);
            if (port.length == 3) ports.add(new BlockPos(port[0], port[1], port[2]));
        }

        rods.clear();
        int rodCount = nbt.getInt("rodCount");
        for (int i = 0; i < rodCount; i++) {
            if (nbt.contains("r" + i)) {
                int[] port = nbt.getIntArray("r" + i);
                if (port.length == 3) rods.add(new BlockPos(port[0], port[1], port[2]));
            }
        }
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);

        tanks[0].writeToNBT(nbt, "t0");
        tanks[1].writeToNBT(nbt, "t1");

        nbt.putBoolean("assembled", assembled);
        nbt.putLong("coreHeatL", coreHeat);
        nbt.putLong("hullHeatL", hullHeat);
        nbt.putDouble("flux", flux);
        nbt.putDouble("rodLevel", rodLevel);
        nbt.putDouble("rodTarget", rodTarget);
        nbt.putInt("typeLoaded", typeLoaded);
        nbt.putInt("amountLoaded", amountLoaded);
        nbt.putDouble("progress", progress);
        nbt.putDouble("processTime", processTime);
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
            BlockPos pos = ports.get(i);
            nbt.putIntArray("p" + i, new int[] { pos.getX(), pos.getY(), pos.getZ() });
        }

        nbt.putInt("rodCount", rods.size());
        for (int i = 0; i < rods.size(); i++) {
            BlockPos pos = rods.get(i);
            nbt.putIntArray("r" + i, new int[] { pos.getX(), pos.getY(), pos.getZ() });
        }
    }

    // ── Sonstiges ───────────────────────────────────────────────────────────

    @Override protected Component getDefaultName() { return Component.translatable("container.pwrController"); }
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
    //?}
}
