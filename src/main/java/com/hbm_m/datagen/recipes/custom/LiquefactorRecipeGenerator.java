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
        put(writer, "coaloil_from_coal",     Items.COAL,                               ModFluids.COALOIL, 250);
        put(writer, "coaloil_from_coal_powder", ModMaterialItems.item(ModMaterials.COAL, MaterialShape.POWDER), ModFluids.COALOIL, 250);
        put(writer, "coaloil_from_lignite",  ModItems.LIGNITE.get(),                   ModFluids.COALOIL, 150);
        put(writer, "coaloil_from_lignite_powder", ModItems.LIGNITE_POWDER.get(), ModFluids.COALOIL, 150);
        // Teere → Bitumen (KEY_OIL_TAR = Rohteer, KEY_CRACK_TAR, KEY_COAL_TAR)
        put(writer, "bitumen_from_oil_tar",   ModItems.OIL_TAR_CRUDE.get(), ModFluids.BITUMEN, 75);
        put(writer, "bitumen_from_crack_tar", ModItems.OIL_TAR_CRACK.get(), ModFluids.BITUMEN, 100);
        put(writer, "bitumen_from_coal_tar",  ModItems.OIL_TAR_COAL.get(),  ModFluids.BITUMEN, 50);
        put(writer, "bitumen_from_oil_sand",  com.hbm_m.block.ModBlocks.ORE_OIL_SAND.get().asItem(), ModFluids.BITUMEN, 100);
        LiquefactorRecipeBuilder.liquefactorRecipe(Ingredient.of(net.minecraft.tags.ItemTags.LOGS), FluidStack.create(ModFluids.MUG.getSource(), 100L))
                .save(writer, "liquefactor/mug_from_logs");
        put(writer, "sodium_from_powder",     ModItems.POWDER_SODIUM.get(), ModFluids.SODIUM, 100);
        put(writer, "lead_from_ingot",       ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.INGOT),  ModFluids.LEAD,    100);
        put(writer, "lead_from_powder",      ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.POWDER), ModFluids.LEAD,    100);
        put(writer, "lead_from_block",       CraftingGen.m(ModMaterials.LEAD, MaterialShape.BLOCK),            ModFluids.LEAD,    900);

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
        put(writer, "ethanol_from_melon",     Items.MELON_SLICE, ModFluids.ETHANOL, 100);
        // Original: plant_flower Meta 3 (Hanf) 100 mB, Meta 4 (CD0) 50 mB
        put(writer, "ethanol_from_weed",      com.hbm_m.block.ModBlocks.PLANT_FLOWER_WEED.get().asItem(), ModFluids.ETHANOL, 100);
        put(writer, "ethanol_from_cd0",       com.hbm_m.block.ModBlocks.PLANT_FLOWER_CD0.get().asItem(),  ModFluids.ETHANOL, 50);
        put(writer, "biogas_from_biomass",    ModItems.BIOMASS.get(), ModFluids.BIOGAS, 125);
        put(writer, "biogas_from_glyphid_gland", ModItems.GLYPHID_GLAND_EMPTY.get(), ModFluids.BIOGAS, 2000);
        put(writer, "fishoil_from_cod",       Items.COD,       ModFluids.FISHOIL, 100);
        put(writer, "fishoil_from_salmon",    Items.SALMON,    ModFluids.FISHOIL, 100);
        put(writer, "fishoil_from_tropical_fish", Items.TROPICAL_FISH, ModFluids.FISHOIL, 100);
        put(writer, "fishoil_from_pufferfish",    Items.PUFFERFISH,    ModFluids.FISHOIL, 100);
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
