package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.ItemFluidTank;

import com.google.gson.JsonObject;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import static com.hbm_m.datagen.recipes.custom.CraftingGen.p;
import static com.hbm_m.datagen.recipes.custom.OreDictIngredients.*;

/**
 * Konfig-Rezepte des Originals, die nicht ueber die Rezeptlisten laufen: die LBSM-Hilfsaufrufe
 * ({@code ArmorRecipes.addHelmet..}, {@code ToolRecipes.addSword..}), die LBSM-Feindraht-Schleife in
 * {@code CraftingManager} und das LBSM-Rezept des Legierungsofens. Alle nur mit den jeweiligen Schaltern
 * ({@link ConfigRecipes}). AUTOMATISCH ERZEUGT durch {@code rc/config_extra.py}.
 */
public final class ConfigVariantRecipeGenerator {

    private ConfigVariantRecipeGenerator() {}

    /** Schreibt die NBT des Ergebnisses mit (Kanister mit Fluessigkeitssorte); BaseRecipeBuilder laesst sie weg. */
    private static Consumer<FinishedRecipe> outputNbt(Consumer<FinishedRecipe> w, ItemStack out) {
        return r -> w.accept(new FinishedRecipe() {
            @Override public void serializeRecipeData(JsonObject json) {
                r.serializeRecipeData(json);
                if (out.getTag() != null) json.getAsJsonObject("output").addProperty("nbt", out.getTag().toString());
            }
            @Override public ResourceLocation getId() { return r.getId(); }
            @Override public RecipeSerializer<?> getType() { return r.getType(); }
            @Override public JsonObject serializeAdvancement() { return r.serializeAdvancement(); }
            @Override public ResourceLocation getAdvancementId() { return r.getAdvancementId(); }
        });
    }

    public static void generate(Consumer<FinishedRecipe> writer) {
        CraftingGen g = new CraftingGen(writer, "config");
        // ArmorRecipes: LBSM (enableLBSMSimpleArmorRecipes) - einfache Ruestung aus Barren
        g.when("lbsm_armor").shaped(item("hbm_m:starmetal_helmet"), 1, p("XXX", "X X"), 'X', ore("oredict/ingot/starmetal"));
        g.when("lbsm_armor").shaped(item("hbm_m:starmetal_plate"), 1, p("X X", "XXX", "XXX"), 'X', ore("oredict/ingot/starmetal"));
        g.when("lbsm_armor").shaped(item("hbm_m:starmetal_legs"), 1, p("XXX", "X X", "X X"), 'X', ore("oredict/ingot/starmetal"));
        g.when("lbsm_armor").shaped(item("hbm_m:starmetal_boots"), 1, p("X X", "X X"), 'X', ore("oredict/ingot/starmetal"));
        g.when("lbsm_armor").shaped(item("hbm_m:schrabidium_helmet"), 1, p("XXX", "X X"), 'X', ore("oredict/ingot/schrabidium"));
        g.when("lbsm_armor").shaped(item("hbm_m:schrabidium_plate"), 1, p("X X", "XXX", "XXX"), 'X', ore("oredict/ingot/schrabidium"));
        g.when("lbsm_armor").shaped(item("hbm_m:schrabidium_legs"), 1, p("XXX", "X X", "X X"), 'X', ore("oredict/ingot/schrabidium"));
        g.when("lbsm_armor").shaped(item("hbm_m:schrabidium_boots"), 1, p("X X", "X X"), 'X', ore("oredict/ingot/schrabidium"));
        // ToolRecipes: LBSM (enableLBSMSimpleToolRecipes) - einfache Werkzeuge
        g.when("lbsm_tool").shaped(item("hbm_m:cobalt_decorated_sword"), 1, p("X", "X", "#"), 'X', ore("oredict/block/cobalt"), '#', ore("oredict/stick_wood"));
        g.when("lbsm_tool").shaped(item("hbm_m:cobalt_decorated_pickaxe"), 1, p("XXX", " # ", " # "), 'X', ore("oredict/block/cobalt"), '#', ore("oredict/stick_wood"));
        g.when("lbsm_tool").shaped(item("hbm_m:cobalt_decorated_axe"), 1, p("XX", "X#", " #"), 'X', ore("oredict/block/cobalt"), '#', ore("oredict/stick_wood"));
        g.when("lbsm_tool").shaped(item("hbm_m:cobalt_decorated_shovel"), 1, p("X", "#", "#"), 'X', ore("oredict/block/cobalt"), '#', ore("oredict/stick_wood"));
        g.when("lbsm_tool").shaped(item("hbm_m:cobalt_decorated_hoe"), 1, p("XX", " #", " #"), 'X', ore("oredict/block/cobalt"), '#', ore("oredict/stick_wood"));
        g.when("lbsm_tool").shaped(item("hbm_m:starmetal_sword"), 1, p("X", "X", "#"), 'X', ore("oredict/ingot/starmetal"), '#', ore("oredict/stick_wood"));
        g.when("lbsm_tool").shaped(item("hbm_m:starmetal_pickaxe"), 1, p("XXX", " # ", " # "), 'X', ore("oredict/ingot/starmetal"), '#', ore("oredict/stick_wood"));
        g.when("lbsm_tool").shaped(item("hbm_m:starmetal_axe"), 1, p("XX", "X#", " #"), 'X', ore("oredict/ingot/starmetal"), '#', ore("oredict/stick_wood"));
        g.when("lbsm_tool").shaped(item("hbm_m:starmetal_shovel"), 1, p("X", "#", "#"), 'X', ore("oredict/ingot/starmetal"), '#', ore("oredict/stick_wood"));
        g.when("lbsm_tool").shaped(item("hbm_m:starmetal_hoe"), 1, p("XX", " #", " #"), 'X', ore("oredict/ingot/starmetal"), '#', ore("oredict/stick_wood"));
        g.when("lbsm_tool").shaped(item("hbm_m:schrabidium_sword"), 1, p("X", "X", "#"), 'X', ore("oredict/ingot/schrabidium"), '#', ore("oredict/stick_wood"));
        g.when("lbsm_tool").shaped(item("hbm_m:schrabidium_pickaxe"), 1, p("XXX", " # ", " # "), 'X', ore("oredict/ingot/schrabidium"), '#', ore("oredict/stick_wood"));
        g.when("lbsm_tool").shaped(item("hbm_m:schrabidium_axe"), 1, p("XX", "X#", " #"), 'X', ore("oredict/ingot/schrabidium"), '#', ore("oredict/stick_wood"));
        g.when("lbsm_tool").shaped(item("hbm_m:schrabidium_shovel"), 1, p("X", "#", "#"), 'X', ore("oredict/ingot/schrabidium"), '#', ore("oredict/stick_wood"));
        g.when("lbsm_tool").shaped(item("hbm_m:schrabidium_hoe"), 1, p("XX", " #", " #"), 'X', ore("oredict/ingot/schrabidium"), '#', ore("oredict/stick_wood"));
        // CraftingManager: LBSM (enableLBSMSimpleCrafting) - Feindraht x24 aus drei Barren, je Material/Name
        g.when("lbsm_crafting").shaped(item("hbm_m:wire_carbon"), 24, p("###"), '#', ore("oredict/ingot/carbon"));
        g.when("lbsm_crafting").shaped(item("hbm_m:wire_gold"), 24, p("###"), '#', ore("oredict/ingot/gold"));
        g.when("lbsm_crafting").shaped(item("hbm_m:wire_schrabidium"), 24, p("###"), '#', ore("oredict/ingot/schrabidium"));
        g.when("lbsm_crafting").shaped(item("hbm_m:wire_copper"), 24, p("###"), '#', ore("oredict/ingot/copper"));
        g.when("lbsm_crafting").shaped(item("hbm_m:wire_tungsten"), 24, p("###"), '#', ore("oredict/ingot/tungsten"));
        g.when("lbsm_crafting").shaped(item("hbm_m:wire_aluminium"), 24, p("###"), '#', ore("oredict/ingot/aluminum"));
        g.when("lbsm_crafting").shaped(item("hbm_m:wire_lead"), 24, p("###"), '#', ore("oredict/ingot/lead"));
        g.when("lbsm_crafting").shaped(item("hbm_m:wire_zirconium"), 24, p("###"), '#', ore("oredict/ingot/zirconium"));
        g.when("lbsm_crafting").shaped(item("hbm_m:wire_steel"), 24, p("###"), '#', ore("oredict/ingot/steel"));
        g.when("lbsm_crafting").shaped(item("hbm_m:wire_red_copper"), 24, p("###"), '#', ore("oredict/ingot/mingrade"));
        g.when("lbsm_crafting").shaped(item("hbm_m:wire_magnetized_tungsten"), 24, p("###"), '#', ore("oredict/ingot/magnetized_tungsten"));
        // BlastFurnaceRecipes: LBSM (enableLBSMSimpleChemsitry) - Kanister + Kohle -> Rohoel-Kanister
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.CRUDE_OIL.getSource(), 1),
                        Ingredient.of(item("hbm_m:canister_empty")), net.minecraftforge.common.crafting.CompoundIngredient.of(ore("oredict/gem/coal"), ore("oredict/dust/coal")))
                .save(outputNbt(ConfigRecipes.when(writer, "lbsm_chemistry"), ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.CRUDE_OIL.getSource(), 1)),
                        "blast_furnace/lbsm_canister_oil");
        checkMissing("ConfigVariantRecipeGenerator");
    }
}
//?}
