package com.hbm_m.inventory.material;

import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Расплавляемые материалы тигля — 1:1 с {@code NTMMaterial} оригинала 1.7.10:
 * оригинальные числовые id, цвета расплава ({@code moltenColor}) и поведение
 * при плавке ({@code SmeltingBehavior}): SMELTABLE можно наливать, ADDITIVE
 * (флюс, рудные материалы) остаётся в тигле и рисуется в GUI по смещённой
 * текстуре (+34), NOT_SMELTABLE не плавится вовсе.
 */
public enum MaterialType {

    // ── Базовые металлы (оригинальные makeSmeltable/makeAdditive) ──
    IRON        (2600, "iron",        0xFFA259, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE_CAST)),
    GOLD        (7900, "gold",        0xE8D754, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.PLATE_CAST)),
    COPPER      (2900, "copper",      0xC18336, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_CAST)),
    TITANIUM    (2200, "titanium",    0xA99E79, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_CAST)),
    ALUMINIUM   (1300, "aluminium",   0xD0B8EB, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.PLATE_CAST)),
    TUNGSTEN    (7400, "tungsten",    0x977474, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.PLATE_CAST)),
    ZIRCONIUM   (4000, "zirconium",   0xADA688, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.PLATE_CAST)),
    OSMIRIDIUM  (7699, "osmiridium",  0xACBDD9, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_CAST)),
    LEAD        (8200, "lead",        0x646470, SmeltingBehavior.SMELTABLE, null),
    BISMUTH     (8300, "bismuth",     0xB200FF, SmeltingBehavior.SMELTABLE, null),
    BERYLLIUM   (400,  "beryllium",   0xAE9572, SmeltingBehavior.SMELTABLE, null),
    COBALT      (2700, "cobalt",      0x8F72AE, SmeltingBehavior.SMELTABLE, null),
    NICKEL      (2800, "nickel",      0xAABB88, SmeltingBehavior.SMELTABLE, null),
    ARSENIC     (3300, "arsenic",     0x558080, SmeltingBehavior.SMELTABLE, null),
    STRONTIUM   (3800, "strontium",   0xCAC193, SmeltingBehavior.SMELTABLE, null),
    CALCIUM     (2000, "calcium",     0xB7B784, SmeltingBehavior.SMELTABLE, null),
    CADMIUM     (4800, "cadmium",     0xA85600, SmeltingBehavior.SMELTABLE, null),
    TECHNETIUM  (4399, "technetium",  0xCADFDF, SmeltingBehavior.SMELTABLE, null),
    URANIUM238  (9238, "u238",        0x9AA196, SmeltingBehavior.SMELTABLE, null),
    SCHRABIDIUM (12626,"schrabidium", 0x32FFFF, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.PLATE_CAST)),

    // ── Сплавы (_AS + n) ──
    STEEL       (30,   "steel",       0x4A4A4A, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_CAST)),
    MINGRADE    (31,   "mingrade",    0xE44C0F, SmeltingBehavior.SMELTABLE, null),
    DURA_STEEL  (33,   "dura_steel",  0x42665C, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.PLATE_CAST)),
    SATURNITE   (34,   "saturnite",   0x7A5A3A, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.PLATE_CAST)),
    STAR_METAL  (35,   "star_metal",  0xA5A5D3, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.STAR_METAL, MaterialShape.PLATE_CAST)),
    TCALLOY     (36,   "tcalloy",     0x9CA6A6, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.TCALLOY, MaterialShape.PLATE_CAST)),
    FERRO       (37,   "ferro",       0x6B6B8B, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.FERROURANIUM, MaterialShape.PLATE_CAST)),
    MAGTUNG     (38,   "magtung",     0x22A2A2, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.MAGNETIZED_TUNGSTEN, MaterialShape.INGOT)),
    CMB         (39,   "cmb",         0x6F6FB4, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.CMB, MaterialShape.PLATE_CAST)),
    CDALLOY     (43,   "cdalloy",     0xFBD368, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.CDALLOY, MaterialShape.PLATE_CAST)),
    BBRONZE     (46,   "bbronze",     0x987D65, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.BBRONZE, MaterialShape.PLATE_CAST)),
    ABRONZE     (47,   "abronze",     0x77644D, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.ABRONZE, MaterialShape.PLATE_CAST)),
    BSCCO       (48,   "bscco",       0x5E62C0, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.BSCCO, MaterialShape.INGOT)),
    SLAG        (41,   "slag",        0x6C6562, SmeltingBehavior.SMELTABLE, ModItems_ingotSlag()),
    ALLOY       (32,   "alloy",       0xFF7318, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.ALLOY, MaterialShape.PLATE_CAST)),
    DESH        (42,   "desh",        0xF22929, SmeltingBehavior.SMELTABLE, () -> ModMaterialItems.item(ModMaterials.DESH, MaterialShape.PLATE_CAST)),

    // ── Добавки (ADDITIVE — не наливаются, в GUI рисуются смещённой текстурой) ──
    FLUX        (40,   "flux",        0xDECCAD, SmeltingBehavior.ADDITIVE, null),
    HEMATITE    (2601, "hematite",    0x6E463D, SmeltingBehavior.ADDITIVE, null),
    MALACHITE   (2901, "malachite",   0x61AF87, SmeltingBehavior.ADDITIVE, null),
    CARBON      (699,  "carbon",      0x404040, SmeltingBehavior.ADDITIVE, null),
    MUD         (44,   "mud",         0x96783B, SmeltingBehavior.ADDITIVE, null),

    // ── Прочие расплавы ──
    REDSTONE    (1,    "redstone",    0xFF1000, SmeltingBehavior.SMELTABLE, null),

    // ── Ванильные/прочие расплавы оригинала (MatDistribution) ──
    // Оригинальные id: MAT_STONE=_VS+0, MAT_OBSIDIAN=_VS+2, MAT_SODIUM=1100,
    // MAT_GUNMETAL=_AS+19, MAT_WEAPONSTEEL=_AS+20. Форм для литья нет — расплав
    // годится только для сплавления (порт makeSmeltable без setAutogen).
    STONE       (0,    "stone",       0x4D2F23, SmeltingBehavior.SMELTABLE, null),
    OBSIDIAN    (2,    "obsidian",    0x3D234D, SmeltingBehavior.SMELTABLE, null),
    SODIUM      (1100, "sodium",      0x7E9493, SmeltingBehavior.SMELTABLE, null),
    GUNMETAL    (49,   "gunmetal",    0xF9C62C, SmeltingBehavior.SMELTABLE, null),
    WEAPONSTEEL (50,   "weaponsteel", 0x808080, SmeltingBehavior.SMELTABLE, null);

    /** Порт {@code NTMMaterial.SmeltingBehavior}. */
    public enum SmeltingBehavior {
        NOT_SMELTABLE, SMELTABLE, ADDITIVE
    }

    public final int    id;
    public final String name;
    public final int    color;
    public final SmeltingBehavior smeltable;
    private final java.util.function.Supplier<Item> castPlateSupplier;

    MaterialType(int id, String name, int color, SmeltingBehavior smeltable,
                 @Nullable java.util.function.Supplier<Item> castPlateSupplier) {
        this.id               = id;
        this.name             = name;
        this.color            = color;
        this.smeltable        = smeltable;
        this.castPlateSupplier = castPlateSupplier;
    }

    /** Оригинал: добавки нельзя наливать ({@code tryPourStack} отвергает не-SMELTABLE). */
    public boolean isPourable() {
        return smeltable == SmeltingBehavior.SMELTABLE;
    }

    public boolean isAdditive() {
        return smeltable == SmeltingBehavior.ADDITIVE;
    }

    public @Nullable ItemStack getCastPlate(int count) {
        if (castPlateSupplier == null) return null;
        Item item = castPlateSupplier.get();
        if (item == null) return null;
        return new ItemStack(item, count);
    }

    public boolean hasCastPlate() { return castPlateSupplier != null; }

    private static java.util.function.Supplier<Item> ModItems_ingotSlag() {
        return () -> com.hbm_m.item.ModItems.INGOT_SLAG.get();
    }

    private static final Map<Integer, MaterialType> BY_ID   = new HashMap<>();
    private static final Map<String,  MaterialType> BY_NAME = new HashMap<>();

    static {
        for (MaterialType t : values()) {
            BY_ID.put(t.id, t);
            BY_NAME.put(t.name, t);
        }
    }

    public static @Nullable MaterialType byId(int id)      { return BY_ID.get(id); }
    public static @Nullable MaterialType byName(String name){ return BY_NAME.get(name); }

    /** Ключ для экранного информера Устройства настройки (аналог NTMMaterial.getUnlocalizedName → hbmmat.*). */
    public String getUnlocalizedName() {
        return "material.hbm_m." + name;
    }
}
