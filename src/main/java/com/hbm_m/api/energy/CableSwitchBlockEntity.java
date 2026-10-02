package com.hbm_m.api.energy;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.interfaces.IEnergyConnector;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
//?}

/**
 * 1:1 {@code TileEntityCableSwitch} (fuer {@code cable_switch} und {@code cable_detector}): ein Kabelstueck, das nur
 * im Zustand AN (Metadaten 1) einen Netzknoten bildet - nach allen sechs Seiten wie {@code TileEntityCableBaseNT}.
 */
public class CableSwitchBlockEntity extends BlockEntity implements PowerConductor {

    //? if forge {
    private final LazyOptional<IEnergyConnector> hbmConnector = LazyOptional.of(() -> this);
    //?}

    public CableSwitchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABLE_SWITCH.get(), pos, state);
    }

    private boolean on() {
        BlockState s = getBlockState();
        return s.hasProperty(CableSwitchBlocks.ON) && s.getValue(CableSwitchBlocks.ON);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CableSwitchBlockEntity te) {
        if (level.isClientSide) return;
        ServerLevel sl = (ServerLevel) level;
        if (te.on() && Nodespace.getNode(sl, pos) == null) Nodespace.createNode(sl, te.createNode(pos));
    }

    /** {@code updateState}: AUS zerstoert den Knoten, danach tritt der Block keinem Netz mehr bei. */
    public void updateState() {
        if (level instanceof ServerLevel sl && !on() && Nodespace.getNode(sl, worldPosition) != null) Nodespace.destroyNode(sl, worldPosition);
    }

    private void destroyOwnNode() {
        if (level instanceof ServerLevel sl) Nodespace.destroyNode(sl, worldPosition);
    }

    @Override public boolean canConnectEnergy(Direction side) { return true; }

    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.HBM_ENERGY_CONNECTOR) return hbmConnector.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        hbmConnector.invalidate();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        destroyOwnNode();
    }
    //?}

    @Override
    public void setRemoved() {
        super.setRemoved();
        destroyOwnNode();
    }
}
