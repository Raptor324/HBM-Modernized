package com.hbm_m.datagen.recipes.custom;

import java.util.function.Consumer;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.nuclear.WatzPelletType;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;

/**
 * Port von {@code PUREXRecipes} (Original 1.7.10): Pile-, ZIRNOX-, Platten-, PWR-, Watz-, ICF-, Verglasungs- und
 * Schrabidium-Rezepte 1:1 mit den Original-Dauern und -Leistungen. Thoriumsalz-Regeneration mit
 * Ausgaben nach Wahrscheinlichkeit (item_output_chances).
 */
public class PurexRecipeGenerator {

    // Original-Leistungskonstanten (PUREXRecipes)
    private static final int PILE = 100, ZIRNOX = 1_000, PLATE = 1_500, PWR = 2_500, VIT = 1_000;

    public static void generate(Consumer<FinishedRecipe> writer) {

        // CP-1 (Pile-Brennstaebe MK2), Schwefelsaeure 100 mB, 40 Ticks
        pile(writer, "pilepu239", "pile_rod_mk2_pu239", o("billet_pu239", 2), o("billet_uranium", 1));
        pile(writer, "pilergp", "pile_rod_mk2_rgp", o("billet_pu_mix", 2), o("billet_nuclear_waste", 1));
        pile(writer, "pilethorium", "pile_rod_mk2_thorium_fuel", o("billet_thorium_fuel", 2), o("billet_nuclear_waste", 1));
        pile(writer, "pilewaste", "pile_rod_mk2_waste", o("billet_nuclear_waste", 2), o("billet_polonium", 1));

        // ZIRNOX (Original; ersetzt die frueheren vereinfachten waste_*-Rezepte des Ports)
        std(writer, "zirnoxnu", ZIRNOX, "waste_natural_uranium", o("nugget_u238", 1), o("nugget_pu_mix", 2), o("nugget_pu239", 1), o("nuclear_waste_tiny", 2));
        std(writer, "zirnoxmeu", ZIRNOX, "waste_uranium", o("nugget_pu_mix", 1), o("nugget_plutonium", 2), o("nugget_technetium", 1), o("nuclear_waste_tiny", 2));
        std(writer, "zirnoxthmeu", ZIRNOX, "waste_thorium", o("nugget_u238", 1), o("nugget_th232", 1), o("nugget_u233", 2), o("nuclear_waste_tiny", 2));
        std(writer, "zirnoxmox", ZIRNOX, "waste_mox", o("nugget_pu_mix", 1), o("nugget_technetium", 1), o("nugget_u238", 1), o("nuclear_waste_tiny", 3));
        std(writer, "zirnoxmep", ZIRNOX, "waste_plutonium", o("nugget_pu_mix", 2), o("nugget_technetium", 1), o("nuclear_waste_tiny", 3));
        std(writer, "zirnoxheu233", ZIRNOX, "waste_u233", o("nugget_u235", 1), o("nugget_neptunium", 1), o("nugget_technetium", 1), o("nuclear_waste_tiny", 3));
        std(writer, "zirnoxheu235", ZIRNOX, "waste_u235", o("nugget_pu238", 1), o("nugget_neptunium", 1), o("nugget_technetium", 1), o("nuclear_waste_tiny", 3));
        std(writer, "zirnoxles", ZIRNOX, "waste_schrabidium", o("nugget_beryllium", 2), o("nugget_pu239", 1), o("nuclear_waste_tiny", 1), o("nuclear_waste_tiny", 2));
        std(writer, "zirnoxzfbmox", ZIRNOX, "waste_zfb_mox", o("nugget_zirconium", 3), o("nugget_technetium", 1), o("nugget_pu_mix", 1), o("nuclear_waste_tiny", 1));

        // Plattenbrennstoff
        std(writer, "platemox", PLATE, "waste_plate_mox", o("sr90_powder_tiny", 1), o("nugget_pu_mix", 3), o("cs137_powder_tiny", 1), o("nuclear_waste_tiny", 4));
        std(writer, "platepu238be", PLATE, "waste_plate_pu238be", o("nugget_beryllium", 1), o("nugget_pu238", 1), o("coal_powder_tiny", 2), o("nugget_lead", 2));
        std(writer, "platepu239", PLATE, "waste_plate_pu239", o("nugget_pu240", 2), o("nugget_technetium", 1), o("cs137_powder_tiny", 1), o("nuclear_waste_tiny", 5));
        std(writer, "platera226be", PLATE, "waste_plate_ra226be", o("nugget_beryllium", 2), o("nugget_polonium", 2), o("coal_powder_tiny", 1), o("nugget_lead", 1));
        std(writer, "platesa326", PLATE, "waste_plate_sa326", o("nugget_solinium", 1), o("neodymium_powder_tiny", 1), o("nugget_tantalium", 1), o("nuclear_waste_tiny", 6));
        std(writer, "plateu233", PLATE, "waste_plate_u233", o("nugget_u235", 1), o("i131_powder_tiny", 1), o("sr90_powder_tiny", 1), o("nuclear_waste_tiny", 6));
        std(writer, "plateu235", PLATE, "waste_plate_u235", o("nugget_neptunium", 1), o("nugget_pu238", 1), o("nugget_technetium", 1), o("nuclear_waste_tiny", 6));

        // PWR
        std(writer, "pwrmeu", PWR, "pwr_fuel_meu_depleted", o("nugget_u238", 3), o("nugget_plutonium", 4), o("nugget_technetium", 2), o("nuclear_waste_tiny", 3));
        std(writer, "pwrheu233", PWR, "pwr_fuel_heu233_depleted", o("nugget_u235", 3), o("nugget_pu238", 3), o("nugget_technetium", 1), o("nuclear_waste_tiny", 5));
        std(writer, "pwrheu235", PWR, "pwr_fuel_heu235_depleted", o("nugget_neptunium", 3), o("nugget_pu238", 3), o("nugget_technetium", 1), o("nuclear_waste_tiny", 5));
        std(writer, "pwrmen", PWR, "pwr_fuel_men_depleted", o("nugget_u238", 3), o("nugget_pu239", 4), o("nugget_technetium", 2), o("nuclear_waste_tiny", 3));
        std(writer, "pwrhen237", PWR, "pwr_fuel_hen237_depleted", o("nugget_pu238", 2), o("nugget_pu239", 4), o("nugget_technetium", 1), o("nuclear_waste_tiny", 5));
        std(writer, "pwrmox", PWR, "pwr_fuel_mox_depleted", o("nugget_u238", 3), o("nugget_pu240", 4), o("nugget_technetium", 2), o("nuclear_waste_tiny", 3));
        std(writer, "pwrmep", PWR, "pwr_fuel_mep_depleted", o("nugget_lead", 2), o("nugget_pu_mix", 4), o("nugget_technetium", 2), o("nuclear_waste_tiny", 3));
        std(writer, "pwrhep239", PWR, "pwr_fuel_hep239_depleted", o("nugget_pu_mix", 2), o("nugget_pu240", 4), o("nugget_technetium", 1), o("nuclear_waste_tiny", 5));
        std(writer, "pwrhep241", PWR, "pwr_fuel_hep241_depleted", o("nugget_lead", 3), o("nugget_zirconium", 2), o("nugget_technetium", 1), o("nuclear_waste_tiny", 6));
        std(writer, "pwrmea", PWR, "pwr_fuel_mea_depleted", o("nugget_lead", 3), o("nugget_zirconium", 2), o("nugget_technetium", 1), o("nuclear_waste_tiny", 6));
        std(writer, "pwrhea242", PWR, "pwr_fuel_hea242_depleted", o("nugget_lead", 3), o("nugget_zirconium", 2), o("nugget_technetium", 1), o("nuclear_waste_tiny", 6));
        std(writer, "pwrhes326", PWR, "pwr_fuel_hes326_depleted", o("nugget_solinium", 3), o("nugget_lead", 2), o("nugget_euphemium", 1), o("nuclear_waste_tiny", 6));
        std(writer, "pwrhes327", PWR, "pwr_fuel_hes327_depleted", o("nugget_australium", 4), o("nugget_lead", 1), o("nugget_euphemium", 1), o("nuclear_waste_tiny", 6));
        std(writer, "pwrbfbam", PWR, "pwr_fuel_bfb_am_mix_depleted", o("nugget_am_mix", 9), o("nugget_pu_mix", 2), o("nugget_bismuth", 6), o("nuclear_waste_tiny", 1));
        std(writer, "pwrbfpu241", PWR, "pwr_fuel_bfb_pu241_depleted", o("nugget_pu241", 9), o("nugget_pu_mix", 2), o("nugget_bismuth", 6), o("nuclear_waste_tiny", 1));

        // Watz (Original: purex.watz*, setup(60, watzPower = 10_000); NQD/NQR nur mit fremdem nuggetNaquadria)
        watz(writer, "watzschrab", WatzPelletType.SCHRABIDIUM, nug(ModMaterials.SOLINIUM, 15), nug(ModMaterials.EUPHEMIUM, 3));
        watz(writer, "watzhes", WatzPelletType.HES, nug(ModMaterials.SOLINIUM, 17), nug(ModMaterials.EUPHEMIUM, 1));
        watz(writer, "watzmes", WatzPelletType.MES, nug(ModMaterials.SOLINIUM, 12), nug(ModMaterials.TANTALIUM, 6));
        watz(writer, "watzles", WatzPelletType.LES, nug(ModMaterials.SOLINIUM, 9), nug(ModMaterials.TANTALIUM, 9));
        watz(writer, "watzhen", WatzPelletType.HEN, nug(ModMaterials.PLUTONIUM239, 12), nug(ModMaterials.TECHNETIUM, 6));
        watz(writer, "watzmeu", WatzPelletType.MEU, nug(ModMaterials.PLUTONIUM239, 12), nug(ModMaterials.BISMUTH, 6));
        watz(writer, "watzmep", WatzPelletType.MEP, nug(ModMaterials.PLUTONIUM241, 12), nug(ModMaterials.BISMUTH, 6));
        watz(writer, "watzlead", WatzPelletType.LEAD, nug(ModMaterials.LEAD, 6), nug(ModMaterials.BISMUTH, 12));
        watz(writer, "watzboron", WatzPelletType.BORON, new ItemStack(ModMaterialItems.item(ModMaterials.COAL, MaterialShape.POWDER_TINY), 12), nug(ModMaterials.CO60, 6));
        watz(writer, "watzdu", WatzPelletType.DU, nug(ModMaterials.POLONIUM, 12), nug(ModMaterials.PLUTONIUM238, 6));

        // ICF-Pellet-Recycling
        PurexRecipeBuilder.purexRecipe(300, 10_000).addItemInput(item("icf_pellet_depleted"), 1)
                .addItemOutput(o("icf_pellet_empty", 1)).addItemOutput(o("pellet_charged", 1)).addItemOutput(o("iron_powder", 1))
                .addFluidOutput(ModFluids.HELIUM4.getSource(), 1_250).save(writer, "purex/icf");

        // Verglasung (Bleisand)
        PurexRecipeBuilder.purexRecipe(100, VIT).addItemInput(item("sand_lead"), 1).addFluidInput(ModFluids.WASTEFLUID.getSource(), 1_000)
                .addItemOutput(o("nuclear_waste_vitrified", 1)).save(writer, "purex/vitliquid");
        PurexRecipeBuilder.purexRecipe(100, VIT).addItemInput(item("sand_lead"), 1).addFluidInput(ModFluids.WASTEGAS.getSource(), 1_000)
                .addItemOutput(o("nuclear_waste_vitrified", 1)).save(writer, "purex/vitgaseous");
        PurexRecipeBuilder.purexRecipe(300, VIT).addItemInput(item("sand_lead"), 1).addItemInput(item("nuclear_waste"), 4)
                .addItemOutput(o("nuclear_waste_vitrified", 4)).save(writer, "purex/vitsolid");

        // Schrabidium
        PurexRecipeBuilder.purexRecipe(200, 1_000).addItemInput(item("schraranium_ingot"), 1)
                .addFluidInput(ModFluids.KEROSENE.getSource(), 2_000).addFluidInput(ModFluids.NITRIC_ACID.getSource(), 1_000)
                .addItemOutput(o("nugget_schrabidium", 3)).addItemOutput(o("nugget_uranium", 3)).addItemOutput(o("nugget_neptunium", 2))
                .save(writer, "purex/schraranium");
        schrab(writer, "schrabzirnox", "waste_plutonium");
        schrab(writer, "schrabpwr", "pwr_fuel_mep_depleted");
        schrab(writer, "schrabmen", "pwr_fuel_men_depleted");
        // Thoriumsalz-Regeneration (Original l.374): 50 % U-233-Nugget, 25 % winziger Atommuell
        PurexRecipeBuilder.purexRecipe(20, 10_000)
                .addItemInput(item("nugget_th232"), 2)
                .addFluidInput(ModFluids.THORIUM_SALT_DEPLETED.getSource(), 16_000)
                .addFluidOutput(ModFluids.THORIUM_SALT.getSource(), 16_000)
                .addItemOutput(o("nugget_u233", 1), 0.5F)
                .addItemOutput(o("nuclear_waste_tiny", 1), 0.25F)
                .withIconFluid(ModFluids.THORIUM_SALT.getSource())
                .save(writer, "purex/thoriumsalt");
    }

    private static net.minecraft.world.item.Item item(String id) {
        net.minecraft.world.item.Item it = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hbm_m", id));
        if (it == net.minecraft.world.item.Items.AIR) throw new IllegalStateException("PUREX: unbekanntes Item hbm_m:" + id);
        return it;
    }

    private static ItemStack o(String id, int count) { return new ItemStack(item(id), count); }

    /** Standard-Wiederaufbereitung: Kerosin 500 + Salpetersaeure 250, 100 Ticks. */
    private static void std(Consumer<FinishedRecipe> writer, String name, int power, String input, ItemStack... outputs) {
        PurexRecipeBuilder b = PurexRecipeBuilder.purexRecipe(100, power)
                .addItemInput(item(input), 1)
                .addFluidInput(ModFluids.KEROSENE.getSource(), 500)
                .addFluidInput(ModFluids.NITRIC_ACID.getSource(), 250);
        for (ItemStack out : outputs) b.addItemOutput(out);
        b.save(writer, "purex/" + name);
    }

    private static void pile(Consumer<FinishedRecipe> writer, String name, String input, ItemStack... outputs) {
        PurexRecipeBuilder b = PurexRecipeBuilder.purexRecipe(40, PILE)
                .addItemInput(item(input), 1)
                .addFluidInput(ModFluids.SULFURIC_ACID.getSource(), 100);
        for (ItemStack out : outputs) b.addItemOutput(out);
        b.save(writer, "purex/" + name);
    }

    /** Schrabidium-Extraktion: Loesungsmittel 4000 + Schrabidinsaeure 250, 200 Ticks, 50 kHE. */
    private static void schrab(Consumer<FinishedRecipe> writer, String name, String input) {
        PurexRecipeBuilder.purexRecipe(200, 50_000)
                .addItemInput(item(input), 1)
                .addFluidInput(ModFluids.SOLVENT.getSource(), 4_000)
                .addFluidInput(ModFluids.SCHRABIDIC.getSource(), 250)
                .addItemOutput(o("schrabidium_powder", 1)).addItemOutput(o("nugget_technetium", 3)).addItemOutput(o("nuclear_waste_tiny", 4))
                .save(writer, "purex/" + name);
    }

    private static ItemStack nug(ModMaterials mat, int count) {
        return new ItemStack(ModMaterialItems.item(mat, MaterialShape.NUGGET), count);
    }

    /** 1:1 purex.watz*: verbrauchtes Pellet + 500 Kerosin + 250 Salpetersaeure -> zwei Nuggetsorten + 2 Atommuell + 1000 Schlamm. */
    private static void watz(Consumer<FinishedRecipe> writer, String name, WatzPelletType type, ItemStack a, ItemStack b) {
        PurexRecipeBuilder.purexRecipe(60, 10_000)
                .addItemInput(ModItems.WATZ_PELLET_DEPLETED.get(type).get(), 1)
                .addFluidInput(ModFluids.KEROSENE.getSource(), 500)
                .addFluidInput(ModFluids.NITRIC_ACID.getSource(), 250)
                .addItemOutput(a)
                .addItemOutput(b)
                .addItemOutput(new ItemStack(ModItems.NUCLEAR_WASTE.get(), 2))
                .addFluidOutput(ModFluids.WATZ.getSource(), 1_000)
                .save(writer, "purex/" + name);
    }

}
