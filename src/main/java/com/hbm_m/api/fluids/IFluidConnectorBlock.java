package com.hbm_m.api.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.Fluid;

/**
 * Аналог {@code IFluidConnectorBlock} из 1.7.10: блок БЕЗ block entity, к которому трубе всё равно
 * разрешено подключаться.
 *
 * <p>{@link IFluidConnectorMK2} реализуют block entity — этого достаточно почти всем машинам, но не
 * блокам вроде парового коннектора РБМК ({@code RBMKLoaderBlock}), которые в оригинале не имеют
 * тайла вовсе и служат лишь точкой подключения. Без этого интерфейса
 * {@code FluidDuctBlock#canConnectTo} упирался в {@code level.getBlockEntity(neighborPos) != null}
 * и такой блок никогда не получал "рукав" трубы.</p>
 */
public interface IFluidConnectorBlock {

    /**
     * @param fluid жидкость трубы ({@link net.minecraft.world.level.material.Fluids#EMPTY} —
     *              неокрашенная труба)
     * @param dir   грань ЭТОГО блока, к которой прилегает труба
     */
    boolean canConnect(Fluid fluid, LevelReader level, BlockPos pos, Direction dir);
}
