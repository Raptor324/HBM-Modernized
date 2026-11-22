package com.hbm_m.interfaces;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Опциональный мост систем координат для турбовентилятора, стоящего на движущейся
 * конструкции. Реализация добавляется извне (compat-mixin) только при наличии
 * соответствующего мода; без него базовая машина работает в мировых координатах.
 */
public interface TurbofanAirflowFrame {

    Vec3 hbm$localVectorToWorld(Vec3 localVector);

    AABB hbm$worldBoundsToLocal(AABB worldBounds);

    double hbm$distanceSquaredToLocalPosition(Vec3 observerPosition, Vec3 localPosition);

    /** Точка локального пространства конструкции -> мировые координаты. */
    default Vec3 hbm$localPosToWorld(Vec3 localPos) {
        return localPos;
    }
}
