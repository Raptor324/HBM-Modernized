package com.hbm_m.api.energy;

import com.hbm_m.block.entity.ModBlockEntities;
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

/**
 * BlockEntity для провода.
 * Это "тупой" коннектор - не хранит энергию, только соединяет блоки в сеть.
 */
public class WireBlockEntity extends BlockEntity implements IEnergyConnector {

    private final LazyOptional<IEnergyConnector> hbmConnector = LazyOptional.of(() -> this);

    public WireBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRE_BE.get(), pos, state);
    }

    /**
     * Периодически проверяем, есть ли мы в сети.
     * Это нужно на случай если провод был размещён раньше соседних машин.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, WireBlockEntity entity) {
        if (level.isClientSide) return;

        ServerLevel serverLevel = (ServerLevel) level;
        EnergyNetworkManager manager = EnergyNetworkManager.get(serverLevel);

        // ПРОВЕРКА КАЖДЫЙ ТИК (для надежности, но можно оптимизировать boolean флагом initialized)
        // hasNode - быстрая операция (проверка HashMap), так что это не ударит по TPS.
        if (!manager.hasNode(pos)) {
            manager.addNode(pos);
        }
    }

    // --- IEnergyConnector ---
    @Override
    public boolean canConnectEnergy(Direction side) {
        // Провод соединяется со всех сторон
        return true;
    }

    // --- Capabilities ---
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
    public void setRemoved() {
        super.setRemoved();
        // [ВАЖНО!] Сообщаем сети, что мы удалены
        if (this.level != null && !this.level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        // [ВАЖНО!] Также сообщаем при выгрузке чанка
        if (this.level != null && !this.level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
        }
    }

    // И при загрузке/установке блока:
    @Override
    public void setLevel(Level pLevel) {
        super.setLevel(pLevel);

    }
}

