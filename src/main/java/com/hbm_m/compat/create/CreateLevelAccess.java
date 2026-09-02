//? if forge || neoforge {
package com.hbm_m.compat.create;

import net.minecraft.world.level.Level;

/**
 * Reflective доступ к ContraptionWorld / VirtualRenderWorld. Эти типы наследуют
 * {@code net.createmod.catnip.levelWrappers.WrappedLevel} (catnip), которого нет
 * на compile-classpath (Create подключён как slim, transitive=false). Чтобы не
 * тянуть catnip в зависимости, обращаемся через reflection — javac не загружает
 * ContraptionWorld/WrappedLevel, только Object → cast к {@link Level}.
 */
public final class CreateLevelAccess {

    private CreateLevelAccess() {}

    /** {@code Contraption#getContraptionWorld()} — уровень для коллизии (сервер и клиент). */
    public static Level contraptionCollisionLevel(Object contraption) {
        if (contraption == null) return null;
        try {
            return (Level) contraption.getClass().getMethod("getContraptionWorld").invoke(contraption);
        } catch (Throwable t) {
            // Create API переименовал/убрал метод — коллизия двери на этом контрапшене
            // останется дефолтной. Не фатально, но полезно видеть в логе при апгрейде Create.
            com.hbm_m.main.MainRegistry.LOGGER.debug(
                    "[HBM/Create] CreateLevelAccess.contraptionCollisionLevel failed: {}", t.toString());
            return null;
        }
    }

    /** {@code ClientContraption#getRenderLevel()} — VirtualRenderWorld (рендер, клиент). */
    public static Level clientRenderLevel(Object clientContraption) {
        if (clientContraption == null) return null;
        try {
            return (Level) clientContraption.getClass().getMethod("getRenderLevel").invoke(clientContraption);
        } catch (Throwable t) {
            // Аналогично — клиентский рендер-уровень недоступен, анимация пойдёт через fallback.
            com.hbm_m.main.MainRegistry.LOGGER.debug(
                    "[HBM/Create] CreateLevelAccess.clientRenderLevel failed: {}", t.toString());
            return null;
        }
    }
}
//?}
