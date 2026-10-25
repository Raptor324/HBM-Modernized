package com.hbm_m.inventory.recipes;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.PartTabMetaItems;
import com.hbm_m.item.nuclear.PWRFuelType;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1:1 {@code com.hbm.inventory.recipes.FuelPoolRecipes}: was im Abklingbecken ({@code machine_waste_drum})
 * aus heissem Abfall wird. Heisser Abfall = die "_cooling"-Items (Original Metadaten 1), heisse PWR-Staebe
 * = {@code pwr_fuel_*_hot}. Die RBMK-Eintraege des Originals werden im Fass nie erreicht (RBMK-Staebe
 * kuehlt der erste Zweig), darum fehlen sie hier.
 */
public final class FuelPoolRecipes {

    private FuelPoolRecipes() {}

    private static final Map<Item, ItemStack> recipes = new HashMap<>();

    private static void build() {
        String[] waste = {"waste_natural_uranium", "waste_uranium", "waste_thorium", "waste_mox", "waste_plutonium",
                "waste_u233", "waste_u235", "waste_schrabidium", "waste_zfb_mox",
                "waste_plate_u233", "waste_plate_u235", "waste_plate_mox", "waste_plate_pu239", "waste_plate_sa326",
                "waste_plate_ra226be", "waste_plate_pu238be"};
        for (String w : waste) {
            Item hot = PartTabMetaItems.itemOrNull(w + "_cooling");
            Item cold = item(w);
            if (hot != null && cold != Items.AIR) recipes.put(hot, new ItemStack(cold));
        }

        for (PWRFuelType pwr : PWRFuelType.values()) {
            Item hot = item("pwr_fuel_" + pwr.name().toLowerCase(java.util.Locale.ROOT) + "_hot");
            if (hot != Items.AIR) recipes.put(hot, new ItemStack(ModItems.PWR_FUEL_DEPLETED.get(pwr).get()));
        }
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, id));
    }

    public static Map<Item, ItemStack> recipes() {
        if (recipes.isEmpty()) build();
        return recipes;
    }

    public static boolean isInput(ItemStack stack) {
        return !stack.isEmpty() && recipes().containsKey(stack.getItem());
    }
}
