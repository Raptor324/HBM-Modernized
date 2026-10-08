package com.hbm_m.datagen.recipes.custom;
//? if forge {

import java.util.function.Consumer;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/**
 * 1:1 {@code SILEXRecipes.register()} (1.7.10): alle Rezepte mit Ladung, Verbrauch, Laserstaerke und Gewichten, inklusive
 * der RBMK-Pellets in allen zehn Zustaenden, der Abfallklassen (und ihrer kleinen Haeufchen mit einem Neuntel Ladung,
 * Original {@code tinyWasteTranslation}), der Fluid-Eingaenge und der Uebersetzungen Pulver -> Barren.
 */
public final class SilexRecipeGenerator {

    private SilexRecipeGenerator() {}

    private static Consumer<FinishedRecipe> w;

    // Wellenlaengen wie EnumWavelengths.ordinal()
    private static final int IR = 1, VISIBLE = 2, UV = 3;

    /** Original-Itemname -> Port-ID. */
    private static Item it(String orig) {
        String id = switch (orig) {
            case "powder_aluminium" -> "aluminum_powder";
            case "powder_ash_fullerene" -> "fullerene";
            // Original-Feld nugget_mercury hat die Registry-ID nugget_mercury_tiny ("Tiny Drop of Mercury")
            case "nugget_mercury" -> "nugget_mercury_tiny";
            default -> {
                if (orig.startsWith("powder_") && orig.endsWith("_tiny")) yield orig.substring(7, orig.length() - 5) + "_powder_tiny";
                if (orig.startsWith("powder_")) yield orig.substring(7) + "_powder";
                if (orig.startsWith("ingot_")) yield orig.substring(6) + "_ingot";
                yield orig;
            }
        };
        if (orig.equals("powder_bromine")) return ModMaterialItems.item(ModMaterials.BROMINE, MaterialShape.POWDER);
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, id));
        if (item == Items.AIR) {
            // Grundformen ohne Praefix (z.B. "sulfur", "fluorite", "nugget_*")
            item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, orig));
        }
        if (item == Items.AIR) throw new IllegalStateException("SILEX: Item fehlt im Port: " + orig + " (" + id + ")");
        return item;
    }

    private static ItemStack s(String orig) { return new ItemStack(it(orig)); }
    private static ItemStack s(String orig, int count) { return new ItemStack(it(orig), count); }
    private static ItemStack s(ItemLike item) { return new ItemStack(item); }

    private static final String[] LONG = { "u235", "u233", "neptunium", "thorium", "schrabidium" };
    private static final String[] SHORT = { "u235", "u233", "neptunium", "pu239", "pu240", "pu241", "am242", "schrabidium" };
    private static final int L_U235 = 0, L_U233 = 1, L_NP = 2, L_TH = 3, L_SA = 4;
    private static final int S_U235 = 0, S_U233 = 1, S_NP = 2, S_PU239 = 3, S_PU240 = 4, S_PU241 = 5, S_SA = 7;

    private static ItemStack wlt(int c) { return s("nuclear_waste_long_tiny_" + LONG[c]); }
    private static ItemStack wst(int c) { return s("nuclear_waste_short_tiny_" + SHORT[c]); }

    private static SilexRecipeBuilder r(int produced, int consumed, int laser) {
        return SilexRecipeBuilder.silex(produced, consumed, laser);
    }

    private static void save(SilexRecipeBuilder b, String id) {
        b.save(w, "silex/" + id);
    }

    public static void generate(Consumer<FinishedRecipe> writer) {
        w = writer;

        Item uIngot = ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.INGOT);
        Item uDust = ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.POWDER);
        save(r(900, 100, VISIBLE).input(Ingredient.of(uIngot, uDust)).fluid(ModFluids.UF6.getSource())
                .out(s("nugget_u235"), 1)
                .out(s("nugget_u238"), 11), "uranium");

        save(r(900, 100, 2).input(Ingredient.of(it("ingot_pu_mix")))
                .out(s("nugget_pu239"), 6)
                .out(s("nugget_pu240"), 3), "pu_mix");

        save(r(900, 100, 2).input(Ingredient.of(it("ingot_am_mix")))
                .out(s("nugget_am241"), 3)
                .out(s("nugget_am242"), 6), "am_mix");

        Item puIngot = ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.INGOT);
        Item puDust = ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.POWDER);
        save(r(900, 100, 2).input(Ingredient.of(puIngot, puDust)).fluid(ModFluids.PUF6.getSource())
                .out(s("nugget_pu238"), 3)
                .out(s("nugget_pu239"), 4)
                .out(s("nugget_pu240"), 2), "plutonium");

        save(r(900, 100, 2).input(Ingredient.of(it("ingot_schraranium")))
                .out(s("nugget_schrabidium"), 4)
                .out(s("nugget_uranium"), 3)
                .out(s("nugget_neptunium"), 2), "schraranium");

        save(r(900, 100, 2).input(Ingredient.of(it("ingot_australium"), it("powder_australium")))
                .out(s("nugget_australium_lesser"), 5)
                .out(s("nugget_australium_greater"), 1), "australium");

        save(r(900, 100, 3).input(Ingredient.of(it("crystal_schraranium")))
                .out(s("nugget_schrabidium"), 5)
                .out(s("nugget_uranium"), 2)
                .out(s("nugget_neptunium"), 2), "crystal_schraranium");

        save(r(900, 100, UV).input(Ingredient.of(com.hbm_m.block.ModBlocks.TIKITE_ORE.get()))
                .out(s("powder_plutonium"), 2)
                .out(s("powder_cobalt"), 3)
                .out(s("powder_niobium"), 3)
                .out(s("powder_nitan_mix"), 2), "ore_tikite");

        save(r(1200, 100, UV).input(Ingredient.of(it("crystal_trixite")))
                .out(s("powder_plutonium"), 2)
                .out(s("powder_cobalt"), 3)
                .out(s("powder_niobium"), 3)
                .out(s("powder_nitan_mix"), 1)
                .out(s("powder_spark_mix"), 1), "crystal_trixite");

        save(r(100, 100, 1).input(Ingredient.of(Items.LAPIS_LAZULI, it("powder_lapis")))
                .out(s("sulfur"), 4)
                .out(s("powder_aluminium"), 3)
                .out(s("powder_cobalt"), 3), "lapis");

        save(r(1000, 1000, 4).fluid(ModFluids.DEATH.getSource())
                .out(s("powder_impure_osmiridium"), 1), "death");

        save(r(1000, 300, IR).fluid(ModFluids.VITRIOL.getSource())
                .out(s("powder_bromine"), 5)
                .out(s("powder_iodine"), 5)
                .out(s("powder_iron"), 5)
                .out(s("sulfur"), 15), "vitriol");

        save(r(300, 50, VISIBLE).fluid(ModFluids.REDMUD.getSource())
                .out(s("powder_aluminium"), 10)
                .out(s("powder_neodymium_tiny", 3), 5)
                .out(s("powder_boron_tiny", 3), 5)
                .out(s("nugget_zirconium"), 5)
                .out(s("powder_iron"), 20)
                .out(s("powder_titanium"), 15)
                .out(s("powder_sodium"), 10), "redmud");

        for (int i = 0; i < 5; i++) pellets(i);

        waste();

        save(r(900, 100, 2).input(Ingredient.of(it("fallout")))
                .out(s("dust_tiny"), 90)
                .out(s("nugget_co60"), 2)
                .out(s("powder_sr90_tiny"), 3)
                .out(s("powder_i131_tiny"), 1)
                .out(s("powder_cs137_tiny"), 3)
                .out(s("nugget_au198"), 1), "fallout");

        save(r(1000, 250, VISIBLE).input(Ingredient.of(Items.GRAVEL))
                .out(s(Items.FLINT), 80)
                .out(s("powder_boron"), 5)
                .out(s("powder_lithium"), 10)
                .out(s("fluorite"), 5), "gravel");

        save(r(1_000, 1_000, VISIBLE).fluid(ModFluids.FULLERENE.getSource())
                .out(s("powder_ash_fullerene"), 1), "fullerene");
    }

    /** Ein Pellet-Rezept: {@code state} 0-4 normal, +5 mit Xenon. */
    private static SilexRecipeBuilder pellet(String pellet, int state, int produced, int consumed, int laser) {
        return r(produced, consumed, laser).input(Ingredient.of(it("rbmk_pellet_" + pellet))).pellet(state);
    }

    private static void pellets(int i) {
        ItemStack xe = s("powder_xe135_tiny");

        // UEU
        save(pellet("ueu", i, 600, 100, 1)
                .out(s("nugget_uranium"), 86 - i * 11)
                .out(i < 2 ? s("nugget_pu239") : s("nugget_pu_mix"), 10 + i * 3)
                .out(wlt(L_U235), 2 + 3 * i)
                .out(wst(S_U235), 2 + 5 * i), "rbmk_pellet_ueu_" + i);
        save(pellet("ueu", i + 5, 600, 100, 1)
                .out(xe, 1)
                .out(s("nugget_uranium"), 86 - i * 11)
                .out(i < 2 ? s("nugget_pu239") : s("nugget_pu_mix"), 10 + i * 3)
                .out(wlt(L_U235), 2 + 3 * i)
                .out(wst(S_U235), 1 + 5 * i), "rbmk_pellet_ueu_" + (i + 5));

        // MEU
        save(pellet("meu", i, 600, 100, 1)
                .out(s("nugget_uranium_fuel"), 84 - i * 16)
                .out(i < 1 ? s("nugget_pu239") : s("nugget_pu_mix"), 6 + i * 4)
                .out(wlt(L_U235), 4 + 5 * i)
                .out(wst(S_U235), 6 + 7 * i), "rbmk_pellet_meu_" + i);
        save(pellet("meu", i + 5, 600, 100, 1)
                .out(xe, 1)
                .out(s("nugget_uranium_fuel"), 83 - i * 16)
                .out(i < 1 ? s("nugget_pu239") : s("nugget_pu_mix"), 6 + i * 4)
                .out(wlt(L_U235), 4 + 5 * i)
                .out(wst(S_U235), 6 + 7 * i), "rbmk_pellet_meu_" + (i + 5));

        // HEU233
        save(pellet("heu233", i, 600, 100, 1)
                .out(s("nugget_u233"), 90 - i * 20)
                .out(wlt(L_U233), 4 + 8 * i)
                .out(wst(S_U233), 6 + 12 * i), "rbmk_pellet_heu233_" + i);
        save(pellet("heu233", i + 5, 600, 100, 1)
                .out(xe, 1)
                .out(s("nugget_u233"), 89 - i * 20)
                .out(wlt(L_U233), 4 + 8 * i)
                .out(wst(S_U233), 6 + 12 * i), "rbmk_pellet_heu233_" + (i + 5));

        // HEU235
        save(pellet("heu235", i, 600, 100, 1)
                .out(s("nugget_u235"), 90 - i * 20)
                .out(wlt(L_U235), 4 + 8 * i)
                .out(wst(S_U235), 6 + 12 * i), "rbmk_pellet_heu235_" + i);
        save(pellet("heu235", i + 5, 600, 100, 1)
                .out(xe, 1)
                .out(s("nugget_u235"), 89 - i * 20)
                .out(wlt(L_U235), 4 + 8 * i)
                .out(wst(S_U235), 6 + 12 * i), "rbmk_pellet_heu235_" + (i + 5));

        // UZH
        save(pellet("uzh", i, 600, 100, 1)
                .out(s("nugget_zirconium"), 75)
                .out(s("nugget_uranium_fuel"), 20 - i * 4)
                .out(s("nugget_pu_mix"), 3 + i * 3)
                .out(wlt(L_U235), 1 + i)
                .out(wst(S_U235), 1 + i), "rbmk_pellet_uzh_" + i);
        save(pellet("uzh", i + 5, 600, 100, 1)
                .out(xe, 1)
                .out(s("nugget_zirconium"), 75)
                .out(s("nugget_uranium_fuel"), 19 - i * 4)
                .out(s("nugget_pu_mix"), 3 + i * 3)
                .out(wlt(L_U235), 1 + i)
                .out(wst(S_U235), 1 + i), "rbmk_pellet_uzh_" + (i + 5));

        // TH232
        save(pellet("thmeu", i, 600, 100, 1)
                .out(s("nugget_thorium_fuel"), 84 - i * 20)
                .out(s("nugget_u233"), 6 + i * 4)
                .out(wlt(L_TH), 10 + 16 * i), "rbmk_pellet_thmeu_" + i);
        save(pellet("thmeu", i + 5, 600, 100, 1)
                .out(xe, 1)
                .out(s("nugget_thorium_fuel"), 83 - i * 20)
                .out(s("nugget_u233"), 6 + i * 4)
                .out(wlt(L_TH), 10 + 16 * i), "rbmk_pellet_thmeu_" + (i + 5));

        // LEP
        save(pellet("lep", i, 600, 100, 1)
                .out(s("nugget_plutonium_fuel"), 84 - i * 14)
                .out(i < 1 ? s("nugget_pu239") : s("nugget_pu_mix"), 6 + i * 2)
                .out(wst(S_PU239), 7 + 8 * i)
                .out(wst(S_PU240), 3 + 4 * i), "rbmk_pellet_lep_" + i);
        save(pellet("lep", i + 5, 600, 100, 1)
                .out(xe, 1)
                .out(s("nugget_plutonium_fuel"), 83 - i * 14)
                .out(i < 1 ? s("nugget_pu239") : s("nugget_pu_mix"), 6 + i * 2)
                .out(wst(S_PU239), 7 + 8 * i)
                .out(wst(S_PU240), 3 + 4 * i), "rbmk_pellet_lep_" + (i + 5));

        // MEP
        save(pellet("mep", i, 600, 100, 1)
                .out(s("nugget_pu_mix"), 85 - i * 20)
                .out(wst(S_PU239), 10 + 10 * i)
                .out(wst(S_PU240), 5 + 5 * i), "rbmk_pellet_mep_" + i);
        save(pellet("mep", i + 5, 600, 100, 1)
                .out(xe, 1)
                .out(s("nugget_pu_mix"), 84 - i * 20)
                .out(wst(S_PU239), 10 + 10 * i)
                .out(wst(S_PU240), 5 + 5 * i), "rbmk_pellet_mep_" + (i + 5));

        // HEP239
        save(pellet("hep239", i, 600, 100, 1)
                .out(s("nugget_pu239"), 85 - i * 20)
                .out(wst(S_PU239), 15 + 20 * i), "rbmk_pellet_hep239_" + i);
        save(pellet("hep239", i + 5, 600, 100, 1)
                .out(xe, 1)
                .out(s("nugget_pu239"), 84 - i * 20)
                .out(wst(S_PU239), 15 + 20 * i), "rbmk_pellet_hep239_" + (i + 5));

        // HEP241
        save(pellet("hep241", i, 600, 100, 2)
                .out(s("nugget_pu241"), 85 - i * 20)
                .out(wst(S_PU241), 15 + 20 * i), "rbmk_pellet_hep241_" + i);
        save(pellet("hep241", i + 5, 600, 100, 2)
                .out(xe, 1)
                .out(s("nugget_pu241"), 84 - i * 20)
                .out(wst(S_PU241), 15 + 20 * i), "rbmk_pellet_hep241_" + (i + 5));

        // MEN
        save(pellet("men", i, 600, 100, 1)
                .out(s("nugget_neptunium_fuel"), 84 - i * 14)
                .out(i < 1 ? s("nugget_pu239") : s("nugget_pu_mix"), 6 + i * 2)
                .out(wlt(L_NP), 4 + 5 * i)
                .out(wst(S_NP), 6 + 7 * i), "rbmk_pellet_men_" + i);
        save(pellet("men", i + 5, 600, 100, 1)
                .out(xe, 1)
                .out(s("nugget_neptunium_fuel"), 83 - i * 14)
                .out(i < 1 ? s("nugget_pu239") : s("nugget_pu_mix"), 6 + i * 2)
                .out(wlt(L_NP), 4 + 5 * i)
                .out(wst(S_NP), 6 + 7 * i), "rbmk_pellet_men_" + (i + 5));

        // HEN
        save(pellet("hen", i, 600, 100, 1)
                .out(s("nugget_neptunium"), 90 - i * 20)
                .out(wlt(L_NP), 4 + 8 * i)
                .out(wst(S_NP), 6 + 12 * i), "rbmk_pellet_hen_" + i);
        save(pellet("hen", i + 5, 600, 100, 1)
                .out(xe, 1)
                .out(s("nugget_neptunium"), 89 - i * 20)
                .out(wlt(L_NP), 4 + 8 * i)
                .out(wst(S_NP), 6 + 12 * i), "rbmk_pellet_hen_" + (i + 5));

        // MOX
        save(pellet("mox", i, 600, 100, 1)
                .out(s("nugget_mox_fuel"), 84 - i * 20)
                .out(s("nugget_pu_mix"), 6 + i * 4)
                .out(wlt(L_U235), 2 + 3 * i)
                .out(wst(S_U235), 3 + 5 * i)
                .out(wst(S_PU239), 5 + 8 * i), "rbmk_pellet_mox_" + i);
        save(pellet("mox", i + 5, 600, 100, 1)
                .out(xe, 1)
                .out(s("nugget_mox_fuel"), 83 - i * 20)
                .out(s("nugget_pu_mix"), 6 + i * 4)
                .out(wlt(L_U235), 2 + 3 * i)
                .out(wst(S_U235), 3 + 5 * i)
                .out(wst(S_PU239), 5 + 8 * i), "rbmk_pellet_mox_" + (i + 5));

        // LEAUS
        save(pellet("leaus", i, 600, 100, 2)
                .out(s("nugget_australium_lesser"), 90 - i * 20)
                .out(s("nugget_lead"), 6 + 12 * i)
                .out(s("nugget_pb209"), 4 + 8 * i), "rbmk_pellet_leaus_" + i);
        save(pellet("leaus", i + 5, 600, 100, 2)
                .out(xe, 1)
                .out(s("nugget_australium_lesser"), 89 - i * 20)
                .out(s("nugget_lead"), 6 + 12 * i)
                .out(s("nugget_pb209"), 4 + 8 * i), "rbmk_pellet_leaus_" + (i + 5));

        // HEAUS
        save(pellet("heaus", i, 600, 100, 2)
                .out(s("nugget_australium_greater"), 90 - i * 20)
                .out(s("nugget_au198"), 5 + 10 * i)
                .out(s(Items.GOLD_NUGGET), 3 + 6 * i)
                .out(s("nugget_pb209"), 2 + 4 * i), "rbmk_pellet_heaus_" + i);
        save(pellet("heaus", i + 5, 600, 100, 2)
                .out(xe, 1)
                .out(s("nugget_australium_greater"), 89 - i * 20)
                .out(s("nugget_au198"), 5 + 10 * i)
                .out(s(Items.GOLD_NUGGET), 3 + 6 * i)
                .out(s("nugget_pb209"), 2 + 4 * i), "rbmk_pellet_heaus_" + (i + 5));

        // LES (Xenon-Variante wie im Original ohne Xenon-Ausgang)
        for (int x = 0; x < 2; x++) {
            save(pellet("les", i + x * 5, 600, 100, 2)
                    .out(s("nugget_les"), 90 - i * 20)
                    .out(wlt(L_NP), 2 + 3 * i)
                    .out(wst(S_NP), 2 + 5 * i)
                    .out(wlt(L_SA), 1 + 2 * i)
                    .out(wst(S_SA), 1 + 2 * i)
                    .out(s("powder_coal_tiny"), 4 + 8 * i), "rbmk_pellet_les_" + (i + x * 5));
        }

        // MES
        for (int x = 0; x < 2; x++) {
            save(pellet("mes", i + x * 5, 600, 100, 2)
                    .out(s("nugget_schrabidium_fuel"), 90 - i * 20)
                    .out(wlt(L_NP), 1 + 3 * i)
                    .out(wst(S_NP), 2 + 4 * i)
                    .out(wlt(L_SA), 1 + 3 * i)
                    .out(wst(S_SA), 2 + 4 * i)
                    .out(s("powder_coal_tiny"), 4 + 6 * i), "rbmk_pellet_mes_" + (i + x * 5));
        }

        // HES
        for (int x = 0; x < 2; x++) {
            save(pellet("hes", i + x * 5, 600, 100, 2)
                    .out(s("nugget_hes"), 90 - i * 20)
                    .out(wlt(L_NP), 1 + 2 * i)
                    .out(wst(S_NP), 1 + 3 * i)
                    .out(wlt(L_SA), 2 + 5 * i)
                    .out(wst(S_SA), 4 + 6 * i)
                    .out(s("powder_coal_tiny"), 2 + 4 * i), "rbmk_pellet_hes_" + (i + x * 5));
        }

        // BALEFIRE
        save(pellet("balefire", i, 400, 100, 3)
                .out(s("powder_balefire"), 90 - i * 20)
                .out(s("nuclear_waste_tiny"), 10 + 20 * i), "rbmk_pellet_balefire_" + i);

        // FLASHGOLD
        save(pellet("balefire_gold", i, 600, 100, 2)
                .out(s("nugget_au198"), 90 - 20 * i)
                .out(s("powder_balefire"), 10 + 20 * i), "rbmk_pellet_balefire_gold_" + i);

        // FLASHLEAD
        save(pellet("flashlead", i, 600, 100, 2)
                .out(s("nugget_au198"), 44 - 10 * i)
                .out(s("nugget_pb209"), 44 - 10 * i)
                .out(s("nugget_bismuth"), 1 + 6 * i)
                .out(s("nugget_mercury"), 1 + 6 * i)
                .out(s("nugget_gh336"), 10 + 8 * i), "rbmk_pellet_flashlead_" + i);

        // POBE
        save(pellet("po210be", i, 600, 100, 1)
                .out(s("nugget_polonium"), 45 - 10 * i)
                .out(s("nugget_beryllium"), 45 - 10 * i)
                .out(s("nugget_lead"), 5 + 10 * i)
                .out(s("powder_coal_tiny"), 5 + 10 * i), "rbmk_pellet_po210be_" + i);

        // PUBE
        save(pellet("pu238be", i, 600, 100, 1)
                .out(s("nugget_pu238"), 45 - 10 * i)
                .out(s("nugget_beryllium"), 45 - 10 * i)
                .out(s("nugget_lead"), 3 + 5 * i)
                .out(s("nuclear_waste_tiny"), 2 + 5 * i)
                .out(s("powder_coal_tiny"), 5 + 10 * i), "rbmk_pellet_pu238be_" + i);
        save(pellet("pu238be", i + 5, 600, 100, 1)
                .out(xe, 1)
                .out(s("nugget_pu238"), 44 - 10 * i)
                .out(s("nugget_beryllium"), 45 - 10 * i)
                .out(s("nugget_lead"), 3 + 5 * i)
                .out(s("nuclear_waste_tiny"), 2 + 5 * i)
                .out(s("powder_coal_tiny"), 5 + 10 * i), "rbmk_pellet_pu238be_" + (i + 5));

        // RABE
        save(pellet("ra226be", i, 600, 100, 1)
                .out(s("nugget_ra226"), 45 - 10 * i)
                .out(s("nugget_beryllium"), 45 - 10 * i)
                .out(s("nugget_lead"), 3 + 5 * i)
                .out(s("nugget_polonium"), 2 + 5 * i)
                .out(s("powder_coal_tiny"), 5 + 10 * i), "rbmk_pellet_ra226be_" + i);

        // DRX
        for (int x = 0; x < 2; x++) {
            SilexRecipeBuilder b = pellet("drx", i + x * 5, 600, 100, 4);
            for (int k = 0; k < 6; k++) b.out(s("undefined"), 1);
            save(b, "rbmk_pellet_drx_" + (i + x * 5));
        }

        // ZFB BI
        save(pellet("zfb_bismuth", i, 600, 100, 2)
                .out(s("nugget_uranium"), 50 - i * 10)
                .out(s("nugget_pu241"), 50 - i * 10)
                .out(s("nugget_bismuth"), 50 + i * 20)
                .out(s("nugget_zirconium"), 150), "rbmk_pellet_zfb_bismuth_" + i);
        save(pellet("zfb_bismuth", i + 5, 600, 100, 2)
                .out(xe, 3)
                .out(s("nugget_uranium"), 50 - i * 10)
                .out(s("nugget_pu241"), 50 - i * 10)
                .out(s("nugget_bismuth"), 50 + i * 20)
                .out(s("nugget_zirconium"), 147), "rbmk_pellet_zfb_bismuth_" + (i + 5));

        // ZFB PU-241
        save(pellet("zfb_pu241", i, 600, 100, 2)
                .out(s("nugget_u235"), 50 - i * 10)
                .out(s("nugget_pu240"), 50 - i * 10)
                .out(s("nugget_pu241"), 50 + i * 20)
                .out(s("nugget_zirconium"), 150), "rbmk_pellet_zfb_pu241_" + i);
        save(pellet("zfb_pu241", i + 5, 600, 100, 2)
                .out(xe, 3)
                .out(s("nugget_u235"), 50 - i * 10)
                .out(s("nugget_pu240"), 50 - i * 10)
                .out(s("nugget_pu241"), 50 + i * 20)
                .out(s("nugget_zirconium"), 147), "rbmk_pellet_zfb_pu241_" + (i + 5));

        // ZFB RG-AM
        save(pellet("zfb_am_mix", i, 600, 100, 2)
                .out(s("nugget_pu241"), 100 - i * 20)
                .out(s("nugget_am_mix"), 50 + i * 20)
                .out(s("nugget_zirconium"), 150), "rbmk_pellet_zfb_am_mix_" + i);
        save(pellet("zfb_am_mix", i + 5, 600, 100, 2)
                .out(xe, 3)
                .out(s("nugget_pu241"), 100 - i * 20)
                .out(s("nugget_am_mix"), 50 + i * 20)
                .out(s("nugget_zirconium"), 147), "rbmk_pellet_zfb_am_mix_" + (i + 5));
    }

    /**
     * Abfall: grosse Stuecke mit 900 mB Ladung; die kleinen Haeufchen nutzen dasselbe Rezept mit
     * {@code (900 / 900) * 100 = 100} mB (Original {@code tinyWasteTranslation}).
     */
    private static void wasteBoth(String kind, String cls, int laser, Object... outs) {
        for (int t = 0; t < 2; t++) {
            String id = t == 0 ? kind + "_" + cls : kind + "_tiny_" + cls;
            SilexRecipeBuilder b = r(t == 0 ? 900 : 100, 100, laser).input(Ingredient.of(it(id)));
            for (int k = 0; k < outs.length; k += 2) b.out(((ItemStack) outs[k]).copy(), (Integer) outs[k + 1]);
            save(b, id);
        }
    }

    private static void waste() {
        String NL = "nuclear_waste_long", NLD = "nuclear_waste_long_depleted", NS = "nuclear_waste_short", NSD = "nuclear_waste_short_depleted";

        wasteBoth(NL, "u235", 1, s("nugget_neptunium"), 20, s("nugget_pu239"), 45, s("nugget_pu240"), 20, s("nugget_technetium"), 15);
        wasteBoth(NLD, "u235", 1, s("nugget_lead"), 65, s("nugget_bismuth"), 20, s("dust_tiny"), 15);
        wasteBoth(NS, "u235", 1, s("nugget_pu238"), 12, s("powder_sr90_tiny"), 10, s("powder_i131_tiny"), 10, s("powder_cs137_tiny"), 12, s("nuclear_waste_tiny"), 56);
        wasteBoth(NSD, "u235", 1, s("nugget_zirconium"), 10, s("dust_tiny"), 32, s("nugget_lead"), 22, s("nugget_u238"), 5, s("nugget_bismuth"), 15, s("nuclear_waste_tiny"), 16);

        wasteBoth(NL, "u233", 1, s("nugget_u235"), 15, s("nugget_neptunium"), 25, s("nugget_pu239"), 45, s("nugget_technetium"), 15);
        wasteBoth(NLD, "u233", 1, s("nugget_lead"), 60, s("nugget_bismuth"), 25, s("dust_tiny"), 15);
        wasteBoth(NS, "u233", 1, s("nugget_pu238"), 4, s("powder_sr90_tiny"), 12, s("powder_i131_tiny"), 10, s("powder_cs137_tiny"), 14, s("nuclear_waste_tiny"), 60);
        wasteBoth(NSD, "u233", 1, s("nugget_zirconium"), 12, s("dust_tiny"), 34, s("nugget_lead"), 13, s("nugget_u238"), 2, s("nugget_bismuth"), 10, s("nuclear_waste_tiny"), 29);

        wasteBoth(NS, "pu239", 1, s("nugget_pu240"), 10, s("nugget_pu241"), 25, s("powder_sr90_tiny"), 2, s("powder_i131_tiny"), 5, s("powder_cs137_tiny"), 6, s("nuclear_waste_tiny"), 52);
        wasteBoth(NSD, "pu239", 1, s("nugget_zirconium"), 2, s("dust_tiny"), 16, s("nugget_lead"), 40, s("nugget_u238"), 3, s("nuclear_waste_tiny"), 39);

        wasteBoth(NS, "pu240", 1, s("nugget_pu241"), 15, s("nugget_neptunium"), 5, s("powder_sr90_tiny"), 2, s("powder_i131_tiny"), 5, s("powder_cs137_tiny"), 7, s("nuclear_waste_tiny"), 66);
        wasteBoth(NSD, "pu240", 1, s("nugget_zirconium"), 2, s("dust_tiny"), 22, s("nugget_bismuth"), 20, s("nugget_lead"), 17, s("nugget_u238"), 3, s("nuclear_waste_tiny"), 36);

        wasteBoth(NS, "pu241", 2, s("nugget_am241"), 25, s("nugget_am242"), 35, s("nugget_technetium"), 5, s("powder_i131_tiny"), 3, s("powder_cs137_tiny"), 7, s("nuclear_waste_tiny"), 25);
        wasteBoth(NSD, "pu241", 2, s("nugget_bismuth"), 60, s("dust_tiny"), 20, s("nugget_lead"), 15, s("nuclear_waste_tiny"), 5);

        wasteBoth(NL, "thorium", 1, s("nugget_u233"), 40, s("nugget_u235"), 35, s("nuclear_waste_tiny"), 25);
        wasteBoth(NLD, "thorium", 1, s("nugget_lead"), 35, s("nugget_bismuth"), 40, s("dust_tiny"), 15, s("nuclear_waste_tiny"), 10);

        wasteBoth(NL, "neptunium", 1, s("nugget_u238"), 15, s("nugget_pu239"), 40, s("nugget_pu240"), 15, s("nugget_technetium"), 15, s("nuclear_waste_tiny"), 15);
        wasteBoth(NLD, "neptunium", 1, s("nugget_u238"), 16, s("nugget_lead"), 55, s("dust_tiny"), 20, s("nuclear_waste_tiny"), 9);
        wasteBoth(NS, "neptunium", 1, s("nugget_pu238"), 40, s("powder_sr90_tiny"), 7, s("powder_i131_tiny"), 5, s("powder_cs137_tiny"), 8, s("nuclear_waste_tiny"), 40);
        wasteBoth(NSD, "neptunium", 1, s("nugget_zirconium"), 7, s("dust_tiny"), 29, s("nugget_u238"), 2, s("nugget_lead"), 45, s("nuclear_waste_tiny"), 17);

        wasteBoth(NL, "schrabidium", 1, s("nugget_solinium"), 25, s("nugget_euphemium"), 18, s("nugget_gh336"), 16, s("nugget_tantalium"), 8, s("powder_neodymium_tiny"), 8, s("nuclear_waste_tiny"), 25);
        wasteBoth(NLD, "schrabidium", 1, s("nugget_solinium"), 20, s("nugget_euphemium"), 18, s("nugget_gh336"), 15, s("nugget_tantalium"), 8, s("powder_neodymium_tiny"), 8, s("nuclear_waste_tiny"), 31);
        wasteBoth(NS, "schrabidium", 1, s("nugget_pb209"), 7, s("nugget_au198"), 7, s("powder_cs137_tiny"), 5, s("powder_i131_tiny"), 5, s("nuclear_waste_tiny"), 76);
        wasteBoth(NSD, "schrabidium", 1, s("nugget_bismuth"), 7, s("nugget_mercury"), 12, s("powder_cerium_tiny"), 14, s("powder_lanthanium_tiny"), 15, s("dust_tiny"), 20, s("nuclear_waste_tiny"), 32);
    }
}
//?}
