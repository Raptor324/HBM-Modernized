package com.hbm_m.interfaces;

import java.util.EnumSet;
import java.util.Set;

import net.minecraft.core.Direction;

/**
 * Опциональный интерфейс для контроллеров (и любых BlockEntity),
 * которые хотят получать настройки "разрешённых сторон" для энергии/жидкостей
 * от {@link com.hbm_m.multiblock.MultiblockStructureHelper} при постройке структуры.
 *
 * Пустой набор трактуется как "все стороны" (совместимость).
 */
public interface IMultiblockSidedIO {

    default void setAllowedEnergySides(Set<Direction> sides) {}

    default Set<Direction> getAllowedEnergySides() {
        return EnumSet.allOf(Direction.class);
    }

    default void setAllowedFluidSides(Set<Direction> sides) {}

    default Set<Direction> getAllowedFluidSides() {
        return EnumSet.allOf(Direction.class);
    }
}

