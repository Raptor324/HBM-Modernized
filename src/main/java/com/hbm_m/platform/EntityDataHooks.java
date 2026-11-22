package com.hbm_m.platform;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;

/**
 * Мост для значений по умолчанию у синхронизируемых данных сущности.
 * <p>
 * На 1.20.1 {@code defineSynchedData()} пишет прямо в {@link SynchedEntityData} сущности,
 * на 1.21.1 — в {@code SynchedEntityData.Builder}. Различаются и сигнатура override'а, и
 * приёмник. Сигнатуру приходится объявлять в самой сущности, а приёмник прячется сюда, чтобы
 * список полей существовал в единственном экземпляре:
 *
 * <pre>{@code
 * //? if < 1.21.1 {
 * /*@Override
 * protected void defineSynchedData() {
 *     super.defineSynchedData();
 *     var defs = EntityDataHooks.sink(this.entityData);
 * *\/ //?} else {
 * @Override
 * protected void defineSynchedData(SynchedEntityData.Builder builder) {
 *     super.defineSynchedData(builder);
 *     var defs = EntityDataHooks.sink(builder);
 * //?}
 *     defs.define(FOO, false);
 * }
 * }</pre>
 */
public final class EntityDataHooks {

    private EntityDataHooks() {}

    /** Приёмник значений по умолчанию; на каждой платформе за ним стоит свой объект. */
    public interface Sink {
        <T> void define(EntityDataAccessor<T> key, T value);
    }

    //? if < 1.21.1 {
    public static Sink sink(SynchedEntityData data) {
        return new Sink() {
            @Override public <T> void define(EntityDataAccessor<T> key, T value) { data.define(key, value); }
        };
    }
    //?} else {
    /*public static Sink sink(SynchedEntityData.Builder builder) {
        return new Sink() {
            @Override public <T> void define(EntityDataAccessor<T> key, T value) { builder.define(key, value); }
        };
    }
    *///?}
}
