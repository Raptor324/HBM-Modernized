//? if forge {
package com.hbm_m.datagen.recipes.custom;

import static com.hbm_m.datagen.recipes.custom.CraftingGen.m;
import static com.hbm_m.datagen.recipes.custom.CraftingGen.p;

import java.util.function.Consumer;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.tags_and_tiers.ModTags;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 1:1 {@code com.hbm.crafting.ToolRecipes} fuer die Faehigkeitswerkzeuge (LBSM aus) plus die Schwertkette des
 * Meteoritenschwerts ({@code PressRecipes}, {@code CrystallizerRecipes}, {@code BreederRecipes}) und die
 * Schwerter aus {@code CraftingManager}. Stahl/Titan/Brecheisen stehen in {@code ModVanillaRecipeProvider}.
 * <p>Offen, weil die Zutaten im Port fehlen: {@code chainsaw} (Block {@code chain}), {@code elec_*}
 * ({@code EnumBatteryPack.BATTERY_LEAD}). {@code mese_gavel} steht in {@code MagicRecipes}.</p>
 */
public final class ToolRecipeGenerator {

    private ToolRecipeGenerator() {}

    private static Ingredient tag(String ns, String path) {
        return Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ns, path)));
    }

    public static void generate(Consumer<FinishedRecipe> w) {
        CraftingGen g = new CraftingGen(w, "tools");
        Ingredient stick = tag("forge", "rods/wooden");
        Ingredient anyPlastic = Ingredient.of(m(ModMaterials.POLYMER, MaterialShape.INGOT), m(ModMaterials.BAKELITE, MaterialShape.INGOT));

        tools(g, stick, m(ModMaterials.COBALT, MaterialShape.INGOT), ModItems.COBALT_SWORD, ModItems.COBALT_PICKAXE, ModItems.COBALT_AXE, ModItems.COBALT_SHOVEL, ModItems.COBALT_HOE);
        tools(g, stick, m(ModMaterials.COMBINE_STEEL, MaterialShape.INGOT), ModItems.CMB_SWORD, ModItems.CMB_PICKAXE, ModItems.CMB_AXE, ModItems.CMB_SHOVEL, ModItems.CMB_HOE);
        tools(g, stick, m(ModMaterials.DESH, MaterialShape.INGOT), ModItems.DESH_SWORD, ModItems.DESH_PICKAXE, ModItems.DESH_AXE, ModItems.DESH_SHOVEL, ModItems.DESH_HOE);

        g.shaped(ModItems.SMASHING_HAMMER, 1, p("STS", "SPS", " P "), 'S', m(ModMaterials.STEEL, MaterialShape.BLOCK), 'T', m(ModMaterials.TUNGSTEN, MaterialShape.BLOCK), 'P', anyPlastic);
        g.shaped(ModItems.DWARVEN_PICKAXE, 1, p("CIC", " S ", " S "), 'C', Items.COPPER_INGOT, 'I', Items.IRON_INGOT, 'S', stick);

        //Super pickaxes
        var bi = m(ModMaterials.BISMUTH, MaterialShape.INGOT);
        var meteorite = m(ModMaterials.METEORITE, MaterialShape.INGOT);
        var wBolt = ModItems.BOLT_TUNGSTEN;
        var duraBolt = ModItems.BOLT_HIGHSPEED_STEEL;
        var fiber = m(ModMaterials.FIBERGLASS, MaterialShape.INGOT);
        var dntPowder = m(ModMaterials.DINEUTRONIUM, MaterialShape.POWDER);
        g.shaped(ModItems.BISMUTH_PICKAXE, 1, p(" BM", "BPB", "TB "), 'B', bi, 'M', meteorite, 'P', ModItems.STARMETAL_PICKAXE, 'T', wBolt);
        g.shaped(ModItems.VOLCANIC_PICKAXE, 1, p(" BM", "BPB", "TB "), 'B', ModItems.GEM_VOLCANIC, 'M', meteorite, 'P', ModItems.STARMETAL_PICKAXE, 'T', wBolt);
        g.shaped(ModItems.CHLOROPHYTE_PICKAXE, 1, p(" SD", "APS", "FA "), 'S', ModItems.BLADES_STEEL, 'D', ModItems.POWDER_CHLOROPHYTE, 'A', fiber, 'P', ModItems.BISMUTH_PICKAXE, 'F', duraBolt);
        g.shaped(ModItems.CHLOROPHYTE_PICKAXE, 1, p(" SD", "APS", "FA "), 'S', ModItems.BLADES_STEEL, 'D', ModItems.POWDER_CHLOROPHYTE, 'A', fiber, 'P', ModItems.VOLCANIC_PICKAXE, 'F', duraBolt);
        g.shaped(ModItems.MESE_PICKAXE, 1, p(" SD", "APS", "FA "), 'S', ModItems.BLADES_DESH, 'D', dntPowder, 'A', ModItems.PLATE_PAA, 'P', ModItems.CHLOROPHYTE_PICKAXE, 'F', ModItems.SHIMMER_HANDLE);

        //Super axes
        g.shaped(ModItems.BISMUTH_AXE, 1, p(" BM", "BPB", "TB "), 'B', bi, 'M', meteorite, 'P', ModItems.STARMETAL_AXE, 'T', wBolt);
        g.shaped(ModItems.VOLCANIC_AXE, 1, p(" BM", "BPB", "TB "), 'B', ModItems.GEM_VOLCANIC, 'M', meteorite, 'P', ModItems.STARMETAL_AXE, 'T', wBolt);
        g.shaped(ModItems.CHLOROPHYTE_AXE, 1, p(" SD", "APS", "FA "), 'S', ModItems.BLADES_STEEL, 'D', ModItems.POWDER_CHLOROPHYTE, 'A', fiber, 'P', ModItems.BISMUTH_AXE, 'F', duraBolt);
        g.shaped(ModItems.CHLOROPHYTE_AXE, 1, p(" SD", "APS", "FA "), 'S', ModItems.BLADES_STEEL, 'D', ModItems.POWDER_CHLOROPHYTE, 'A', fiber, 'P', ModItems.VOLCANIC_AXE, 'F', duraBolt);
        g.shaped(ModItems.MESE_AXE, 1, p(" SD", "APS", "FA "), 'S', ModItems.BLADES_DESH, 'D', dntPowder, 'A', ModItems.PLATE_PAA, 'P', ModItems.CHLOROPHYTE_AXE, 'F', ModItems.SHIMMER_HANDLE);

        // Bleirohr (PB.pipe() = das Bleirohr des Materialsystems)
        g.shaped(ModItems.WEAPON_PIPE_LEAD, 1, p("II", " I", " I"), 'I', ModItems.PIPE_LEAD);

        //Configged (GeneralConfig.enableLBSM = false)
        var star = m(ModMaterials.STARMETAL, MaterialShape.INGOT);
        g.shaped(ModItems.STARMETAL_SWORD, 1, p(" I ", " B ", "ISI"), 'I', star, 'S', ModItems.RING_STARMETAL, 'B', ModItems.COBALT_DECORATED_SWORD);
        g.shaped(ModItems.STARMETAL_PICKAXE, 1, p("ISI", " B ", " I "), 'I', star, 'S', ModItems.RING_STARMETAL, 'B', ModItems.COBALT_DECORATED_PICKAXE);
        g.shaped(ModItems.STARMETAL_AXE, 1, p("IS", "IB", " I"), 'I', star, 'S', ModItems.RING_STARMETAL, 'B', ModItems.COBALT_DECORATED_AXE);
        g.shaped(ModItems.STARMETAL_SHOVEL, 1, p("I", "B", "I"), 'I', star, 'B', ModItems.COBALT_DECORATED_SHOVEL);
        g.shaped(ModItems.STARMETAL_HOE, 1, p("IS", " B", " I"), 'I', star, 'S', ModItems.RING_STARMETAL, 'B', ModItems.COBALT_DECORATED_HOE);
        var saBlock = m(ModMaterials.SCHRABIDIUM, MaterialShape.BLOCK);
        var saIngot = m(ModMaterials.SCHRABIDIUM, MaterialShape.INGOT);
        g.shaped(ModItems.SCHRABIDIUM_SWORD, 1, p("I", "W", "S"), 'I', saBlock, 'W', ModItems.DESH_SWORD, 'S', anyPlastic);
        g.shaped(ModItems.SCHRABIDIUM_PICKAXE, 1, p("BSB", " W ", " P "), 'B', ModItems.BLADES_DESH, 'S', saBlock, 'W', ModItems.DESH_PICKAXE, 'P', anyPlastic);
        g.shaped(ModItems.SCHRABIDIUM_AXE, 1, p("BS", "BW", " P"), 'B', ModItems.BLADES_DESH, 'S', saBlock, 'W', ModItems.DESH_AXE, 'P', anyPlastic);
        g.shaped(ModItems.SCHRABIDIUM_SHOVEL, 1, p("B", "W", "P"), 'B', saBlock, 'W', ModItems.DESH_SHOVEL, 'P', anyPlastic);
        g.shaped(ModItems.SCHRABIDIUM_HOE, 1, p("IW", " S", " S"), 'I', saIngot, 'W', ModItems.DESH_HOE, 'S', anyPlastic);

        // CraftingManager
        g.shaped(ModItems.REDSTONE_SWORD, 1, p("R", "R", "S"), 'R', Items.REDSTONE_BLOCK, 'S', stick);
        g.shaped(ModItems.BIG_SWORD, 1, p("QIQ", "QIQ", "GSG"), 'G', Items.GOLD_INGOT, 'S', stick, 'I', Items.IRON_INGOT, 'Q', Items.QUARTZ);

        // CraftingManager: Maschinen-Upgrades (upgrade_nullifier fehlt noch: powder_fire existiert nicht)
        var cuPlate = m(ModMaterials.COPPER, MaterialShape.PLATE);
        var tiPlate = m(ModMaterials.TITANIUM, MaterialShape.PLATE);
        var steelPlate = m(ModMaterials.STEEL, MaterialShape.PLATE);
        g.shaped(ModItems.UPGRADE_SMELTER, 1, p("PHP", "CUC", "DTD"), 'P', cuPlate, 'H', Items.HOPPER, 'C', ModItems.COIL_TUNGSTEN, 'U', ModItems.UPGRADE_TEMPLATE, 'D', ModItems.COIL_COPPER, 'T', com.hbm_m.block.ModBlocks.MACHINE_TRANSFORMER);
        g.shaped(ModItems.UPGRADE_SHREDDER, 1, p("PHP", "CUC", "DTD"), 'P', ModItems.MOTOR, 'H', Items.HOPPER, 'C', ModItems.BLADES_TITANIUM, 'U', ModItems.UPGRADE_SMELTER, 'D', tiPlate, 'T', com.hbm_m.block.ModBlocks.MACHINE_TRANSFORMER);
        g.shaped(ModItems.UPGRADE_CENTRIFUGE, 1, p("PHP", "PUP", "DTD"), 'P', ModItems.CENTRIFUGE_ELEMENT, 'H', Items.HOPPER, 'U', ModItems.UPGRADE_SHREDDER, 'D', anyPlastic, 'T', com.hbm_m.block.ModBlocks.MACHINE_TRANSFORMER);
        ItemStack peroxideBarrel = com.hbm_m.item.liquids.ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.PEROXIDE.getSource(), 1);
        g.shaped(ModItems.UPGRADE_CRYSTALLIZER, 1, p("PHP", "CUC", "DTD"), 'P', net.minecraftforge.common.crafting.StrictNBTIngredient.of(peroxideBarrel), 'H', ModItems.ADVANCED_CIRCUIT, 'C', com.hbm_m.block.ModBlocks.BARREL_STEEL, 'U', ModItems.UPGRADE_CENTRIFUGE, 'D', ModItems.MOTOR, 'T', com.hbm_m.block.ModBlocks.MACHINE_TRANSFORMER);
        g.shaped(ModItems.UPGRADE_SCREM, 1, p("SUS", "SCS", "SUS"), 'S', steelPlate, 'U', ModItems.UPGRADE_TEMPLATE, 'C', m(ModMaterials.XEN, MaterialShape.CRYSTAL));
        g.shaped(ModItems.UPGRADE_GC_SPEED, 1, p("GNG", "RUR", "GMG"), 'R', m(ModMaterials.RUBBER, MaterialShape.INGOT), 'M', ModItems.MOTOR, 'G', ModItems.COIL_GOLD, 'N', m(ModMaterials.NIOBIUM, MaterialShape.INGOT), 'U', ModItems.UPGRADE_TEMPLATE);
        g.shapeless(ModItems.UPGRADE_5G, 1, ModItems.UPGRADE_TEMPLATE, ModItems.GEM_ALEXANDRITE);

        // ItemHot: SmeltingRecipes erhitzen die Barren/Klinge (heatUp), PowderRecipes: Stahl + Kohlestaub
        g.shapeless(m(ModMaterials.STEEL_DUSTED, MaterialShape.INGOT), 1, m(ModMaterials.STEEL, MaterialShape.INGOT), m(ModMaterials.COAL, MaterialShape.POWDER));
        smeltHot(w, m(ModMaterials.CHAINSSTEEL, MaterialShape.INGOT), 0.0F);
        smeltHot(w, m(ModMaterials.METEORITE, MaterialShape.INGOT), 0.0F);
        smeltHot(w, m(ModMaterials.METEORITE_FORGED, MaterialShape.INGOT), 0.0F);
        smeltHot(w, ModItems.BLADE_METEORITE.get(), 0.0F);
        smeltHot(w, m(ModMaterials.STEEL_DUSTED, MaterialShape.INGOT), 1.0F);
        for (var dusted : ModItems.STEEL_DUSTED_INGOTS) smeltHot(w, dusted.get(), 1.0F);

        // Schwertkette: PressRecipes (FLAT), CrystallizerRecipes (baseTime 600, Standardsaeure), BreederRecipes
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.METEORITE_SWORD_HARDENED.get()))
                .stamp(ModTags.Items.STAMPS_FLAT)
                .material(ModItems.METEORITE_SWORD_REFORGED.get())
                .save(w, "meteorite_sword_hardened");
        CrystallizerRecipeBuilder.crystallizerRecipe(ModItems.METEORITE_SWORD_TREATED.get(), 1,
                FluidStack.create(ModFluids.PEROXIDE.getSource(), 500L),
                new ItemStack(ModItems.METEORITE_SWORD_ETCHED.get()), 600, 0F).save(w, "meteorite_sword_etched");
        BreederRecipeBuilder.breederRecipe(Ingredient.of(ModItems.METEORITE_SWORD_ETCHED.get()),
                new ItemStack(ModItems.METEORITE_SWORD_BRED.get()), 1000).save(w, "breeder/meteorite_sword_bred");
    }

    /** Ofen: Gegenstand -> derselbe Gegenstand mit voller Hitze (Forge liest "nbt" im Ergebnisobjekt). */
    private static void smeltHot(Consumer<FinishedRecipe> w, net.minecraft.world.item.Item item, float xp) {
        ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm_m", "smelting/hot_" + key.getPath());
        w.accept(new FinishedRecipe() {
            @Override
            public void serializeRecipeData(com.google.gson.JsonObject json) {
                json.addProperty("category", "misc");
                json.add("ingredient", Ingredient.of(item).toJson());
                com.google.gson.JsonObject result = new com.google.gson.JsonObject();
                result.addProperty("item", key.toString());
                result.addProperty("nbt", "{heat:" + com.hbm_m.item.special.ItemHot.maxHeat() + "}");
                json.add("result", result);
                json.addProperty("experience", xp);
                json.addProperty("cookingtime", 200);
            }
            @Override public ResourceLocation getId() { return id; }
            @Override public net.minecraft.world.item.crafting.RecipeSerializer<?> getType() { return net.minecraft.world.item.crafting.RecipeSerializer.SMELTING_RECIPE; }
            @Override public com.google.gson.JsonObject serializeAdvancement() { return null; }
            @Override public ResourceLocation getAdvancementId() { return null; }
        });
    }

    private static void tools(CraftingGen g, Ingredient stick, Object ingot, Object sword, Object pick, Object axe, Object shovel, Object hoe) {
        g.shaped(sword, 1, p("X", "X", "#"), 'X', ingot, '#', stick);
        g.shaped(pick, 1, p("XXX", " # ", " # "), 'X', ingot, '#', stick);
        g.shaped(axe, 1, p("XX", "X#", " #"), 'X', ingot, '#', stick);
        g.shaped(shovel, 1, p("X", "#", "#"), 'X', ingot, '#', stick);
        g.shaped(hoe, 1, p("XX", " #", " #"), 'X', ingot, '#', stick);
    }
}
//?}
