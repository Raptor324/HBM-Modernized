package com.hbm_m.api.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

/**
 * Кросс-платформенный доступ к fluid-handler capability блока/BlockEntity.
 *
 * <p><b>Главная разница 1.20.1 ↔ 1.21.1:</b>
 * <ul>
 *   <li><b>Forge 1.20.1</b>: {@code be.getCapability(ForgeCapabilities.FLUID_HANDLER, side)} →
 *       {@code LazyOptional<IFluidHandler>}</li>
 *   <li><b>NeoForge 1.21.1</b>: {@code level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side)} →
 *       {@code IFluidHandler} напрямую (без LazyOptional). Запрос идёт через Level, не через BE.</li>
 *   <li><b>Fabric</b>: {@code FluidStorage.SIDED.find(level, pos, state, be, side)} (Transfer API)</li>
 * </ul>
 *
 * <p>Используйте в жидкостных сетях/адаптерах вместо прямого
 * {@code be.getCapability(ForgeCapabilities.FLUID_HANDLER, ...)}.
 *
 * <p><b>Тип возвращаемого значения</b> {@code IFluidHandler} отличается пакетом на forge vs neoforge,
 * поэтому {@link #getFluidHandler} имеет платформенно-специфичную сигнатуру через {@code //? if}.
 */
public final class FluidBlockAccess {
    private FluidBlockAccess() {}

    /**
     * Получить IFluidHandler блока по позиции и стороне.
     *
     * @param level уровень
     * @param pos   позиция блока
     * @param side  сторона (может быть {@code null} для «без стороны»)
     * @return handler или {@code null}, если блок не предоставляет fluid-capability
     */
    //? if forge {
    @Nullable
    public static net.minecraftforge.fluids.capability.IFluidHandler getFluidHandler(Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || be.isRemoved()) return null;
        return be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
    }
    //?}
    //? if neoforge {
    /*@Nullable
    public static net.neoforged.neoforge.fluids.capability.IFluidHandler getFluidHandler(Level level, BlockPos pos, @Nullable Direction side) {
        // На NeoForge запрос идёт через Level, не через BlockEntity.getCapability.
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || be.isRemoved()) return null;
        return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, side);
    }
    *///?}
}
