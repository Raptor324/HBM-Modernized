package com.hbm_m.recipe;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.ModFluids.FluidEntry;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

/**
 * Direkter (aber material-gefilterter) Java-Port von {@code LiquefactionRecipes} (1.7.10 Original,
 * {@code com.hbm.inventory.recipes.LiquefactionRecipes}): der Liquefactor verfluessigt einzelne
 * Items zu Fluiden. Fest verdrahtete Java-Map ({@code Item -> FluidStack}) statt OreDict-basiertem
 * JSON-Rezeptsystem - Stackgroessen werden wie im Original ignoriert (immer 1 Item pro Zyklus).
 * <p>
 * Ausgelassene Original-Rezepte (Material im Port nicht vorhanden, nach Pruefung von ModItems /
 * ModIngots / ModFluids):
 * <ul>
 *   <li>{@code KEY_OIL_TAR}/{@code KEY_CRACK_TAR}/{@code KEY_COAL_TAR} (Teer-Bytprodukt-Items,
 *       im Original ueber OreDictionary-Strings adressiert) - in diesem Port nicht als eigene
 *       Items gefunden.</li>
 *   <li>{@code NA.dust()} (elementarer Natrium-Staub) - kein elementares Natrium-Item in diesem
 *       Port (nur Fluid {@link ModFluids#SODIUM}).</li>
 *   <li>{@code PB.block()} (Blei-Block) - kein Blei-Block-Item gefunden, nur Barren/Staub.</li>
 *   <li>{@code ModBlocks.ore_oil_sand} (Oelsand-Erz) - Block noch nicht portiert.</li>
 *   <li>{@code glyphid_gland_empty} (Mob-Drop-Item) - zugehoeriger Mob nicht portiert.</li>
 * </ul>
 */
public final class LiquefactorRecipes {

    private LiquefactorRecipes() {}

    public record Output(Fluid fluid, int amountMb) {}

    private static final Map<Item, Output> RECIPES = new HashMap<>();

    private static void put(Item item, int amountMb, FluidEntry fluid) {
        RECIPES.put(item, new Output(fluid.getSource(), amountMb));
    }

    static {
        // Oel-Verarbeitung
        put(Items.COAL, 100, ModFluids.COALOIL);
        put(ModItems.LIGNITE.get(), 50, ModFluids.COALOIL);
        put(ModItems.getIngot(ModIngots.LEAD).get(), 100, ModFluids.LEAD);
        put(ModItems.getPowder(ModIngots.LEAD).get(), 100, ModFluids.LEAD);

        // Allgemeine Nuetzlichkeitsrezepte ("general utility recipes because why not")
        put(Items.NETHERRACK, 250, ModFluids.LAVA);
        put(Items.COBBLESTONE, 250, ModFluids.LAVA);
        put(Items.STONE, 250, ModFluids.LAVA);
        put(Items.OBSIDIAN, 500, ModFluids.LAVA);
        put(Items.SNOWBALL, 125, ModFluids.WATER);
        put(Items.SNOW, 500, ModFluids.WATER);
        put(Items.ICE, 1000, ModFluids.WATER);
        put(Items.PACKED_ICE, 1000, ModFluids.WATER);
        put(Items.ENDER_PEARL, 100, ModFluids.ENDERJUICE);

        put(Items.SUGAR, 100, ModFluids.ETHANOL);
        put(Items.DANDELION, 150, ModFluids.ETHANOL);
        put(Items.POPPY, 50, ModFluids.ETHANOL);
        put(ModItems.BIOMASS.get(), 125, ModFluids.BIOGAS);
        put(Items.COD, 100, ModFluids.FISHOIL);
        put(Items.SALMON, 100, ModFluids.FISHOIL);
        put(Items.SUNFLOWER, 100, ModFluids.SUNFLOWEROIL);

        put(Items.WHEAT_SEEDS, 50, ModFluids.SEEDSLURRY);
        put(Items.FERN, 100, ModFluids.SEEDSLURRY);
        put(Items.GRASS, 100, ModFluids.SEEDSLURRY);
        put(Items.VINE, 100, ModFluids.SEEDSLURRY);
    }

    public static Output getOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return RECIPES.get(stack.getItem());
    }

    public static boolean has(ItemStack stack) {
        return getOutput(stack) != null;
    }
}
