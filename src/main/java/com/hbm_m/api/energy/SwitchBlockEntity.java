package com.hbm_m.api.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;

//? if forge {
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.interfaces.IEnergyConnector;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
//?}

/**
 * Выключатель энергосети — аналог CableSwitch из 1.7.10.
 * В выключенном (POWERED=false... по факту при отсутствии питания/включенном состоянии)
 * состоянии узел уничтожается, разрывая сеть.
 */
public class SwitchBlockEntity extends BlockEntity implements PowerConductor {

    //? if forge {
    private final LazyOptional<IEnergyConnector> hbmConnector = LazyOptional.of(() -> this);
    //?}

    public SwitchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SWITCH_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SwitchBlockEntity entity) {
        if (level.isClientSide) return;

        ServerLevel serverLevel = (ServerLevel) level;
        boolean conducting = state.getValue(SwitchBlock.POWERED);

        if (conducting && Nodespace.getNode(serverLevel, pos) == null) {
            Nodespace.createNode(serverLevel, entity.createNode(pos));
        } else if (!conducting && Nodespace.getNode(serverLevel, pos) != null) {
            Nodespace.destroyNode(serverLevel, pos);
        }
    }

    private void destroyOwnNode() {
        if (this.level != null && !this.level.isClientSide) {
            Nodespace.destroyNode((ServerLevel) this.level, this.getBlockPos());
        }
    }

    private boolean isValidSide(@Nullable Direction side) {
        BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof SwitchBlock)) return false;
        if (!state.getValue(SwitchBlock.POWERED)) return false;

        if (side == null) return true;
        Direction facing = state.getValue(SwitchBlock.FACING);
        return side == facing || side == facing.getOpposite();
    }

    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.HBM_ENERGY_CONNECTOR) {
            if (isValidSide(side)) {
                return hbmConnector.cast();
            }
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
    public boolean canConnectEnergy(Direction side) {
        return isValidSide(side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        destroyOwnNode();
        //? if forge {
        hbmConnector.invalidate();
        //?}
    }
}
