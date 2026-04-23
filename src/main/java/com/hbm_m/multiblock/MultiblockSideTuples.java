package com.hbm_m.multiblock;

/**
 * Именованные tuple для {@link MultiblockStructureHelper#createFromLayersWithRolesAndSides} /
 * {@link MultiblockStructureHelper#createFromLayersWithRoles(String[][], Map, Supplier, Map, Map, Map, Map, Map)}:
 * в вызове {@code ladder(...)} / {@code energy(...)} IDE показывает имена параметров вместо «магического» {@code boolean[]}.
 * Порядок совпадает с внутренней разметкой хелпера (локальные направления схемы до поворота FACING).
 */
public final class MultiblockSideTuples {

    private MultiblockSideTuples() {}

    /**
     * @param north север локально к сетке слоёв (до поворота структуры)
     * @param south юг локально к сетке слоёв
     * @param west запад локально к сетке слоёв
     * @param east восток локально к сетке слоёв
     */
    public static boolean[] ladder(boolean north, boolean south, boolean west, boolean east) {
        return new boolean[] { north, south, west, east };
    }

    /**
     * @param north south west east up down - локально к схеме (как у {@link MultiblockStructureHelper}).
     */
    public static boolean[] energy(boolean north, boolean south, boolean west, boolean east, boolean up, boolean down) {
        return new boolean[] { north, south, west, east, up, down };
    }

    /**
     * @param north south west east up down - локально к схеме (как у {@link MultiblockStructureHelper}).
     */
    public static boolean[] fluid(boolean north, boolean south, boolean west, boolean east, boolean up, boolean down) {
        return new boolean[] { north, south, west, east, up, down };
    }
}
