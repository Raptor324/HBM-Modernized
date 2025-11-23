package com.hbm_m.block.entity;

import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.api.energy.IEnergyConnector;
import com.hbm_m.block.SwitchBlock;
import com.hbm_m.capability.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;

public class SwitchBlockEntity extends BlockEntity implements IEnergyConnector {

    private LazyOptional<IEnergyConnector> hbmConnector = LazyOptional.of(() -> this);

    public SwitchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SWITCH_BE.get(), pos, state);
    }

    // === ДОБАВЛЕН МЕТОД TICK ===
    public static void tick(Level level, BlockPos pos, BlockState state, SwitchBlockEntity entity) {
        if (level.isClientSide) return;

        // Безопасная инициализация в тике
        if (state.getValue(SwitchBlock.POWERED)) {
            ServerLevel serverLevel = (ServerLevel) level;
            EnergyNetworkManager manager = EnergyNetworkManager.get(serverLevel);

            if (!manager.hasNode(pos)) {
                manager.addNode(pos);
            }
        }
    }

    public void updateState(boolean powered) {
        LazyOptional<IEnergyConnector> oldCap = hbmConnector;
        hbmConnector = LazyOptional.of(() -> this);
        oldCap.invalidate();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.HBM_ENERGY_CONNECTOR) {
            if (isValidSide(side)) {
                return hbmConnector.cast();
            }
        }
        return super.getCapability(cap, side);
    }

    private boolean isValidSide(@Nullable Direction side) {
        if (side == null) return true;
        BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof SwitchBlock)) return false;
        Direction facing = state.getValue(SwitchBlock.FACING);
        return side == facing || side == facing.getOpposite();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        hbmConnector.invalidate();
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof SwitchBlock)) return false;
        if (!state.getValue(SwitchBlock.POWERED)) return false;
        return isValidSide(side);
    }

    // === ИСПРАВЛЕН МЕТОД SETLEVEL ===
    @Override
    public void setLevel(Level pLevel) {
        super.setLevel(pLevel);
        // УДАЛЕНО: EnergyNetworkManager.get(...) - это вызывало дедлок!
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
        }
        hbmConnector.invalidate();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (this.level != null && !this.level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
        }
    }
}