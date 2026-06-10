package com.hbm_m.api.energy;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.interfaces.IEnergyConnector;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
//? if forge {
import com.hbm_m.capability.ModCapabilities;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
//?}

//? if fabric {
/*import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentProvider;
*///?}

public class SwitchBlockEntity extends BlockEntity implements IEnergyConnector {

    // Capability всегда "живая", но доступ к ней регулируется через getCapability
    //? if forge {
    private final LazyOptional<IEnergyConnector> hbmConnector = LazyOptional.of(() -> this);
     //?}
    public SwitchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SWITCH_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SwitchBlockEntity entity) {
        if (level.isClientSide) return;

        // Если рубильник включен, он ОБЯЗАН быть в сети
        if (state.getValue(SwitchBlock.POWERED)) {
            ServerLevel serverLevel = (ServerLevel) level;
            EnergyNetworkManager manager = EnergyNetworkManager.get(serverLevel);

            if (!manager.hasNode(pos)) {
                manager.addNode(pos);
            }
        }
    }

    private boolean isValidSide(@Nullable Direction side) {
        // [ИСПРАВЛЕНО] Добавлена проверка POWERED.
        // Теперь, если рубильник выключен, он не отдает Capability.
        // Это синхронизирует логику EnergyNetworkManager с состоянием блока.
        BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof SwitchBlock)) return false;

        if (!state.getValue(SwitchBlock.POWERED)) return false; // <--- ВОТ ЭТОГО НЕ ХВАТАЛО

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
        if (this.level != null && !this.level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
        }
    }
    //?}

    @Override
    public boolean canConnectEnergy(Direction side) {
        // Используем ту же логику проверки
        return isValidSide(side);
    }

    @Override
    public void setLevel(Level pLevel) {
        super.setLevel(pLevel);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
        }
        //? if forge {
        hbmConnector.invalidate();
         //?}
    }
}