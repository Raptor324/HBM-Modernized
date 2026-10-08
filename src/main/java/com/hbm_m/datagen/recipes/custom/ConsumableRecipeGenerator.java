package com.hbm_m.datagen.recipes.custom;

import java.util.function.Consumer;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.lib.RefStrings;

import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/**
 * 1:1-Port von {@code com.hbm.crafting.ConsumableRecipes} (1.7.10), Abschnitte Nahrung, Dosen,
 * Feldflaschen, Limonaden und Medizin. Rezepte mit Fluessigkeits-Zutaten
 * ({@code Fluids.X.getDict(1000)}: Schokomilch, Kreaturen-Dose) folgen mit den Fluid-Behaeltern.
 *
 * <p>OreDict-Schluessel des Originals werden auf die entsprechenden Port-Gegenstaende abgebildet:
 * {@code KEY_ANYPANE} = Glasscheiben-Tag, {@code KEY_ORANGE} = orange Farbe,
 * {@code S.dust()} = Schwefel, {@code F.dust()} = Fluorit, {@code KNO.dust()} = Salpeter.</p>
 */
public final class ConsumableRecipeGenerator {

    private ConsumableRecipeGenerator() {}

    private static Item m(ModMaterials mat, MaterialShape shape) {
        if (shape == MaterialShape.BLOCK && ModBlocks.hasIngotBlock(mat)) return ModBlocks.getIngotBlock(mat).get().asItem();
        Item i = ModMaterialItems.item(mat, shape);
        if (i == null) throw new IllegalStateException("Material fehlt: " + mat + " / " + shape);
        return i;
    }

    private static Ingredient ing(Object o) {
        if (o instanceof Ingredient in) return in;
        if (o instanceof ItemLike il) return Ingredient.of(il);
        if (o instanceof dev.architectury.registry.registries.RegistrySupplier<?> rs) return Ingredient.of((ItemLike) rs.get());
        throw new IllegalArgumentException(String.valueOf(o));
    }

    private static ItemLike like(Object o) {
        if (o instanceof ItemLike il) return il;
        if (o instanceof dev.architectury.registry.registries.RegistrySupplier<?> rs) return (ItemLike) rs.get();
        throw new IllegalArgumentException(String.valueOf(o));
    }

    private static int counter = 0;

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "consumables/" + name + "_" + (counter++));
    }

    /** {@code addRecipeAuto(out, pattern..., key, value...)}. */
    private static void shaped(Consumer<FinishedRecipe> w, Object out, int count, String[] pattern, Object... keys) {
        ShapedRecipeBuilder b = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, like(out), count);
        for (String p : pattern) b.pattern(p);
        String joined = String.join("", pattern);
        for (int i = 0; i < keys.length; i += 2) {
            char c = (Character) keys[i];
            if (joined.indexOf(c) < 0) continue;
            b.define(c, ing(keys[i + 1]));
        }
        b.unlockedBy("has_item", InventoryChangeTrigger.TriggerInstance.hasItems(like(out)));
        b.save(w, id(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(like(out).asItem()).getPath()));
    }

    /** {@code addShapelessAuto(out, inputs...)}. */
    private static void shapeless(Consumer<FinishedRecipe> w, Object out, int count, Object... inputs) {
        ShapelessRecipeBuilder b = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, like(out), count);
        for (Object o : inputs) b.requires(ing(o));
        b.unlockedBy("has_item", InventoryChangeTrigger.TriggerInstance.hasItems(like(out)));
        b.save(w, id(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(like(out).asItem()).getPath()));
    }

    private static String[] p(String... rows) {
        return rows;
    }

    public static void generate(Consumer<FinishedRecipe> w) {
        counter = 0;
        Ingredient anyPane = Ingredient.of(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("forge", "glass_panes")));
        Item coalDust = m(ModMaterials.COAL, MaterialShape.POWDER);
        Item fluorite = ModItems.FLUORITE.get();
        Item sulfur = ModItems.SULFUR.get();
        Item niter = ModItems.NITER.get();

        // Food
        shaped(w, ModItems.BOMB_WAFFLE, 1, p("WEW", "MPM", "WEW"), 'W', Items.WHEAT, 'E', Items.EGG, 'M', Items.MILK_BUCKET, 'P', ModItems.FAT_MAN_CORE);
        shaped(w, ModItems.SCHNITZEL_VEGAN, 3, p("RWR", "WPW", "RWR"), 'W', ModItems.NUCLEAR_WASTE, 'R', Items.SUGAR_CANE, 'P', Items.PUMPKIN_SEEDS);
        shaped(w, ModItems.COTTON_CANDY, 2, p(" S ", "SPS", " H "), 'P', m(ModMaterials.PLUTONIUM239, MaterialShape.NUGGET), 'S', Items.SUGAR, 'H', Items.STICK);
        shaped(w, ModItems.APPLE_SCHRABIDIUM, 1, p("SSS", "SAS", "SSS"), 'S', m(ModMaterials.SCHRABIDIUM, MaterialShape.NUGGET), 'A', Items.APPLE);
        shaped(w, ModItems.APPLE_SCHRABIDIUM_1, 1, p("SSS", "SAS", "SSS"), 'S', m(ModMaterials.SCHRABIDIUM, MaterialShape.INGOT), 'A', Items.APPLE);
        shaped(w, ModItems.APPLE_SCHRABIDIUM_2, 1, p("SSS", "SAS", "SSS"), 'S', m(ModMaterials.SCHRABIDIUM, MaterialShape.BLOCK), 'A', Items.APPLE);
        shaped(w, ModItems.APPLE_LEAD, 1, p("SSS", "SAS", "SSS"), 'S', m(ModMaterials.LEAD, MaterialShape.NUGGET), 'A', Items.APPLE);
        shaped(w, ModItems.APPLE_LEAD_1, 1, p("SSS", "SAS", "SSS"), 'S', m(ModMaterials.LEAD, MaterialShape.INGOT), 'A', Items.APPLE);
        shaped(w, ModItems.APPLE_LEAD_2, 1, p("SSS", "SAS", "SSS"), 'S', m(ModMaterials.LEAD, MaterialShape.BLOCK), 'A', Items.APPLE);
        shaped(w, ModItems.APPLE_EUPHEMIUM, 1, p("EEE", "EAE", "EEE"), 'E', m(ModMaterials.EUPHEMIUM, MaterialShape.NUGGET), 'A', Items.APPLE);
        shapeless(w, ModItems.TEM_FLAKES, 1, Items.GOLD_NUGGET, Items.PAPER);
        shapeless(w, ModItems.TEM_FLAKES_1, 1, Items.GOLD_NUGGET, Items.GOLD_NUGGET, Items.GOLD_NUGGET, Items.PAPER);
        shapeless(w, ModItems.TEM_FLAKES_2, 1, Items.GOLD_INGOT, Items.GOLD_INGOT, Items.GOLD_NUGGET, Items.GOLD_NUGGET, Items.PAPER);
        shapeless(w, ModItems.GLOWING_STEW, 1, Items.BOWL, ModBlocks.MUSH.get(), ModBlocks.MUSH.get());
        shapeless(w, ModItems.BALEFIRE_SCRAMBLED, 1, Items.BOWL, ModItems.EGG_BALEFIRE);
        shapeless(w, ModItems.BALEFIRE_AND_HAM, 1, ModItems.BALEFIRE_SCRAMBLED, Items.COOKED_BEEF);
        shapeless(w, ModItems.MED_IPECAC, 1, Items.GLASS_BOTTLE, Items.NETHER_WART);
        shapeless(w, ModItems.MED_PTSD, 1, ModItems.MED_IPECAC);
        shapeless(w, ModItems.PANCAKE, 1, Items.REDSTONE, m(ModMaterials.DIAMOND, MaterialShape.POWDER), Items.WHEAT, ModItems.BOLT_STEEL,
                m(ModMaterials.COPPER, MaterialShape.WIRE), m(ModMaterials.STEEL, MaterialShape.PLATE));
        shapeless(w, ModItems.PANCAKE, 1, Items.REDSTONE, m(ModMaterials.EMERALD, MaterialShape.POWDER), Items.WHEAT, ModItems.BOLT_STEEL,
                m(ModMaterials.COPPER, MaterialShape.WIRE), m(ModMaterials.STEEL, MaterialShape.PLATE));
        shapeless(w, ModItems.LOOPS, 1, ModItems.FLAME_PONY, Items.WHEAT, Items.SUGAR);
        shapeless(w, ModItems.LOOP_STEW, 1, ModItems.LOOPS, ModItems.CAN_SMART, Items.BOWL);
        shapeless(w, ModItems.COFFEE, 1, coalDust, Items.MILK_BUCKET, Items.POTION, Items.SUGAR);
        shapeless(w, ModItems.COFFEE_RADIUM, 1, ModItems.COFFEE, m(ModMaterials.RA226, MaterialShape.NUGGET));
        shapeless(w, m(ModMaterials.SMORE, MaterialShape.INGOT), 1, Items.WHEAT, ModItems.MARSHMALLOW_ROASTED, Items.COCOA_BEANS);
        shapeless(w, ModItems.MARSHMALLOW, 1, Items.STICK, Items.SUGAR, Items.WHEAT_SEEDS);
        shapeless(w, ModItems.CHEESE_QUESADILLA, 3, ModItems.CHEESE, ModItems.CHEESE, Items.BREAD);
        shapeless(w, ModItems.CANNED_RECURSION, 1, ModItems.CANNED_RECURSION);

        // Peas
        shaped(w, ModItems.PEAS, 1, p(" S ", "SNS", " S "), 'S', Items.WHEAT_SEEDS, 'N', Items.GOLD_NUGGET);

        // Cans
        shaped(w, ModItems.CAN_EMPTY, 1, p("P", "P"), 'P', m(ModMaterials.ALUMINUM, MaterialShape.PLATE));
        shapeless(w, ModItems.CAN_SMART, 1, ModItems.CAN_EMPTY, Items.POTION, Items.SUGAR, niter);
        shapeless(w, ModItems.CAN_REDBOMB, 1, ModItems.CAN_EMPTY, Items.POTION, Items.SUGAR, ModItems.PELLET_CLUSTER);
        shapeless(w, ModItems.CAN_MRSUGAR, 1, ModItems.CAN_EMPTY, Items.POTION, Items.SUGAR, fluorite);
        shapeless(w, ModItems.CAN_OVERCHARGE, 1, ModItems.CAN_EMPTY, Items.POTION, Items.SUGAR, sulfur);
        shapeless(w, ModItems.CAN_LUNA, 1, ModItems.CAN_EMPTY, Items.POTION, Items.SUGAR, m(ModMaterials.METEORITE, MaterialShape.POWDER_TINY));
        shapeless(w, ModItems.MUCHO_MANGO, 1, Items.POTION, Items.SUGAR, Items.SUGAR, Items.ORANGE_DYE);

        // Canteens
        shaped(w, ModItems.CANTEEN_VODKA, 1, p("O", "P"), 'O', Items.POTATO, 'P', m(ModMaterials.STEEL, MaterialShape.PLATE));

        // Soda
        shaped(w, ModItems.BOTTLE_EMPTY, 6, p(" G ", "G G", "GGG"), 'G', anyPane);
        shapeless(w, ModItems.BOTTLE_NUKA, 1, ModItems.BOTTLE_EMPTY, Items.POTION, Items.SUGAR, coalDust);
        shapeless(w, ModItems.BOTTLE_CHERRY, 1, ModItems.BOTTLE_EMPTY, Items.POTION, Items.SUGAR, Items.REDSTONE);
        shapeless(w, ModItems.BOTTLE_QUANTUM, 1, ModItems.BOTTLE_EMPTY, Items.POTION, Items.SUGAR, ModItems.TRINITITE);
        shapeless(w, ModItems.BOTTLE_SPARKLE, 1, ModItems.BOTTLE_NUKA, Items.CARROT, Items.GOLD_NUGGET);
        shapeless(w, ModItems.BOTTLE_RAD, 1, ModItems.BOTTLE_QUANTUM, Items.CARROT, Items.GOLD_NUGGET);
        shaped(w, ModItems.BOTTLE2_EMPTY, 6, p(" G ", "G G", "G G"), 'G', anyPane);
        shapeless(w, ModItems.BOTTLE2_KORL, 1, ModItems.BOTTLE2_EMPTY, Items.POTION, Items.SUGAR, m(ModMaterials.COPPER, MaterialShape.POWDER));
        shapeless(w, ModItems.BOTTLE2_FRITZ, 1, ModItems.BOTTLE2_EMPTY, Items.POTION, Items.SUGAR, m(ModMaterials.TUNGSTEN, MaterialShape.POWDER));

        // Medicine
        shaped(w, ModItems.PILL_IODINE, 8, p("IF"), 'I', m(ModMaterials.IODINE, MaterialShape.POWDER), 'F', fluorite);
        shaped(w, ModItems.PLAN_C, 1, p("PFP"), 'P', ModItems.POWDER_POISON, 'F', fluorite);
        shapeless(w, ModItems.RADX, 1, coalDust, coalDust, fluorite);
        shapeless(w, ModItems.FMN, 1, coalDust, m(ModMaterials.POLONIUM, MaterialShape.POWDER), m(ModMaterials.STRONTIUM, MaterialShape.POWDER));
        shapeless(w, ModItems.FIVE_HTP, 1, coalDust, m(ModMaterials.EUPHEMIUM, MaterialShape.POWDER), ModItems.CANTEEN_VODKA);
        // GeneralConfig.enableLBSMSimpleMedicineRecipes ist standardmaessig aus - der Normalfall des Originals:
        shapeless(w, ModItems.SIOX, 8, coalDust, m(ModMaterials.ASBESTOS, MaterialShape.POWDER), m(ModMaterials.BISMUTH, MaterialShape.NUGGET));
        shapeless(w, ModItems.XANAX, 1, coalDust, niter, m(ModMaterials.BROMINE, MaterialShape.POWDER));
        // cigarette (braucht plant_item TOBACCO) folgt mit den Pflanzen-Items.
        shapeless(w, ModItems.CRACKPIPE, 1, ModItems.CATALYTIC_CONVERTER);

        // Syringes
        Ingredient anyRubber = Ingredient.of(m(ModMaterials.BIORUBBER, MaterialShape.INGOT), m(ModMaterials.RUBBER, MaterialShape.INGOT));
        Item ironPlate = m(ModMaterials.IRON, MaterialShape.PLATE);
        Item steelPlate = m(ModMaterials.STEEL, MaterialShape.PLATE);
        shaped(w, ModItems.SYRINGE_EMPTY, 6, p("P", "C", "B"), 'B', Items.IRON_BARS, 'C', ModItems.CELL_EMPTY, 'P', ironPlate);
        shaped(w, ModItems.SYRINGE_ANTIDOTE, 6, p("SSS", "PMP", "SSS"), 'S', ModItems.SYRINGE_EMPTY, 'P', Items.PUMPKIN_SEEDS, 'M', Items.MILK_BUCKET);
        shaped(w, ModItems.SYRINGE_ANTIDOTE, 6, p("SPS", "SMS", "SPS"), 'S', ModItems.SYRINGE_EMPTY, 'P', Items.PUMPKIN_SEEDS, 'M', Items.MILK_BUCKET);
        shaped(w, ModItems.SYRINGE_ANTIDOTE, 6, p("SSS", "PMP", "SSS"), 'S', ModItems.SYRINGE_EMPTY, 'P', Items.PUMPKIN_SEEDS, 'M', Items.SUGAR_CANE);
        shaped(w, ModItems.SYRINGE_ANTIDOTE, 6, p("SPS", "SMS", "SPS"), 'S', ModItems.SYRINGE_EMPTY, 'P', Items.PUMPKIN_SEEDS, 'M', Items.SUGAR_CANE);
        shaped(w, ModItems.SYRINGE_POISON, 1, p("SLS", "LCL", "SLS"), 'C', ModItems.SYRINGE_EMPTY, 'S', Items.SPIDER_EYE, 'L', m(ModMaterials.LEAD, MaterialShape.POWDER));
        shaped(w, ModItems.SYRINGE_POISON, 1, p("SLS", "LCL", "SLS"), 'C', ModItems.SYRINGE_EMPTY, 'S', Items.SPIDER_EYE, 'L', ModItems.POWDER_POISON);
        shaped(w, ModItems.SYRINGE_AWESOME, 1, p("SPS", "NCN", "SPS"), 'C', ModItems.SYRINGE_EMPTY, 'S', sulfur, 'P', m(ModMaterials.PLUTONIUM239, MaterialShape.NUGGET), 'N', m(ModMaterials.PLUTONIUM238, MaterialShape.NUGGET));
        shaped(w, ModItems.SYRINGE_AWESOME, 1, p("SNS", "PCP", "SNS"), 'C', ModItems.SYRINGE_EMPTY, 'S', sulfur, 'P', m(ModMaterials.PLUTONIUM239, MaterialShape.NUGGET), 'N', m(ModMaterials.PLUTONIUM238, MaterialShape.NUGGET));
        shaped(w, ModItems.SYRINGE_METAL_EMPTY, 6, p("P", "C", "B"), 'B', Items.IRON_BARS, 'C', ModItems.ROD_EMPTY, 'P', ironPlate);
        shaped(w, ModItems.SYRINGE_METAL_STIMPAK, 1, p(" N ", "NSN", " N "), 'N', Items.NETHER_WART, 'S', ModItems.SYRINGE_METAL_EMPTY);
        shapeless(w, ModItems.SYRINGE_METAL_STIMPAK, 1, ModItems.NITRA_SMALL, ModItems.NITRA_SMALL, ModItems.NITRA_SMALL, ModItems.SYRINGE_METAL_EMPTY);
        shaped(w, ModItems.SYRINGE_METAL_MEDX, 1, p(" N ", "NSN", " N "), 'N', Items.QUARTZ, 'S', ModItems.SYRINGE_METAL_EMPTY);
        shaped(w, ModItems.SYRINGE_METAL_PSYCHO, 1, p(" N ", "NSN", " N "), 'N', Items.GLOWSTONE_DUST, 'S', ModItems.SYRINGE_METAL_EMPTY);
        shaped(w, ModItems.SYRINGE_METAL_SUPER, 1, p(" N ", "PSP", "L L"), 'N', ModItems.BOTTLE_NUKA, 'P', steelPlate, 'S', ModItems.SYRINGE_METAL_STIMPAK, 'L', Items.LEATHER);
        shaped(w, ModItems.SYRINGE_METAL_SUPER, 1, p(" N ", "PSP", "L L"), 'N', ModItems.BOTTLE_NUKA, 'P', steelPlate, 'S', ModItems.SYRINGE_METAL_STIMPAK, 'L', anyRubber);
        shaped(w, ModItems.SYRINGE_METAL_SUPER, 1, p(" N ", "PSP", "L L"), 'N', ModItems.BOTTLE_CHERRY, 'P', steelPlate, 'S', ModItems.SYRINGE_METAL_STIMPAK, 'L', Items.LEATHER);
        shaped(w, ModItems.SYRINGE_METAL_SUPER, 1, p(" N ", "PSP", "L L"), 'N', ModItems.BOTTLE_CHERRY, 'P', steelPlate, 'S', ModItems.SYRINGE_METAL_STIMPAK, 'L', anyRubber);
        shapeless(w, ModItems.SYRINGE_TAINT, 1, ModItems.BOTTLE2_EMPTY, ModItems.SYRINGE_METAL_EMPTY, ModItems.DUCTTAPE, ModItems.POWDER_MAGIC, m(ModMaterials.SCHRABIDIUM, MaterialShape.NUGGET), Items.POTION);

        // Med bags
        shaped(w, ModItems.MED_BAG, 1, p("LLL", "SIS", "LLL"), 'L', Items.LEATHER, 'S', ModItems.SYRINGE_METAL_STIMPAK, 'I', ModItems.SYRINGE_ANTIDOTE);
        shaped(w, ModItems.MED_BAG, 1, p("LLL", "SIS", "LLL"), 'L', Items.LEATHER, 'S', ModItems.SYRINGE_METAL_STIMPAK, 'I', ModItems.PILL_IODINE);
        shaped(w, ModItems.MED_BAG, 1, p("LL", "SI", "LL"), 'L', Items.LEATHER, 'S', ModItems.SYRINGE_METAL_SUPER, 'I', ModItems.RADAWAY);
        shaped(w, ModItems.MED_BAG, 1, p("LLL", "SIS", "LLL"), 'L', anyRubber, 'S', ModItems.SYRINGE_METAL_STIMPAK, 'I', ModItems.SYRINGE_ANTIDOTE);
        shaped(w, ModItems.MED_BAG, 1, p("LLL", "SIS", "LLL"), 'L', anyRubber, 'S', ModItems.SYRINGE_METAL_STIMPAK, 'I', ModItems.PILL_IODINE);
        shaped(w, ModItems.MED_BAG, 1, p("LL", "SI", "LL"), 'L', anyRubber, 'S', ModItems.SYRINGE_METAL_SUPER, 'I', ModItems.RADAWAY);

        // IV Bags
        shaped(w, ModItems.IV_EMPTY, 4, p("S", "I", "S"), 'S', anyRubber, 'I', ironPlate);
        shapeless(w, ModItems.IV_XP_EMPTY, 1, ModItems.IV_EMPTY, ModItems.POWDER_MAGIC);

        // Radaway
        shapeless(w, ModItems.RADAWAY, 1, ModItems.IV_BLOOD, coalDust, Items.PUMPKIN_SEEDS);
        shapeless(w, ModItems.RADAWAY_STRONG, 1, ModItems.RADAWAY, ModBlocks.MUSH.get());
        shapeless(w, ModItems.RADAWAY_FLUSH, 1, ModItems.RADAWAY_STRONG, m(ModMaterials.IODINE, MaterialShape.POWDER));

        // CraftingManager: cbt_device; jetpack_tank braucht Kerosin-Fluessigkeitszutat (R5).
        shapeless(w, ModItems.CBT_DEVICE, 1, ModItems.BOLT_STEEL, ModItems.WRENCH);

        // Cladding
        Ingredient anyPlastic = Ingredient.of(m(ModMaterials.POLYMER, MaterialShape.INGOT), m(ModMaterials.BAKELITE, MaterialShape.INGOT));
        Item leadNugget = m(ModMaterials.LEAD, MaterialShape.NUGGET);
        shapeless(w, ModItems.PAINT_CLADDING, 1, leadNugget, leadNugget, leadNugget, leadNugget, Items.CLAY_BALL, Items.GLASS_BOTTLE);
        shaped(w, ModItems.RUBBER_CLADDING, 1, p("RCR", "CDC", "RCR"), 'R', anyRubber, 'C', coalDust, 'D', ModItems.DUCTTAPE);
        shaped(w, ModItems.LEAD_CLADDING, 1, p("DPD", "PRP", "DPD"), 'R', ModItems.RUBBER_CLADDING, 'P', m(ModMaterials.LEAD, MaterialShape.PLATE), 'D', ModItems.DUCTTAPE);
        shaped(w, ModItems.DESH_CLADDING, 1, p("DPD", "PRP", "DPD"), 'R', ModItems.LEAD_CLADDING, 'P', m(ModMaterials.DESH, MaterialShape.PLATE), 'D', ModItems.DUCTTAPE);
        shaped(w, ModItems.GHIORSIUM_CLADDING, 1, p("DPD", "PRP", "DPD"), 'R', ModItems.DESH_CLADDING, 'P', m(ModMaterials.GH336, MaterialShape.INGOT), 'D', ModItems.DUCTTAPE);
        shaped(w, ModItems.CLADDING_OBSIDIAN, 1, p("OOO", "PDP", "OOO"), 'O', Items.OBSIDIAN, 'P', steelPlate, 'D', ModItems.DUCTTAPE);
        shaped(w, ModItems.CLADDING_IRON, 1, p("OOO", "PDP", "OOO"), 'O', ironPlate, 'P', m(ModMaterials.POLYMER, MaterialShape.PLATE), 'D', ModItems.DUCTTAPE);

        // Inserts
        shaped(w, ModItems.INSERT_STEEL, 1, p("DPD", "PSP", "DPD"), 'D', ModItems.DUCTTAPE, 'P', ironPlate, 'S', m(ModMaterials.STEEL, MaterialShape.BLOCK));
        shaped(w, ModItems.INSERT_DU, 1, p("DPD", "PSP", "DPD"), 'D', ModItems.DUCTTAPE, 'P', ironPlate, 'S', m(ModMaterials.URANIUM238, MaterialShape.BLOCK));
        shaped(w, ModItems.INSERT_GHIORSIUM, 1, p("DPD", "PSP", "DPD"), 'D', ModItems.DUCTTAPE, 'P', m(ModMaterials.GH336, MaterialShape.INGOT), 'S', m(ModMaterials.URANIUM238, MaterialShape.INGOT));
        shaped(w, ModItems.INSERT_POLONIUM, 1, p("DPD", "PSP", "DPD"), 'D', ModItems.DUCTTAPE, 'P', ironPlate, 'S', ModBlocks.POLONIUM210_BLOCK.get());
        shaped(w, ModItems.INSERT_ERA, 1, p("DPD", "PSP", "DPD"), 'D', ModItems.DUCTTAPE, 'P', ironPlate, 'S', m(ModMaterials.SEMTEX, MaterialShape.INGOT));
        shaped(w, ModItems.INSERT_KEVLAR, 1, p("KIK", "IDI", "KIK"), 'K', ModItems.PLATE_KEVLAR, 'I', anyRubber, 'D', ModItems.DUCTTAPE);
        shaped(w, ModItems.INSERT_SAPI, 1, p("PKP", "DPD", "PKP"), 'P', anyPlastic, 'K', ModItems.INSERT_KEVLAR, 'D', ModItems.DUCTTAPE);
        shaped(w, ModItems.INSERT_ESAPI, 1, p("PKP", "DSD", "PKP"), 'P', anyPlastic, 'K', ModItems.INSERT_SAPI, 'D', ModItems.DUCTTAPE, 'S', m(ModMaterials.WEAPONSTEEL, MaterialShape.PLATE));
        shaped(w, ModItems.INSERT_XSAPI, 1, p("PKP", "DSD", "PKP"), 'P', m(ModMaterials.ASBESTOS, MaterialShape.INGOT), 'K', ModItems.INSERT_ESAPI, 'D', ModItems.DUCTTAPE, 'S', m(ModMaterials.SATURNITE, MaterialShape.PLATE));
        shaped(w, ModItems.INSERT_YHARONITE, 1, p("YIY", "IYI", "YIY"), 'Y', m(ModMaterials.YHARONITE, MaterialShape.BILLET), 'I', ModItems.INSERT_DU);
        shaped(w, ModItems.AUSTRALIUM_III, 1, p("WSW", "PAP", "SPS"), 'S', m(ModMaterials.STEEL, MaterialShape.PLATE_WELDED), 'P', anyPlastic, 'A', m(ModMaterials.AUSTRALIUM, MaterialShape.INGOT), 'W', m(ModMaterials.GOLD, MaterialShape.WIRE_DENSE));

        // Servos
        shaped(w, ModItems.SERVO_SET, 1, p("MBM", "PBP", "MBM"), 'M', ModItems.MOTOR, 'B', ModItems.BOLT_STEEL, 'P', ironPlate);
        shaped(w, ModItems.SERVO_SET_DESH, 1, p("MBM", "PSP", "MBM"), 'M', ModItems.MOTOR_DESH, 'B', ModItems.BOLT_HIGHSPEED_STEEL, 'P', m(ModMaterials.DESH, MaterialShape.PLATE), 'S', ModItems.SERVO_SET);

        // Boot Mods
        shaped(w, ModItems.PADS_RUBBER, 1, p("P P", "IDI", "P P"), 'P', anyRubber, 'I', ironPlate, 'D', ModItems.DUCTTAPE);
        shaped(w, ModItems.PADS_SLIME, 1, p("SPS", "DSD", "SPS"), 'S', Items.SLIME_BALL, 'P', ModItems.PADS_RUBBER, 'D', ModItems.DUCTTAPE);
        shaped(w, ModItems.PADS_STATIC, 1, p("CDC", "ISI", "CDC"), 'C', Items.COPPER_INGOT, 'D', ModItems.DUCTTAPE, 'I', anyRubber, 'S', ModItems.PADS_SLIME);

        // Batteries: brauchen battery_pack (EnumBatteryPack-Kondensatoren) - folgen mit den Batterie-Items.

        // Special Mods
        Item redWireDense = m(ModMaterials.RED_COPPER, MaterialShape.WIRE_DENSE);
        shaped(w, ModItems.HORSESHOE_MAGNET, 1, p("L L", "I I", "ILI"), 'L', ModItems.LODESTONE, 'I', Items.IRON_INGOT);
        shaped(w, ModItems.INDUSTRIAL_MAGNET, 1, p("SMS", " B ", "SMS"), 'S', m(ModMaterials.STEEL, MaterialShape.INGOT), 'M', ModItems.HORSESHOE_MAGNET, 'B', redWireDense);
        shaped(w, ModItems.HEART_CONTAINER, 1, p("HAH", "ACA", "HAH"), 'H', ModItems.HEART_PIECE, 'A', m(ModMaterials.ALUMINUM, MaterialShape.INGOT), 'C', ModItems.COIN_CREEPER);
        shaped(w, ModItems.HEART_BOOSTER, 1, p("GHG", "MCM", "GHG"), 'G', Items.GOLD_INGOT, 'H', ModItems.HEART_CONTAINER, 'M', ModItems.MORNING_GLORY, 'C', ModItems.COIN_MASKMAN);
        shaped(w, ModItems.HEART_FAB, 1, p("GHG", "MCM", "GHG"), 'G', m(ModMaterials.POLONIUM, MaterialShape.BILLET), 'H', ModItems.HEART_BOOSTER, 'M',
                Ingredient.of(ModItems.COAL_COKE.get(), ModItems.LIGNITE_COKE.get(), ModItems.COKE_PETROLEUM.get()), 'C', ModItems.COIN_WORM);
        shaped(w, ModItems.INK, 1, p("FPF", "PIP", "FPF"), 'F', Ingredient.of(Items.POPPY, Items.BLUE_ORCHID, Items.ALLIUM, Items.AZURE_BLUET, Items.RED_TULIP, Items.ORANGE_TULIP, Items.WHITE_TULIP, Items.PINK_TULIP, Items.OXEYE_DAISY),
                'P', ModItems.ARMOR_POLISH, 'I', Items.BLACK_DYE);
        shaped(w, ModItems.BATHWATER_MK2, 1, p("MWM", "WBW", "MWM"), 'M', ModItems.BOTTLE_MERCURY, 'W', ModItems.NUCLEAR_WASTE, 'B', ModItems.BATHWATER);
        shaped(w, ModItems.BACK_TESLA, 1, p("DGD", "GTG", "DGD"), 'D', ModItems.DUCTTAPE, 'G', m(ModMaterials.GOLD, MaterialShape.WIRE), 'T', ModBlocks.TESLA.get());
        shaped(w, ModItems.MEDAL_LIQUIDATOR, 1, p("GBG", "BFB", "GBG"), 'G', m(ModMaterials.AU198, MaterialShape.NUGGET), 'B', m(ModMaterials.BORON, MaterialShape.INGOT), 'F', ModItems.DEBRIS_FUEL);
        shapeless(w, ModItems.INJECTOR_5HTP, 1, ModItems.FIVE_HTP, ModItems.INTEGRATED_CIRCUIT, m(ModMaterials.SATURNITE, MaterialShape.PLATE));
        shapeless(w, ModItems.INJECTOR_KNIFE, 1, ModItems.INJECTOR_5HTP, Items.IRON_SWORD);
        // shackles braucht den Block "chain" - folgt mit den Deko-Bloecken.
        shaped(w, ModItems.BLACK_DIAMOND, 1, p("NIN", "IGI", "NIN"), 'N', m(ModMaterials.AU198, MaterialShape.NUGGET), 'I', ModItems.INK, 'G', ModItems.GEM_VOLCANIC);
        shaped(w, ModItems.PROTECTION_CHARM, 1, p(" M ", "MDM", " M "), 'M', ModItems.FRAGMENT_METEORITE, 'D', Items.DIAMOND);
        shaped(w, ModItems.METEOR_CHARM, 1, p(" M ", "MDM", " M "), 'M', ModItems.FRAGMENT_METEORITE, 'D', ModItems.GEM_VOLCANIC);
        shaped(w, ModItems.NEUTRINO_LENS, 1, p("PSP", "SCS", "PSP"), 'P', anyPlastic, 'S', m(ModMaterials.STARMETAL, MaterialShape.INGOT), 'C', ModItems.BISMOID_CIRCUIT);
        shaped(w, ModItems.GAS_TESTER, 1, p("G", "C", "I"), 'G', m(ModMaterials.GOLD, MaterialShape.PLATE), 'C', ModItems.VACUUM_TUBE, 'I', ironPlate);
        shaped(w, ModItems.DEFUSER_GOLD, 1, p("GPG", "PRP", "GPG"), 'G', Items.GUNPOWDER, 'P', m(ModMaterials.GOLD, MaterialShape.PLATE), 'R',
                Ingredient.of(net.minecraft.tags.ItemTags.MUSIC_DISCS));
        shaped(w, ModItems.BALLISTIC_GAUNTLET, 1, p(" WS", "WRS", " RS"), 'W', m(ModMaterials.COPPER, MaterialShape.WIRE), 'R', ModItems.RING_STARMETAL, 'S', steelPlate);
        shaped(w, ModItems.NIGHT_VISION, 1, p("P P", "GCG"), 'P', anyPlastic, 'G', Ingredient.of(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("forge", "glass"))), 'C', ModItems.INTEGRATED_CIRCUIT);

        // Stealth boy
        shaped(w, ModItems.STEALTH_BOY, 1, p(" B", "LI", "LC"), 'B', Items.STONE_BUTTON, 'L', Items.LEATHER, 'I', m(ModMaterials.STEEL, MaterialShape.INGOT), 'C', ModItems.INTEGRATED_CIRCUIT);

        // MineralRecipes: Behelfs-Munitionskiste (ammo_container Meta 1) aus 4 Nitra
        shaped(w, ModItems.AMMO_CONTAINER_1, 1, p("##", "##"), '#', ModItems.NITRA);
    }
}
