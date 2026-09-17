package com.hbm_m.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

/**
 * Форсирует UTF-8 в аппендерах log4j2 (файлы debug.log/latest.log и консоль).
 * <p>
 * Зачем: Forge-конфиг log4j2 строит PatternLayout с платформенным charset
 * (на русской Windows — Cp1251): кириллица и русские месяцы в датах пишутся
 * в Cp1251, и debug.log при чтении как UTF-8 «broken». JVM-аргумент
 * {@code -Dfile.encoding=UTF-8} на это НЕ влияет (layout захватывает дефолт
 * при создании), поэтому подменяем charset в строковых layout'ах рантаймом.
 * <p>
 * Контекст берём у РЕАЛЬНО пишущего логгера ({@code MainRegistry.LOGGER}):
 * {@code LogManager.getContext(false)} из-под мод-класслоадера может вернуть
 * другой (пустой) контекст log4j2 ClassLoaderContextSelector — на этом уже
 * обжигались (патч «зелёный», а файл всё ещё в Cp1251).
 * <p>
 * Вызывается из статического блока {@link MainRegistry}. Ошибки глотаются:
 * патчер не имеет права ронять загрузку мода, но каждая проблема логируется.
 */
public final class LogCharsetPatcher {

    private static volatile boolean applied = false;
    private static Field charsetField;
    private static Field appenderLayoutField;

    private LogCharsetPatcher() {}

    public static synchronized void apply() {
        if (applied) {
            return;
        }
        applied = true;
        try {
            LoggerContext ctx = resolveContext();
            if (ctx == null) {
                MainRegistry.LOGGER.warn("Log charset patch: no core LoggerContext found");
                return;
            }
            Configuration config = ctx.getConfiguration();
            int patched = 0;
            for (Appender appender : config.getAppenders().values()) {
                if (tryPatch(appender)) {
                    patched++;
                }
            }
            ctx.updateLoggers();
            MainRegistry.LOGGER.info("Log charset patch: UTF-8 forced on {} of {} appenders",
                    patched, config.getAppenders().size());
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("Log charset patch failed: {}", t.toString());
        }
    }

    /**
     * Контекст берём через именованный логгер мода: он заведомо пишется в
     * файловые аппендеры, значит его контекст — тот самый, с Forge-конфигом.
     */
    private static LoggerContext resolveContext() {
        try {
            org.apache.logging.log4j.core.Logger coreLogger =
                    (org.apache.logging.log4j.core.Logger) LogManager.getLogger(MainRegistry.LOGGER.getName());
            if (coreLogger != null && coreLogger.getContext() != null) {
                return coreLogger.getContext();
            }
        } catch (Throwable ignored) {
        }
        return LogManager.getContext(false) instanceof LoggerContext ctx ? ctx : null;
    }

    private static boolean tryPatch(Appender appender) {
        try {
            if (!(appender.getLayout() instanceof AbstractStringLayout layout)) {
                MainRegistry.LOGGER.debug("Log charset patch: skip {} (layout {})",
                        appender.getName(), appender.getLayout().getClass().getName());
                return false;
            }
            if (layout.getCharset() == StandardCharsets.UTF_8) {
                return false;
            }
            // Быстрый путь: подмена final-поля charset в AbstractStringLayout.
            // Layout читает его на каждую запись — live-swap без stop/start
            // (пауза аппендера роняла бы записи параллельных тредов).
            if (charsetField == null) {
                charsetField = AbstractStringLayout.class.getDeclaredField("charset");
                charsetField.setAccessible(true);
            }
            charsetField.set(layout, StandardCharsets.UTF_8);
            if (layout.getCharset() == StandardCharsets.UTF_8) {
                MainRegistry.LOGGER.info("Log charset patch: {} -> UTF-8",
                        appender.getName());
                return true;
            }
            // Fallback: пересборка PatternLayout с явным UTF-8 и подмена поля
            // layout в AbstractAppender (на случай, если charset копируется).
            if (rebuildLayout(appender, layout)) {
                MainRegistry.LOGGER.info("Log charset patch: {} rebuilt with UTF-8",
                        appender.getName());
                return true;
            }
            MainRegistry.LOGGER.warn("Log charset patch: {} could not be patched", appender.getName());
            return false;
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("Log charset patch: appender {} failed ({})",
                    appender.getName(), t.toString());
            return false;
        }
    }

    private static boolean rebuildLayout(Appender appender, AbstractStringLayout layout) throws Exception {
        java.lang.String pattern = null;
        for (String fieldName : new String[] {"eventPattern", "pattern"}) {
            try {
                Field f = layout.getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                Object value = f.get(layout);
                if (value instanceof java.lang.String s) {
                    pattern = s;
                    break;
                }
            } catch (NoSuchFieldException ignored) {
            }
        }
        if (pattern == null) {
            return false;
        }
        org.apache.logging.log4j.core.layout.PatternLayout rebuilt =
                org.apache.logging.log4j.core.layout.PatternLayout.newBuilder()
                        .withCharset(StandardCharsets.UTF_8)
                        .withPattern(pattern)
                        .withAlwaysWriteExceptions(true)
                        .build();
        if (appenderLayoutField == null) {
            appenderLayoutField = org.apache.logging.log4j.core.appender.AbstractAppender.class
                    .getDeclaredField("layout");
            appenderLayoutField.setAccessible(true);
        }
        appenderLayoutField.set(appender, rebuilt);
        return rebuilt.getCharset() == StandardCharsets.UTF_8
                && appender.getLayout() == rebuilt;
    }
}
