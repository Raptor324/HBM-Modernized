package com.hbm_m.inventory.material;

/**
 * Порт {@code com.hbm.inventory.material.MaterialShapes} (1.7.10).
 *
 * <p>Расплавленный металл измеряется в квантах — минимальной неделимой порции материала.
 * 1 квант = 1/72 слитка; все стоимости форм, ёмкости литейки и порции налива выражены
 * целыми квантами (как в оригинале) — дробных пересчётов нет в принципе.</p>
 */
public final class MaterialShapes {

    /** Минимальная единица материала. */
    public static final int QUANTUM = 1;

    /** Самородок / tiny / fragment / dust_tiny. */
    public static final int NUGGET = q(8);
    /** Тонкая проволока / болт. */
    public static final int WIRE = q(9);
    /** Биллет. */
    public static final int BILLET = q(48);
    /** Слиток / gem / crystal / dust / dense wire / plate. */
    public static final int INGOT = q(72);
    /** Литая тройная плита. */
    public static final int CASTPLATE = q(216);
    /** Сварная шестерная плита. */
    public static final int WELDEDPLATE = q(432);
    /** Гильза (shell). */
    public static final int SHELL = q(288);
    /** Труба. */
    public static final int PIPE = q(216);
    /** Четверть блока. */
    public static final int QUART = q(162);
    /** Блок хранения. */
    public static final int BLOCK = q(648);

    /** Оружейные детали: стволы, ресиверы, механизмы, приклад, рукоять. */
    public static final int LIGHTBARREL    = q(216);
    public static final int HEAVYBARREL    = q(432);
    public static final int LIGHTRECEIVER  = q(288);
    public static final int HEAVYRECEIVER  = q(648);
    public static final int MECHANISM      = q(288);
    public static final int STOCK          = q(288);
    public static final int GRIP           = q(144);

    private MaterialShapes() {}

    /** Умножение кванта — аналог {@code MaterialShapes.q(n)} оригинала. */
    public static int q(int mult) { return QUANTUM * mult; }
}
