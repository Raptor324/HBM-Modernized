package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.item.fekal_electric.ModBatteryItem;
import com.hbm_m.lib.RefStrings;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import static com.hbm_m.datagen.recipes.custom.CraftingGen.p;
import static com.hbm_m.datagen.recipes.custom.OreDictIngredients.*;

/**
 * Von Hand uebersetzte Original-Rezepte, die die Generatoren ({@code rc/craftgen.py}, {@code rc/machines.py}) nicht
 * auswerten koennen: Ausgaben mit NBT ({@code ItemBattery.getFullBattery}), Zutaten wie
 * {@code EnumBatteryPack.BATTERY_LEAD.stack()} und Schleifen ueber Enums (Wachsmalstifte, Chemiefarben).
 */
public final class RestRecipeGenerator {

    private RestRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> w) {
        crafting(w);
        crystallizer(w);
        purex(w);
        fluidPacks(w);
        checkMissing("RestRecipeGenerator");
    }

    // ---------------------------------------------------------------- Montagemaschine: Fluessigkeitspakete

    private static void fluidPacks(Consumer<FinishedRecipe> w) {
        // Fluessigkeits-Traits (FT_NoContainer) werden sonst erst im Common-Setup geladen, das der Datagen nicht durchlaeuft
        if (!com.hbm_m.inventory.fluid.FluidType.forFluid(ModFluids.SPENTSTEAM.getSource()).hasNoContainer()) {
            com.hbm_m.api.fluids.bootstrap.ModFluidTraitsBootstrap.registerAll();
        }
        Item empty = item("hbm_m:fluid_pack_empty");
        Item full = item("hbm_m:fluid_pack_full");
        // AssemblyMachineRecipes: for(type : Fluids.getInNiceOrder() ab 1) ohne hasNoContainer -> ass.package / ass.unpackage
        for (ModFluids.FluidEntry e : com.hbm_m.api.fluids.HbmFluidRegistry.getOrderedFluids()) {
            net.minecraft.world.level.material.Fluid f = e.getSource();
            if (f == ModFluids.NONE.getSource()) continue;
            if (com.hbm_m.inventory.fluid.FluidType.forFluid(f).hasNoContainer()) continue;
            String n = BuiltInRegistries.FLUID.getKey(f).getPath();
            AssemblerRecipeBuilder.assemblerRecipe(com.hbm_m.item.liquids.ItemFluidTank.make(full, f, 1), 40, 100)
                    .addIngredient(empty, 1)
                    .addFluidInput(f, 32_000)
                    .save(w, "assembler/package_" + n);
            AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(empty), 40, 100)
                    .addIngredient(filled("hbm_m:fluid_pack_full", f), 1)
                    .addFluidOutput(f, 32_000)
                    .save(w, "assembler/unpackage_" + n);
        }
    }

    // ---------------------------------------------------------------- PUREX

    private static void purex(Consumer<FinishedRecipe> w) {
        // PUREXRecipes: purex.uzh / purex.flashgold / purex.flashlead (setup 600, 1_000)
        PurexRecipeBuilder.purexRecipe(600, 1_000)
                .addItemInput(Ingredient.of(item("hbm_m:billet_uranium_fuel")), 1)
                .addItemInput(ore("oredict/billet/zirconium"), 3)
                .addFluidInput(ModFluids.NITRIC_ACID.getSource(), 1_000)
                .addFluidInput(ModFluids.HYDROGEN.getSource(), 4_000)
                .addItemOutput(stack("hbm_m:billet_uzh", 4))
                .save(w, "purex/uzh");
        PurexRecipeBuilder.purexRecipe(600, 1_000)
                .addItemInput(ore("oredict/billet/gold198"), 1)
                .addItemInput(Ingredient.of(item("hbm_m:pellet_charged")), 1)
                .addFluidInput(ModFluids.AMAT.getSource(), 1_000)
                .addItemOutput(stack("hbm_m:billet_balefire_gold", 2))
                .save(w, "purex/flashgold");
        PurexRecipeBuilder.purexRecipe(600, 1_000)
                .addItemInput(ore("oredict/billet/lead209"), 1)
                .addItemInput(Ingredient.of(item("hbm_m:billet_balefire_gold")), 1)
                .addFluidInput(ModFluids.AMAT.getSource(), 1_000)
                .addItemOutput(stack("hbm_m:billet_flashlead", 1))
                .save(w, "purex/flashlead");
    }

    // ---------------------------------------------------------------- Werkbank

    /** Schreibt geformte Rezepte als {@code hbm_m:container_upgrade} (Original ContainerUpgradeCraftingHandler). */
    private static Consumer<FinishedRecipe> containerUpgrade(Consumer<FinishedRecipe> w) {
        return r -> w.accept(new FinishedRecipe() {
            @Override public void serializeRecipeData(com.google.gson.JsonObject json) { r.serializeRecipeData(json); }
            @Override public net.minecraft.resources.ResourceLocation getId() { return r.getId(); }
            @Override public net.minecraft.world.item.crafting.RecipeSerializer<?> getType() { return com.hbm_m.recipe.ModRecipes.CONTAINER_UPGRADE.get(); }
            @Override public com.google.gson.JsonObject serializeAdvancement() { return r.serializeAdvancement(); }
            @Override public net.minecraft.resources.ResourceLocation getAdvancementId() { return r.getAdvancementId(); }
        });
    }

    private static void crafting(Consumer<FinishedRecipe> w) {
        CraftingGen g = new CraftingGen(w, "rest");

        // CraftingManager: ItemBattery.getFullBattery(battery_potato) / (battery_potatos) - voll geladen
        shapelessFull(w, "rest/battery_potato", item("hbm_m:battery_potato"),
                Ingredient.of(item("minecraft:potato")), ore("oredict/wire_fine/aluminum"), ore("oredict/wire_fine/copper"));
        shapelessFull(w, "rest/battery_potatos", item("hbm_m:battery_potatos"),
                Ingredient.of(item("hbm_m:battery_potato")), Ingredient.of(item("hbm_m:turret_chip")), ore("oredict/dust/redstone"));
        // CraftingManager: ItemBattery.getEmptyBattery(anchor_remote)
        g.shapeless(item("hbm_m:anchor_remote"), 1, ore("oredict/gem/diamond"), Ingredient.of(item("hbm_m:ducttape")),
                Ingredient.of(item("hbm_m:integrated_circuit")));

        // ToolRecipes: Elektrowerkzeuge mit EnumBatteryPack.BATTERY_LEAD.stack()
        Ingredient lead = Ingredient.of(item("hbm_m:battery_pack_battery_lead"));
        Ingredient plastic = ore("oredict/ingot/any_plastic");
        Ingredient dura = ore("oredict/ingot/dura_steel");
        Ingredient bolt = ore("oredict/bolt/dura_steel");
        Ingredient motor = Ingredient.of(item("hbm_m:motor"));
        g.shaped(item("hbm_m:elec_sword"), 1, p("RPR", "RPR", " B "), 'P', plastic, 'R', bolt, 'B', lead);
        g.shaped(item("hbm_m:elec_pickaxe"), 1, p("RDM", " PB", " P "), 'P', plastic, 'D', dura, 'R', bolt, 'M', motor, 'B', lead);
        g.shaped(item("hbm_m:elec_axe"), 1, p(" DP", "RRM", " PB"), 'P', plastic, 'D', dura, 'R', bolt, 'M', motor, 'B', lead);
        g.shaped(item("hbm_m:elec_shovel"), 1, p("  P", "RRM", "  B"), 'P', plastic, 'R', bolt, 'M', motor, 'B', lead);
        // ToolRecipes: ItemToolAbilityFueled.getEmptyTool(chainsaw) - leerer Tank
        g.shaped(item("hbm_m:chainsaw"), 1, p("CCH", "BBP", "CCE"), 'H', ore("oredict/shell/steel"),
                'B', Ingredient.of(item("hbm_m:blades_steel")), 'P', Ingredient.of(item("hbm_m:piston_selenium")),
                'C', Ingredient.of(item("hbm_m:dungeon_chain")), 'E', Ingredient.of(item("hbm_m:canister_empty")));
        g.shaped(item("hbm_m:power_net_tool"), 1, p("WRW", " I ", " B "), 'W', ore("oredict/wire_fine/mingrade"),
                'R', ore("oredict/dust/redstone"), 'I', ore("oredict/ingot/iron"), 'B', lead);
        // ToolRecipes: ItemBlowtorch.getEmptyTool(...) - leerer Tank
        g.shaped(item("hbm_m:blowtorch"), 1, p("CC ", " I ", "CCC"), 'C', ore("oredict/plate/copper"), 'I', ore("oredict/ingot/iron"));
        g.shaped(item("hbm_m:acetylene_torch"), 1, p("SS ", " PS", " T "), 'S', ore("oredict/plate/steel"), 'P', plastic,
                'T', Ingredient.of(item("hbm_m:tank_steel")));

        // CraftingManager: ContainerUpgradeCraftingHandler(safe) "LAL","ACA","LAL" - Inhalt der Stahlkiste geht mit
        new CraftingGen(containerUpgrade(w), "rest").shaped(item("hbm_m:safe"), 1, p("LAL", "ACA", "LAL"), 'L', ore("oredict/plate/lead"), 'A', ore("oredict/plate/titanium"),
                'C', Ingredient.of(item("hbm_m:crate_steel")));
        // CraftingManager: die uebrigen ContainerUpgradeCraftingHandler-Rezepte (Desh-/Wolframkiste, Massenspeicher Meta 1/2)
        CraftingGen up = new CraftingGen(containerUpgrade(w), "rest");
        up.shaped(item("hbm_m:crate_desh"), 1, p(" D ", "DSD", " D "), 'D', Ingredient.of(item("hbm_m:plate_desh")),
                'S', Ingredient.of(item("hbm_m:crate_steel")));
        up.shaped(item("hbm_m:crate_tungsten"), 1, p("BPB", "PCP", "BPB"), 'B', ore("oredict/block/tungsten"),
                'P', ore("oredict/plate_triple/copper"), 'C', Ingredient.of(item("hbm_m:crate_steel")));
        up.shaped(item("hbm_m:mass_storage_desh"), 1, p(" C ", "PMP", " P "), 'P', ore("oredict/ingot/desh"),
                'C', Ingredient.of(item("hbm_m:microchip")), 'M', Ingredient.of(item("hbm_m:mass_storage_iron")));
        up.shaped(item("hbm_m:mass_storage"), 1, p(" C ", "PMP", " P "), 'P', ore("oredict/ingot/any_resistant_alloy"),
                'C', Ingredient.of(item("hbm_m:advanced_circuit")), 'M', Ingredient.of(item("hbm_m:mass_storage_desh")));

        // PowderRecipes: for(i < 15) crayon(4, i) <- chemical_dye(i) + ANY_TAR + Papier (EnumChemDye ohne WHITE)
        String[] dyes = { "black", "red", "green", "brown", "blue", "purple", "cyan", "silver", "gray", "pink", "lime",
                "yellow", "lightblue", "magenta", "orange" };
        for (String d : dyes) {
            g.shapeless(item("hbm_m:crayon_" + d), 4, Ingredient.of(item("hbm_m:chemical_dye_" + d)), ore("oredict/any/tar"),
                    Ingredient.of(item("minecraft:paper")));
        }

        // CraftingManager: for(i < 16) concrete_colored(8, i) <- 8 Glattbeton + "dye" + dyes[15 - i] (Wollfarben-Reihenfolge)
        String[][] concrete = { { "white", "white" }, { "orange", "orange" }, { "magenta", "magenta" }, { "light_blue", "light_blue" },
                { "yellow", "yellow" }, { "lime", "lime" }, { "pink", "pink" }, { "gray", "gray" }, { "silver", "light_gray" },
                { "cyan", "cyan" }, { "purple", "purple" }, { "blue", "blue" }, { "brown", "brown" }, { "green", "green" },
                { "red", "red" }, { "black", "black" } };
        for (String[] c : concrete) {
            g.shaped(item("hbm_m:concrete_" + c[0]), 8, p("CCC", "CDC", "CCC"), 'C', Ingredient.of(item("hbm_m:concrete_smooth")),
                    'D', ore("oredict/dye_" + c[1]));
        }

        // CraftingManager: for(i < 14) platemetal(8, i + 1) <- 8 Grundplatte + plateDyes[i] (PlatemetalType ab BLACK)
        String[] plate = { "black", "white", "red", "green", "light_gray", "blue", "purple", "cyan", "pink", "lime", "yellow",
                "light_blue", "magenta", "orange" };
        for (String c : plate) {
            g.shaped(item("hbm_m:platemetal_" + c), 8, p("PPP", "PDP", "PPP"), 'P', Ingredient.of(item("hbm_m:platemetal_base")),
                    'D', ore("oredict/dye_" + c));
        }

        // MineralRecipes: for(EnumCokeType) add1To9PairSameMeta(block_coke, coke, i)
        String[][] coke = { { "block_coke_coal", "coal_coke" }, { "block_coke_lignite", "lignite_coke" },
                { "block_coke_petroleum", "coke_petroleum" } };
        for (String[] c : coke) {
            g.shapeless(item("hbm_m:" + c[1]), 9, Ingredient.of(item("hbm_m:" + c[0])));
            g.shaped(item("hbm_m:" + c[0]), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:" + c[1])));
        }

        // PowderRecipes "Metal powders": ItemScraps.create(MaterialStack) - Schrott mit Materialmenge (NBT amount)
        shapelessStack(w, "rest/scraps_mingrade", scraps(Mats.MAT_MINGRADE, 2), ore("oredict/dust/copper"), ore("oredict/dust/redstone"));
        shapelessStack(w, "rest/scraps_magtung", scraps(Mats.MAT_MAGTUNG, 1), ore("oredict/dust/tungsten"), ore("oredict/nugget/schrabidium"));
        shapelessStack(w, "rest/scraps_tcalloy", scraps(Mats.MAT_TCALLOY, 1), ore("oredict/dust/steel"), ore("oredict/nugget/technetium99"));
        shapelessStack(w, "rest/scraps_steel_1", scraps(Mats.MAT_STEEL, 1), ore("oredict/dust/iron"), ore("oredict/dust/coal"));
        shapelessStack(w, "rest/scraps_steel_4", scraps(Mats.MAT_STEEL, 4), ore("oredict/dust/iron"), ore("oredict/dust/iron"),
                ore("oredict/dust/iron"), ore("oredict/dust/iron"), ore("oredict/dust/coal"), ore("oredict/dust/coal"),
                ore("oredict/dust/coal"), ore("oredict/dust/coal"));

        // CraftingManager: for(Mats mit BOLT) bolt(16) <- "#","#" Barren (Stahl, Wolfram, Blei, Desh-Stahl/DURA)
        g.shaped(item("hbm_m:bolt_tungsten"), 16, p("#", "#"), '#', ore("oredict/ingot/tungsten"));
        g.shaped(item("hbm_m:bolt_lead"), 16, p("#", "#"), '#', ore("oredict/ingot/lead"));
        g.shaped(item("hbm_m:bolt_steel"), 16, p("#", "#"), '#', ore("oredict/ingot/steel"));
    }

    /** Formloses Rezept, dessen Ergebnis eine voll geladene Batterie ist (Forge liest "nbt" im Ergebnis). */
    private static void shapelessFull(Consumer<FinishedRecipe> w, String path, Item out, Ingredient... inputs) {
        ItemStack stack = new ItemStack(out);
        ModBatteryItem.setEnergy(stack, Long.MAX_VALUE);
        shapelessStack(w, path, stack, inputs);
    }

    /** Formloses Rezept mit beliebigem Ergebnisstapel inkl. NBT (z.B. ItemScraps mit Materialmenge). */
    private static void shapelessStack(Consumer<FinishedRecipe> w, String path, ItemStack stack, Ingredient... inputs) {
        Item out = stack.getItem();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path);
        w.accept(new FinishedRecipe() {
            @Override
            public void serializeRecipeData(JsonObject json) {
                json.addProperty("category", "misc");
                JsonArray ings = new JsonArray();
                for (Ingredient i : inputs) ings.add(i.toJson());
                json.add("ingredients", ings);
                JsonObject result = new JsonObject();
                result.addProperty("item", BuiltInRegistries.ITEM.getKey(out).toString());
                if (stack.getCount() > 1) result.addProperty("count", stack.getCount());
                if (stack.getTag() != null) result.addProperty("nbt", stack.getTag().toString());
                json.add("result", result);
            }

            @Override
            public ResourceLocation getId() { return id; }

            @Override
            public RecipeSerializer<?> getType() { return RecipeSerializer.SHAPELESS_RECIPE; }

            @Override
            public JsonObject serializeAdvancement() { return null; }

            @Override
            public ResourceLocation getAdvancementId() { return null; }
        });
    }

    private static ItemStack scraps(com.hbm_m.inventory.material.NTMMaterial mat, int ingots) {
        return com.hbm_m.item.material.ItemScraps.create(new Mats.MaterialStack(mat, MaterialShapes.INGOT.q(ingots)));
    }

    // ---------------------------------------------------------------- Kristallisierer

    private static void crystallizer(Consumer<FinishedRecipe> w) {
        // CrystallizerRecipes: for(dye : WOODOIL/FISHOIL/LIGHTOIL 100) Staub -> 4 Chemiefarbe, mixingTime 20, prod 0.15
        // CrystallizerRecipes: MALACHITE.ingot() + 250 Schwefelsaeure -> Kupferschrott (1 Barren), 300 Ticks, prod 0.1
        CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/ingot/malachite"), 1,
                        FluidStack.create(ModFluids.SULFURIC_ACID.getSource(), 250L), scraps(Mats.MAT_COPPER, 1), 300, 0.1F)
                .save(w, "rest/crystallizer/malachite");

        ModFluids.FluidEntry[] oils = { ModFluids.WOODOIL, ModFluids.FISHOIL, ModFluids.LIGHTOIL };
        String[][] dyes = { { "coal", "black" }, { "titanium", "white" }, { "iron", "red" }, { "tungsten", "yellow" },
                { "copper", "green" }, { "cobalt", "blue" } };
        for (ModFluids.FluidEntry oil : oils) {
            String oilName = BuiltInRegistries.FLUID.getKey(oil.getSource()).getPath();
            for (String[] d : dyes) {
                CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/dust/" + d[0]), 1,
                                FluidStack.create(oil.getSource(), 100L), stack("hbm_m:chemical_dye_" + d[1], 4), 20, 0.15F)
                        .save(w, "rest/crystallizer/chemical_dye_" + d[1] + "_" + oilName);
            }
        }
    }
}
//?}
