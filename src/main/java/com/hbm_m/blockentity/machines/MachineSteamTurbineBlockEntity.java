package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.trait.FT_Coolable;
import com.hbm_m.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm_m.inventory.menu.MachineSteamTurbineMenu;
import com.hbm_m.interfaces.IEnergyModeHolder;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

public class MachineSteamTurbineBlockEntity extends BaseMachineBlockEntity implements IEnergyModeHolder {

    public static final int SLOT_FLUID_ID_IN = 0;
    public static final int SLOT_FLUID_ID_OUT = 1;
    public static final int SLOT_INPUT_IO_IN = 2;
    public static final int SLOT_INPUT_IO_OUT = 3;
    public static final int SLOT_BATTERY = 4;
    public static final int SLOT_OUTPUT_IO_IN = 5;
    public static final int SLOT_OUTPUT_IO_OUT = 6;
    public static final int INVENTORY_SIZE = 7;

    private static final int STEAM_CONSUMPTION_RATE = 8;
    private static final int TANK_CAPACITY = 24_000;

    private final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(TANK_CAPACITY),
            new FluidTank(ModFluids.SPENTSTEAM.getSource(), TANK_CAPACITY)
    };

    private int progress = 0;
    private static final int MAX_PROGRESS = 200;
    private boolean active = false;

    public MachineSteamTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_TURBINE_BE.get(), pos, state, INVENTORY_SIZE, 1_000_000L, 0L, 50_000L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSteamTurbineBlockEntity be) {
        if (level.isClientSide()) return;
        be.ensureNetworkInitialized();
        boolean wasActive = be.active;

        be.active = be.processSteam();
        if (be.active) {
            be.progress = (be.progress + 1) % MAX_PROGRESS;
        } else {
            be.progress = 0;
        }

        if (be.energy > 0L && level.getGameTime() % 10L == 0L) {
            be.updateEnergyDelta(be.getEnergyStored());
        }

        if (wasActive != be.active || be.active) {
            be.setChanged();
            be.sendUpdateToClient();
        }
    }

    /**
     * 1:1 aus {@code TileEntityMachineTurbine}: Umsatzverhaeltnis, Ausgabefluid und Energie kommen
     * aus der {@link FT_Coolable}-Eigenschaft des Dampfes. Der Ausgang ist damit die
     * naechstniedrigere Dampfstufe ({@code coolsTo}), nicht pauschal Altdampf - erst so laesst sich
     * eine Turbinenkaskade bauen. Auch die Verhaeltnisse kommen von dort: Dampf setzt 100 mB zu
     * 1 mB Altdampf um, die heissen Stufen 1 zu 10.
     */
    /**
     * Annahmepruefung des Fluid-Handlers. Sie muss dieselbe Bedingung stellen wie die
     * Verarbeitung, seit diese ueber {@link FT_Coolable} laeuft - sonst nimmt die Turbine Dampf an,
     * den sie anschliessend nicht verwerten kann.
     */
    private boolean acceptsAsSteam(Fluid fluid) {
        FT_Coolable trait = FluidType.getTrait(fluid, FT_Coolable.class);
        return trait != null
                && trait.amountReq > 0
                && trait.amountProduced > 0
                && trait.getEfficiency(CoolingType.TURBINE) > 0;
    }

    private boolean processSteam() {
        FT_Coolable trait = FluidType.getTrait(tanks[0].getStoredFluid(), FT_Coolable.class);

        if (trait == null || trait.amountReq <= 0 || trait.amountProduced <= 0) {
            tanks[1].setTankType(ModFluids.NONE.getSource());
            return false;
        }

        double eff = trait.getEfficiency(CoolingType.TURBINE);
        if (eff <= 0) {
            tanks[1].setTankType(ModFluids.NONE.getSource());
            return false;
        }

        tanks[1].setTankType(trait.coolsTo);

        int inputOps  = tanks[0].getFill() / trait.amountReq;
        int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / trait.amountProduced;
        // Original: cap = maxSteamPerTick / amountReq - die Drossel je Tick.
        int cap       = STEAM_CONSUMPTION_RATE / trait.amountReq;
        int ops       = Math.min(inputOps, Math.min(outputOps, cap));
        if (ops <= 0) return false;

        tanks[0].drainMb(ops * trait.amountReq);
        tanks[1].fillMb(trait.coolsTo, ops * trait.amountProduced);

        long output = (long) (ops * trait.heatEnergy * eff);
        setEnergyStored(Math.min(getMaxEnergyStored(), getEnergyStored() + output));
        return true;
    }

    public FluidTank[] getTanks() {
        return tanks;
    }

    public int getPowerScaled(int scale) {
        long max = Math.max(getMaxEnergyStored(), 1L);
        return (int) Math.min(scale, getEnergyStored() * scale / max);
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return MAX_PROGRESS;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public int getCurrentMode() {
        return 2; // OUTPUT only, so the energy network treats this as a generator.
    }

    @Override
    protected void writeNbtData(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("progress", progress);
        tag.putBoolean("active", active);
        tanks[0].writeToNBT(tag, "input");
        tanks[1].writeToNBT(tag, "output");
    }

    @Override
    protected void readNbtData(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        progress = tag.getInt("progress");
        active = tag.getBoolean("active");
        tanks[0].readFromNBT(tag, "input");
        tanks[1].readFromNBT(tag, "output");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.steam_turbine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineSteamTurbineMenu.create(id, inv, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.steam_turbine");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_ID_IN -> stack.getItem() instanceof IItemFluidIdentifier || stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_FLUID_ID_OUT, SLOT_INPUT_IO_OUT, SLOT_OUTPUT_IO_OUT -> false;
            case SLOT_INPUT_IO_IN, SLOT_OUTPUT_IO_IN -> PlatformHooks.isFluidContainer(stack);
            case SLOT_BATTERY -> stack.getItem() instanceof ItemCreativeBattery || isEnergyProviderItem(stack) || isEnergyReceiverItem(stack);
            default -> false;
        };
    }

    //? if forge {
    @Override
    protected void setupFluidCapability() {
        setFluidHandler(new UnifiedFluidHandler(this));
    }

    private static class UnifiedFluidHandler implements IFluidHandler {
        private final MachineSteamTurbineBlockEntity be;

        UnifiedFluidHandler(MachineSteamTurbineBlockEntity be) {
            this.be = be;
        }

        @Override
        public int getTanks() { return 2; }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            if (tank == 0) {
                return new net.minecraftforge.fluids.FluidStack(be.tanks[0].getTankType(), be.tanks[0].getFill());
            }
            if (tank == 1) {
                return new net.minecraftforge.fluids.FluidStack(be.tanks[1].getTankType(), be.tanks[1].getFill());
            }
            return net.minecraftforge.fluids.FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            if (tank == 0) return be.tanks[0].getMaxFill();
            if (tank == 1) return be.tanks[1].getMaxFill();
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack stack) {
            return tank == 0 && be.acceptsAsSteam(stack.getFluid());
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !be.acceptsAsSteam(resource.getFluid())) return 0;
            int space = be.tanks[0].getMaxFill() - be.tanks[0].getFill();
            int toFill = Math.min(space, resource.getAmount());
            if (toFill <= 0) return 0;
            if (action.execute()) {
                be.tanks[0].fillMb(resource.getFluid(), toFill);
            }
            return toFill;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || be.tanks[1].getFill() <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            if (!VanillaFluidEquivalence.sameSubstance(resource.getFluid(), be.tanks[1].getTankType())) {
                return net.minecraftforge.fluids.FluidStack.EMPTY;
            }
            int toDrain = Math.min(resource.getAmount(), be.tanks[1].getFill());
            net.minecraftforge.fluids.FluidStack drained = new net.minecraftforge.fluids.FluidStack(be.tanks[1].getTankType(), toDrain);
            if (action.execute()) {
                be.tanks[1].drainMb(toDrain);
            }
            return drained;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || be.tanks[1].getFill() <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            int toDrain = Math.min(maxDrain, be.tanks[1].getFill());
            net.minecraftforge.fluids.FluidStack drained = new net.minecraftforge.fluids.FluidStack(be.tanks[1].getTankType(), toDrain);
            if (action.execute()) {
                be.tanks[1].drainMb(toDrain);
            }
            return drained;
        }
    }
    //?}
}
