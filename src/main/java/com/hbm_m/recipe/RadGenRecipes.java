package com.hbm_m.recipe;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.item.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Direkter Java-Port von {@code TileEntityMachineRadGen.fuels} (1.7.10 Original). Feste Java-Map
 * statt JSON-Rezeptsystem, analog zu {@link CokerRecipes}.
 * <p>
 * SCOPE-Entscheidung: Das Original iteriert ueber {@code ItemWasteShort.WasteClass}/{@code
 * ItemWasteLong.WasteClass} (mehrere Metadata-Subtypen pro Abfallklasse) - dieser Port hat nur je
 * ein einzelnes Item pro Abfalltyp (kein Metadata-Multi-Item-System), daher je ein Eintrag statt
 * mehrerer pro Klasse.
 */
public final class RadGenRecipes {

    private RadGenRecipes() {}

    public record Recipe(int power, int duration, ItemStack output) {}

    private static final Map<Item, Recipe> RECIPES = new HashMap<>();

    private static void put(Item input, int power, int duration, ItemStack output) {
        RECIPES.put(input, new Recipe(power, duration, output));
    }

    static {
        put(ModItems.NUCLEAR_WASTE_SHORT.get(), 1500, 30 * 60 * 20, new ItemStack(ModItems.NUCLEAR_WASTE_SHORT_DEPLETED.get()));
        put(ModItems.NUCLEAR_WASTE_SHORT_TINY.get(), 150, 3 * 60 * 20, new ItemStack(ModItems.NUCLEAR_WASTE_SHORT_DEPLETED_TINY.get()));
        put(ModItems.NUCLEAR_WASTE_LONG.get(), 500, 2 * 60 * 60 * 20, new ItemStack(ModItems.NUCLEAR_WASTE_LONG_DEPLETED.get()));
        put(ModItems.NUCLEAR_WASTE_LONG_TINY.get(), 50, 12 * 60 * 20, new ItemStack(ModItems.NUCLEAR_WASTE_LONG_DEPLETED_TINY.get()));
        put(ModItems.SCRAP_NUCLEAR.get(), 50, 5 * 60 * 20, ItemStack.EMPTY);
        put(ModItems.GEM_RAD.get(), 25_000, 30 * 60 * 20, new ItemStack(Items.DIAMOND));
    }

    public static Recipe get(Item item) {
        return RECIPES.get(item);
    }
}
