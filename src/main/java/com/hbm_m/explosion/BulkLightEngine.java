// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.explosion;

import java.util.concurrent.CompletableFuture;

import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Duck-интерфейс, навешиваемый миксином на {@code ThreadedLevelLightEngine}:
 * пакетирует обновления света кратера в ОДНУ light-задачу вместо пачки
 * мелких (updateSectionStatus/checkBlock на каждый вызов).
 */
public interface BulkLightEngine {

    CompletableFuture<?> hbm$updateLight(LevelChunk chunk, LongList positions, long sectionMask);
}
