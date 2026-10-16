package com.hbm_m.api.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.hbm_m.blockentity.ModBlockEntities;
//? if forge {
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.interfaces.IEnergyConnector;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
//?}

/**
 * BlockEntity для провода.
 * Проводник энергосети: создает узел (PowerNode) в UniNodespace, энергии не хранит.
 * Аналог TileEntityCableBaseNT из 1.7.10.
 */
public class WireBlockEntity extends BlockEntity implements PowerConductor {

    //? if forge {
    private final LazyOptional<IEnergyConnector> hbmConnector = LazyOptional.of(() -> this);
    //?}

    public WireBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRE_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WireBlockEntity entity) {
        if (level.isClientSide) return;

        ServerLevel serverLevel = (ServerLevel) level;
        if (Nodespace.getNode(serverLevel, pos) == null) {
            Nodespace.createNode(serverLevel, entity.createNode(pos));
        }
    }

    private void destroyOwnNode() {
        if (this.level != null && !this.level.isClientSide) {
            Nodespace.destroyNode((ServerLevel) this.level, this.getBlockPos());
        }
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return true;
    }

    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.HBM_ENERGY_CONNECTOR) {
            return hbmConnector.cast();
        }
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
        //? if forge {
        hbmConnector.invalidate();
        //?}
    }

    @Override
    public void setLevel(Level pLevel) {
        super.setLevel(pLevel);
    }
}
