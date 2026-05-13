package com.hbm_m.multiblock;

import java.util.Map;

import com.hbm_m.block.ModBlocks;

/**
 * Заглушки описания мультиблока до портирования полной схемы из 1.7.10 / Neo.
 * Одноблочный контроллер: без фантомных частей, проверка постройки всегда проходит.
 * TODO заменить на реальный мультиблок
 */
public final class MultiblockStructureStubs {

    private static volatile MultiblockStructureHelper singleController;

    private MultiblockStructureStubs() {}

    public static MultiblockStructureHelper singleController() {
        MultiblockStructureHelper cached = singleController;
        if (cached != null) {
            return cached;
        }
        synchronized (MultiblockStructureStubs.class) {
            if (singleController == null) {
                singleController = MultiblockStructureHelper.createFromLayersWithRoles(
                        new String[][] { { "C" } },
                        Map.of(),
                        () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
                        Map.of('C', PartRole.CONTROLLER));
            }
            return singleController;
        }
    }
}
