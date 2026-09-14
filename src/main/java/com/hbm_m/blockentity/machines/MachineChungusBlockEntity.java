package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.api.fluids.IFluidStandardSenderMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.trait.FT_Coolable;
import com.hbm_m.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm_m.interfaces.IEnergyModeHolder;
import com.hbm_m.sound.ClientSoundBootstrap;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

/**
 * Chungus / Leviathan Steam Turbine BlockEntity - das Endgame-Upgrade zur Industrial Turbine.
 * Kein Inventar/GUI (wie im Original) - Dampf kommt ausschließlich über Rohre an den
 * UNIVERSAL_CONNECTOR-Phantomblöcken der Multiblock-Struktur an, Energie geht über den
 * ENERGY_CONNECTOR-Phantomblock raus. Verbraucht (anders als die Industrial Turbine) pro Tick
 * 100% des verfügbaren Dampfs - das Flywheel ist hier der einzige Puffer/Dämpfer.
 * <p>
 * Dampfumsatz, Ausgabefluid und Energie kommen wie im Original ({@code TileEntityTurbineBase})
 * aus der {@code FT_Coolable}-Eigenschaft; der Ausgang ist die naechstniedrigere Dampfstufe.
 * <p><b>Der Hebel</b> schaltet wie im Original die Dampfstufe durch: Dampf, Heissdampf,
 * Ueberhitzter, Ultraheisser und wieder zurueck. Der Ausgang folgt jeweils eine Stufe darunter.
 * Solange die Turbine laeuft, laesst sich nichts umstellen - man muss sie erst leerlaufen
 * lassen.</p>
 *
 * <p>{@code operational} ist dabei <b>kein Schalter</b>, sondern das Ergebnis: die Turbine gilt
 * als laufend, solange tatsaechlich Dampf umgesetzt wird.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class MachineChungusBlockEntity extends BaseMachineBlockEntity
        implements IEnergyModeHolder, IFluidStandardReceiverMK2, IFluidStandardSenderMK2 {

    // Capacity constants (Platzhalter, ~16x Industrial Turbine - beim Playtesten nachjustieren)
    private static final long ENERGY_CAPACITY = 8_000_000L;
    private static final long ENERGY_EXTRACT_RATE = 160_000L;
    private static final int STEAM_CAPACITY = 1_024_000;
    private static final int SPENT_STEAM_CAPACITY = 1_024_000;

    // Conversion constants
    private static final double CONSUMPTION_PERCENT = 1.0D; // Original: consumptionPercent() = 1D (alles pro Tick)
    private static final double EFFICIENCY = 0.85D;         // Original: efficiency-Feld, Default 0.85

    // Flywheel (Spin-up/Spin-down-Trägheit), gleiches Prinzip wie bei der Industrial Turbine.
    private static final double FLYWHEEL_MAX_ENERGY = ENERGY_CAPACITY * 4.0;
    private double spin = 0.0;
    private long flywheelEnergy = 0L;
    private long maxPower = 0L;

    // Fluid tanks
    private final FluidTank steamTank;
    private final FluidTank spentSteamTank;

    //? if forge {
    private LazyOptional<IFluidHandler> lazySpentHandler;
    //?}

    private boolean isActive = false;
    /** Lever-Ersatz: per Rechtsklick auf den Controller umgeschaltet, gated den Dampfverbrauch. */
    private boolean operational = false;
    private float anim = 0.0F;
    private float prevAnim = 0.0F;

    public MachineChungusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CHUNGUS_BE.get(), pos, state,
              0, ENERGY_CAPACITY, 0L, ENERGY_EXTRACT_RATE);

        // Standardmäßig auf normalen Dampf konformiert (wie spentSteamTank auf SPENTSTEAM), damit das
        // Fluid-Netzwerk den Tank sofort erkennt, statt erst nach dem ersten manuellen Einfüllen
        // (UniversalMachinePartBlockEntity#collectControllerFluidTypes ignoriert Tanks ohne Typ).
        this.steamTank = new FluidTank(ModFluids.STEAM.getSource(), STEAM_CAPACITY);
        this.spentSteamTank = new FluidTank(ModFluids.SPENTSTEAM.getSource(), SPENT_STEAM_CAPACITY);

        //? if forge {
        this.lazySpentHandler = LazyOptional.empty();
        //?}
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineChungusBlockEntity be) {
        be.prevAnim = be.anim;

        if (level.isClientSide()) {
            if (be.isActive) {
                be.anim += 0.15F;
                if (be.anim > (float) (Math.PI * 2.0)) {
                    be.anim -= (float) (Math.PI * 2.0);
                }
            }
            ClientSoundBootstrap.updateSound(be, be.spin > 0.001D,
                    () -> be.createLoopingSoundReflect(ModSounds.CHUNGUS_TURBINE.get()));
            return;
        }

        be.ensureNetworkInitialized();

        boolean wasActive = be.isActive;

        be.processTurbine();

        if (be.energy > 0 && level.getGameTime() % 10L == 0L) {
            be.updateEnergyDelta(be.getEnergyStored());
        }

        if (wasActive != be.isActive) {
            be.setChanged();
            be.sendUpdateToClient();
        }
    }

    /**
     * 1:1-Port von {@code onLeverPull}: die naechste Dampfstufe. Ein- und Ausgang wandern zusammen
     * weiter, damit der Ausgang immer die Stufe darunter ist.
     *
     * @return false, wenn gerade gearbeitet wird - dann bleibt alles stehen
     */
    public boolean pullLever() {
        if (operational) return false;

        var in = steamTank.getTankType();

        if (in == ModFluids.STEAM.getSource()) {
            steamTank.setTankType(ModFluids.HOTSTEAM.getSource());
            spentSteamTank.setTankType(ModFluids.STEAM.getSource());
        } else if (in == ModFluids.HOTSTEAM.getSource()) {
            steamTank.setTankType(ModFluids.SUPERHOTSTEAM.getSource());
            spentSteamTank.setTankType(ModFluids.HOTSTEAM.getSource());
        } else if (in == ModFluids.SUPERHOTSTEAM.getSource()) {
            steamTank.setTankType(ModFluids.ULTRAHOTSTEAM.getSource());
            spentSteamTank.setTankType(ModFluids.SUPERHOTSTEAM.getSource());
        } else {
            // Ultraheiss und alles Unbekannte fallen auf gewoehnlichen Dampf zurueck.
            steamTank.setTankType(ModFluids.STEAM.getSource());
            spentSteamTank.setTankType(ModFluids.SPENTSTEAM.getSource());
        }

        setChanged();
        sendUpdateToClient();
        return true;
    }

    /** Alt: schaltete den Betrieb von Hand. Bleibt fuer den Aufrufer im Block bestehen. */
    public boolean toggleOperational() {
        this.operational = !this.operational;
        setChanged();
        sendUpdateToClient();
        return this.operational;
    }

    public boolean isOperational() {
        return operational;
    }

    private void processTurbine() {
        // 1. Dampf verbrauchen (100% des Tankinhalts/Tick, nur solange operational) und Energie-Potential
        // in das Flywheel laden.
        //
        // 1:1 aus TileEntityTurbineBase: Umsatzverhaeltnis, Ausgabefluid und Energie kommen aus der
        // FT_Coolable-Eigenschaft des Dampfes. Der Ausgang ist damit die naechstniedrigere
        // Dampfstufe (coolsTo), nicht pauschal Altdampf.
        // 1:1: operational ergibt sich aus dem Durchsatz, es ist kein Schalter.
        operational = false;
        FT_Coolable trait = FluidType.getTrait(steamTank.getStoredFluid(), FT_Coolable.class);

        if (trait != null && trait.amountReq > 0 && trait.amountProduced > 0) {
            double eff = trait.getEfficiency(CoolingType.TURBINE) * EFFICIENCY;

            if (eff > 0) {
                spentSteamTank.setTankType(trait.coolsTo);

                int available = steamTank.getFill();
                int inputOps = (int) (Math.min(Math.ceil(available * CONSUMPTION_PERCENT), available) / trait.amountReq);
                int outputOps = (spentSteamTank.getMaxFill() - spentSteamTank.getFill()) / trait.amountProduced;
                int ops = Math.min(inputOps, outputOps);

                if (ops > 0) {
                    operational = true;
                    steamTank.drainMb(ops * trait.amountReq);
                    spentSteamTank.fillMb(trait.coolsTo, ops * trait.amountProduced);

                    maxPower = (long) (ops * trait.heatEnergy * eff);
                    flywheelEnergy += maxPower;
                }
            }
        }

        // 2. Flywheel-Trägheit: die Turbine fährt hoch/runter statt sofort volle Leistung zu liefern.
        // Läuft auch weiter, wenn operational == false, damit das Flywheel sichtbar auslaufen kann.
        spin = flywheelEnergy / FLYWHEEL_MAX_ENERGY;

        long energySpace = Math.max(0L, getMaxEnergyStored() - getEnergyStored());
        long potentialOutput = (long) (Math.max(spin, 0.05D) * maxPower);
        long output = Math.min(Math.min(potentialOutput, flywheelEnergy), energySpace);

        boolean generating = output > 0;
        if (generating) {
            flywheelEnergy -= output;
            setEnergyStored(getEnergyStored() + output);
            setChanged();
            sendUpdateToClient();
        }

        isActive = generating || flywheelEnergy > 0;
    }

    /**
     * Annahmepruefung des Fluid-Handlers. Sie muss dieselbe Bedingung stellen wie die
     * Verarbeitung, seit diese ueber {@link FT_Coolable} laeuft - sonst nimmt die Turbine Dampf an,
     * den sie anschliessend nicht verwerten kann.
     */
    private boolean acceptsAsSteam(net.minecraft.world.level.material.Fluid fluid) {
        FT_Coolable trait = FluidType.getTrait(fluid, FT_Coolable.class);
        return trait != null
                && trait.amountReq > 0
                && trait.amountProduced > 0
                && trait.getEfficiency(CoolingType.TURBINE) > 0;
    }

    public void drops() {
        // Kein Inventar - nichts zu droppen.
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.machine_chungus");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null; // Kein GUI, wie im Original (nur Look-Overlay-HUD, hier per Chat-Nachricht ersetzt)
    }

    @Override
    public int getCurrentMode() {
        return 2; // OUTPUT only, so the energy network treats this as a generator.
    }

    // --- MK2 Fluid-Netzwerk (UNIVERSAL_CONNECTOR-Phantomblöcke der Multiblock-Struktur) ---

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[]{ steamTank, spentSteamTank };
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[]{ steamTank };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[]{ spentSteamTank };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved();
    }

    // --- Accessors ---

    public FluidTank getSteamTank() {
        return steamTank;
    }

    public FluidTank getSpentSteamTank() {
        return spentSteamTank;
    }

    public boolean isActive() {
        return isActive;
    }

    /** 0.0 = Flywheel steht, 1.0 = volle Drehzahl. Steuert Sound-Pitch/Volume und Anim-Geschwindigkeit. */
    public double getSpin() {
        return spin;
    }

    private static final String CHUNGUS_LOOP_SOUND_FACTORY = "com.hbm_m.client.sound.ChungusLoopSoundFactory";

    //? if forge || neoforge {
    @OnlyIn(Dist.CLIENT)
    //?}
    private Object createLoopingSoundReflect(SoundEvent sound) {
        try {
            return Class.forName(CHUNGUS_LOOP_SOUND_FACTORY)
                    .getMethod("create", MachineChungusBlockEntity.class, SoundEvent.class)
                    .invoke(null, this, sound);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public float getAnim(float partialTicks) {
        return prevAnim + (anim - prevAnim) * partialTicks;
    }

    // --- NBT ---

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        steamTank.writeToNBT(tag, "steam");
        spentSteamTank.writeToNBT(tag, "spent");
        tag.putBoolean("active", isActive);
        tag.putBoolean("operational", operational);
        tag.putFloat("anim", anim);
        tag.putFloat("prevAnim", prevAnim);
        tag.putLong("flywheelEnergy", flywheelEnergy);
        tag.putLong("maxPower", maxPower);
        tag.putDouble("spin", spin);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        steamTank.readFromNBT(tag, "steam");
        spentSteamTank.readFromNBT(tag, "spent");
        isActive = tag.getBoolean("active");
        operational = tag.getBoolean("operational");
        anim = tag.getFloat("anim");
        prevAnim = tag.getFloat("prevAnim");
        flywheelEnergy = tag.getLong("flywheelEnergy");
        maxPower = tag.getLong("maxPower");
        spin = tag.getDouble("spin");
    }

    // --- Capabilities ---

    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        // UP = spent output; остальные стороны (steam input) отдаёт базовый fluidHandlerOpt.
        if (cap == ForgeCapabilities.FLUID_HANDLER && side == Direction.UP) {
            return lazySpentHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void setupFluidCapability() {
        // Steam input — обработчик по умолчанию — через базовый fluidHandlerOpt.
        setFluidHandler(new SteamInputHandler(this));
        lazySpentHandler = LazyOptional.of(() -> new SpentSteamOutputHandler(this));
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazySpentHandler.invalidate();
    }

    private static class SteamInputHandler implements IFluidHandler {
        private final MachineChungusBlockEntity be;

        SteamInputHandler(MachineChungusBlockEntity be) {
            this.be = be;
        }

        @Override
        public int getTanks() { return 1; }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            return new net.minecraftforge.fluids.FluidStack(be.steamTank.getTankType(), be.steamTank.getFill());
        }

        @Override
        public int getTankCapacity(int tank) {
            return be.steamTank.getMaxFill();
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack stack) {
            return be.acceptsAsSteam(stack.getFluid());
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !be.acceptsAsSteam(resource.getFluid())) return 0;
            if (be.steamTank.getFill() > 0 && be.steamTank.getTankType() != resource.getFluid()) return 0;
            int space = be.steamTank.getMaxFill() - be.steamTank.getFill();
            int toFill = Math.min(space, resource.getAmount());
            if (toFill <= 0) return 0;
            if (action.execute()) {
                be.steamTank.setTankType(resource.getFluid());
                be.steamTank.fill(be.steamTank.getFill() + toFill);
            }
            return toFill;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            return net.minecraftforge.fluids.FluidStack.EMPTY;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            return net.minecraftforge.fluids.FluidStack.EMPTY;
        }
    }

    private static class SpentSteamOutputHandler implements IFluidHandler {
        private final MachineChungusBlockEntity be;

        SpentSteamOutputHandler(MachineChungusBlockEntity be) {
            this.be = be;
        }

        @Override
        public int getTanks() { return 1; }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            return new net.minecraftforge.fluids.FluidStack(be.spentSteamTank.getTankType(), be.spentSteamTank.getFill());
        }

        @Override
        public int getTankCapacity(int tank) {
            return be.spentSteamTank.getMaxFill();
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack stack) {
            return false;
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || be.spentSteamTank.getFill() <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            if (resource.getFluid() != be.spentSteamTank.getTankType()) return net.minecraftforge.fluids.FluidStack.EMPTY;
            int toDrain = Math.min(resource.getAmount(), be.spentSteamTank.getFill());
            net.minecraftforge.fluids.FluidStack drained = new net.minecraftforge.fluids.FluidStack(be.spentSteamTank.getTankType(), toDrain);
            if (action.execute()) {
                be.spentSteamTank.fill(be.spentSteamTank.getFill() - toDrain);
            }
            return drained;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || be.spentSteamTank.getFill() <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            int toDrain = Math.min(maxDrain, be.spentSteamTank.getFill());
            net.minecraftforge.fluids.FluidStack drained = new net.minecraftforge.fluids.FluidStack(be.spentSteamTank.getTankType(), toDrain);
            if (action.execute()) {
                be.spentSteamTank.fill(be.spentSteamTank.getFill() - toDrain);
            }
            return drained;
        }
    }
    //?}

    @Override
    public AABB getRenderBoundingBox() {
        // Struktur reicht (richtungsabhängig) bis zu 10 Blöcke nach hinten, 4-5 nach vorne/seitlich
        // und 5 nach oben - großzügig symmetrisch geschätzt, unabhängig von der Facing-Richtung.
        return new AABB(worldPosition).inflate(12.0, 0.0, 12.0).expandTowards(0.0, 5.0, 0.0);
    }

    // Энергопорты мультиблока: позиции фантомов структуры, ранее регистрировавшиеся блоком.
    // Ядро (worldPosition) подписывается в BaseMachineBlockEntity.ensureNetworkInitialized().
    @Override
    protected BlockPos[] getExtraEnergyPorts() {
        if (level == null || level.isClientSide) return new BlockPos[0];
        if (!(getBlockState().getBlock() instanceof com.hbm_m.block.machines.MachineChungusBlock block)) return new BlockPos[0];

        var helper = block.getStructureHelper();
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);

        java.util.List<BlockPos> ports = new java.util.ArrayList<>();
        for (BlockPos localPos : helper.getStructureMap().keySet()) {
            if (true) {
                ports.add(helper.getRotatedPos(worldPosition, localPos, facing));
            }
        }
        return ports.toArray(new BlockPos[0]);
    }}
