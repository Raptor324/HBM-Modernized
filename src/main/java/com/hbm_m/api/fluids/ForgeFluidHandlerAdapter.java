package com.hbm_m.api.fluids;

import java.util.Objects;

import com.hbm_m.block.entity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.jetbrains.annotations.Nullable;

/**
 * Адаптер: оборачивает соседний BlockEntity с Forge IFluidHandler как MK2-транзивер.
 * Используется FluidDuctBlockEntity для регистрации обычных машин в жидкостной сети.
 *
 * Поддерживает только pressure=0 (Forge IFluidHandler не знает о давлении).
 */
public class ForgeFluidHandlerAdapter implements IFluidStandardTransceiverMK2 {

    private final Level level;
    /** Позиция BE для {@link #getCapability} (для FLUID_CONNECTOR мультиблока — контроллер). */
    private final BlockPos machinePos;
    /**
     * Сторона {@code machinePos} для capability; для жидкостного коннектора цистерны — {@code null}
     * (как у {@code UniversalMachinePartBlockEntity} при делегировании на контроллер).
     */
    @Nullable
    private final Direction sideOfMachineFacingDuct;
    private final Fluid targetFluid;

    public ForgeFluidHandlerAdapter(Level level, BlockPos neighborPos,
                                    Direction sideOfNeighborFacingDuct, Fluid targetFluid) {
        this.level = level;
        this.targetFluid = targetFluid;
        BlockEntity neighborBe = level.getBlockEntity(neighborPos);
        if (neighborBe instanceof IMultiblockPart part
                && part.getControllerPos() != null
                && part.getPartRole() == PartRole.FLUID_CONNECTOR) {
            this.machinePos = part.getControllerPos().immutable();
            this.sideOfMachineFacingDuct = null;
        } else {
            this.machinePos = neighborPos.immutable();
            this.sideOfMachineFacingDuct = sideOfNeighborFacingDuct;
        }
    }

    // --- ILoadedEntry ---

    @Override
    public boolean isLoaded() {
        BlockEntity be = level.getBlockEntity(machinePos);
        return be != null && !be.isRemoved();
    }

    // --- IFluidConnectorMK2 ---

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fluid == targetFluid && fromDir != null;
    }

    // --- IFluidUserMK2 ---

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[0]; // адаптер не имеет собственных HBM-баков
    }

    // --- IFluidStandardSenderMK2 ---

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[0]; // используем прямой override ниже
    }

    @Override
    public long getFluidAvailable(Fluid fluid, int pressure) {
        if (fluid != targetFluid || pressure != 0) return 0;
        IFluidHandler handler = getHandler();
        if (handler == null) return 0;
        FluidStack simulated = handler.drain(new FluidStack(fluid, Integer.MAX_VALUE), FluidAction.SIMULATE);
        return simulated.isEmpty() ? 0 : simulated.getAmount();
    }

    @Override
    public void useUpFluid(Fluid fluid, int pressure, long amount) {
        if (fluid != targetFluid || pressure != 0 || amount <= 0) return;
        IFluidHandler handler = getHandler();
        if (handler == null) return;
        handler.drain(new FluidStack(fluid, clampInt(amount)), FluidAction.EXECUTE);
    }

    @Override
    public int[] getProvidingPressureRange(Fluid fluid) {
        return fluid == targetFluid ? IFluidUserMK2.DEFAULT_PRESSURE_RANGE : new int[]{ 1, 0 };
    }

    // --- IFluidStandardReceiverMK2 ---

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[0];
    }

    @Override
    public long getDemand(Fluid fluid, int pressure) {
        if (fluid != targetFluid || pressure != 0) return 0;
        IFluidHandler handler = getHandler();
        if (handler == null) return 0;
        return handler.fill(new FluidStack(fluid, Integer.MAX_VALUE), FluidAction.SIMULATE);
    }

    /**
     * Как в 1.7.10 {@code TileEntityMachineFluidTank#getFluidPriority}: цистерна в режиме «Буфер»
     * получает низкий приоритет приёма, чтобы обычные приёмники (ввод/вывод) забирали жидкость первыми.
     */
    @Override
    public ConnectionPriority getFluidPriority() {
        BlockEntity be = level.getBlockEntity(machinePos);
        if (be instanceof MachineFluidTankBlockEntity tank
                && !tank.hasExploded
                && tank.getMode() == 1) {
            return ConnectionPriority.LOW;
        }
        return ConnectionPriority.NORMAL;
    }

    @Override
    public long transferFluid(Fluid fluid, int pressure, long amount) {
        if (fluid != targetFluid || pressure != 0 || amount <= 0) return amount;
        IFluidHandler handler = getHandler();
        if (handler == null) return amount;
        int filled = handler.fill(new FluidStack(fluid, clampInt(amount)), FluidAction.EXECUTE);
        return amount - filled;
    }

    @Override
    public int[] getReceivingPressureRange(Fluid fluid) {
        return fluid == targetFluid ? IFluidUserMK2.DEFAULT_PRESSURE_RANGE : new int[]{ 1, 0 };
    }

    // --- Helpers ---

    private IFluidHandler getHandler() {
        BlockEntity be = level.getBlockEntity(machinePos);
        if (be == null || be.isRemoved()) return null;
        return be.getCapability(ForgeCapabilities.FLUID_HANDLER, sideOfMachineFacingDuct)
                .orElse(null);
    }

    private static int clampInt(long v) {
        return (int) Math.min(v, Integer.MAX_VALUE);
    }

    /** Объект создаётся каждый раз при необходимости, идентичность определяется по (pos, dir, fluid). */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ForgeFluidHandlerAdapter other)) return false;
        return machinePos.equals(other.machinePos)
                && Objects.equals(sideOfMachineFacingDuct, other.sideOfMachineFacingDuct)
                && targetFluid == other.targetFluid;
    }

    @Override
    public int hashCode() {
        int result = machinePos.hashCode();
        result = 31 * result + (sideOfMachineFacingDuct != null ? sideOfMachineFacingDuct.ordinal() : 0);
        result = 31 * result + System.identityHashCode(targetFluid);
        return result;
    }
}
