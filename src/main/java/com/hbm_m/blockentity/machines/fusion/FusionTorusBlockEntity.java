package com.hbm_m.blockentity.machines.fusion;

import java.util.List;
import java.util.Map.Entry;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fusion.IFusionPowerReceiver;
import com.hbm_m.api.fusion.KlystronNetwork;
import com.hbm_m.api.fusion.KlystronNetworkProvider;
import com.hbm_m.api.fusion.PlasmaNetwork;
import com.hbm_m.api.fusion.PlasmaNetworkProvider;
import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.network.NodeNet;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.api.redstoneoverradio.IRORInteractive;
import com.hbm_m.api.redstoneoverradio.IRORValueProvider;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineFusionTorusMenu;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.item.ModItems;
import com.hbm_m.recipe.FusionRecipe;
import com.hbm_m.recipe.ModRecipes;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/**
 * 1:1-Port von {@code TileEntityFusionTorus} (1.7.10) - das Herz des Fusionsreaktors.
 *
 * <p>Der Torus sammelt ueber vier Klystron-Knoten die Zuendenergie ein, faehrt damit das gewaehlte
 * Fusionsrezept und verteilt die entstehende Plasmaleistung ueber vier Plasma-Knoten an alle
 * angeschlossenen {@link IFusionPowerReceiver}. Zusaetzlich enthaelt er die Kuehlmittellogik des
 * Originals ({@code TileEntityCooledBase}), die hier - wie im Original - direkt im Tick steht
 * statt ueber die Basisklasse zu laufen.</p>
 *
 * <p><b>Nicht portiert:</b> {@code checkTilt(TiltType.CONFIG, true)} - die "Maschinen-Schwerkraft"
 * des Originals ist ein globales, per Config abschaltbares System, das es in diesem Port (noch)
 * nicht gibt. Mit ausgeschalteter Config verhaelt sich das Original identisch.</p>
 */
public class FusionTorusBlockEntity extends BaseMachineBlockEntity
        implements IFluidStandardTransceiverMK2, NodeNet.ILoadedEntry, IRORValueProvider, IRORInteractive {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_BLUEPRINT = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int INVENTORY_SIZE = 3;

    public static final long MAX_POWER = 10_000_000L;

    // --- Kuehlung (Original: TileEntityCooledBase) ---
    public static final float KELVIN = 273F;
    public static final float TEMPERATURE_TARGET = KELVIN - 150F;
    public static final float TEMP_CHANGE_PER_MB = 0.5F;
    public static final float TEMP_PASSIVE_HEATING = 2.5F;
    public static final float TEMP_CHANGE_MAX = 5F + TEMP_PASSIVE_HEATING;

    public float temperature = KELVIN + 20;
    public final FluidTank[] coolantTanks;

    // --- Fusion ---
    public final FluidTank[] tanks;
    public boolean didProcess = false;
    /** Im laufenden Tick von Klystrons/Kopplern eingespeiste Energie (jeden Tick zurueckgesetzt). */
    public long klystronEnergy;
    /**
     * Wert des Vortticks. Das Original serialisiert in {@code networkPackNT} sofort und setzt
     * {@link #klystronEnergy} erst danach zurueck; der Vanilla-Blockupdate-Pfad packt den Tag
     * dagegen erst spaeter im Tick, deshalb wird der Anzeigewert hier festgehalten.
     */
    public long klystronEnergySync;
    public long plasmaEnergy;
    public double fuelConsumption;

    // Rendering (Client)
    public float magnet;
    public float prevMagnet;
    public float magnetSpeed;
    public static final float MAGNET_ACCELERATION = 0.25F;

    /** Original: {@code timeOffset} - zufaelliger Versatz, damit mehrere Reaktoren nicht synchron pulsieren. */
    private int timeOffset = -1;

    public int renderTimeOffset() {
        if (timeOffset == -1 && level != null) timeOffset = level.random.nextInt(30_000);
        return Math.max(timeOffset, 0);
    }

    @SuppressWarnings("unchecked")
    private final GenNode<KlystronNetwork>[] klystronNodes = new GenNode[4];
    @SuppressWarnings("unchecked")
    private final GenNode<PlasmaNetwork>[] plasmaNodes = new GenNode[4];
    /** Fuer das Rendering: an welchen der vier Ports haengt etwas? */
    public final boolean[] connections = new boolean[4];

    // --- Rezeptmodul (Original: ModuleMachineFusion) ---
    @Nullable
    private ResourceLocation selectedRecipeId = null;
    public double progress;
    public double bonus;
    private double processSpeed = 1D;
    private double bonusSpeed = 0D;

    public FusionTorusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_TORUS_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, Long.MAX_VALUE, 0L);

        this.coolantTanks = new FluidTank[] {
                new FluidTank(ModFluids.PERFLUOROMETHYL_COLD.getSource(), 4_000),
                new FluidTank(ModFluids.PERFLUOROMETHYL.getSource(), 4_000)
        };

        this.tanks = new FluidTank[] {
                new FluidTank(4_000), new FluidTank(4_000), new FluidTank(4_000), new FluidTank(4_000)
        };
    }

    // ═══════════════════════════════════ Tick ═══════════════════════════════════

    public static void tick(Level level, BlockPos pos, BlockState state, FusionTorusBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.serverTick(serverLevel, pos);
        } else {
            be.clientTick();
        }
    }

    private void serverTick(ServerLevel level, BlockPos pos) {

        ensureNetworkInitialized();

        for (int i = 0; i < 4; i++) {
            Direction dir = Direction.from3DDataValue(i + 2); // NORTH, SOUTH, WEST, EAST
            if (klystronNodes[i] == null || klystronNodes[i].expired) {
                klystronNodes[i] = createNode(level, pos, KlystronNetworkProvider.THE_PROVIDER, dir);
            }
            if (plasmaNodes[i] == null || plasmaNodes[i].expired) {
                plasmaNodes[i] = createNode(level, pos, PlasmaNetworkProvider.THE_PROVIDER, dir);
            }

            if (klystronNodes[i].net != null) klystronNodes[i].net.addReceiver(this);
            if (plasmaNodes[i].net != null) plasmaNodes[i].net.addProvider(this);
        }

        // Kuehlung (Original: identischer Block in TileEntityFusionTorus.updateEntity)
        this.temperature += TEMP_PASSIVE_HEATING;
        if (this.temperature > KELVIN + 20) this.temperature = KELVIN + 20;

        if (this.temperature > TEMPERATURE_TARGET) {
            int cyclesTemp = (int) Math.ceil(Math.min(this.temperature - TEMPERATURE_TARGET, TEMP_CHANGE_MAX) / TEMP_CHANGE_PER_MB);
            int cyclesCool = coolantTanks[0].getFill();
            int cyclesHot = coolantTanks[1].getMaxFill() - coolantTanks[1].getFill();
            int cycles = Math.min(cyclesTemp, Math.min(cyclesCool, cyclesHot));

            coolantTanks[0].setFill(coolantTanks[0].getFill() - cycles);
            coolantTanks[1].setFill(coolantTanks[1].getFill() + cycles);
            this.temperature -= TEMP_CHANGE_PER_MB * cycles;
        }

        for (NodeDirPos con : getConPos(pos)) {

            if (level.getGameTime() % 20 == 0) {
                trySubscribe(level, con.getX(), con.getY(), con.getZ(), con.getDir());
                trySubscribe(coolantTanks[0].getTankType(), level, con.getPos(), con.getDir());
                for (int i = 0; i < 3; i++) {
                    if (tanks[i].getTankType() != Fluids.EMPTY) {
                        trySubscribe(tanks[i].getTankType(), level, con.getPos(), con.getDir());
                    }
                }
            }

            if (coolantTanks[1].getFill() > 0) tryProvide(coolantTanks[1], level, con.getPos(), con.getDir());
            if (tanks[3].getFill() > 0) tryProvide(tanks[3], level, con.getPos(), con.getDir());
        }

        chargeFromBatterySlot(SLOT_BATTERY);

        // Plasma-Abnehmer zaehlen: die Ausgangsleistung wird unter ihnen aufgeteilt.
        int receiverCount = 0;
        // Kollektoren bestimmen die Geschwindigkeit des Bonusbalkens.
        int collectors = 0;

        for (int i = 0; i < 4; i++) {
            connections[i] = false;
            if (klystronNodes[i] != null && klystronNodes[i].hasValidNet() && !klystronNodes[i].net.providerEntries.isEmpty()) connections[i] = true;
            if (!connections[i] && plasmaNodes[i] != null && plasmaNodes[i].hasValidNet() && !plasmaNodes[i].net.receiverEntries.isEmpty()) connections[i] = true;

            if (plasmaNodes[i] != null && plasmaNodes[i].hasValidNet() && !plasmaNodes[i].net.receiverEntries.isEmpty()) {

                for (Entry<?, ?> entry : plasmaNodes[i].net.receiverEntries.entrySet()) {
                    Object thing = entry.getKey();
                    if (thing instanceof NodeNet.ILoadedEntry loaded && !loaded.isLoaded()) continue;
                    if (thing instanceof IFusionPowerReceiver rec && rec.receivesFusionPower()) receiverCount++;
                    if (thing instanceof FusionCollectorBlockEntity) collectors++;
                    break; // Original: nur der erste Eintrag pro Port
                }
            }
        }

        FusionRecipe recipe = getRecipe(level);

        double powerFactor = getSpeedScaled(getMaxEnergyStored(), getEnergyStored());
        int inputCount = recipe != null ? recipe.getFluidInputs().size() : 0;
        double fuel0Factor = inputCount > 0 ? getSpeedScaled(tanks[0].getMaxFill(), tanks[0].getFill()) : 1D;
        double fuel1Factor = inputCount > 1 ? getSpeedScaled(tanks[1].getMaxFill(), tanks[1].getFill()) : 1D;
        double fuel2Factor = inputCount > 2 ? getSpeedScaled(tanks[2].getMaxFill(), tanks[2].getFill()) : 1D;

        double factor = Math.min(powerFactor, Math.min(fuel0Factor, Math.min(fuel1Factor, fuel2Factor)));

        boolean ignition = recipe == null || recipe.getIgnitionTemp() <= this.klystronEnergy;

        float r = 0F;
        float g = 0F;
        float b = 0F;
        this.plasmaEnergy = 0;
        this.fuelConsumption = 0;

        this.processSpeed = factor;
        this.bonusSpeed = collectors * 0.5D;
        moduleUpdate(recipe, isCool() && ignition);

        if (didProcess && recipe != null) {
            this.plasmaEnergy = (long) Math.ceil(recipe.getOutputTemp() * factor);
            this.fuelConsumption = factor;
            r = recipe.getR();
            g = recipe.getG();
            b = recipe.getB();

            if (level != null && level.getGameTime() % 20 == 15) {
                com.hbm_m.satellite.RayScanEvents.reportEvent(level, worldPosition, com.hbm_m.satellite.RayScanEvents.INFO_PARTICLE, 200);
            }
        }

        double outputIntensity = getOutputIntensity(receiverCount);
        double outputFlux = recipe != null ? recipe.getNeutronFlux() * factor : 0D;

        if (this.plasmaEnergy > 0) for (int i = 0; i < 4; i++) {

            if (plasmaNodes[i] != null && plasmaNodes[i].hasValidNet() && !plasmaNodes[i].net.receiverEntries.isEmpty()) {

                for (Entry<?, ?> entry : plasmaNodes[i].net.receiverEntries.entrySet()) {
                    if (entry.getKey() instanceof IFusionPowerReceiver rec) {
                        long powerReceived = (long) Math.ceil(this.plasmaEnergy * outputIntensity);
                        rec.receiveFusionPower(powerReceived, outputFlux, r, g, b);
                    }
                }
            }
        }

        this.klystronEnergySync = this.klystronEnergy;

        setChanged();
        sendUpdateToClient();

        this.klystronEnergy = 0;
    }

    private void clientTick() {
        double powerFactor = getSpeedScaled(getMaxEnergyStored(), getEnergyStored());
        if (this.didProcess) this.magnetSpeed += MAGNET_ACCELERATION;
        else this.magnetSpeed -= MAGNET_ACCELERATION;

        this.magnetSpeed = Mth.clamp(this.magnetSpeed, 0F, 30F * (float) powerFactor);

        this.prevMagnet = this.magnet;
        this.magnet += this.magnetSpeed;

        if (this.magnet >= 360F) {
            this.magnet -= 360F;
            this.prevMagnet -= 360F;
        }
    }

    // ═════════════════════════════ Rezeptmodul ═════════════════════════════

    /** Original: {@code ModuleMachineBase.setupTanks} - Tanktypen an das Rezept angleichen. */
    private void setupTanks(@Nullable FusionRecipe recipe) {
        if (recipe == null) return;

        List<FluidStack> in = recipe.getFluidInputs();
        for (int i = 0; i < 3; i++) {
            if (in.size() > i) tanks[i].conform(in.get(i).getFluid());
            else tanks[i].resetTank();
        }

        List<FluidStack> out = recipe.getFluidOutputs();
        if (!out.isEmpty()) tanks[3].conform(out.get(0).getFluid());
        else tanks[3].resetTank();
    }

    /** Original: {@code ModuleMachineFusion.hasInput} - beachte die eigenwillige {@code > 0}-Bedingung. */
    private boolean hasInput(FusionRecipe recipe) {
        if (processSpeed <= 0) return false;

        List<FluidStack> in = recipe.getFluidInputs();
        for (int i = 0; i < Math.min(in.size(), 3); i++) {
            int req = (int) Math.ceil(in.get(i).getAmount() * processSpeed);
            if (tanks[i].getFill() > 0 && tanks[i].getFill() < req) return false;
        }
        return true;
    }

    /** Original: {@code ModuleMachineBase.canFitOutput}. */
    private boolean canFitOutput(FusionRecipe recipe) {

        List<ItemStack> outItems = recipe.getItemOutputs();
        if (!outItems.isEmpty()) {
            ItemStack single = outItems.get(0);
            ItemStack stack = getInventory().getStackInSlot(SLOT_OUTPUT);
            if (!stack.isEmpty()) {
                if (single.isEmpty()) return false;
                if (stack.getItem() != single.getItem()) return false;
                if (stack.getCount() + single.getCount() > stack.getMaxStackSize()) return false;
            }
        }

        List<FluidStack> outFluids = recipe.getFluidOutputs();
        if (!outFluids.isEmpty()) {
            if (outFluids.get(0).getAmount() + tanks[3].getFill() > tanks[3].getMaxFill()) return false;
        }

        return true;
    }

    private boolean canProcess(@Nullable FusionRecipe recipe) {
        if (recipe == null) return false;
        if (getEnergyStored() < recipe.getPower()) return false;
        if (!hasInput(recipe)) return false;
        return canFitOutput(recipe);
    }

    /** Original: {@code ModuleMachineFusion.process}. */
    private void process(FusionRecipe recipe) {
        setEnergyStored(getEnergyStored() - (long) Math.ceil(recipe.getPower() * processSpeed));

        double step = Math.min((double) 1 / recipe.getDuration() * processSpeed, 1D);
        this.progress += step;
        this.bonus += step * this.bonusSpeed;
        // Der Bonus wird u.U. nicht sofort eingeloest, deshalb 50% Puffer.
        this.bonus = Math.min(this.bonus, 1.5D);

        // Einzigartig fuer den Fusionsreaktor: der Brennstoff wird laufend verbraucht,
        // nicht erst bei fertigem Rezept.
        List<FluidStack> in = recipe.getFluidInputs();
        for (int i = 0; i < Math.min(in.size(), 3); i++) {
            int req = (int) Math.ceil(in.get(i).getAmount() * processSpeed);
            tanks[i].setFill(Math.max(tanks[i].getFill() - req, 0));
        }

        if (this.progress >= 1D) {
            produceItem(recipe);
            if (canProcess(recipe)) this.progress -= 1D;
            else this.progress = 0D;
        }

        if (this.bonus >= 1D && canFitOutput(recipe)) {
            produceItem(recipe);
            this.bonus -= 1D;
        }
    }

    /** Original: {@code ModuleMachineBase.produceItem}. */
    private void produceItem(FusionRecipe recipe) {

        List<ItemStack> outItems = recipe.getItemOutputs();
        if (!outItems.isEmpty() && !outItems.get(0).isEmpty()) {
            ItemStack result = outItems.get(0);
            ItemStack current = getInventory().getStackInSlot(SLOT_OUTPUT);
            if (current.isEmpty()) {
                getInventory().setStackInSlot(SLOT_OUTPUT, result.copy());
            } else {
                current.grow(result.getCount());
                getInventory().setStackInSlot(SLOT_OUTPUT, current);
            }
        }

        List<FluidStack> outFluids = recipe.getFluidOutputs();
        if (!outFluids.isEmpty()) {
            tanks[3].setFill(tanks[3].getFill() + (int) outFluids.get(0).getAmount());
        }
    }

    /** Original: {@code ModuleMachineBase.update(1D, 1D, extraCondition, blueprint)}. */
    private void moduleUpdate(@Nullable FusionRecipe recipe, boolean extraCondition) {
        setupTanks(recipe);

        this.didProcess = false;

        if (extraCondition && canProcess(recipe)) {
            process(recipe);
            this.didProcess = true;
        } else {
            this.progress = 0D;
        }
    }

    @Nullable
    public FusionRecipe getRecipe(Level level) {
        if (selectedRecipeId == null) return null;
        return level.getRecipeManager().byKey(selectedRecipeId)
                .filter(r -> r instanceof FusionRecipe)
                .map(r -> (FusionRecipe) r)
                .orElse(null);
    }

    @Nullable
    public ResourceLocation getSelectedRecipeId() {
        return selectedRecipeId;
    }

    /** Original: {@code receiveControl} mit {@code index == 0} - Rezeptauswahl im GUI. */
    public void setSelectedRecipeId(@Nullable ResourceLocation id) {
        this.selectedRecipeId = id;
        this.progress = 0D;
        setChanged();
    }

    // ═════════════════════════════ Hilfsfunktionen ═════════════════════════════

    /** Original: {@code getOuputIntensity} - je mehr Abnehmer, desto mehr Gesamtleistung (100%..175%). */
    public static double getOutputIntensity(int receiverCount) {
        if (receiverCount == 1) return 1D;      // 100%
        if (receiverCount == 2) return 0.625D;  // 125%
        if (receiverCount == 3) return 0.5D;    // 150%
        return 0.4375D;                          // 175%
    }

    /** Original: Skaliert linear von 0% auf 100% zwischen 0 und der Haelfte des Maximums. */
    public static double getSpeedScaled(double max, double level) {
        if (max == 0) return 0D;
        if (level >= max * 0.5) return 1D;
        return level / max * 2D;
    }

    public boolean isCool() {
        return this.temperature <= TEMPERATURE_TARGET;
    }

    private <N extends NodeNet<?, ?, ?>> GenNode<N> createNode(ServerLevel level, BlockPos pos,
                                  com.hbm_m.api.network.INetworkProvider<N> provider, Direction dir) {

        BlockPos nodePos = pos.offset(dir.getStepX() * 7, 2, dir.getStepZ() * 7);
        GenNode<N> node = UniNodespace.getNode(level, nodePos, provider);
        if (node != null) return node;

        node = new GenNode<>(provider, nodePos)
                .setConnections(new NodeDirPos(pos.offset(dir.getStepX() * 8, 2, dir.getStepZ() * 8), dir));

        UniNodespace.createNode(level, node);
        return node;
    }

    /** Original: {@code getConPos()} - 26 Anschlusspunkte ober- und unterhalb der Portzellen. */
    public NodeDirPos[] getConPos(BlockPos pos) {
        return new NodeDirPos[] {
                new NodeDirPos(pos.offset(0, -1, 0), Direction.DOWN),
                new NodeDirPos(pos.offset(0, 5, 0), Direction.UP),

                new NodeDirPos(pos.offset(6, -1, 0), Direction.DOWN),
                new NodeDirPos(pos.offset(6, 5, 0), Direction.UP),
                new NodeDirPos(pos.offset(6, -1, 2), Direction.DOWN),
                new NodeDirPos(pos.offset(6, 5, 2), Direction.UP),
                new NodeDirPos(pos.offset(6, -1, -2), Direction.DOWN),
                new NodeDirPos(pos.offset(6, 5, -2), Direction.UP),

                new NodeDirPos(pos.offset(-6, -1, 0), Direction.DOWN),
                new NodeDirPos(pos.offset(-6, 5, 0), Direction.UP),
                new NodeDirPos(pos.offset(-6, -1, 2), Direction.DOWN),
                new NodeDirPos(pos.offset(-6, 5, 2), Direction.UP),
                new NodeDirPos(pos.offset(-6, -1, -2), Direction.DOWN),
                new NodeDirPos(pos.offset(-6, 5, -2), Direction.UP),

                new NodeDirPos(pos.offset(0, -1, 6), Direction.DOWN),
                new NodeDirPos(pos.offset(0, 5, 6), Direction.UP),
                new NodeDirPos(pos.offset(2, -1, 6), Direction.DOWN),
                new NodeDirPos(pos.offset(2, 5, 6), Direction.UP),
                new NodeDirPos(pos.offset(-2, -1, 6), Direction.DOWN),
                new NodeDirPos(pos.offset(-2, 5, 6), Direction.UP),

                new NodeDirPos(pos.offset(0, -1, -6), Direction.DOWN),
                new NodeDirPos(pos.offset(0, 5, -6), Direction.UP),
                new NodeDirPos(pos.offset(2, -1, -6), Direction.DOWN),
                new NodeDirPos(pos.offset(2, 5, -6), Direction.UP),
                new NodeDirPos(pos.offset(-2, -1, -6), Direction.DOWN),
                new NodeDirPos(pos.offset(-2, 5, -6), Direction.UP),
        };
    }

    // ═════════════════════════════ Standardkram ═════════════════════════════

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return true;
        if (slot == SLOT_BLUEPRINT) return stack.is(ModItems.BLUEPRINT_FOLDER.get());
        return false;
    }

    @Override
    public FluidTank[] getSendingTanks() { return new FluidTank[] { coolantTanks[1], tanks[3] }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { coolantTanks[0], tanks[0], tanks[1], tanks[2] }; }

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { coolantTanks[0], coolantTanks[1], tanks[0], tanks[1], tanks[2], tanks[3] };
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel) {
            for (GenNode<?> node : klystronNodes) if (node != null) UniNodespace.destroyNode(serverLevel, node);
            for (GenNode<?> node : plasmaNodes) if (node != null) UniNodespace.destroyNode(serverLevel, node);
        }
    }

    @Override
    protected void writeNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        for (int i = 0; i < 4; i++) tag.put("ft" + i, tanks[i].writeNBT(new CompoundTag()));
        tag.put("ct0", coolantTanks[0].writeNBT(new CompoundTag()));
        tag.put("ct1", coolantTanks[1].writeNBT(new CompoundTag()));
        tag.putFloat("temperature", temperature);
        tag.putDouble("progress", progress);
        tag.putDouble("bonus", bonus);
        tag.putBoolean("didProcess", didProcess);
        tag.putLong("klystronEnergy", klystronEnergySync);
        tag.putLong("plasmaEnergy", plasmaEnergy);
        tag.putDouble("fuelConsumption", fuelConsumption);
        if (selectedRecipeId != null) tag.putString("recipe", selectedRecipeId.toString());
        for (int i = 0; i < 4; i++) tag.putBoolean("con" + i, connections[i]);
    }

    @Override
    protected void readNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        for (int i = 0; i < 4; i++) if (tag.contains("ft" + i)) tanks[i].readNBT(tag.getCompound("ft" + i));
        if (tag.contains("ct0")) coolantTanks[0].readNBT(tag.getCompound("ct0"));
        if (tag.contains("ct1")) coolantTanks[1].readNBT(tag.getCompound("ct1"));
        this.temperature = tag.contains("temperature") ? tag.getFloat("temperature") : KELVIN + 20;
        this.progress = tag.getDouble("progress");
        this.bonus = tag.getDouble("bonus");
        this.didProcess = tag.getBoolean("didProcess");
        this.klystronEnergySync = tag.getLong("klystronEnergy");
        this.klystronEnergy = this.klystronEnergySync;
        this.plasmaEnergy = tag.getLong("plasmaEnergy");
        this.fuelConsumption = tag.getDouble("fuelConsumption");
        this.selectedRecipeId = tag.contains("recipe") ? ResourceLocation.tryParse(tag.getString("recipe")) : null;
        for (int i = 0; i < 4; i++) connections[i] = tag.getBoolean("con" + i);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.fusion_torus");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineFusionTorusMenu.create(id, inventory, this);
    }


    // ═════════════════════════ Redstone over Radio ═════════════════════════

    /** 1:1 aus {@code TileEntityFusionTorus.getFunctionInfo}. */
    public static final String[] ROR = new String[] {
        PREFIX_VALUE + "plasma",
        PREFIX_VALUE + "consumption",
        PREFIX_VALUE + "progress",
        PREFIX_VALUE + "recipe",
        PREFIX_VALUE + "active",
        PREFIX_VALUE + "temp",
        PREFIX_FUNCTION + "setrecipe" + NAME_SEPARATOR + "name",
    };

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "plasma").equals(name))      return "" + this.plasmaEnergy;
        if ((PREFIX_VALUE + "consumption").equals(name)) return "" + (int) (this.fuelConsumption * 100);
        if ((PREFIX_VALUE + "progress").equals(name))    return "" + (int) Math.round(this.progress * 100);
        if ((PREFIX_VALUE + "recipe").equals(name))      return selectedRecipeId != null ? selectedRecipeId.toString() : "null";
        if ((PREFIX_VALUE + "active").equals(name))      return "" + (this.didProcess ? 1 : 0);
        if ((PREFIX_VALUE + "temp").equals(name))        return "" + (int) this.temperature;
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setrecipe").equals(name) && params.length == 1) {
            setSelectedRecipeId(ResourceLocation.tryParse(params[0]));
            sendUpdateToClient();
        }
        return null;
    }

    private AABB renderBounds = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (renderBounds == null) {
            renderBounds = new AABB(
                    worldPosition.getX() - 8, worldPosition.getY(), worldPosition.getZ() - 8,
                    worldPosition.getX() + 9, worldPosition.getY() + 5, worldPosition.getZ() + 9);
        }
        return renderBounds;
    }

    /** Nur fuer die Rezeptauswahl im GUI: alle registrierten Fusionsrezepte. */
    public static List<FusionRecipe> getAllRecipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.FUSION_TYPE.get());
    }
}
