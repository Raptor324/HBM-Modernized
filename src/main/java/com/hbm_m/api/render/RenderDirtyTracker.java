package com.hbm_m.api.render;

/**
 * Dirty-flag контракт для GPU-driven сбора машин движка Nucleus
 * ({@code NucleusDispatcherBypass} / {@code MachineBer}).
 *
 * <p>Реализуется базовым {@code BaseHbmBlockEntity}. Машина, не помеченная
 * грязной и не протухшая по TTL, пропускает ежекадровую сборку (матрицы, свет,
 * сравнение записей) — её инстанс-записи уже лежат в instance-буферах
 * part-рендереров, и кадр ограничивается дешёвой roster-проверкой.
 *
 * <p>Флаг ставится при:
 * <ul>
 *   <li>{@code onLoad} — загрузка чанка / пересоздание BE (смена blockstate);</li>
 *   <li>{@code setBlockState} — смена состояния с сохранением BE-инстанса;</li>
 *   <li>клиентских update-пакетах ({@code handleUpdateTag} / {@code onDataPacket});</li>
 *   <li>явных вызовах из логики машины (включение/выключение и т.п. — опционально).</li>
 * </ul>
 *
 * <p>Свет и дистанционный fade не имеют событийной модели ванилы — они обновляются
 * периодическим TTL-пересбором (штамп {@code gameTick}, кадровая очередь размазана
 * по позициям) и глобальной сменой {@code worldGen} (якорь/камера-инвалидации).
 */
public interface RenderDirtyTracker {

    /** true, если визуальное состояние изменилось и запись надо пересобрать немедленно. */
    boolean isRenderDirty();

    /** Пометить запись грязной (пересборка в ближайшем кадре). */
    void markRenderDirty();

    /**
     * Протухла ли запись: грязный флаг, смена {@code worldGen} (глобальная
     * инвалидация записей — дрейф якоря, смена Iris-состояния) или истёк TTL
     * (обновление света/fade — светлый кеш живёт 15 тиков).
     */
    boolean isRenderStale(long gameTick, long worldGen);

    /** Записать штамп успешной сборки (полной или roster-assert). */
    void onRenderCollected(long gameTick, long worldGen);
}
