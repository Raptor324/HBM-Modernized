package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов промышленного миксера ({@code hbm_m:mixer}).
 *
 * <p>1:1-порт FFA-таблицы {@code MixerRecipes.registerDefaults} (40 выходов, 50 рецептов: SOLVENT ×4,
 * LUBRICANT ×3, FRACKSOL/BIOFUEL/NITROGLYCERIN/OXYHYDROGEN/ALUMINA ×2). Жидкости/предметы/количества/
 * processTime идентичны таблице; {@code energy_per_tick} = 0 (в FFA-таблице энергии нет). ID стабильные
 * snake_case {@code mixer/<output>_<variant>}; вариант-суффикс — по входам (например
 * {@code mixer/solvent_naphtha_aromatics}). Рецепты без жидкостных входов (FISHOOL, SUNFLOWEROIL,
 * BITUMEN) — solid-only. Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только
 * для 1.20.1-forge.</p>
 */
public final class MixerRecipeGenerator {

    private MixerRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        // COOLANT: Wasser + Salpeter.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.WATER, 1_800), FluidStack.empty(),
                fluid(ModFluids.COOLANT, 2_000), 50, 0L
        ).solidItem(ModItems.NITER, 1).save(writer, "mixer/coolant");

        // CRYOGEL: Coolant + Eispulver.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.COOLANT, 1_800), FluidStack.empty(),
                fluid(ModFluids.CRYOGEL, 2_000), 50, 0L
        ).solidItem(ModItems.POWDER_ICE, 1).save(writer, "mixer/cryogel");

        // NITAN: Kerosin + Quecksilber + Nitan-Mix-Pulver.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.KEROSENE, 600), fluid(ModFluids.MERCURY, 200),
                fluid(ModFluids.NITAN, 1_000), 50, 0L
        ).solidItem(ModItems.POWDER_NITAN_MIX, 1).save(writer, "mixer/nitan");

        // FRACKSOL: zwei Varianten (Saeure- bzw. Wasser-Basis, beide mit Petroleum).
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.SULFURIC_ACID, 900), fluid(ModFluids.PETROLEUM, 100),
                fluid(ModFluids.FRACKSOL, 1_000), 20, 0L
        ).save(writer, "mixer/fracksol_sulfuric_acid_petroleum");
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.WATER, 1_000), fluid(ModFluids.PETROLEUM, 100),
                fluid(ModFluids.FRACKSOL, 1_000), 20, 0L
        ).solidItem(ModItems.SULFUR, 1).save(writer, "mixer/fracksol_water_petroleum");

        // ENDERJUICE: XP-Saft + Diamantpulver.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.XPJUICE, 500), FluidStack.empty(),
                fluid(ModFluids.ENDERJUICE, 100), 100, 0L
        ).solidItem(() -> ModMaterialItems.item(ModMaterials.DIAMOND, MaterialShape.POWDER), 1)
                .save(writer, "mixer/enderjuice");

        // SALIENT: Seedslurry + Blut.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.SEEDSLURRY, 500), fluid(ModFluids.BLOOD, 500),
                fluid(ModFluids.SALIENT, 1_000), 20, 0L
        ).save(writer, "mixer/salient");

        // COLLOID: Wasser + Staub.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.WATER, 500), FluidStack.empty(),
                fluid(ModFluids.COLLOID, 500), 20, 0L
        ).solidItem(ModItems.DUST, 1).save(writer, "mixer/colloid");

        // PHOSGEN: Ungesaettigte + Chlor.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.UNSATURATEDS, 500), fluid(ModFluids.CHLORINE, 500),
                fluid(ModFluids.PHOSGENE, 1_000), 20, 0L
        ).save(writer, "mixer/phosgene");

        // MUSTARDGAS: Reformat + Chlor + Schwefel.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.REFORMGAS, 750), fluid(ModFluids.CHLORINE, 250),
                fluid(ModFluids.MUSTARDGAS, 1_000), 20, 0L
        ).solidItem(ModItems.SULFUR, 1).save(writer, "mixer/mustardgas");

        // IONGEL: Wasser + Wasserstoff + geladene Pellets.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.WATER, 1_000), fluid(ModFluids.HYDROGEN, 200),
                fluid(ModFluids.IONGEL, 1_000), 50, 0L
        ).solidItem(ModItems.PELLET_CHARGED, 1).save(writer, "mixer/iongel");

        // EGG: Radiosolvent + Ei.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.RADIOSOLVENT, 500), FluidStack.empty(),
                fluid(ModFluids.EGG, 1_000), 50, 0L
        ).solidItem(() -> Items.EGG, 1).save(writer, "mixer/egg");

        // FISHOIL: solid-only, beliebiger Fisch (Original ItemTags.FISHES).
        MixerRecipeBuilder.mixerRecipe(
                FluidStack.empty(), FluidStack.empty(),
                fluid(ModFluids.FISHOIL, 100), 50, 0L
        ).solidTag(ItemTags.FISHES, 1).save(writer, "mixer/fishoil");

        // SUNFLOWEROIL: solid-only, Sonnenblume.
        MixerRecipeBuilder.mixerRecipe(
                FluidStack.empty(), FluidStack.empty(),
                fluid(ModFluids.SUNFLOWEROIL, 100), 50, 0L
        ).solidItem(() -> Items.SUNFLOWER, 1).save(writer, "mixer/sunfloweroil");

        // FULLERENE: Radiosolvent + Aschensonot.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.RADIOSOLVENT, 500), FluidStack.empty(),
                fluid(ModFluids.FULLERENE, 250), 50, 0L
        ).solidItem(ModItems.ASH_SOOT, 1).save(writer, "mixer/fullerene");

        // SOLVENT: vier Naphtha-Sorten, jeweils + Aromaten.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.NAPHTHA, 500), fluid(ModFluids.AROMATICS, 500),
                fluid(ModFluids.SOLVENT, 1_000), 50, 0L
        ).save(writer, "mixer/solvent_naphtha_aromatics");
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.NAPHTHA_CRACK, 500), fluid(ModFluids.AROMATICS, 500),
                fluid(ModFluids.SOLVENT, 1_000), 50, 0L
        ).save(writer, "mixer/solvent_naphtha_crack_aromatics");
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.NAPHTHA_DS, 500), fluid(ModFluids.AROMATICS, 500),
                fluid(ModFluids.SOLVENT, 1_000), 50, 0L
        ).save(writer, "mixer/solvent_naphtha_ds_aromatics");
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.NAPHTHA_COKER, 500), fluid(ModFluids.AROMATICS, 500),
                fluid(ModFluids.SOLVENT, 1_000), 50, 0L
        ).save(writer, "mixer/solvent_naphtha_coker_aromatics");

        // SULFURIC_ACID: Peroxid + Schwefel.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.PEROXIDE, 800), FluidStack.empty(),
                fluid(ModFluids.SULFURIC_ACID, 500), 50, 0L
        ).solidItem(ModItems.SULFUR, 1).save(writer, "mixer/sulfuric_acid");

        // NITRIC_ACID: Schwefelsaeure + Salpeter.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.SULFURIC_ACID, 500), FluidStack.empty(),
                fluid(ModFluids.NITRIC_ACID, 1_000), 50, 0L
        ).solidItem(ModItems.NITER, 1).save(writer, "mixer/nitric_acid");

        // RADIOSOLVENT: Reformat + Chlor.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.REFORMGAS, 750), fluid(ModFluids.CHLORINE, 250),
                fluid(ModFluids.RADIOSOLVENT, 1_000), 50, 0L
        ).save(writer, "mixer/radiosolvent");

        // SCHRABIDIC: SAS3 + Peroxid + geladene Pellets.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.SAS3, 8_000), fluid(ModFluids.PEROXIDE, 6_000),
                fluid(ModFluids.SCHRABIDIC, 16_000), 100, 0L
        ).solidItem(ModItems.PELLET_CHARGED, 1).save(writer, "mixer/schrabidic");

        // PETROIL: Reclaimed + Schmiermittel.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.RECLAIMED, 800), fluid(ModFluids.LUBRICANT, 200),
                fluid(ModFluids.PETROIL, 1_000), 30, 0L
        ).save(writer, "mixer/petroil");

        // LUBRICANT: drei Varianten.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.HEATINGOIL, 500), fluid(ModFluids.UNSATURATEDS, 500),
                fluid(ModFluids.LUBRICANT, 1_000), 20, 0L
        ).save(writer, "mixer/lubricant_heatingoil_unsaturateds");
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.FISHOIL, 800), fluid(ModFluids.ETHANOL, 200),
                fluid(ModFluids.LUBRICANT, 1_000), 20, 0L
        ).save(writer, "mixer/lubricant_fishoil_ethanol");
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.SUNFLOWEROIL, 800), fluid(ModFluids.ETHANOL, 200),
                fluid(ModFluids.LUBRICANT, 1_000), 20, 0L
        ).save(writer, "mixer/lubricant_sunfloweroil_ethanol");

        // BIOFUEL: zwei Varianten (Ausbeute unterschiedlich: 250 vs. 200).
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.FISHOIL, 500), fluid(ModFluids.WOODOIL, 500),
                fluid(ModFluids.BIOFUEL, 250), 20, 0L
        ).save(writer, "mixer/biofuel_fishoil_woodoil");
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.SUNFLOWEROIL, 500), fluid(ModFluids.WOODOIL, 500),
                fluid(ModFluids.BIOFUEL, 200), 20, 0L
        ).save(writer, "mixer/biofuel_sunfloweroil_woodoil");

        // NITROGLYCERIN: zwei Varianten.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.PETROLEUM, 1_000), fluid(ModFluids.NITRIC_ACID, 1_000),
                fluid(ModFluids.NITROGLYCERIN, 1_000), 20, 0L
        ).save(writer, "mixer/nitroglycerin_petroleum_nitric_acid");
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.FISHOIL, 500), fluid(ModFluids.NITRIC_ACID, 500),
                fluid(ModFluids.NITROGLYCERIN, 1_000), 20, 0L
        ).save(writer, "mixer/nitroglycerin_fishoil_nitric_acid");

        // THORIUM_SALT: Chlor + Thorium-Pulver.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.CHLORINE, 1_000), FluidStack.empty(),
                fluid(ModFluids.THORIUM_SALT, 1_000), 30, 0L
        ).solidItem(() -> ModMaterialItems.item(ModMaterials.THORIUM, MaterialShape.POWDER), 1)
                .save(writer, "mixer/thorium_salt");

        // SYNGAS: Kohlenoel + Dampf.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.COALOIL, 500), fluid(ModFluids.STEAM, 500),
                fluid(ModFluids.SYNGAS, 1_000), 50, 0L
        ).save(writer, "mixer/syngas");

        // OXYHYDROGEN: Wasserstoff + Luft bzw. Sauerstoff.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.HYDROGEN, 500), fluid(ModFluids.AIR, 2_000),
                fluid(ModFluids.OXYHYDROGEN, 1_000), 50, 0L
        ).save(writer, "mixer/oxyhydrogen_hydrogen_air");
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.HYDROGEN, 500), fluid(ModFluids.OXYGEN, 500),
                fluid(ModFluids.OXYHYDROGEN, 1_000), 50, 0L
        ).save(writer, "mixer/oxyhydrogen_hydrogen_oxygen");

        // Leaded-Kraftstoffe: Basis + Antiklopf-Additiv.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.PETROIL, 10_000), FluidStack.empty(),
                fluid(ModFluids.PETROIL_LEADED, 12_000), 40, 0L
        ).solidItem(ModItems.FUEL_ADDITIVE_ANTIKNOCK, 1).save(writer, "mixer/petroil_leaded");
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.GASOLINE, 10_000), FluidStack.empty(),
                fluid(ModFluids.GASOLINE_LEADED, 12_000), 40, 0L
        ).solidItem(ModItems.FUEL_ADDITIVE_ANTIKNOCK, 1).save(writer, "mixer/gasoline_leaded");
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.COALGAS, 10_000), FluidStack.empty(),
                fluid(ModFluids.COALGAS_LEADED, 12_000), 40, 0L
        ).solidItem(ModItems.FUEL_ADDITIVE_ANTIKNOCK, 1).save(writer, "mixer/coalgas_leaded");

        // Reformat-Kraftstoffe: Basis 900 + Reformat 100.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.DIESEL, 900), fluid(ModFluids.REFORMATE, 100),
                fluid(ModFluids.DIESEL_REFORM, 1_000), 50, 0L
        ).save(writer, "mixer/diesel_reform");
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.DIESEL_CRACK, 900), fluid(ModFluids.REFORMATE, 100),
                fluid(ModFluids.DIESEL_CRACK_REFORM, 1_000), 50, 0L
        ).save(writer, "mixer/diesel_crack_reform");
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.KEROSENE, 900), fluid(ModFluids.REFORMATE, 100),
                fluid(ModFluids.KEROSENE_REFORM, 1_000), 50, 0L
        ).save(writer, "mixer/kerosene_reform");

        // CHLOROCALCITE_SOLUTION: Wasser + Salpetersaeure + Chlorocalcit-Pulver.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.WATER, 250), fluid(ModFluids.NITRIC_ACID, 250),
                fluid(ModFluids.CHLOROCALCITE_SOLUTION, 500), 50, 0L
        ).solidItem(ModItems.POWDER_CHLOROCALCITE, 1).save(writer, "mixer/chlorocalcite_solution");

        // CHLOROCALCITE_MIX: Loesung + Schwefelsaeure + Flussmittel.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.CHLOROCALCITE_SOLUTION, 500), fluid(ModFluids.SULFURIC_ACID, 500),
                fluid(ModFluids.CHLOROCALCITE_MIX, 1_000), 50, 0L
        ).solidItem(ModItems.POWDER_FLUX, 1).save(writer, "mixer/chlorocalcite_mix");

        // PHEROMONE_M: Pheromon + Blut + Kraeuter-Pille.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.PHEROMONE, 1_500), fluid(ModFluids.BLOOD, 500),
                fluid(ModFluids.PHEROMONE_M, 2_000), 10, 0L
        ).solidItem(ModItems.PILL_HERBAL, 1).save(writer, "mixer/pheromone_m");

        // BAUXITE_SOLUTION: Lauge + Bauxit-Block.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.LYE, 50), FluidStack.empty(),
                fluid(ModFluids.BAUXITE_SOLUTION, 300), 80, 0L
        ).solidItem(() -> ModBlocks.STONE_RESOURCE_BAUXITE.get().asItem(), 1)
                .save(writer, "mixer/bauxite_solution");

        // LYE: Wasser + Holzasche.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.WATER, 100), FluidStack.empty(),
                fluid(ModFluids.LYE, 100), 100, 0L
        ).solidItem(ModItems.ASH_WOOD, 1).save(writer, "mixer/lye");

        // ALUMINA: zwei Varianten (Fluorit x3 bzw. Kryolith-Brocken).
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.SODIUM_ALUMINATE, 150), FluidStack.empty(),
                fluid(ModFluids.ALUMINA, 200), 40, 0L
        ).solidItem(ModItems.FLUORITE, 3).save(writer, "mixer/alumina_fluorite");
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.SODIUM_ALUMINATE, 150), FluidStack.empty(),
                fluid(ModFluids.ALUMINA, 300), 40, 0L
        ).solidItem(ModItems.CRYOLITE_CHUNK, 1).save(writer, "mixer/alumina_cryolite_chunk");

        // PERFLUOROMETHYL: Petroleum + Ungesaettigte + Fluorit.
        MixerRecipeBuilder.mixerRecipe(
                fluid(ModFluids.PETROLEUM, 1_000), fluid(ModFluids.UNSATURATEDS, 500),
                fluid(ModFluids.PERFLUOROMETHYL, 1_000), 20, 0L
        ).solidItem(ModItems.FLUORITE, 1).save(writer, "mixer/perfluoromethyl");

        // Original ANY_TAR: Rohoel-, Kohle-, Crack- und Holzteer (solid-only).
        MixerRecipeBuilder.mixerRecipe(
                FluidStack.empty(), FluidStack.empty(),
                fluid(ModFluids.BITUMEN, 50), 20, 0L
        ).solidAnyOf(1, ModItems.OIL_TAR_CRUDE, ModItems.OIL_TAR_COAL, ModItems.OIL_TAR_CRACK, ModItems.OIL_TAR_WOOD)
                .save(writer, "mixer/bitumen_any_tar");
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int amountMb) {
        return FluidStack.create(entry.getSource(), (long) amountMb);
    }
}
//?}
