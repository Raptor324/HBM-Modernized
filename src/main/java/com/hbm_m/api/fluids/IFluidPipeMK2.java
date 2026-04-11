package com.hbm_m.api.fluids;

import com.hbm_m.api.network.NodeDirPos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;

/**
 * Проводник-труба: IFluidConnectorMK2 + фабричный метод создания узла.
 * Аналог IFluidPipeMK2 из 1.7.10.
 * Реализуется BlockEntity'ями, которые являются узлами топологии сети.
 */
public interface IFluidPipeMK2 extends IFluidConnectorMK2 {

    Fluid getFluidType();

    /** Создать стандартный FluidNode с 6 соединениями (все грани). */
    default FluidNode createNode(Fluid fluid, BlockPos pos) {
        return new FluidNode(FluidNetProvider.forFluid(fluid), pos).setConnections(
                new NodeDirPos(pos.relative(Direction.EAST),  Direction.EAST),
                new NodeDirPos(pos.relative(Direction.WEST),  Direction.WEST),
                new NodeDirPos(pos.relative(Direction.UP),    Direction.UP),
                new NodeDirPos(pos.relative(Direction.DOWN),  Direction.DOWN),
                new NodeDirPos(pos.relative(Direction.SOUTH), Direction.SOUTH),
                new NodeDirPos(pos.relative(Direction.NORTH), Direction.NORTH)
        );
    }
}
