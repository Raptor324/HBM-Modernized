package com.hbm_m.interfaces;

import java.util.EnumSet;
import java.util.Set;

import net.minecraft.core.Direction;

/**
 * Опциональный интерфейс для контроллеров (и любых BlockEntity),
 * которые хотят получать настройки "разрешённых сторон" для энергии/жидкостей
 * от {@link com.hbm_m.multiblock.MultiblockStructureHelper} при постройке структуры.
 *
 * <p>Для обычного {@link #setAllowedFluidSides} пустой набор часто означает «не задано → все стороны»
 * (совместимость). Для tuple из fluidSideMap контроллера используйте
 * {@link #setAllowedFluidSidesFromMultiblockStructure}: там пустой набор означает «ни одна сторона»
 * (если BE поддерживает режим явной структуры).
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

    /**
     * Вызывается при постройке мультиблока, если в {@code fluidSideMap} задан символ контроллера.
     * {@code worldSides} — разрешённые стороны после поворота FACING; пустой набор = закрыть все грани контроллера для жидкости.
     */
    default void setAllowedFluidSidesFromMultiblockStructure(Set<Direction> worldSides) {
        setAllowedFluidSides(worldSides);
    }
}
