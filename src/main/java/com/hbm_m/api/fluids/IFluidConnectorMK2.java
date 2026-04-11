package com.hbm_m.api.fluids;

import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;

/**
 * Базовый интерфейс для всех блок-энтити, участвующих в MK2 fluid сети.
 * Аналог IFluidConnectorMK2 из 1.7.10.
 */
public interface IFluidConnectorMK2 {

    /**
     * Может ли этот коннектор принимать/отдавать жидкость указанного типа с указанной стороны.
     *
     * @param fluid тип жидкости
     * @param fromDir направление ОТ ЭТОГО узла к соседу (сторона, через которую идёт соединение)
     */
    default boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }
}
