package com.hbm_m.datagen.recipes.custom;

import static com.hbm_m.datagen.recipes.custom.CraftingGen.m;
import static com.hbm_m.datagen.recipes.custom.CraftingGen.p;

import java.util.function.Consumer;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.PartTabMetaItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 1:1 {@code com.hbm.crafting.ArmorRecipes} (ohne LBSM-Einfachrezepte, Standard ist aus) plus die Jacken aus
 * {@code CraftingManager}. OreDict-Abbildung: ANY_RUBBER = (Bio-)Gummi, ANY_PLASTIC = Polymer/Bakelit,
 * KEY_ANYPANE = Glasscheiben, KEY_RED/KEY_BLACK = Farbstoffe; circuit BASIC = integrated_circuit,
 * ADVANCED = advanced_circuit, QUANTUM = quantum_circuit, ANALOG = analog_circuit, CHIP = microchip.
 */
public final class ArmorRecipeGenerator {

    private ArmorRecipeGenerator() {}

    private static Ingredient tag(String ns, String path) {
        return Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ns, path)));
    }

    public static void generate(Consumer<FinishedRecipe> w) {
        CraftingGen g = new CraftingGen(w, "armor");

        Ingredient anyRubber = Ingredient.of(m(ModMaterials.BIORUBBER, MaterialShape.INGOT), m(ModMaterials.RUBBER, MaterialShape.INGOT));
        Ingredient anyPlastic = Ingredient.of(m(ModMaterials.POLYMER, MaterialShape.INGOT), m(ModMaterials.BAKELITE, MaterialShape.INGOT));
        Ingredient anyPane = tag("forge", "glass_panes");
        Ingredient keyRed = tag("forge", "dyes/red");
        Ingredient keyBlack = tag("forge", "dyes/black");
        Ingredient anyWool = Ingredient.of(ItemTags.WOOL);

        var steelIngot = m(ModMaterials.STEEL, MaterialShape.INGOT);
        var steelPlate = m(ModMaterials.STEEL, MaterialShape.PLATE);
        var tiIngot = m(ModMaterials.TITANIUM, MaterialShape.INGOT);
        var tiPlate = m(ModMaterials.TITANIUM, MaterialShape.PLATE);
        var tiCast = m(ModMaterials.TITANIUM, MaterialShape.PLATE_CAST);
        var ironPlate = m(ModMaterials.IRON, MaterialShape.PLATE);
        var star = m(ModMaterials.STARMETAL, MaterialShape.INGOT);
        var desh = m(ModMaterials.DESH, MaterialShape.INGOT);
        var cuPlate = m(ModMaterials.COPPER, MaterialShape.PLATE);
        var sa326 = m(ModMaterials.SCHRABIDIUM, MaterialShape.INGOT);
        var basic = ModItems.INTEGRATED_CIRCUIT;
        var advanced = ModItems.ADVANCED_CIRCUIT;

        //Armor mod table
        // (armor_table: siehe ModVanillaRecipeProvider)

        //Regular armor
        g.shaped(ModItems.STEEL_HELMET, 1, p("XXX", "X X"), 'X', steelIngot);
        g.shaped(ModItems.STEEL_PLATE, 1, p("X X", "XXX", "XXX"), 'X', steelIngot);
        g.shaped(ModItems.STEEL_LEGS, 1, p("XXX", "X X", "X X"), 'X', steelIngot);
        g.shaped(ModItems.STEEL_BOOTS, 1, p("X X", "X X"), 'X', steelIngot);
        g.shaped(ModItems.TITANIUM_HELMET, 1, p("XXX", "X X"), 'X', tiIngot);
        g.shaped(ModItems.TITANIUM_PLATE, 1, p("X X", "XXX", "XXX"), 'X', tiIngot);
        g.shaped(ModItems.TITANIUM_LEGS, 1, p("XXX", "X X", "X X"), 'X', tiIngot);
        g.shaped(ModItems.TITANIUM_BOOTS, 1, p("X X", "X X"), 'X', tiIngot);
        var cmb = m(ModMaterials.COMBINE_STEEL, MaterialShape.INGOT);
        g.shaped(ModItems.CMB_HELMET, 1, p("XXX", "X X"), 'X', cmb);
        g.shaped(ModItems.CMB_PLATE, 1, p("X X", "XXX", "XXX"), 'X', cmb);
        g.shaped(ModItems.CMB_LEGS, 1, p("XXX", "X X", "X X"), 'X', cmb);
        g.shaped(ModItems.CMB_BOOTS, 1, p("X X", "X X"), 'X', cmb);
        g.shaped(ModItems.ROBES_HELMET, 1, p("XXX", "X X"), 'X', ModItems.RAG);
        g.shaped(ModItems.ROBES_PLATE, 1, p("X X", "XXX", "XXX"), 'X', ModItems.RAG);
        g.shaped(ModItems.ROBES_LEGS, 1, p("XXX", "X X", "X X"), 'X', ModItems.RAG);
        g.shaped(ModItems.ROBES_BOOTS, 1, p("R R", "P P"), 'R', ModItems.RAG, 'P', anyRubber);
        var coBillet = m(ModMaterials.COBALT, MaterialShape.BILLET);
        g.shaped(ModItems.COBALT_HELMET, 1, p("ECE"), 'E', coBillet, 'C', ModItems.STEEL_HELMET);
        g.shaped(ModItems.COBALT_PLATE, 1, p(" E ", "ECE", " E "), 'E', coBillet, 'C', ModItems.STEEL_PLATE);
        g.shaped(ModItems.COBALT_LEGS, 1, p("ECE", "E E"), 'E', coBillet, 'C', ModItems.STEEL_LEGS);
        g.shaped(ModItems.COBALT_BOOTS, 1, p("ECE"), 'E', coBillet, 'C', ModItems.STEEL_BOOTS);
        g.shaped(ModItems.SECURITY_HELMET, 1, p("SSS", "IGI"), 'S', steelPlate, 'I', anyRubber, 'G', anyPane);
        g.shaped(ModItems.SECURITY_PLATE, 1, p("KWK", "IKI", "WKW"), 'K', ModItems.PLATE_KEVLAR, 'I', anyPlastic, 'W', anyWool);
        g.shaped(ModItems.SECURITY_LEGS, 1, p("IWI", "K K", "W W"), 'K', ModItems.PLATE_KEVLAR, 'I', anyPlastic, 'W', anyWool);
        g.shaped(ModItems.SECURITY_BOOTS, 1, p("P P", "I I"), 'P', steelPlate, 'I', anyRubber);
        var dnt = m(ModMaterials.DINEUTRONIUM, MaterialShape.INGOT);
        g.shaped(ModItems.DNT_HELMET, 1, p("EEE", "EE "), 'E', dnt);
        g.shaped(ModItems.DNT_PLATE, 1, p("EE ", "EEE", "EEE"), 'E', dnt);
        g.shaped(ModItems.DNT_LEGS, 1, p("EE ", "EEE", "E E"), 'E', dnt);
        g.shaped(ModItems.DNT_BOOTS, 1, p("  E", "E  ", "E E"), 'E', dnt);
        g.shaped(ModItems.ZIRCONIUM_LEGS, 1, p("EEE", "E E", "E E"), 'E', m(ModMaterials.ZIRCONIUM, MaterialShape.INGOT));

        //Power armor
        g.shaped(ModItems.T51_HELMET, 1, p("PPC", "PBP", "IXI"), 'P', ModItems.PLATE_ARMOR_TITANIUM, 'C', basic, 'I', anyRubber, 'X', ModItems.GAS_MASK_M65, 'B', ModItems.TITANIUM_HELMET);
        g.shaped(ModItems.T51_PLATE, 1, p("MPM", "TBT", "PPP"), 'M', ModItems.MOTOR, 'P', ModItems.PLATE_ARMOR_TITANIUM, 'T', ModItems.GAS_EMPTY, 'B', ModItems.TITANIUM_PLATE);
        g.shaped(ModItems.T51_LEGS, 1, p("MPM", "PBP", "P P"), 'M', ModItems.MOTOR, 'P', ModItems.PLATE_ARMOR_TITANIUM, 'B', ModItems.TITANIUM_LEGS);
        g.shaped(ModItems.T51_BOOTS, 1, p("P P", "PBP"), 'P', ModItems.PLATE_ARMOR_TITANIUM, 'B', ModItems.TITANIUM_BOOTS);
        g.shaped(ModItems.AJR_HELMET, 1, p("PPC", "PBP", "IXI"), 'P', ModItems.PLATE_ARMOR_AJR, 'C', basic, 'I', anyPlastic, 'X', ModItems.GAS_MASK_M65, 'B', ModItems.TITANIUM_HELMET);
        g.shaped(ModItems.AJR_PLATE, 1, p("MPM", "TBT", "PPP"), 'M', ModItems.MOTOR_DESH, 'P', ModItems.PLATE_ARMOR_AJR, 'T', ModItems.GAS_EMPTY, 'B', ModItems.TITANIUM_PLATE);
        g.shaped(ModItems.AJR_LEGS, 1, p("MPM", "PBP", "P P"), 'M', ModItems.MOTOR_DESH, 'P', ModItems.PLATE_ARMOR_AJR, 'B', ModItems.TITANIUM_LEGS);
        g.shaped(ModItems.AJR_BOOTS, 1, p("P P", "PBP"), 'P', ModItems.PLATE_ARMOR_AJR, 'B', ModItems.TITANIUM_BOOTS);
        g.shapeless(ModItems.AJRO_HELMET, 1, ModItems.AJR_HELMET, keyRed, keyBlack);
        g.shapeless(ModItems.AJRO_PLATE, 1, ModItems.AJR_PLATE, keyRed, keyBlack);
        g.shapeless(ModItems.AJRO_LEGS, 1, ModItems.AJR_LEGS, keyRed, keyBlack);
        g.shapeless(ModItems.AJRO_BOOTS, 1, ModItems.AJR_BOOTS, keyRed, keyBlack);
        g.shaped(ModItems.BJ_HELMET, 1, p("SBS", " C ", " I "), 'S', Items.STRING, 'B', Items.BLACK_WOOL, 'C', advanced, 'I', star);
        g.shaped(ModItems.BJ_PLATE, 1, p("N N", "MSM", "NCN"), 'N', ModItems.PLATE_ARMOR_LUNAR, 'M', ModItems.MOTOR_DESH, 'S', ModItems.STARMETAL_PLATE, 'C', advanced);
        g.shaped(ModItems.BJ_PLATE_JETPACK, 1, p("NFN", "TPT", "ICI"), 'N', ModItems.PLATE_ARMOR_LUNAR, 'F', ModItems.FINS_QUAD_TITANIUM,
                'T', net.minecraftforge.common.crafting.StrictNBTIngredient.of(com.hbm_m.item.liquids.ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), com.hbm_m.inventory.fluid.ModFluids.XENON.getSource(), 1)),
                'P', ModItems.BJ_PLATE, 'I', com.hbm_m.item.missile.MissilePartItems.get("mp_thruster_10_xenon").get(), 'C', m(ModMaterials.PHOSPHORUS, MaterialShape.CRYSTAL));
        g.shaped(ModItems.BJ_LEGS, 1, p("MBM", "NSN", "N N"), 'N', ModItems.PLATE_ARMOR_LUNAR, 'M', ModItems.MOTOR_DESH, 'S', ModItems.STARMETAL_LEGS, 'B', ModBlocks.BLOCK_STARMETAL);
        g.shaped(ModItems.BJ_BOOTS, 1, p("N N", "BSB"), 'N', ModItems.PLATE_ARMOR_LUNAR, 'S', ModItems.STARMETAL_BOOTS, 'B', ModBlocks.BLOCK_STARMETAL);
        g.shaped(ModItems.HEV_HELMET, 1, p("PPC", "PBP", "IFI"), 'P', ModItems.PLATE_ARMOR_HEV, 'C', basic, 'B', ModItems.TITANIUM_HELMET, 'I', anyPlastic, 'F', ModItems.GAS_MASK_FILTER);
        g.shaped(ModItems.HEV_PLATE, 1, p("MPM", "IBI", "PPP"), 'P', ModItems.PLATE_ARMOR_HEV, 'B', ModItems.TITANIUM_PLATE, 'I', anyPlastic, 'M', ModItems.MOTOR_DESH);
        g.shaped(ModItems.HEV_LEGS, 1, p("MPM", "IBI", "P P"), 'P', ModItems.PLATE_ARMOR_HEV, 'B', ModItems.TITANIUM_LEGS, 'I', anyPlastic, 'M', ModItems.MOTOR_DESH);
        g.shaped(ModItems.HEV_BOOTS, 1, p("P P", "PBP"), 'P', ModItems.PLATE_ARMOR_HEV, 'B', ModItems.TITANIUM_BOOTS);
        var polonium = m(ModMaterials.POLONIUM, MaterialShape.BILLET);
        g.shaped(ModItems.FAU_HELMET, 1, p("PWP", "PBP", "FSF"), 'P', ModItems.PLATE_ARMOR_FAU, 'W', Items.RED_WOOL, 'B', ModItems.STARMETAL_HELMET, 'F', ModItems.GAS_MASK_FILTER, 'S', ModItems.PIPE_STEEL);
        g.shaped(ModItems.FAU_PLATE, 1, p("MCM", "PBP", "PSP"), 'M', ModItems.MOTOR_DESH, 'C', ModItems.DEMON_CORE_CLOSED, 'P', ModItems.PLATE_ARMOR_FAU, 'B', ModItems.STARMETAL_PLATE, 'S', ModBlocks.ANCIENT_SCRAP);
        g.shaped(ModItems.FAU_LEGS, 1, p("MPM", "PBP", "PDP"), 'M', ModItems.MOTOR_DESH, 'P', ModItems.PLATE_ARMOR_FAU, 'B', ModItems.STARMETAL_LEGS, 'D', polonium);
        g.shaped(ModItems.FAU_BOOTS, 1, p("PDP", "PBP"), 'P', ModItems.PLATE_ARMOR_FAU, 'D', polonium, 'B', ModItems.STARMETAL_BOOTS);
        var chainsteel = m(ModMaterials.CHAINSSTEEL, MaterialShape.INGOT);
        g.shaped(ModItems.DNS_HELMET, 1, p("PCP", "PBP", "PSP"), 'P', ModItems.PLATE_ARMOR_DNT, 'S', chainsteel, 'B', ModItems.BJ_HELMET, 'C', ModItems.QUANTUM_CIRCUIT);
        g.shaped(ModItems.DNS_PLATE, 1, p("PCP", "PBP", "PSP"), 'P', ModItems.PLATE_ARMOR_DNT, 'S', chainsteel, 'B', ModItems.BJ_PLATE_JETPACK, 'C', ModItems.SINGULARITY_SPARK);
        g.shaped(ModItems.DNS_LEGS, 1, p("PCP", "PBP", "PSP"), 'P', ModItems.PLATE_ARMOR_DNT, 'S', chainsteel, 'B', ModItems.BJ_LEGS, 'C', ModItems.COIN_WORM);
        g.shaped(ModItems.DNS_BOOTS, 1, p("PCP", "PBP", "PSP"), 'P', ModItems.PLATE_ARMOR_DNT, 'S', chainsteel, 'B', ModItems.BJ_BOOTS, 'C', ModItems.DEMON_CORE_CLOSED);
        var legendary2 = PartTabMetaItems.get("parts_legendary_tier2");
        g.shaped(ModItems.RPA_HELMET, 1, p("KPK", "PLP", " F "), 'L', legendary2, 'K', ModItems.PLATE_KEVLAR, 'P', ModItems.PLATE_ARMOR_AJR, 'F', ModItems.GAS_MASK_FILTER_COMBO);
        g.shaped(ModItems.RPA_PLATE, 1, p("P P", "MLM", "PKP"), 'L', legendary2, 'K', ModItems.PLATE_KEVLAR, 'P', ModItems.PLATE_ARMOR_AJR, 'M', ModItems.MOTOR_DESH);
        g.shaped(ModItems.RPA_LEGS, 1, p("MPM", "KLK", "P P"), 'L', legendary2, 'K', ModItems.PLATE_KEVLAR, 'P', ModItems.PLATE_ARMOR_AJR, 'M', ModItems.MOTOR_DESH);
        g.shaped(ModItems.RPA_BOOTS, 1, p("KLK", "P P"), 'L', legendary2, 'K', ModItems.PLATE_KEVLAR, 'P', ModItems.PLATE_ARMOR_AJR);
        g.shaped(ModItems.STEAMSUIT_HELMET, 1, p("DCD", "CXC", " F "), 'D', desh, 'C', cuPlate, 'X', ModItems.STEEL_HELMET, 'F', ModItems.GAS_MASK_FILTER);
        g.shaped(ModItems.STEAMSUIT_PLATE, 1, p("C C", "DXD", "CFC"), 'D', desh, 'C', cuPlate, 'X', ModItems.STEEL_PLATE, 'F', ModItems.TANK_STEEL);
        g.shaped(ModItems.STEAMSUIT_LEGS, 1, p("CCC", "DXD", "C C"), 'D', desh, 'C', cuPlate, 'X', ModItems.STEEL_LEGS);
        g.shaped(ModItems.STEAMSUIT_BOOTS, 1, p("C C", "DXD"), 'D', desh, 'C', cuPlate, 'X', ModItems.STEEL_BOOTS);
        g.shaped(ModItems.DIESELSUIT_HELMET, 1, p("W W", "W W", "SCS"), 'W', Items.RED_WOOL, 'S', steelIngot, 'C', ModItems.ANALOG_CIRCUIT);
        g.shaped(ModItems.DIESELSUIT_PLATE, 1, p("W W", "CDC", "SWS"), 'W', Items.RED_WOOL, 'S', steelIngot, 'C', ModItems.ANALOG_CIRCUIT, 'D', ModBlocks.DIESELGEN);
        g.shaped(ModItems.DIESELSUIT_LEGS, 1, p("M M", "S S", "W W"), 'W', Items.RED_WOOL, 'S', steelIngot, 'M', ModItems.MOTOR);
        g.shaped(ModItems.DIESELSUIT_BOOTS, 1, p("W W", "S S"), 'W', Items.RED_WOOL, 'S', steelIngot);
        var rubber = m(ModMaterials.RUBBER, MaterialShape.INGOT);
        g.shaped(ModItems.ENVSUIT_HELMET, 1, p("TCT", "TGT", "RRR"), 'T', tiPlate, 'C', ModItems.MICROCHIP, 'G', anyPane, 'R', rubber);
        g.shaped(ModItems.ENVSUIT_PLATE, 1, p("T T", "TCT", "RRR"), 'T', tiPlate, 'C', tiCast, 'R', rubber);
        g.shaped(ModItems.ENVSUIT_LEGS, 1, p("TCT", "R R", "T T"), 'T', tiPlate, 'C', tiCast, 'R', rubber);
        g.shaped(ModItems.ENVSUIT_BOOTS, 1, p("R R", "T T"), 'T', tiPlate, 'R', rubber);

        //Bismuth fursui- I mean armor
        var bismuthPlate = m(ModMaterials.BISMUTH, MaterialShape.PLATE);
        var goldWire = m(ModMaterials.GOLD, MaterialShape.WIRE);
        g.shaped(ModItems.BISMUTH_HELMET, 1, p("GPP", "P  ", "FPP"), 'G', Items.GOLD_INGOT, 'P', bismuthPlate, 'F', ModItems.RAG);
        g.shaped(ModItems.BISMUTH_PLATE, 1, p("RWR", "PCP", "SFS"), 'R', m(ModMaterials.RARE, MaterialShape.CRYSTAL), 'W', goldWire, 'P', bismuthPlate, 'C', ModItems.LASER_CRYSTAL_BISMUTH, 'S', ModItems.RING_STARMETAL, 'F', ModItems.RAG);
        g.shaped(ModItems.BISMUTH_LEGS, 1, p("FSF", "   ", "FSF"), 'F', ModItems.RAG, 'S', ModItems.RING_STARMETAL);
        g.shaped(ModItems.BISMUTH_BOOTS, 1, p("W W", "P P"), 'W', goldWire, 'P', bismuthPlate);

        //Euphemium armor
        var euph = m(ModMaterials.EUPHEMIUM, MaterialShape.PLATE);
        g.shaped(ModItems.EUPHEMIUM_HELMET, 1, p("EEE", "E E"), 'E', euph);
        g.shaped(ModItems.EUPHEMIUM_PLATE, 1, p("EWE", "EEE", "EEE"), 'E', euph, 'W', ModItems.WATCH);
        g.shaped(ModItems.EUPHEMIUM_LEGS, 1, p("EEE", "E E", "E E"), 'E', euph);
        g.shaped(ModItems.EUPHEMIUM_BOOTS, 1, p("E E", "E E"), 'E', euph);

        //Jetpacks
        g.shaped(ModItems.JETPACK_FLY, 1, p("ACA", "TLT", "D D"), 'A', m(ModMaterials.ALUMINUM, MaterialShape.PLATE), 'C', basic, 'T', ModItems.TANK_STEEL, 'L', Items.LEATHER, 'D', ModItems.THRUSTER_SMALL);
        g.shaped(ModItems.JETPACK_BREAK, 1, p("ICI", "TJT", "I I"), 'C', basic, 'T', m(ModMaterials.DURA_STEEL, MaterialShape.INGOT), 'J', ModItems.JETPACK_FLY, 'I', m(ModMaterials.POLYMER, MaterialShape.PLATE));
        g.shaped(ModItems.JETPACK_VECTOR, 1, p("TCT", "MJM", "B B"), 'C', advanced, 'T', ModItems.TANK_STEEL, 'J', ModItems.JETPACK_BREAK, 'M', ModItems.MOTOR, 'B', ModItems.BOLT_HIGHSPEED_STEEL);
        g.shaped(ModItems.JETPACK_BOOST, 1, p("PCP", "DJD", "PAP"), 'C', advanced, 'P', m(ModMaterials.SATURNITE, MaterialShape.PLATE), 'D', desh, 'J', ModItems.JETPACK_VECTOR, 'A', m(ModMaterials.COPPER, MaterialShape.PLATE_CAST));

        //Hazmat
        g.shaped(ModItems.HAZMAT_HELMET, 1, p("EEE", "EIE", " P "), 'E', ModItems.HAZMAT_CLOTH, 'I', anyPane, 'P', ironPlate);
        g.shaped(ModItems.HAZMAT_PLATE, 1, p("E E", "EEE", "EEE"), 'E', ModItems.HAZMAT_CLOTH);
        g.shaped(ModItems.HAZMAT_LEGS, 1, p("EEE", "E E", "E E"), 'E', ModItems.HAZMAT_CLOTH);
        g.shaped(ModItems.HAZMAT_BOOTS, 1, p("E E", "E E"), 'E', ModItems.HAZMAT_CLOTH);
        g.shaped(ModItems.HAZMAT_HELMET_RED, 1, p("EEE", "IEI", "EFE"), 'E', ModItems.HAZMAT_CLOTH_RED, 'I', anyPane, 'F', ironPlate);
        g.shaped(ModItems.HAZMAT_PLATE_RED, 1, p("E E", "EEE", "EEE"), 'E', ModItems.HAZMAT_CLOTH_RED);
        g.shaped(ModItems.HAZMAT_LEGS_RED, 1, p("EEE", "E E", "E E"), 'E', ModItems.HAZMAT_CLOTH_RED);
        g.shaped(ModItems.HAZMAT_BOOTS_RED, 1, p("E E", "E E"), 'E', ModItems.HAZMAT_CLOTH_RED);
        g.shaped(ModItems.HAZMAT_HELMET_GREY, 1, p("EEE", "IEI", "EFE"), 'E', ModItems.HAZMAT_CLOTH_GREY, 'I', anyPane, 'F', ironPlate);
        g.shaped(ModItems.HAZMAT_PLATE_GREY, 1, p("E E", "EEE", "EEE"), 'E', ModItems.HAZMAT_CLOTH_GREY);
        g.shaped(ModItems.HAZMAT_LEGS_GREY, 1, p("EEE", "E E", "E E"), 'E', ModItems.HAZMAT_CLOTH_GREY);
        g.shaped(ModItems.HAZMAT_BOOTS_GREY, 1, p("E E", "E E"), 'E', ModItems.HAZMAT_CLOTH_GREY);
        g.shaped(ModItems.ASBESTOS_HELMET, 1, p("EEE", "EIE"), 'E', ModItems.ASBESTOS_CLOTH, 'I', m(ModMaterials.GOLD, MaterialShape.PLATE));
        g.shaped(ModItems.ASBESTOS_PLATE, 1, p("E E", "EEE", "EEE"), 'E', ModItems.ASBESTOS_CLOTH);
        g.shaped(ModItems.ASBESTOS_LEGS, 1, p("EEE", "E E", "E E"), 'E', ModItems.ASBESTOS_CLOTH);
        g.shaped(ModItems.ASBESTOS_BOOTS, 1, p("E E", "E E"), 'E', ModItems.ASBESTOS_CLOTH);
        g.shaped(ModItems.HAZMAT_PAA_HELMET, 1, p("EEE", "IEI", " P "), 'E', ModItems.PLATE_PAA, 'I', anyPane, 'P', ironPlate);
        g.shaped(ModItems.HAZMAT_PAA_PLATE, 1, p("E E", "EEE", "EEE"), 'E', ModItems.PLATE_PAA);
        g.shaped(ModItems.HAZMAT_PAA_LEGS, 1, p("EEE", "E E", "E E"), 'E', ModItems.PLATE_PAA);
        g.shaped(ModItems.HAZMAT_PAA_BOOTS, 1, p("E E", "E E"), 'E', ModItems.PLATE_PAA);
        g.shaped(ModItems.PAA_PLATE, 1, p("E E", "NEN", "ENE"), 'E', ModItems.PLATE_PAA, 'N', ModItems.NEUTRON_REFLECTOR);
        g.shaped(ModItems.PAA_LEGS, 1, p("EEE", "N N", "E E"), 'E', ModItems.PLATE_PAA, 'N', ModItems.NEUTRON_REFLECTOR);
        g.shaped(ModItems.PAA_BOOTS, 1, p("E E", "N N"), 'E', ModItems.PLATE_PAA, 'N', ModItems.NEUTRON_REFLECTOR);

        //Liquidator Suit
        g.shaped(ModItems.LIQUIDATOR_HELMET, 1, p("III", "CBC", "III"), 'I', anyRubber, 'C', ModItems.LEAD_CLADDING, 'B', ModItems.HAZMAT_HELMET_GREY);
        g.shaped(ModItems.LIQUIDATOR_PLATE, 1, p("ICI", "TBT", "ICI"), 'I', anyRubber, 'C', ModItems.LEAD_CLADDING, 'B', ModItems.HAZMAT_PLATE_GREY, 'T', ModItems.GAS_EMPTY);
        g.shaped(ModItems.LIQUIDATOR_LEGS, 1, p("III", "CBC", "I I"), 'I', anyRubber, 'C', ModItems.LEAD_CLADDING, 'B', ModItems.HAZMAT_LEGS_GREY);
        g.shaped(ModItems.LIQUIDATOR_BOOTS, 1, p("ICI", "IBI"), 'I', anyRubber, 'C', ModItems.LEAD_CLADDING, 'B', ModItems.HAZMAT_BOOTS_GREY);

        //Masks
        g.shaped(ModItems.GOGGLES, 1, p("P P", "GPG"), 'G', anyPane, 'P', steelPlate);
        g.shaped(ModItems.MASK_OF_INFAMY, 1, p("III", "III", " I "), 'I', ironPlate);
        g.shaped(ModItems.ASHGLASSES, 1, p("I I", "GPG"), 'I', anyRubber, 'G', ModBlocks.GLASS_ASH, 'P', anyPlastic);

        //Configged (LBSM aus)
        g.shaped(ModItems.STARMETAL_HELMET, 1, p("EEE", "ECE"), 'E', star, 'C', ModItems.COBALT_HELMET);
        g.shaped(ModItems.STARMETAL_PLATE, 1, p("ECE", "EEE", "EEE"), 'E', star, 'C', ModItems.COBALT_PLATE);
        g.shaped(ModItems.STARMETAL_LEGS, 1, p("EEE", "ECE", "E E"), 'E', star, 'C', ModItems.COBALT_LEGS);
        g.shaped(ModItems.STARMETAL_BOOTS, 1, p("E E", "ECE"), 'E', star, 'C', ModItems.COBALT_BOOTS);
        g.shaped(ModItems.SCHRABIDIUM_HELMET, 1, p("EEE", "ESE", " P "), 'E', sa326, 'S', ModItems.STARMETAL_HELMET, 'P', ModItems.PELLET_CHARGED);
        g.shaped(ModItems.SCHRABIDIUM_PLATE, 1, p("ESE", "EPE", "EEE"), 'E', sa326, 'S', ModItems.STARMETAL_PLATE, 'P', ModItems.PELLET_CHARGED);
        g.shaped(ModItems.SCHRABIDIUM_LEGS, 1, p("EEE", "ESE", "EPE"), 'E', sa326, 'S', ModItems.STARMETAL_LEGS, 'P', ModItems.PELLET_CHARGED);
        g.shaped(ModItems.SCHRABIDIUM_BOOTS, 1, p("EPE", "ESE"), 'E', sa326, 'S', ModItems.STARMETAL_BOOTS, 'P', ModItems.PELLET_CHARGED);

        // CraftingManager: Jacken
        g.shaped(ModItems.JACKT, 1, p("S S", "LIL", "LIL"), 'S', steelPlate, 'L', Items.LEATHER, 'I', anyRubber);
        g.shaped(ModItems.JACKT2, 1, p("S S", "LIL", "III"), 'S', steelPlate, 'L', Items.LEATHER, 'I', anyRubber);
    }
}
