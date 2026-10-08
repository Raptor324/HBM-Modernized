package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.fluids.FluidNetProvider;
import com.hbm_m.api.fluids.FluidNode;
import com.hbm_m.api.fluids.IFluidPipeMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.api.redstoneoverradio.IRORInteractive;
import com.hbm_m.api.redstoneoverradio.IRORValueProvider;
import com.hbm_m.block.machines.FluidValveBlock;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 1:1 {@code TileEntityFluidValve} / {@code TileEntityFluidCounterValve} ({@code TileEntityPipeBaseNT} mit
 * {@code shouldCreateNode = meta == 1}): im Zustand AUS gibt es keinen Knoten, das Netz ist getrennt. Das
 * Zaehlventil addiert jeden Tick den Durchsatz seines Netzes ({@code fluidTracker}) und ist per Funk abfrag- und schaltbar.
 */
public class FluidValveBlockEntity extends BaseHbmBlockEntity implements IFluidPipeMK2, IRORValueProvider, IRORInteractive {

    private static final String NBT_FLUID_TYPE = "FluidType";

    private Fluid fluidType = Fluids.EMPTY;
    private long counter;
    private FluidNode node;

    public FluidValveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_VALVE_BE.get(), pos, state);
    }

    private boolean isOn() {
        BlockState s = getBlockState();
        return s.hasProperty(FluidValveBlock.ON) && s.getValue(FluidValveBlock.ON);
    }

    private boolean isCounter() {
        return getBlockState().getBlock() instanceof FluidValveBlock b && b.getMode() == FluidValveBlock.Mode.COUNTER;
    }

    @Override public Fluid getFluidType() { return fluidType; }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && VanillaFluidEquivalence.sameSubstance(fluid, this.fluidType);
    }

    public long getCounter() { return counter; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidValveBlockEntity te) {
        if (!(level instanceof ServerLevel sl)) return;
        if (te.isOn()) te.ensureNode(sl);
        if (te.isCounter() && te.node != null && te.node.net != null && te.fluidType != Fluids.EMPTY) {
            long add = te.node.net.fluidTracker;
            if (add != 0) {
                te.counter += add;
                te.setChanged();
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    public void setFluidType(Fluid fluid) {
        Fluid prev = this.fluidType;
        this.fluidType = fluid != null ? fluid : Fluids.EMPTY;
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            destroyCurrentNode(serverLevel);
            if (prev != null && prev != Fluids.EMPTY && prev != fluidType) UniNodespace.destroyNode(serverLevel, worldPosition, FluidNetProvider.forFluid(prev));
            if (isOn()) ensureNode(serverLevel);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** {@code updateState}: AUS zerstoert den Knoten. */
    public void updateState() {
        if (level instanceof ServerLevel sl && !isOn()) destroyCurrentNode(sl);
    }

    private void destroyCurrentNode(ServerLevel serverLevel) {
        if (node != null && !node.isExpired()) UniNodespace.destroyNode(serverLevel, node);
        node = null;
    }

    private void ensureNode(ServerLevel serverLevel) {
        if (fluidType == Fluids.EMPTY) return;
        if (node == null || node.isExpired()) {
            var existing = UniNodespace.getNode(serverLevel, worldPosition, FluidNetProvider.forFluid(fluidType));
            if (existing instanceof FluidNode fn && !fn.isExpired()) {
                node = fn;
            } else {
                node = createNode(fluidType, worldPosition);
                UniNodespace.createNode(serverLevel, node);
            }
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel sl) destroyCurrentNode(sl);
        node = null;
        super.setRemoved();
    }

    //? if forge || neoforge {
    @Override
    public void onChunkUnloaded() {
        if (node != null) node.expired = true;
        super.onChunkUnloaded();
    }
    //?}

    // ---- Redstone ueber Funk (nur Zaehlventil) ----
    private void setState(int state) {
        if (level != null) FluidValveBlock.setState(level, worldPosition, state == 1, 1.0F);
    }

    @Override
    public String provideRORValue(String name) {
        if (!isCounter()) return null;
        if ((PREFIX_VALUE + "value").equals(name)) return String.valueOf(counter);
        if ((PREFIX_VALUE + "state").equals(name)) return String.valueOf(isOn() ? 1 : 0);
        return null;
    }

    @Override
    public String[] getFunctionInfo() {
        if (!isCounter()) return new String[0];
        return new String[] { PREFIX_VALUE + "value", PREFIX_VALUE + "state", PREFIX_FUNCTION + "reset", PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "state" };
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if (!isCounter()) return null;
        if (name.equals(PREFIX_FUNCTION + "reset")) {
            counter = 0;
            setChanged();
        } else if (name.equals(PREFIX_FUNCTION + "setstate")) {
            setState(IRORInteractive.parseInt(params[0], 0, 1));
        }
        return null;
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        ResourceLocation loc = BuiltInRegistries.FLUID.getKey(fluidType);
        if (loc != null) tag.putString(NBT_FLUID_TYPE, loc.toString());
        tag.putLong("counter", counter);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains(NBT_FLUID_TYPE)) {
            Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(tag.getString(NBT_FLUID_TYPE)));
            this.fluidType = f != null ? f : Fluids.EMPTY;
        }
        counter = Math.max(tag.getLong("counter"), 0);
    }
}
