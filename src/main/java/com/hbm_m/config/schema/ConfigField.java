package com.hbm_m.config.schema;

import com.hbm_m.config.ModClothConfig;
import net.minecraft.util.Mth;

import java.util.Objects;

/**
 * Метаданные одного конфигурационного поля + адаптер доступа к живому значению.
 *
 * <p>Поле описывает, как его показывать в GUI (категория, границы, тултип),
 * на какой стороне оно лежит, в какой режим применяется, и предоставляет
 * типобезопасный доступ get/set к соответствующему полю {@link ModClothConfig}.
 *
 * <p><b>Зачем нужен адаптер:</b> GUI и JSON-сериализация работают со схемой
 * одинаково, не зная конкретного поля. Это удаляет необходимость менять
 * ~120 call-сайтов {@code ModClothConfig.get().field} — схема лишь читает/пишет
 * существующие публичные поля POJO-холдера.
 */
public final class ConfigField {

    /**
     * Тип данных поля (определяет виджет: boolean→toggle, int/long/double/float→slider/editbox, enum→cycle).
     * Не храним сам тип класса, а выводим enum для удобства построения виджета.
     */
    public enum FieldType {
        BOOLEAN,
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        ENUM
    }

    private final String key;           // Уникальный ключ (обычно = имя поля). Для вложенных: parent.child
    private final ConfigSide side;      // CLIENT / SERVER
    private final ApplyMode applyMode;
    private final String category;      // Категория в GUI (совпадает с группировкой оригинала)
    private final String fieldName;     // Имя поля в ModClothConfig (для верхнего уровня) или во вложенном объекте
    private final String parentObject;  // Имя поля-объекта в ModClothConfig (null → верхний уровень)
    private final FieldType type;
    private final Double min;           // Нижняя граница (для числовых полей; null = без клэмпа)
    private final Double max;           // Верхняя граница
    private String comment = null;      // Комментарий в JSON (для ручного редактирования пользователем; null = без комментария)

    private ConfigField(Builder b) {
        this.key = Objects.requireNonNull(b.key);
        this.side = Objects.requireNonNull(b.side);
        this.applyMode = Objects.requireNonNull(b.applyMode);
        this.category = Objects.requireNonNull(b.category);
        this.parentObject = b.parentObject;
        this.fieldName = b.fieldName != null ? b.fieldName : last(b.key);
        this.type = Objects.requireNonNull(b.type);
        this.min = b.min;
        this.max = b.max;
    }

    // ================================================================
    // Builder (единая точка построения; покрывает верхний уровень и вложенные поля)
    //
    // Примеры:
    //   ConfigField.builder("enableRadiation", FieldType.BOOLEAN).server().live().category("general").build()
    //   ConfigField.builder("maxPlayerRad", FieldType.FLOAT).server().live().category("player").range(1F, 100_000F).build()
    //   ConfigField.builder("frackingTower.maxPower", FieldType.LONG).server().live().category("machines")
    //              .parent("frackingTower").range(0L, 100_000_000_000L).build()
    // ================================================================

    public static Builder builder(String key, FieldType type) {
        return new Builder(key, type);
    }

    public static final class Builder {
        private final String key;
        private final FieldType type;
        private ConfigSide side = ConfigSide.SERVER;
        private ApplyMode applyMode = ApplyMode.LIVE;
        private String category = "general";
        private String parentObject;   // null → верхний уровень
        private String fieldName;      // null → last(key)
        private Double min;
        private Double max;

        private Builder(String key, FieldType type) {
            this.key = Objects.requireNonNull(key);
            this.type = Objects.requireNonNull(type);
        }

        public Builder side(ConfigSide s) { this.side = s; return this; }
        public Builder server() { return side(ConfigSide.SERVER); }
        public Builder client() { return side(ConfigSide.CLIENT); }

        public Builder applyMode(ApplyMode m) { this.applyMode = m; return this; }
        public Builder live() { return applyMode(ApplyMode.LIVE); }
        public Builder requiresRestart() { return applyMode(ApplyMode.REQUIRES_RESTART); }
        public Builder requiresResourceReload() { return applyMode(ApplyMode.REQUIRES_RESOURCE_RELOAD); }

        public Builder category(String c) { this.category = c; return this; }

        /** Вложенный объект (имя поля-объекта в ModClothConfig). */
        public Builder parent(String parentObject) { this.parentObject = parentObject; return this; }

        /** Границы числового поля (clamping). Для boolean/enum игнорируются. */
        public Builder range(double min, double max) { this.min = min; this.max = max; return this; }

        /** Явное имя поля (по умолчанию — сегмент ключа после последней точки). */
        public Builder fieldName(String name) { this.fieldName = name; return this; }

        public ConfigField build() { return new ConfigField(this); }
    }

    /** Ключ "parent.child" → "child" (имя поля вложенного объекта). */
    private static String last(String key) {
        int i = key.lastIndexOf('.');
        return i >= 0 ? key.substring(i + 1) : key;
    }

    // ================================================================
    // Компактные фабрики (делегируют в Builder — единая точка построения).
    // Используются ConfigSchema для компактной регистрации ~80 полей.
    // Для ad-hoc вариантов (GUI/сеть) есть полный builder(key, type).
    // ================================================================

    /** Boolean, верхний уровень. */
    public static ConfigField bool(String key, ConfigSide side, ApplyMode mode, String category) {
        return builder(key, FieldType.BOOLEAN).side(side).applyMode(mode).category(category).build();
    }

    /** Boolean во вложенном объекте {@code parent}. */
    public static ConfigField boolNested(String key, ConfigSide side, ApplyMode mode, String category, String parent) {
        return builder(key, FieldType.BOOLEAN).side(side).applyMode(mode).category(category).parent(parent).build();
    }

    /** int с границами, верхний уровень. */
    public static ConfigField integer(String key, ConfigSide side, ApplyMode mode, String category, int min, int max) {
        return builder(key, FieldType.INT).side(side).applyMode(mode).category(category).range(min, max).build();
    }

    /** int с границами во вложенном объекте {@code parent}. */
    public static ConfigField intNested(String key, ConfigSide side, ApplyMode mode, String category, String parent, int min, int max) {
        return builder(key, FieldType.INT).side(side).applyMode(mode).category(category).parent(parent).range(min, max).build();
    }

    /** float с границами, верхний уровень. */
    public static ConfigField floatNum(String key, ConfigSide side, ApplyMode mode, String category, float min, float max) {
        return builder(key, FieldType.FLOAT).side(side).applyMode(mode).category(category).range(min, max).build();
    }

    /** float с границами во вложенном объекте {@code parent}. */
    public static ConfigField floatNested(String key, ConfigSide side, ApplyMode mode, String category, String parent, float min, float max) {
        return builder(key, FieldType.FLOAT).side(side).applyMode(mode).category(category).parent(parent).range(min, max).build();
    }

    /** long с границами во вложенном объекте {@code parent}. */
    public static ConfigField longNested(String key, ConfigSide side, ApplyMode mode, String category, String parent, long min, long max) {
        return builder(key, FieldType.LONG).side(side).applyMode(mode).category(category).parent(parent).range(min, max).build();
    }

    /** double с границами во вложенном объекте {@code parent}. */
    public static ConfigField doubleNested(String key, ConfigSide side, ApplyMode mode, String category, String parent, double min, double max) {
        return builder(key, FieldType.DOUBLE).side(side).applyMode(mode).category(category).parent(parent).range(min, max).build();
    }

    /** enum (значения определяются рефлексивно по типу поля), верхний уровень. */
    public static ConfigField enumField(String key, ConfigSide side, ApplyMode mode, String category) {
        return builder(key, FieldType.ENUM).side(side).applyMode(mode).category(category).build();
    }

    // ================================================================
    // Доступ к значению (адаптер к живому объекту ModClothConfig)
    // ================================================================

    /** Публичное поле-адаптер. Применяет валидацию границ перед записью. */
    public Object get(ModClothConfig cfg) {
        Object target = resolveTarget(cfg);
        try {
            var f = target.getClass().getField(fieldName);
            return f.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Не удалось прочитать поле конфига: " + key, e);
        }
    }

    /** Записывает значение в поле с применением клэмпа (для числовых) и приведением типа. */
    public void set(ModClothConfig cfg, Object value) {
        Object target = resolveTarget(cfg);
        Object clamped = clamp(value);
        try {
            var f = target.getClass().getField(fieldName);
            f.set(target, clamped);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Не удалось записать поле конфига: " + key + " = " + value, e);
        }
    }

    /** Устанавливает значение из строки (для JSON/сети). Проверяет тип и клэмпит. */
    public void setFromString(ModClothConfig cfg, String raw) {
        Object parsed = parse(raw);
        set(cfg, parsed);
    }

    /** Распознаёт значение из строки по типу поля. */
    public Object parse(String raw) {
        Objects.requireNonNull(raw);
        try {
            return switch (type) {
                case BOOLEAN -> Boolean.parseBoolean(raw);
                case INT -> Integer.parseInt(raw);
                case LONG -> Long.parseLong(raw);
                case FLOAT -> Float.parseFloat(raw);
                case DOUBLE -> Double.parseDouble(raw);
                case ENUM -> parseEnum(raw);
            };
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Некорректное значение '" + raw + "' для поля " + key + " (тип " + type + ")");
        }
    }

    /** Возвращает реальный тип объекта для GSON (чтобы не было кавычек) */
    public Object getForSerialization(ModClothConfig cfg) {
        Object val = get(cfg);
        if (type == FieldType.ENUM) {
            return val != null ? val.toString() : null;
        }
        return val; // Отдаст настоящий Boolean или Number (Integer, Float и т.д.)
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object parseEnum(String raw) {
        Class<?> enumClass = resolveFieldClass();
        if (enumClass == null || !enumClass.isEnum()) {
            throw new IllegalArgumentException("Поле " + key + " не является enum");
        }
        return Enum.valueOf((Class<Enum>) enumClass, raw);
    }

    /** Возвращает строковое представление текущего значения для сериализации. */
    public String getAsString(ModClothConfig cfg) {
        return String.valueOf(get(cfg));
    }

    // ================================================================
    // Внутренние утилиты
    // ================================================================

    /** Разрешает целевой объект: либо сам cfg (верхний уровень), либо вложенный объект. */
    private Object resolveTarget(ModClothConfig cfg) {
        if (parentObject == null) return cfg;
        try {
            var f = cfg.getClass().getField(parentObject);
            return f.get(cfg);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Не удалось получить вложенный объект " + parentObject, e);
        }
    }

    /**
     * Клэмпит числовые значения по границам схемы и всегда приводит к типу поля
     * (даже без границ) — защита от {@code IllegalArgumentException} рефлексии
     * при записи, например, Double в поле float.
     */
    private Object clamp(Object value) {
        if (value == null) return null;
        if (!(value instanceof Number n) || type == FieldType.BOOLEAN || type == FieldType.ENUM) {
            return value;
        }
        double d = n.doubleValue();
        if (min != null && max != null) {
            d = Mth.clamp(d, Math.min(min, max), Math.max(min, max));
        }
        return coerceNumber(d);
    }

    /** Приводит число к типу поля после клэмпа (double → int/float/long). */
    private Object coerceNumber(double v) {
        return switch (type) {
            case INT -> (int) v;
            case LONG -> (long) v;
            case FLOAT -> (float) v;
            case DOUBLE -> v;
            default -> v;
        };
    }

    /** Класс типа поля (нужен для enum-парсинга). */
    private Class<?> resolveFieldClass() {
        try {
            ModClothConfig cfg = ModClothConfig.get();
            Object target = resolveTarget(cfg);
            return target.getClass().getField(fieldName).getType();
        } catch (Exception e) {
            return null;
        }
    }

    // Метод для чейнинга при регистрации
    public ConfigField withComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getComment() {
        return comment;
    }


    // ================================================================
    // Accessors для GUI/сериализации
    // ================================================================

    public String getKey() { return key; }
    public ConfigSide getSide() { return side; }
    public ApplyMode getApplyMode() { return applyMode; }
    public String getCategory() { return category; }
    public String getFieldName() { return fieldName; }
    public String getParentObject() { return parentObject; }
    public FieldType getType() { return type; }
    public Double getMin() { return min; }
    public Double getMax() { return max; }

    /** Требуется ли перезапуск после изменения. */
    public boolean requiresRestart() { return applyMode == ApplyMode.REQUIRES_RESTART; }

    /** Требуется ли перезагрузка ресурсов. */
    public boolean requiresResourceReload() { return applyMode == ApplyMode.REQUIRES_RESOURCE_RELOAD; }

    /** Может ли применяться немедленно. */
    public boolean isLive() { return applyMode == ApplyMode.LIVE; }

    /** Является ли серверным (синхронизируемым). */
    public boolean isServer() { return side == ConfigSide.SERVER; }

    /** Является ли клиентским. */
    public boolean isClient() { return side == ConfigSide.CLIENT; }

    @Override
    public String toString() {
        return "ConfigField[" + key + ", side=" + side + ", mode=" + applyMode + ", cat=" + category + "]";
    }
}
