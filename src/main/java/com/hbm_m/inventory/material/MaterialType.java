package com.hbm_m.inventory.material;

import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public enum MaterialType {

    IRON        (2600, "iron",        0xB7B7B7, () -> ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE_CAST)),
    GOLD        (7900, "gold",        0xFDD835, () -> ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.PLATE_CAST)),
    COPPER      (2900, "copper",      0xD97C2E, () -> ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_CAST)),
    TITANIUM    (2200, "titanium",    0x8FA0B5, () -> ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_CAST)),
    ALUMINIUM   (1300, "aluminium",   0xC8C8C8, () -> ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.PLATE_CAST)),
    TUNGSTEN    (7400, "tungsten",    0x555555, () -> ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.PLATE_CAST)),
    ZIRCONIUM   (4000, "zirconium",   0x9FBFBF, () -> ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.PLATE_CAST)),
    OSMIRIDIUM  (7699, "osmiridium",  0x6A6A8F, () -> ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_CAST)),
    STEEL       (30,   "steel",       0x9AA0A6, () -> ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_CAST)),
    ALLOY       (32,   "alloy",       0x7AA0C8, () -> ModMaterialItems.item(ModMaterials.ALLOY, MaterialShape.PLATE_CAST)),
    DURA_STEEL  (33,   "dura_steel",  0x8A9AB0, () -> ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.PLATE_CAST)),
    DESH        (42,   "desh",        0xC08050, () -> ModMaterialItems.item(ModMaterials.DESH, MaterialShape.PLATE_CAST)),
    STAR_METAL  (35,   "star_metal",  0x88CCFF, () -> ModMaterialItems.item(ModMaterials.STAR_METAL, MaterialShape.PLATE_CAST)),
    TCALLOY     (36,   "tcalloy",     0x4A7A9B, () -> ModMaterialItems.item(ModMaterials.TCALLOY, MaterialShape.PLATE_CAST)),
    CDALLOY     (43,   "cdalloy",     0x6A8A7A, () -> ModMaterialItems.item(ModMaterials.CDALLOY, MaterialShape.PLATE_CAST)),
    CMB         (39,   "cmb",         0x3A3A5A, () -> ModMaterialItems.item(ModMaterials.CMB, MaterialShape.PLATE_CAST)),
    SCHRABIDIUM (12626,"schrabidium", 0xFFAA00, () -> ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.PLATE_CAST)),
    BBRONZE     (46,   "bbronze",     0xCD7F32, () -> ModMaterialItems.item(ModMaterials.BBRONZE, MaterialShape.PLATE_CAST)),
    ABRONZE     (47,   "abronze",     0xB87333, () -> ModMaterialItems.item(ModMaterials.ABRONZE, MaterialShape.PLATE_CAST)),
    SATURNITE   (34,   "saturnite",   0x7A5A3A, () -> ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.PLATE_CAST)),
    LEAD        (8200, "lead",        0x888888, null),
    BISMUTH     (8300, "bismuth",     0xAA88AA, null),
    BERYLLIUM   (400,  "beryllium",   0xAACC88, null),
    COBALT      (2700, "cobalt",      0x4466BB, null),
    NICKEL      (2800, "nickel",      0xAABB88, null),

    /** Alloying intermediates — molten-only, never cast into a plate. */
    CARBON      (50001,"carbon",      0x2B2B2B, null),
    ARSENIC     (50002,"arsenic",     0x8B7FBF, null),
    TECHNETIUM  (50003,"technetium",  0x66AACC, null),
    REDSTONE    (50004,"redstone",    0xC02020, null),
    MINGRADE    (50005,"mingrade",    0xB56A3A, null),
    CADMIUM     (50006,"cadmium",     0xB0B0C0, null);

    public final int    id;
    public final String name;
    public final int    color;
    private final java.util.function.Supplier<Item> castPlateSupplier;

    MaterialType(int id, String name, int color, @Nullable java.util.function.Supplier<Item> castPlateSupplier) {
        this.id               = id;
        this.name             = name;
        this.color            = color;
        this.castPlateSupplier = castPlateSupplier;
    }

    public @Nullable ItemStack getCastPlate(int count) {
        if (castPlateSupplier == null) return null;
        Item item = castPlateSupplier.get();
        if (item == null) return null;
        return new ItemStack(item, count);
    }

    public boolean hasCastPlate() { return castPlateSupplier != null; }

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
}
