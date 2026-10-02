package com.hbm_m.datagen.recipes.custom;

import static com.hbm_m.datagen.recipes.custom.CraftingGen.m;
import static com.hbm_m.datagen.recipes.custom.CraftingGen.p;

import java.util.function.Consumer;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.nuclear.WatzPelletType;
import com.hbm_m.item.PartTabMetaItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 1:1 {@code com.hbm.crafting.RodRecipes} ohne den RBMK-Teil (der steht in
 * {@code ModVanillaRecipeProvider}). Brutstaebe ({@code rod/rod_dual/rod_quad} mit
 * {@code BreedingRodType}) folgen mit ihren Items.
 */
public final class RodRecipeGenerator {

    private RodRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> w) {
        CraftingGen g = new CraftingGen(w, "rods");

        //Zirnox Fuel
        g.shaped(ModItems.ROD_ZIRNOX_EMPTY, 4, p("Z Z", "ZBZ", "Z Z"), 'Z', m(ModMaterials.ZIRCONIUM, MaterialShape.NUGGET), 'B', m(ModMaterials.BERYLLIUM, MaterialShape.INGOT));
        zirnox(g, m(ModMaterials.URANIUM, MaterialShape.BILLET), ModItems.ROD_ZIRNOX_NATURAL_URANIUM_FUEL.get());
        zirnox(g, m(ModMaterials.URANIUM_FUEL, MaterialShape.BILLET), ModItems.ROD_ZIRNOX_URANIUM_FUEL.get());
        zirnox(g, m(ModMaterials.THORIUM232, MaterialShape.BILLET), ModItems.ROD_ZIRNOX_TH232.get());
        zirnox(g, m(ModMaterials.THORIUM_FUEL, MaterialShape.BILLET), ModItems.ROD_ZIRNOX_THORIUM_FUEL.get());
        zirnox(g, m(ModMaterials.MOX_FUEL, MaterialShape.BILLET), ModItems.ROD_ZIRNOX_MOX_FUEL.get());
        zirnox(g, m(ModMaterials.PLUTONIUM_FUEL, MaterialShape.BILLET), ModItems.ROD_ZIRNOX_PLUTONIUM_FUEL.get());
        zirnox(g, m(ModMaterials.URANIUM233, MaterialShape.BILLET), ModItems.ROD_ZIRNOX_U233_FUEL.get());
        zirnox(g, m(ModMaterials.URANIUM235, MaterialShape.BILLET), ModItems.ROD_ZIRNOX_U235_FUEL.get());
        zirnox(g, m(ModMaterials.LES_FUEL, MaterialShape.BILLET), ModItems.ROD_ZIRNOX_LES_FUEL.get());
        g.shapeless(ModItems.ROD_ZIRNOX_LITHIUM, 1, ModItems.ROD_ZIRNOX_EMPTY, ModItems.LITHIUM, ModItems.LITHIUM);
        g.shapeless(ModItems.ROD_ZIRNOX_ZFB_MOX, 1, ModItems.ROD_ZIRNOX_EMPTY, m(ModMaterials.MOX_FUEL, MaterialShape.BILLET), m(ModMaterials.ZIRCONIUM, MaterialShape.BILLET));

        // abgebrannte Staebe -> 2x heisser Abfall (Original: Metadaten 1)
        depleted(g, "waste_natural_uranium", ModItems.ROD_ZIRNOX_NATURAL_URANIUM_FUEL_DEPLETED.get());
        depleted(g, "waste_uranium", ModItems.ROD_ZIRNOX_URANIUM_FUEL_DEPLETED.get());
        depleted(g, "waste_thorium", ModItems.ROD_ZIRNOX_THORIUM_FUEL_DEPLETED.get());
        depleted(g, "waste_mox", ModItems.ROD_ZIRNOX_MOX_FUEL_DEPLETED.get());
        depleted(g, "waste_plutonium", ModItems.ROD_ZIRNOX_PLUTONIUM_FUEL_DEPLETED.get());
        depleted(g, "waste_u233", ModItems.ROD_ZIRNOX_U233_FUEL_DEPLETED.get());
        depleted(g, "waste_u235", ModItems.ROD_ZIRNOX_U235_FUEL_DEPLETED.get());
        depleted(g, "waste_schrabidium", ModItems.ROD_ZIRNOX_LES_FUEL_DEPLETED.get());
        depleted(g, "waste_zfb_mox", ModItems.ROD_ZIRNOX_ZFB_MOX_DEPLETED.get());

        //Breeding Rods (leere Staebe)
        g.shaped(ModItems.ROD_EMPTY, 16, p("SSS", "L L", "SSS"), 'S', m(ModMaterials.STEEL, MaterialShape.PLATE), 'L', m(ModMaterials.LEAD, MaterialShape.PLATE));
        g.shapeless(ModItems.ROD_EMPTY, 2, ModItems.ROD_DUAL_EMPTY);
        g.shapeless(ModItems.ROD_DUAL_EMPTY, 1, ModItems.ROD_EMPTY, ModItems.ROD_EMPTY);
        g.shapeless(ModItems.ROD_EMPTY, 4, ModItems.ROD_QUAD_EMPTY);
        g.shapeless(ModItems.ROD_QUAD_EMPTY, 1, ModItems.ROD_EMPTY, ModItems.ROD_EMPTY, ModItems.ROD_EMPTY, ModItems.ROD_EMPTY);
        g.shapeless(ModItems.ROD_QUAD_EMPTY, 1, ModItems.ROD_DUAL_EMPTY, ModItems.ROD_DUAL_EMPTY);

        //PWR fuel
        Item polymer = m(ModMaterials.POLYMER, MaterialShape.PLATE);
        pwr(g, ModItems.PWR_FUEL_MEU.get(), m(ModMaterials.URANIUM_FUEL, MaterialShape.BILLET), polymer);
        pwr(g, ModItems.PWR_FUEL_HEU233.get(), m(ModMaterials.URANIUM233, MaterialShape.BILLET), polymer);
        pwr(g, ModItems.PWR_FUEL_HEU235.get(), m(ModMaterials.URANIUM235, MaterialShape.BILLET), polymer);
        pwr(g, ModItems.PWR_FUEL_MEN.get(), m(ModMaterials.NEPTUNIUM_FUEL, MaterialShape.BILLET), polymer);
        pwr(g, ModItems.PWR_FUEL_HEN237.get(), m(ModMaterials.NEPTUNIUM, MaterialShape.BILLET), polymer);
        pwr(g, ModItems.PWR_FUEL_MOX.get(), m(ModMaterials.MOX_FUEL, MaterialShape.BILLET), polymer);
        pwr(g, ModItems.PWR_FUEL_MEP.get(), m(ModMaterials.PU_MIX, MaterialShape.BILLET), polymer);
        pwr(g, ModItems.PWR_FUEL_HEP239.get(), m(ModMaterials.PLUTONIUM239, MaterialShape.BILLET), polymer);
        pwr(g, ModItems.PWR_FUEL_HEP241.get(), m(ModMaterials.PLUTONIUM241, MaterialShape.BILLET), polymer);
        pwr(g, ModItems.PWR_FUEL_MEA.get(), m(ModMaterials.AM_MIX, MaterialShape.BILLET), polymer);
        pwr(g, ModItems.PWR_FUEL_HEA242.get(), m(ModMaterials.AM242, MaterialShape.BILLET), polymer);
        pwr(g, ModItems.PWR_FUEL_HES326.get(), m(ModMaterials.SCHRABIDIUM, MaterialShape.BILLET), polymer);
        pwr(g, ModItems.PWR_FUEL_HES327.get(), m(ModMaterials.SOLINIUM, MaterialShape.BILLET), polymer);
        g.shaped(ModItems.PWR_FUEL_BFB_AM_MIX, 1, p("NFN", "NIN", "NBN"), 'F', m(ModMaterials.AM_MIX, MaterialShape.BILLET), 'I', polymer,
                'B', m(ModMaterials.BISMUTH, MaterialShape.BILLET), 'N', m(ModMaterials.PLUTONIUM_FUEL, MaterialShape.NUGGET));
        g.shaped(ModItems.PWR_FUEL_BFB_PU241, 1, p("NFN", "NIN", "NBN"), 'F', m(ModMaterials.PLUTONIUM241, MaterialShape.BILLET), 'I', polymer,
                'B', m(ModMaterials.BISMUTH, MaterialShape.BILLET), 'N', m(ModMaterials.URANIUM_FUEL, MaterialShape.NUGGET));

        g.shaped(ModItems.ICF_PELLET_EMPTY, 1, p("ZLZ", "L L", "ZLZ"), 'Z', m(ModMaterials.ZIRCONIUM, MaterialShape.WIRE), 'L', m(ModMaterials.LEAD, MaterialShape.WIRE));

        //Watz fuel (NQD/NQR nur mit fremdem ingotNaquadahEnriched/ingotNaquadria im OreDict - gibt es nicht)
        pellet(g, m(ModMaterials.SCHRABIDIUM, MaterialShape.INGOT), WatzPelletType.SCHRABIDIUM);
        pellet(g, ModItems.INGOT_HES.get(), WatzPelletType.HES);
        pellet(g, m(ModMaterials.SCHRABIDIUM_FUEL, MaterialShape.INGOT), WatzPelletType.MES);
        pellet(g, ModItems.INGOT_LES.get(), WatzPelletType.LES);
        pellet(g, m(ModMaterials.NEPTUNIUM, MaterialShape.INGOT), WatzPelletType.HEN);
        pellet(g, m(ModMaterials.URANIUM_FUEL, MaterialShape.INGOT), WatzPelletType.MEU);
        pellet(g, m(ModMaterials.PU_MIX, MaterialShape.INGOT), WatzPelletType.MEP);
        pellet(g, m(ModMaterials.LEAD, MaterialShape.INGOT), WatzPelletType.LEAD);
        pellet(g, m(ModMaterials.BORON, MaterialShape.INGOT), WatzPelletType.BORON);
        pellet(g, m(ModMaterials.URANIUM238, MaterialShape.INGOT), WatzPelletType.DU);

        // CraftingManager: watz_pump / struct_watz_core (ANY_RESISTANTALLOY = TCAlloy + CDAlloy)
        Ingredient resistantCast = Ingredient.of(m(ModMaterials.TCALLOY, MaterialShape.PLATE_CAST), m(ModMaterials.CDALLOY, MaterialShape.PLATE_CAST));
        g.shaped(ModBlocks.WATZ_PUMP.get(), 1, p("MPM", "PCP", "PSP"), 'M', ModItems.MOTOR_DESH, 'P', resistantCast,
                'C', ModItems.BISMOID_CIRCUIT, 'S', ModItems.PIPE_DURA_STEEL);
        g.shaped(ModBlocks.STRUCT_WATZ_CORE.get(), 1, p("CBC", "BHB", "CBC"), 'C', ModItems.ADVANCED_CIRCUIT, 'B', resistantCast,
                'H', ModBlocks.WATZ_COOLER.get());
    }

    /** Original: {@code RodRecipes.addPellet}. */
    private static void pellet(CraftingGen g, Item ingot, WatzPelletType type) {
        g.shaped(ModItems.WATZ_PELLET.get(type), 1, p(" I ", "IGI", " I "), 'I', ingot, 'G', m(ModMaterials.GRAPHITE, MaterialShape.INGOT));
    }

    private static void zirnox(CraftingGen g, Item billet, Item rod) {
        g.shapeless(rod, 1, ModItems.ROD_ZIRNOX_EMPTY, billet, billet);
    }

    private static void depleted(CraftingGen g, String wasteId, Item rod) {
        g.shapeless(PartTabMetaItems.get(wasteId + "_cooling").get(), 2, rod);
    }

    private static void pwr(CraftingGen g, Item out, Item billet, Item plate) {
        g.shaped(out, 1, p("F", "I", "F"), 'F', billet, 'I', plate);
    }
}
