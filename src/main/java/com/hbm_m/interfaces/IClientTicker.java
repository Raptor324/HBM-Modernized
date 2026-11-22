package com.hbm_m.interfaces;

import javax.annotation.Nullable;

/**
 * Клиентское состояние-тикер станка, живущее рядом с common-BlockEntity
 * (звуки, анимационные счётчики, клиентские Random-процессы). Единый
 * контракт для ВСЕХ машин фабрики — common-код дергает только его,
 * рендерер дополнительно кастует к конкретному классу тикера для своих
 * машинно-специфичных значений (углы, фазы).
 * <p>
 * Тикер создаётся привязанным к своему BE (см. {@link com.hbm_m.util.ClientTickerRegistry});
 * dedicated server классы-реализации не загружает, реестр пуст → тикер null.
 */
public interface IClientTicker {

    /** Один клиентский тик: level/pos/state тикер читает у своего BE. */
    void clientTick();

    /** BE снимается с рендера/удаляется: остановить звуки и пр. */
    void onRemoved();

    /** Пустой тикер (no-op) — дефолт до создания реального / заглушка на сервере. */
    IClientTicker NOOP = new IClientTicker() {
        @Override public void clientTick() {}
        @Override public void onRemoved() {}
    };

    /** Null-safe вызов тика. */
    static void tick(@Nullable IClientTicker ticker) {
        if (ticker != null) {
            ticker.clientTick();
        }
    }
}
