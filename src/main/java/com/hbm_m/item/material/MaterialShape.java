package com.hbm_m.item.material;

/**
 * Форма предмета материала (порт идеи MaterialShapes из 1.7.10).
 * Каждая форма знает, как построить id регистрации предмета из id материала.
 * Идентификаторы исторические: <code>lead</code> -> <code>ingot_lead</code>,
 * <code>lead</code> -> <code>lead_powder</code> (порошки — суффикс, остальные — префикс).
 */
public enum MaterialShape {

    INGOT("", "_ingot"),
    NUGGET("nugget_", ""),
    BILLET("billet_", ""),
    POWDER("", "_powder"),
    POWDER_TINY("", "_powder_tiny"),
    CRYSTAL("crystal_", ""),
    PLATE("plate_", ""),
    PLATE_CAST("plate_cast_", ""),
    PLATE_WELDED("plate_welded_", ""),
    WIRE("wire_", ""),
    WIRE_DENSE("wire_dense_", ""),
    /** Лом: id предмета совпадает с id материала (scrap, scrap_nuclear, ...). */
    SCRAP("", ""),
    /** Блок хранения; регистрируется в {@code ModBlocks}, не в {@code ModItems}. */
    BLOCK("block_", "");

    private final String prefix;
    private final String suffix;

    MaterialShape(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    /** Id регистрации предмета/блока для материала в этой форме. */
    public String itemId(String materialId) {
        return prefix + materialId + suffix;
    }

    /** Id регистрации для материала (удобная перегрузка). */
    public String itemId(ModMaterials material) {
        return itemId(material.getId());
    }
}
