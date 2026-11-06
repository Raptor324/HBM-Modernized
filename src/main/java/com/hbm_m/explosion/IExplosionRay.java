// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.explosion;

import java.util.UUID;

/**
 * Длительный лучевой взрыв MK5: вся работа выполняется в {@link #update(long)}
 * с бюджетом времени на тик, без выгрузки состояния в NBT.
 */
public interface IExplosionRay {

    /** Один шаг работы с бюджетом {@code msBudget} миллисекунд. */
    void update(long msBudget);

    /** Отмена: освободить все карты, маски и тикеты. */
    void cancel();

    boolean isComplete();

    /** Аварийное завершение (ошибка рабочего потока) — сущность просто убирается. */
    default boolean hasFailed() {
        return false;
    }

    /** Энергия лучей ушла за пределы радиуса (взрыв вышел наружу). */
    boolean isContained();

    void setDetonator(UUID detonator);
}
