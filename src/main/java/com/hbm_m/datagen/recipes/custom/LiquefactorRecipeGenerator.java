package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов ликвейфактора ({@code hbm_m:liquefactor}).
 *
 * <p>Порт 24 рецептов из удалённого статического {@code LiquefactorRecipes} (static-блок;
 * оригинал 1.7.10 — {@code com.hbm.inventory.recipes.LiquefactionRecipes}): предмет → жидкость (mB),
 * 1 предмет за цикл. Жидкостные стаки создаются через {@link FluidStack#create} из {@link ModFluids}.
 * Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class LiquefactorRecipeGenerator {

    private LiquefactorRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        // ── Переработка нефти/металлов ─────────────────────────────────────────
        put(writer, "coaloil_from_coal",     Items.COAL,                               ModFluids.COALOIL, 100);
        put(writer, "coaloil_from_lignite",  ModItems.LIGNITE.get(),                   ModFluids.COALOIL, 50);
        put(writer, "lead_from_ingot",       ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.INGOT),  ModFluids.LEAD,    100);
        put(writer, "lead_from_powder",      ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.POWDER), ModFluids.LEAD,    100);

        // ── «General utility recipes because why not» ─────────────────────────
        put(writer, "lava_from_netherrack",  Items.NETHERRACK,  ModFluids.LAVA,  250);
        put(writer, "lava_from_cobblestone", Items.COBBLESTONE, ModFluids.LAVA,  250);
        put(writer, "lava_from_stone",       Items.STONE,       ModFluids.LAVA,  250);
        put(writer, "lava_from_obsidian",    Items.OBSIDIAN,    ModFluids.LAVA,  500);
        put(writer, "water_from_snowball",   Items.SNOWBALL,    ModFluids.WATER, 125);
        put(writer, "water_from_snow",       Items.SNOW,        ModFluids.WATER, 500);
        put(writer, "water_from_ice",        Items.ICE,         ModFluids.WATER, 1000);
        put(writer, "water_from_packed_ice", Items.PACKED_ICE,  ModFluids.WATER, 1000);
        put(writer, "enderjuice_from_pearl", Items.ENDER_PEARL, ModFluids.ENDERJUICE, 100);

        put(writer, "ethanol_from_sugar",     Items.SUGAR,     ModFluids.ETHANOL, 100);
        put(writer, "ethanol_from_dandelion", Items.DANDELION, ModFluids.ETHANOL, 150);
        put(writer, "ethanol_from_poppy",     Items.POPPY,     ModFluids.ETHANOL, 50);
        put(writer, "biogas_from_biomass",    ModItems.BIOMASS.get(), ModFluids.BIOGAS, 125);
        put(writer, "fishoil_from_cod",       Items.COD,       ModFluids.FISHOIL, 100);
        put(writer, "fishoil_from_salmon",    Items.SALMON,    ModFluids.FISHOIL, 100);
        put(writer, "sunfloweroil",           Items.SUNFLOWER, ModFluids.SUNFLOWEROIL, 100);

        put(writer, "seedslurry_from_wheat_seeds", Items.WHEAT_SEEDS, ModFluids.SEEDSLURRY, 50);
        put(writer, "seedslurry_from_fern",        Items.FERN,        ModFluids.SEEDSLURRY, 100);
        put(writer, "seedslurry_from_grass",       Items.GRASS,       ModFluids.SEEDSLURRY, 100);
        put(writer, "seedslurry_from_vine",        Items.VINE,        ModFluids.SEEDSLURRY, 100);
    }

    private static void put(Consumer<FinishedRecipe> writer, String id, net.minecraft.world.item.Item item,
                            ModFluids.FluidEntry fluid, int amountMb) {
        LiquefactorRecipeBuilder.liquefactorRecipe(Ingredient.of(item), FluidStack.create(fluid.getSource(), (long) amountMb))
                .save(writer, "liquefactor/" + id);
    }
}
//?}
