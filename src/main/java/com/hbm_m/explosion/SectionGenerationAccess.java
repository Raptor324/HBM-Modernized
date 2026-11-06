// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.explosion;

/**
 * Duck-интерфейс к счётчику поколений секции, добавляемому миксином
 * {@link com.hbm_m.mixin.LevelChunkSectionMixin}. Используется, чтобы публикация
 * снапшота секций не затёрла параллельные изменения живого чанка.
 */
public interface SectionGenerationAccess {

    long hbm$getGeneration();
}
