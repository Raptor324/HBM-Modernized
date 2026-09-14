package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.api.fluids.IFluidStandardSenderMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyModeHolder;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.trait.FT_Coolable;
import com.hbm_m.inventory.fluid.trait.FT_Coolable.CoolingType;
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
 * Industrial Turbine BlockEntity - converts steam to energy (HE).
 * Kein Inventar/GUI - Dampf kommt ausschließlich über Rohre an den UNIVERSAL_CONNECTOR-
 * Phantomblöcken der Multiblock-Struktur an (gleiches Prinzip wie bei Chungus).
 *
 * Dampfumsatz, Ausgabefluid und Energie kommen wie im Original ({@code TileEntityTurbineBase})
 * aus der {@code FT_Coolable}-Eigenschaft des jeweiligen Dampfes. Der Ausgang ist damit die
 * naechstniedrigere Stufe, nicht pauschal Altdampf - erst so laesst sich eine Kaskade bauen:
 * Ultraheissdampf zu Superheissdampf zu Heissdampf zu Dampf zu Altdampf. Auch die Verhaeltnisse
 * kommen von dort (Dampf setzt 100 mB zu 1 mB Altdampf um, die heissen Stufen 1 zu 10).
 * <p>
 * Verbrauch je Tick: 20% des Tankinhalts ({@code consumptionPercent}), Wirkungsgrad 1,0.
 * <p>
 * <p><b>Der Hebel</b> schaltet wie im Original die Dampfstufe durch - und weil diese Turbine
 * {@code doesResizeCompressor() == true} hat, <b>schrumpfen die Tanks dabei auf ein Zehntel</b>.
 * Das ist kein Nebeneffekt, sondern der Sinn der Sache: heisserer Dampf traegt pro Millibucket
 * viel mehr Energie, also braucht man weniger Volumen. Beim Zuruecksetzen auf gewoehnlichen Dampf
 * gehen die Tanks um das Tausendfache wieder auf.</p>
 *
 * <p>Umstellen geht nur im Stillstand.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class MachineIndustrialTurbineBlockEntity extends BaseMachineBlockEntity
        implements IEnergyModeHolder, IFluidStandardReceiverMK2, IFluidStandardSenderMK2 {

    // Capacity constants
    private static final long ENERGY_CAPACITY = 500_000L;
    private static final long ENERGY_EXTRACT_RATE = 10_000L;
    private static final int STEAM_CAPACITY = 64_000;
    private static final int SPENT_STEAM_CAPACITY = 64_000;

    // Conversion constants
    private static final double CONSUMPTION_PERCENT = 0.2D; // Anteil des Tankinhalts, der pro Tick verbraucht wird
    /** Original: {@code TileEntityMachineIndustrialTurbine.efficiency = 1D}. */
    private static final double EFFICIENCY = 1.0D;

    // Flywheel (Spin-up/Spin-down-Trägheit der Turbine). Werte sind an ENERGY_CAPACITY angepasst
    // und ggf. beim Playtesting nachzujustieren.
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
    private float anim = 0.0F;
    private float prevAnim = 0.0F;

    public MachineIndustrialTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUSTRIAL_TURBINE_BE.get(), pos, state,
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

    public static void tick(Level level, BlockPos pos, BlockState state, MachineIndustrialTurbineBlockEntity be) {
        be.prevAnim = be.anim;

        if (level.isClientSide()) {
            if (be.isActive) {
                be.anim += 0.15F;
                if (be.anim > (float) (Math.PI * 2.0)) {
                    be.anim -= (float) (Math.PI * 2.0);
                }
            }
            ClientSoundBootstrap.updateSound(be, be.spin > 0.001D,
                    () -> be.createLoopingSoundReflect(ModSounds.LARGE_TURBINE.get()));
            return;
        }

        be.ensureNetworkInitialized();

        boolean wasActive = be.isActive;

        be.processTurbine();

        // Push energy to network
        if (be.energy > 0 && level.getGameTime() % 10L == 0L) {
            be.updateEnergyDelta(be.getEnergyStored());
        }

        if (wasActive != be.isActive) {
            be.setChanged();
            be.sendUpdateToClient();
        }
    }

    /**
     * 1:1-Port von {@code onLeverPull} mit {@code doesResizeCompressor() == true}: die naechste
     * Dampfstufe, und die Tanks wandern in der Groesse mit.
     *
     * @return false, wenn gerade gearbeitet wird
     */
    public boolean pullLever() {
        if (operational) return false;

        var in = steamTank.getTankType();

        if (in == ModFluids.STEAM.getSource()) {
            steamTank.setTankType(ModFluids.HOTSTEAM.getSource());
            spentSteamTank.setTankType(ModFluids.STEAM.getSource());
            shrinkTanks();
        } else if (in == ModFluids.HOTSTEAM.getSource()) {
            steamTank.setTankType(ModFluids.SUPERHOTSTEAM.getSource());
            spentSteamTank.setTankType(ModFluids.HOTSTEAM.getSource());
            shrinkTanks();
        } else if (in == ModFluids.SUPERHOTSTEAM.getSource()) {
            steamTank.setTankType(ModFluids.ULTRAHOTSTEAM.getSource());
            spentSteamTank.setTankType(ModFluids.SUPERHOTSTEAM.getSource());
            shrinkTanks();
        } else if (in == ModFluids.ULTRAHOTSTEAM.getSource()) {
            steamTank.setTankType(ModFluids.STEAM.getSource());
            spentSteamTank.setTankType(ModFluids.SPENTSTEAM.getSource());
            // Original: mal 1000 - zurueck auf die volle Groesse.
            steamTank.changeTankSize(steamTank.getMaxFill() * 1000);
            spentSteamTank.changeTankSize(spentSteamTank.getMaxFill() * 1000);
        } else {
            steamTank.setTankType(ModFluids.STEAM.getSource());
            spentSteamTank.setTankType(ModFluids.SPENTSTEAM.getSource());
        }

        setChanged();
        sendUpdateToClient();
        return true;
    }

    /** Original: {@code changeTankSize(getMaxFill() / 10)} bei jeder Stufe nach oben. */
    private void shrinkTanks() {
        steamTank.changeTankSize(Math.max(1, steamTank.getMaxFill() / 10));
        spentSteamTank.changeTankSize(Math.max(1, spentSteamTank.getMaxFill() / 10));
    }

    /** Original: {@code operational} - kein Schalter, sondern ob gerade Dampf umgesetzt wird. */
    private boolean operational = false;

    public boolean isOperational() { return operational; }

    private void processTurbine() {
        operational = false;
        // 1. Dampf verbrauchen (20% des aktuellen Tankinhalts/Tick) und Energie-Potential in das Flywheel laden.
        //
        // 1:1 aus TileEntityTurbineBase: Umsatzverhaeltnis, Ausgabefluid und Energie kommen aus der
        // FT_Coolable-Eigenschaft des Dampfes. Der Ausgang ist damit die naechstniedrigere
        // Dampfstufe (coolsTo), nicht pauschal Altdampf - erst so laesst sich eine Turbinenkaskade
        // bauen (Ultraheissdampf -> Superheissdampf -> Heissdampf -> Dampf -> Altdampf).
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
        return Component.translatable("container.hbm_m.industrial_turbine");
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
        return null; // Kein GUI - Dampf läuft ausschließlich über Rohrverbindungen.
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

    private static final String TURBINE_LOOP_SOUND_FACTORY = "com.hbm_m.client.sound.TurbineLoopSoundFactory";

    //? if forge || neoforge {
    @OnlyIn(Dist.CLIENT)
    //?}
    private Object createLoopingSoundReflect(SoundEvent sound) {
        try {
            return Class.forName(TURBINE_LOOP_SOUND_FACTORY)
                    .getMethod("create", MachineIndustrialTurbineBlockEntity.class, SoundEvent.class)
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
        private final MachineIndustrialTurbineBlockEntity be;

        SteamInputHandler(MachineIndustrialTurbineBlockEntity be) {
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
        private final MachineIndustrialTurbineBlockEntity be;

        SpentSteamOutputHandler(MachineIndustrialTurbineBlockEntity be) {
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


    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(
            worldPosition.getX() - 2, worldPosition.getY() - 1, worldPosition.getZ() - 4,
            worldPosition.getX() + 3, worldPosition.getY() + 4, worldPosition.getZ() + 8
        );
    }

    // Энергопорты мультиблока: позиции фантомов структуры, ранее регистрировавшиеся блоком.
    // Ядро (worldPosition) подписывается в BaseMachineBlockEntity.ensureNetworkInitialized().
    @Override
    protected BlockPos[] getExtraEnergyPorts() {
        if (level == null || level.isClientSide) return new BlockPos[0];
        if (!(getBlockState().getBlock() instanceof com.hbm_m.block.machines.MachineIndustrialTurbineBlock block)) return new BlockPos[0];

        var helper = block.getStructureHelper();
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);

        java.util.List<BlockPos> ports = new java.util.ArrayList<>();
        for (BlockPos localPos : helper.getStructureMap().keySet()) {
            if (helper.resolvePartRole(localPos, block).canReceiveEnergy()) {
                ports.add(helper.getRotatedPos(worldPosition, localPos, facing));
            }
        }
        return ports.toArray(new BlockPos[0]);
    }}
