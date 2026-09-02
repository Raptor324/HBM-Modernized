package com.hbm_m.interfaces;

import org.jetbrains.annotations.Nullable;

// Интерфейс для части мультиблочной структуры. Позволяет частям знать позицию контроллера и свою роль в структуре.
// Используется вместе с MultiblockStructureHelper для управления мультиблочными структурами.

import net.minecraft.core.BlockPos;

import com.hbm_m.multiblock.PartRole;


public interface IMultiblockPart {

    /**
     * @return Позиция главного блока-контроллера, или null, если она не установлена.
     */
    @Nullable
    BlockPos getControllerPos();

    /**
     * Устанавливает позицию главного блока-контроллера.
     * Этот метод вызывается из MultiblockStructureHelper при постройке структуры.
     * @param pos Позиция контроллера.
     */
    void setControllerPos(BlockPos pos);

    /**
     * Оффсет этой части относительно контроллера в ЛОКАЛЬНОЙ сетке структуры
     * (до применения поворота facing). Вращение-инвариантен: не зависит от того,
     * как структура повёрнута в мире.
     *
     * <p>Сохраняется в NBT части. Когда contraption (Create / Aeronautics / Sable)
     * разбирается и часть оказывается в мире на новых координатах, часть может
     * {@link net.minecraft.core.Direction#getNearest детерминированно} вычислить,
     * где обязан стоять её контроллер:
     * {@code controllerPos = partWorldPos - rotate(localOffsetFromController, partFacing)}.
     *
     * <p>Это работает потому, что Create's {@code StructureTransform} вращает и позиции,
     * и blockstate (включая FACING) одним и тем же поворотом R вокруг оси Y, а Y-осевые
     * повороты коммутативны: {@code R(rotate(v, F)) = rotate(v, R(F)) = rotate(v, newFacing)}.
     * Значит формула остаётся верной после любого Y-осевого поворота контрапшена.
     *
     * <p>Для контрапшенов с наклоном/креном (Aeronautics pitch/roll) формула может
     * дать неверный результат — тогда работает fallback: радиус-поиск в
     * {@link com.hbm_m.multiblock.MultiblockStructureHelper#relinkOrphanedPart}.
     *
     * @return локальный оффсет от контроллера, или null если не задан (старый NBT / не часть структуры).
     */
    @Nullable
    default BlockPos getLocalOffsetFromController() {
        return null;
    }

    /**
     * Устанавливает локальный оффсет части относительно контроллера (в сетке структуры,
     * до поворота facing). Вызывается {@link com.hbm_m.multiblock.MultiblockStructureHelper}
     * при постройке / перепривязке структуры.
     *
     * @param offset оффсет в локальной сетке ({@code gridPos - controllerOffset}), или null для сброса.
     */
    default void setLocalOffsetFromController(@Nullable BlockPos offset) {
        // no-op по умолчанию — части, не хранящие оффсет (старые реализации), игнорируют.
    }

    /**
     * Устанавливает роль для этой части. Вызывается контроллером при постройке.
     * @param role Роль, назначенная этой части.
     */
    void setPartRole(PartRole role);

    /**
     * Возвращает текущую роль части (может быть DEFAULT если не назначено).
     */
    PartRole getPartRole();

    // Метод для работы с направлениями лестниц.
    void setAllowedClimbSides(java.util.Set<net.minecraft.core.Direction> sides);
    
    java.util.Set<net.minecraft.core.Direction> getAllowedClimbSides();

    /**
     * Стороны, с которых часть-коннектор принимает/отдаёт энергию (мировые направления после постройки).
     * Для частей без роли энергоконнектора можно не вызывать; по умолчанию - см. default-реализацию.
     */
    default void setAllowedEnergySides(java.util.Set<net.minecraft.core.Direction> sides) {
        // no-op для частей без хранения (например двери)
    }

    /**
     * @return разрешённые стороны энергии; пустой набор на коннекторе трактуется как «все стороны» (совместимость).
     */
    default java.util.Set<net.minecraft.core.Direction> getAllowedEnergySides() {
        return java.util.EnumSet.allOf(net.minecraft.core.Direction.class);
    }

    /**
     * Стороны, с которых часть-коннектор принимает/отдаёт жидкости (мировые направления после постройки).
     * Для частей без роли жидкостного коннектора можно не вызывать; по умолчанию - см. default-реализацию.
     */
    default void setAllowedFluidSides(java.util.Set<net.minecraft.core.Direction> sides) {
        // no-op по умолчанию
    }

    /**
     * @return разрешённые стороны для жидкостей; пустой набор трактуется как «все стороны» (совместимость).
     */
    default java.util.Set<net.minecraft.core.Direction> getAllowedFluidSides() {
        return java.util.EnumSet.allOf(net.minecraft.core.Direction.class);
    }
}