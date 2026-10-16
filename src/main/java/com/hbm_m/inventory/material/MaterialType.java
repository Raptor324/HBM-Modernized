package com.hbm_m.inventory.material;

import com.hbm_m.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public enum MaterialType {

    IRON        (2600, "iron",        0xB7B7B7, () -> ModItems.PLATE_CAST_IRON.get()),
    GOLD        (7900, "gold",        0xFDD835, () -> ModItems.PLATE_CAST_GOLD.get()),
    COPPER      (2900, "copper",      0xD97C2E, () -> ModItems.PLATE_CAST_COPPER.get()),
    TITANIUM    (2200, "titanium",    0x8FA0B5, () -> ModItems.PLATE_CAST_TITANIUM.get()),
    ALUMINIUM   (1300, "aluminium",   0xC8C8C8, () -> ModItems.PLATE_CAST_ALUMINIUM.get()),
    TUNGSTEN    (7400, "tungsten",    0x555555, () -> ModItems.PLATE_CAST_TUNGSTEN.get()),
    ZIRCONIUM   (4000, "zirconium",   0x9FBFBF, () -> ModItems.PLATE_CAST_ZIRCONIUM.get()),
    OSMIRIDIUM  (7699, "osmiridium",  0x6A6A8F, () -> ModItems.PLATE_CAST_OSMIRIDIUM.get()),
    STEEL       (30,   "steel",       0x9AA0A6, () -> ModItems.PLATE_CAST_STEEL.get()),
    ALLOY       (32,   "alloy",       0x7AA0C8, () -> ModItems.PLATE_CAST_ALLOY.get()),
    DURA_STEEL  (33,   "dura_steel",  0x8A9AB0, () -> ModItems.PLATE_CAST_DURA_STEEL.get()),
    DESH        (42,   "desh",        0xC08050, () -> ModItems.PLATE_CAST_DESH.get()),
    STAR_METAL  (35,   "star_metal",  0x88CCFF, () -> ModItems.PLATE_CAST_STAR_METAL.get()),
    TCALLOY     (36,   "tcalloy",     0x4A7A9B, () -> ModItems.PLATE_CAST_TCALLOY.get()),
    CDALLOY     (43,   "cdalloy",     0x6A8A7A, () -> ModItems.PLATE_CAST_CDALLOY.get()),
    CMB         (39,   "cmb",         0x3A3A5A, () -> ModItems.PLATE_CAST_CMB.get()),
    SCHRABIDIUM (12626,"schrabidium", 0xFFAA00, () -> ModItems.PLATE_CAST_SCHRABIDIUM.get()),
    BBRONZE     (46,   "bbronze",     0xCD7F32, () -> ModItems.PLATE_CAST_BBRONZE.get()),
    ABRONZE     (47,   "abronze",     0xB87333, () -> ModItems.PLATE_CAST_ABRONZE.get()),
    SATURNITE   (34,   "saturnite",   0x7A5A3A, () -> ModItems.PLATE_CAST_SATURNITE.get()),
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
