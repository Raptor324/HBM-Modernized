package com.hbm_m.explosion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.hbm_m.main.MainRegistry;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Совместимость с Lithium/Radium при wholesale-замене секций кратером.
 *
 * <p>Lithium миксинами вешает на {@link LevelChunkSection} приватные поля
 * {@code changeListener} (ChunkSectionChangeCallback) и {@code listeningMask};
 * колбэк регистрируется в LithiumData по позиции секции и оповещает трекеры
 * AI block-sensing сущностей. Когда мы заменяем секцию целиком, старый колбэк
 * остаётся подписанным на уже мёртвый инстанс.
 *
 * <p>Обнаружение структурное (по имени поля на ванильном классе), без привязки
 * к пакету мода — работает с официальным lithium 0.15.x NeoForge (caffeinemc),
 * lithium 0.11.x (me.jellysquid) через Sinytra Connector и форками (Radium),
 * пока они сохраняют имена полей. Любое расхождение — тихий no-op.
 */
public final class LithiumSectionCompat {

    private static volatile boolean initialized;
    /** null — lithium не обнаружен. */
    private static volatile Field changeListenerField;
    /** onChunkSectionInvalidated(SectionPos); есть не во всех версиях. */
    private static volatile Method invalidatedMethod;
    private static volatile boolean failureLogged;

    private LithiumSectionCompat() {}

    /**
     * Секция {@code oldSection} заменяется новой. Если на старой висел lithium-
     * колбэк — помечаем подписанные трекеры грязными (семантика выгрузки секции
     * у самого lithium): трекер перепроверит содержимое и переподпишется на новую
     * секцию при следующей оценке.
     */
    public static void replaced(ServerLevel level, long sectionPos, LevelChunkSection oldSection) {
        if (!initialized) init();
        Field field = changeListenerField;
        if (field == null) return;
        try {
            Object callback = field.get(oldSection);
            if (callback == null) return;
            Method invalidate = invalidatedMethod;
            if (invalidate != null) {
                invalidate.invoke(callback, SectionPos.of(sectionPos));
            }
        } catch (Throwable t) {
            if (!failureLogged) {
                failureLogged = true;
                MainRegistry.LOGGER.warn("Lithium section compat failed, disabling for this session", t);
            }
        }
    }

    private static void init() {
        synchronized (LithiumSectionCompat.class) {
            if (initialized) return;
            initialized = true;
            try {
                Field found = null;
                for (Field f : LevelChunkSection.class.getDeclaredFields()) {
                    if (f.getName().equals("changeListener")) {
                        found = f;
                        break;
                    }
                }
                if (found == null) return;
                found.setAccessible(true);
                changeListenerField = found;
                try {
                    invalidatedMethod = found.getType().getMethod("onChunkSectionInvalidated", SectionPos.class);
                } catch (NoSuchMethodException ignored) {
                    // 0.11.x: инвалидации нет — трекеры остаются на ленивой перепроверке
                }
            } catch (Throwable t) {
                if (!failureLogged) {
                    failureLogged = true;
                    MainRegistry.LOGGER.warn("Lithium section compat init failed", t);
                }
            }
        }
    }
}
