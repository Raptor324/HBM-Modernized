//? if forge {
package com.hbm_m.datagen.recipes.custom;

import java.util.function.Consumer;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.PartTabMetaItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterials;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Die Rezepte des alten Grundgesteinserzes ({@code ItemBedrockOre}, {@code ItemByproduct}) 1:1:
 * <pre>
 * [BEDROCK x1] -C-> [CENTRIFUGED x4] -(PER)-> [CLEANED x4] -C-> [SEPARATED x16] -(SUL)-> [PURIFIED x16] -C-> [ENRICHED x64]
 *                                                                    \-(NIT)-> [NITRATED x16] -C-> [NITROCRYSTALLINE x32] -(ORG)-> [DEEP-CLEANED x32] -C-> [ENRICHED x64]
 *                                                                                                                \-(HPS)-> [SEARED x32] -C-> [ENRICHED x64]
 * </pre>
 * Zentrifuge (CentrifugeRecipes), Kristallisator mit Saeuren (CrystallizerRecipes, oreTime 200), Schredder
 * (alles -> angereichert), Ofen (Grundgesteinserz -> 16 Bruchstein) und die Nebenprodukt-9er-Rezepte
 * (MineralRecipes).
 */
public final class LegacyBedrockOreRecipeGenerator {

    private LegacyBedrockOreRecipeGenerator() {}

    /** Erz, Nebenprodukt Stufe 1, 2, 3 (EnumBedrockOre). */
    private static final String[][] ORES = {
            {"iron", "b_sulfur", "b_titanium", "b_titanium"},
            {"copper", "b_sulfur", "b_sulfur", "b_sulfur"},
            {"borax", "b_lithium", "b_calcium", "b_calcium"},
            {"asbestos", "b_silicon", "b_silicon", "b_silicon"},
            {"niobium", "b_iron", "b_iron", "b_iron"},
            {"titanium", "b_silicon", "b_calcium", "b_aluminium"},
            {"tungsten", "b_lead", "b_iron", "b_bismuth"},
            {"gold", "b_lead", "b_copper", "b_bismuth"},
            {"uranium", "b_lead", "b_radium", "b_polonium"},
            {"thorium", "b_silicon", "b_uranium", "b_technetium"},
            {"chlorocalcite", "b_lithium", "b_silicon", "b_silicon"},
            {"fluorite", "b_silicon", "b_lithium", "b_aluminium"},
            {"hematite", "b_sulfur", "b_titanium", "b_titanium"},
            {"malachite", "b_sulfur", "b_sulfur", "b_sulfur"},
            {"neodymium", "b_lithium", "b_silicon", "b_bismuth"},
    };

    private static Item it(String stage, String ore) {
        return PartTabMetaItems.get(stage + "_" + ore).get();
    }

    private static ItemStack by(String b) {
        // tier == null -> ModItems.dust (kommt in EnumBedrockOre derzeit nicht vor)
        return b == null ? new ItemStack(ModItems.DUST.get()) : new ItemStack(PartTabMetaItems.get("ore_byproduct_" + b).get());
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int mb) {
        return FluidStack.create(entry.getSource(), (long) mb);
    }

    public static void generate(Consumer<FinishedRecipe> w) {
        FluidStack peroxide = fluid(ModFluids.PEROXIDE, 500);
        FluidStack sulfur = fluid(ModFluids.SULFURIC_ACID, 500);
        FluidStack nitric = fluid(ModFluids.NITRIC_ACID, 500);
        FluidStack organic = fluid(ModFluids.SOLVENT, 500);
        FluidStack hiperf = fluid(ModFluids.RADIOSOLVENT, 500);
        int oreTime = 200;

        for (String[] o : ORES) {
            String ore = o[0];
            String p = "legacy_bedrock/" + ore + "/";

            // Zentrifuge
            CentrifugeRecipeBuilder.itemRecipe(it("ore_bedrock", ore), n(it("ore_centrifuged", ore), 4)).save(w, p + "centrifuge_bedrock");
            CentrifugeRecipeBuilder.itemRecipe(it("ore_cleaned", ore), n(it("ore_separated", ore), 4)).save(w, p + "centrifuge_cleaned");
            CentrifugeRecipeBuilder.itemRecipe(it("ore_purified", ore), n(it("ore_enriched", ore), 4)).save(w, p + "centrifuge_purified");
            CentrifugeRecipeBuilder.itemRecipe(it("ore_nitrated", ore),
                    new ItemStack(it("ore_nitrocrystalline", ore)), new ItemStack(it("ore_nitrocrystalline", ore)), by(o[1]), by(o[1])).save(w, p + "centrifuge_nitrated");
            CentrifugeRecipeBuilder.itemRecipe(it("ore_deepcleaned", ore),
                    new ItemStack(it("ore_enriched", ore)), new ItemStack(it("ore_enriched", ore)), by(o[2]), by(o[2])).save(w, p + "centrifuge_deepcleaned");
            CentrifugeRecipeBuilder.itemRecipe(it("ore_seared", ore),
                    new ItemStack(it("ore_enriched", ore)), new ItemStack(it("ore_enriched", ore)), by(o[3]), by(o[3])).save(w, p + "centrifuge_seared");

            // Kristallisator (Standard-Saeure = Peroxid)
            CrystallizerRecipeBuilder.crystallizerRecipe(it("ore_centrifuged", ore), 1, peroxide, new ItemStack(it("ore_cleaned", ore)), oreTime, 0F).save(w, p + "crystallizer_cleaned");
            CrystallizerRecipeBuilder.crystallizerRecipe(it("ore_separated", ore), 1, sulfur, new ItemStack(it("ore_purified", ore)), oreTime, 0F).save(w, p + "crystallizer_purified");
            CrystallizerRecipeBuilder.crystallizerRecipe(it("ore_separated", ore), 1, nitric, new ItemStack(it("ore_nitrated", ore)), oreTime, 0F).save(w, p + "crystallizer_nitrated");
            CrystallizerRecipeBuilder.crystallizerRecipe(it("ore_nitrocrystalline", ore), 1, organic, new ItemStack(it("ore_deepcleaned", ore)), oreTime, 0F).save(w, p + "crystallizer_deepcleaned");
            CrystallizerRecipeBuilder.crystallizerRecipe(it("ore_nitrocrystalline", ore), 1, hiperf, new ItemStack(it("ore_seared", ore)), oreTime, 0F).save(w, p + "crystallizer_seared");

            // Schredder: jede Stufe -> angereichert
            for (String stage : new String[] {"ore_bedrock", "ore_centrifuged", "ore_cleaned", "ore_separated", "ore_purified",
                    "ore_nitrated", "ore_nitrocrystalline", "ore_deepcleaned", "ore_seared"}) {
                ShredderRecipeBuilder.shredderRecipe(it(stage, ore), new ItemStack(it("ore_enriched", ore))).save(w, p + "shredder_" + stage);
            }

            // Ofen: Grundgesteinserz -> 16 Bruchstein (0,1 XP); Forge liest "result" als Objekt mit count
            smelting16(w, it("ore_bedrock", ore), ResourceLocation.fromNamespaceAndPath("hbm_m", p + "smelting_cobblestone"));
        }

        // MineralRecipes.add9To1: 9 Nebenprodukt-Fragmente -> Material
        CraftingGen g = new CraftingGen(w, "legacy_bedrock");
        nine(g, "b_iron", CraftingGen.m(ModMaterials.IRON, MaterialShape.POWDER), 1);
        nine(g, "b_copper", CraftingGen.m(ModMaterials.COPPER, MaterialShape.POWDER), 1);
        nine(g, "b_lithium", CraftingGen.m(ModMaterials.LITHIUM, MaterialShape.POWDER), 1);
        nine(g, "b_silicon", CraftingGen.m(ModMaterials.SILICON, MaterialShape.NUGGET), 3);
        nine(g, "b_lead", CraftingGen.m(ModMaterials.LEAD, MaterialShape.POWDER), 1);
        nine(g, "b_titanium", CraftingGen.m(ModMaterials.TITANIUM, MaterialShape.POWDER), 1);
        nine(g, "b_aluminium", CraftingGen.m(ModMaterials.ALUMINUM, MaterialShape.POWDER), 1);
        nine(g, "b_sulfur", ModItems.SULFUR.get(), 1);
        nine(g, "b_calcium", CraftingGen.m(ModMaterials.CALCIUM, MaterialShape.POWDER), 1);
        nine(g, "b_bismuth", CraftingGen.m(ModMaterials.BISMUTH, MaterialShape.POWDER), 1);
        nine(g, "b_radium", CraftingGen.m(ModMaterials.RA226, MaterialShape.POWDER), 1);
        nine(g, "b_technetium", CraftingGen.m(ModMaterials.TECHNETIUM, MaterialShape.BILLET), 1);
        nine(g, "b_polonium", CraftingGen.m(ModMaterials.POLONIUM, MaterialShape.BILLET), 1);
        nine(g, "b_uranium", CraftingGen.m(ModMaterials.URANIUM, MaterialShape.POWDER), 1);
    }

    private static void smelting16(Consumer<FinishedRecipe> w, Item input, ResourceLocation id) {
        w.accept(new FinishedRecipe() {
            @Override
            public void serializeRecipeData(com.google.gson.JsonObject json) {
                json.addProperty("category", "misc");
                json.add("ingredient", Ingredient.of(input).toJson());
                com.google.gson.JsonObject result = new com.google.gson.JsonObject();
                result.addProperty("item", "minecraft:cobblestone");
                result.addProperty("count", 16);
                json.add("result", result);
                json.addProperty("experience", 0.1F);
                json.addProperty("cookingtime", 200);
            }
            @Override public ResourceLocation getId() { return id; }
            @Override public net.minecraft.world.item.crafting.RecipeSerializer<?> getType() { return net.minecraft.world.item.crafting.RecipeSerializer.SMELTING_RECIPE; }
            @Override public com.google.gson.JsonObject serializeAdvancement() { return null; }
            @Override public ResourceLocation getAdvancementId() { return null; }
        });
    }

    private static ItemStack[] n(Item item, int count) {
        ItemStack[] out = new ItemStack[count];
        for (int i = 0; i < count; i++) out[i] = new ItemStack(item);
        return out;
    }

    private static void nine(CraftingGen g, String b, Item out, int count) {
        g.shaped(out, count, CraftingGen.p("###", "###", "###"), '#', PartTabMetaItems.get("ore_byproduct_" + b));
    }
}
//?}
