package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;
import com.hbm_m.lib.RefStrings;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.function.Consumer;

public final class CentrifugeRecipeGenerator {

    private CentrifugeRecipeGenerator() {
    }

    public static void generate(Consumer<FinishedRecipe> writer) {
        registerOreRecipes(writer);
        registerCrystalRecipes(writer);
        registerBedrockRecipes(writer);
    }

    private static void registerOreRecipes(Consumer<FinishedRecipe> writer) {
        // Vanilla Ores
        tagRecipe(writer, "coal_ore", "forge:ores/coal",
                modPowderCount(ModPowders.COAL, 2), modPowderCount(ModPowders.COAL, 2), modPowderCount(ModPowders.COAL, 2), gravel());
        tagRecipe(writer, "iron_ore", "forge:ores/iron",
                modPowder(ModPowders.IRON), modPowder(ModPowders.IRON), modPowder(ModPowders.IRON), gravel());
        tagRecipe(writer, "gold_ore", "forge:ores/gold",
                modPowder(ModPowders.GOLD), modPowder(ModPowders.GOLD), modPowder(ModPowders.GOLD), gravel());
        tagRecipe(writer, "copper_ore", "forge:ores/copper",
                modPowder(ModPowders.COPPER), modPowder(ModPowders.COPPER), modPowder(ModPowders.GOLD), gravel());
        tagRecipe(writer, "diamond_ore", "forge:ores/diamond",
                modPowder(ModPowders.DIAMOND), modPowder(ModPowders.DIAMOND), modPowder(ModPowders.DIAMOND), gravel());
        tagRecipe(writer, "emerald_ore", "forge:ores/emerald",
                modPowder(ModPowders.EMERALD), modPowder(ModPowders.EMERALD), modPowder(ModPowders.EMERALD), gravel());
        // tagRecipe(writer, "redstone_ore", "forge:ores/redstone",
        //         stack(Items.REDSTONE, 3), stack(Items.REDSTONE, 3), ingot(ModIngots.MERCURY, 1), gravel());
        tagRecipe(writer, "lapis_ore", "forge:ores/lapis",
                modPowderCount(ModPowders.LAPIS, 6), ingotPowderCount(ModIngots.COBALT, 1), stack(ModItems.GEM_SODALITE.get(), 1), gravel());
        tagRecipe(writer, "quartz_ore", "forge:ores/quartz",
                modPowder(ModPowders.QUARTZ), modPowder(ModPowders.QUARTZ), ingotPowder(ModIngots.LITHIUM_INGOT), stack(Items.NETHERRACK, 1));

        // HBM Ores
        tagRecipe(writer, "uranium_ore", "forge:ores/uranium",
                ingotPowder(ModIngots.URANIUM), ingotPowder(ModIngots.URANIUM), ingot(ModIngots.RA226, 1), gravel());
        tagRecipe(writer, "thorium_ore", "forge:ores/thorium",
                ingotPowder(ModIngots.THORIUM), ingotPowder(ModIngots.THORIUM), ingotPowder(ModIngots.URANIUM), gravel());
        tagRecipe(writer, "titanium_ore", "forge:ores/titanium",
                ingotPowder(ModIngots.TITANIUM), ingotPowder(ModIngots.TITANIUM), modPowder(ModPowders.IRON), gravel());
        tagRecipe(writer, "tungsten_ore", "forge:ores/tungsten",
                ingotPowder(ModIngots.TUNGSTEN), ingotPowder(ModIngots.TUNGSTEN), modPowder(ModPowders.IRON), gravel());
        tagRecipe(writer, "lead_ore", "forge:ores/lead",
                ingotPowder(ModIngots.LEAD), ingotPowder(ModIngots.LEAD), modPowder(ModPowders.GOLD), gravel());
        tagRecipe(writer, "beryllium_ore", "forge:ores/beryllium",
                ingotPowder(ModIngots.BERYLLIUM), ingotPowder(ModIngots.BERYLLIUM), stack(Items.EMERALD, 1), gravel());
        tagRecipe(writer, "fluorite_ore", "forge:ores/fluorite",
                stack(ModItems.FLUORITE.get(), 3), stack(ModItems.FLUORITE.get(), 3), stack(ModItems.RAREGROUND_ORE_CHUNK.get(), 1), gravel());
        tagRecipe(writer, "aluminum_ore", "forge:ores/aluminum",
                ingotPowder(ModIngots.ALUMINUM), ingotPowder(ModIngots.ALUMINUM), ingotPowder(ModIngots.TITANIUM), gravel());
        tagRecipe(writer, "sulfur_ore", "forge:ores/sulfur",
                stack(ModItems.SULFUR.get(), 4), stack(ModItems.SULFUR.get(), 4), stack(ModItems.SULFUR.get(), 4), gravel());
        tagRecipe(writer, "lignite_ore", "forge:ores/lignite",
                stack(ModItems.LIGNITE.get(), 2), stack(ModItems.LIGNITE.get(), 2), stack(ModItems.LIGNITE.get(), 2), gravel());
        tagRecipe(writer, "asbestos_ore", "forge:ores/asbestos",
                ingotPowder(ModIngots.ASBESTOS), ingotPowder(ModIngots.ASBESTOS), ingotPowder(ModIngots.ASBESTOS), gravel());
        tagRecipe(writer, "cinnabar_ore", "forge:ores/cinnabar",
                stack(ModItems.CINNABAR.get(), 2), stack(ModItems.CINNABAR.get(), 2), stack(ModItems.SULFUR.get(), 1), gravel());
        tagRecipe(writer, "rareground_ore", "forge:ores/rareground",
                stack(ModItems.RAREGROUND_ORE_CHUNK.get(), 1), stack(ModItems.RAREGROUND_ORE_CHUNK.get(), 1), stack(ModItems.DUST.get(), 2), gravel());
        tagRecipe(writer, "cobalt_ore", "forge:ores/cobalt",
                ingotPowderCount(ModIngots.COBALT, 2), modPowder(ModPowders.IRON), modPowder(ModPowders.COPPER), gravel());
    }

    private static void registerCrystalRecipes(Consumer<FinishedRecipe> writer) {
        itemRecipe(writer, "crystal_coal", ModItems.CRYSTAL_COAL.get(),
                modPowderCount(ModPowders.COAL, 3), modPowderCount(ModPowders.COAL, 3), modPowderCount(ModPowders.COAL, 3), ingotPowder(ModIngots.LITHIUM_INGOT));
        itemRecipe(writer, "crystal_iron", ModItems.CRYSTAL_IRON.get(),
                modPowderCount(ModPowders.IRON, 2), modPowderCount(ModPowders.IRON, 2), ingotPowder(ModIngots.TITANIUM), ingotPowder(ModIngots.LITHIUM_INGOT));
        itemRecipe(writer, "crystal_gold", ModItems.CRYSTAL_GOLD.get(),
                modPowderCount(ModPowders.GOLD, 2), modPowderCount(ModPowders.GOLD, 2), modPowderCount(ModPowders.GOLD, 2), gravel());
        // itemRecipe(writer, "crystal_redstone", ModItems.CRYSTAL_REDSTONE.get(),
        //         stack(Items.REDSTONE, 3), stack(Items.REDSTONE, 3), stack(Items.REDSTONE, 3), ingot(ModIngots.MERCURY, 3));
        itemRecipe(writer, "crystal_lapis", ModItems.CRYSTAL_LAPIS.get(),
                modPowderCount(ModPowders.LAPIS, 4), modPowderCount(ModPowders.LAPIS, 4), ingotPowder(ModIngots.COBALT), stack(ModItems.GEM_SODALITE.get(), 2));
        itemRecipe(writer, "crystal_diamond", ModItems.CRYSTAL_DIAMOND.get(),
                modPowder(ModPowders.DIAMOND), modPowder(ModPowders.DIAMOND), modPowder(ModPowders.DIAMOND), modPowder(ModPowders.DIAMOND));
        itemRecipe(writer, "crystal_uranium", ModItems.CRYSTAL_URANIUM.get(),
                ingotPowderCount(ModIngots.URANIUM, 2), ingotPowderCount(ModIngots.URANIUM, 2), ingot(ModIngots.RA226, 2), ingotPowder(ModIngots.LITHIUM_INGOT));
        itemRecipe(writer, "crystal_thorium", ModItems.CRYSTAL_THORIUM.get(),
                ingotPowderCount(ModIngots.THORIUM, 2), ingotPowderCount(ModIngots.THORIUM, 2), ingotPowder(ModIngots.URANIUM), ingot(ModIngots.RA226, 1));
        itemRecipe(writer, "crystal_plutonium", ModItems.CRYSTAL_PLUTONIUM.get(),
                ingotPowderCount(ModIngots.PLUTONIUM, 2), ingotPowderCount(ModIngots.PLUTONIUM, 2), ingotPowder(ModIngots.POLONIUM), ingotPowder(ModIngots.LITHIUM_INGOT));
        itemRecipe(writer, "crystal_titanium", ModItems.CRYSTAL_TITANIUM.get(),
                ingotPowderCount(ModIngots.TITANIUM, 2), ingotPowderCount(ModIngots.TITANIUM, 2), modPowder(ModPowders.IRON), ingotPowder(ModIngots.LITHIUM_INGOT));
        // itemRecipe(writer, "crystal_sulfur", ModItems.CRYSTAL_SULFUR.get(),
        //         stack(ModItems.SULFUR.get(), 4), stack(ModItems.SULFUR.get(), 4), modPowder(ModPowders.IRON), ingot(ModIngots.MERCURY, 1));
        itemRecipe(writer, "crystal_copper", ModItems.CRYSTAL_COPPER.get(),
                modPowderCount(ModPowders.COPPER, 2), modPowderCount(ModPowders.COPPER, 2), stack(ModItems.SULFUR.get(), 1), ingotPowder(ModIngots.COBALT));
        itemRecipe(writer, "crystal_tungsten", ModItems.CRYSTAL_TUNGSTEN.get(),
                ingotPowderCount(ModIngots.TUNGSTEN, 2), ingotPowderCount(ModIngots.TUNGSTEN, 2), modPowder(ModPowders.IRON), ingotPowder(ModIngots.LITHIUM_INGOT));
        itemRecipe(writer, "crystal_aluminium", ModItems.CRYSTAL_ALUMINIUM.get(),
                ingotPowderCount(ModIngots.ALUMINUM, 2), ingotPowder(ModIngots.TITANIUM), modPowder(ModPowders.IRON), ingotPowder(ModIngots.LITHIUM_INGOT));
        itemRecipe(writer, "crystal_fluorite", ModItems.CRYSTAL_FLUORITE.get(),
                stack(ModItems.FLUORITE.get(), 4), stack(ModItems.FLUORITE.get(), 4), stack(ModItems.GEM_SODALITE.get(), 2), ingotPowder(ModIngots.LITHIUM_INGOT));
        itemRecipe(writer, "crystal_beryllium", ModItems.CRYSTAL_BERYLLIUM.get(),
                ingotPowderCount(ModIngots.BERYLLIUM, 2), ingotPowderCount(ModIngots.BERYLLIUM, 2), modPowder(ModPowders.QUARTZ), ingotPowder(ModIngots.LITHIUM_INGOT));
        itemRecipe(writer, "crystal_lead", ModItems.CRYSTAL_LEAD.get(),
                ingotPowderCount(ModIngots.LEAD, 2), ingotPowderCount(ModIngots.LEAD, 2), modPowder(ModPowders.GOLD), ingotPowder(ModIngots.LITHIUM_INGOT));
        itemRecipe(writer, "crystal_schrabidium", ModItems.CRYSTAL_SCHRABIDIUM.get(),
                ingotPowderCount(ModIngots.SCHRABIDIUM, 2), ingotPowderCount(ModIngots.SCHRABIDIUM, 2), ingotPowder(ModIngots.PLUTONIUM), ingotPowder(ModIngots.LITHIUM_INGOT));
        itemRecipe(writer, "crystal_lithium", ModItems.CRYSTAL_LITHIUM.get(),
                ingotPowderCount(ModIngots.LITHIUM_INGOT, 2), ingotPowderCount(ModIngots.LITHIUM_INGOT, 2), modPowder(ModPowders.QUARTZ), stack(ModItems.FLUORITE.get(), 1));
        itemRecipe(writer, "crystal_cobalt", ModItems.CRYSTAL_COBALT.get(),
                ingotPowderCount(ModIngots.COBALT, 2), modPowderCount(ModPowders.IRON, 3), modPowderCount(ModPowders.COPPER, 3), ingotPowder(ModIngots.LITHIUM_INGOT));
    }

    private static void registerBedrockRecipes(Consumer<FinishedRecipe> writer) {
        // Light
        addBedrockPhase(writer, "light", ModItems.BEDROCK_ORE_BASE_LIGHT.get(), ModItems.BEDROCK_ORE_PRIMARY_LIGHT.get(), stack(Items.RAW_IRON, 9), stack(Items.RAW_COPPER, 9), ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_LIGHT.get(), ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_LIGHT.get(), ModItems.BEDROCK_ORE_RAD_BYPRODUCT_LIGHT.get(), ModItems.BEDROCK_ORE_CRUMBS_LIGHT.get());
        itemRecipe(writer, "bedrock_sulfuric_washed_light", ModItems.BEDROCK_ORE_SULFURIC_WASHED_LIGHT.get(), stack(ModItems.TITANIUM_RAW.get(), 6), stack(ModBlocks.RESOURCE_BAUXITE.get(), 9), stack(ModItems.CRYOLITE.get(), 3));
        itemRecipe(writer, "bedrock_solvent_washed_light", ModItems.BEDROCK_ORE_SOLVENT_WASHED_LIGHT.get(), stack(ModItems.POWDER_CHLOROCALCITE.get(), 5), ingot(ModIngots.LITHIUM_INGOT, 5), stack(ModItems.POWDER_SODIUM.get(), 3));
        itemRecipe(writer, "bedrock_rad_washed_light", ModItems.BEDROCK_ORE_RAD_WASHED_LIGHT.get(), stack(ModItems.POWDER_CHLOROCALCITE.get(), 6), ingot(ModIngots.LITHIUM_INGOT, 6), stack(ModItems.POWDER_SODIUM.get(), 6));

        // Heavy
        addBedrockPhase(writer, "heavy", ModItems.BEDROCK_ORE_BASE_HEAVY.get(), ModItems.BEDROCK_ORE_PRIMARY_HEAVY.get(), stack(ModItems.TUNGSTEN_RAW.get(), 9), stack(ModItems.LEAD_RAW.get(), 9), ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_HEAVY.get(), ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_HEAVY.get(), ModItems.BEDROCK_ORE_RAD_BYPRODUCT_HEAVY.get(), ModItems.BEDROCK_ORE_CRUMBS_HEAVY.get());
        itemRecipe(writer, "bedrock_sulfuric_washed_heavy", ModItems.BEDROCK_ORE_SULFURIC_WASHED_HEAVY.get(), stack(Items.RAW_GOLD, 2), stack(Items.RAW_GOLD, 2), stack(ModItems.BERYLLIUM_RAW.get(), 3));
        itemRecipe(writer, "bedrock_solvent_washed_heavy", ModItems.BEDROCK_ORE_SOLVENT_WASHED_HEAVY.get(), stack(ModItems.TUNGSTEN_RAW.get(), 9), stack(ModItems.LEAD_RAW.get(), 9), stack(Items.RAW_GOLD, 5));
        itemRecipe(writer, "bedrock_rad_washed_heavy", ModItems.BEDROCK_ORE_RAD_WASHED_HEAVY.get(), ingot(ModIngots.BISMUTH, 2), ingot(ModIngots.TANTALIUM, 2), stack(Items.RAW_GOLD, 6));

        // Rare
        addBedrockPhase(writer, "rare", ModItems.BEDROCK_ORE_BASE_RARE.get(), ModItems.BEDROCK_ORE_PRIMARY_RARE.get(), stack(ModItems.COBALT_RAW.get(), 5), stack(ModItems.RAREEARTH_RAW.get(), 5), ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_RARE.get(), ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_RARE.get(), ModItems.BEDROCK_ORE_RAD_BYPRODUCT_RARE.get(), ModItems.BEDROCK_ORE_CRUMBS_RARE.get());
        itemRecipe(writer, "bedrock_sulfuric_washed_rare", ModItems.BEDROCK_ORE_SULFURIC_WASHED_RARE.get(), ingot(ModIngots.BORON, 5), ingot(ModIngots.LANTHANIUM, 3), ingot(ModIngots.NIOBIUM, 4));
        itemRecipe(writer, "bedrock_solvent_washed_rare", ModItems.BEDROCK_ORE_SOLVENT_WASHED_RARE.get(), ingot(ModIngots.NEODYMIUM, 3), ingot(ModIngots.STRONTIUM, 3), ingot(ModIngots.ZIRCONIUM, 3));
        itemRecipe(writer, "bedrock_rad_washed_rare", ModItems.BEDROCK_ORE_RAD_WASHED_RARE.get(), ingot(ModIngots.NIOBIUM, 5), ingot(ModIngots.NEODYMIUM, 5), ingot(ModIngots.STRONTIUM, 3));

        // Actinide
        addBedrockPhase(writer, "actinide", ModItems.BEDROCK_ORE_BASE_ACTINIDE.get(), ModItems.BEDROCK_ORE_PRIMARY_ACTINIDE.get(), stack(ModItems.URANIUM_RAW.get(), 4), stack(ModItems.THORIUM_RAW.get(), 4), ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_ACTINIDE.get(), ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_ACTINIDE.get(), ModItems.BEDROCK_ORE_RAD_BYPRODUCT_ACTINIDE.get(), ModItems.BEDROCK_ORE_CRUMBS_ACTINIDE.get());
        itemRecipe(writer, "bedrock_sulfuric_washed_actinide", ModItems.BEDROCK_ORE_SULFURIC_WASHED_ACTINIDE.get(), stack(ModItems.RADIUM_RAW.get(), 2), stack(ModItems.BILLET_POLONIUM.get(), 1));
        itemRecipe(writer, "bedrock_solvent_washed_actinide", ModItems.BEDROCK_ORE_SOLVENT_WASHED_ACTINIDE.get(), stack(ModItems.RADIUM_RAW.get(), 2), stack(ModItems.BILLET_POLONIUM.get(), 1));
        itemRecipe(writer, "bedrock_rad_washed_actinide", ModItems.BEDROCK_ORE_RAD_WASHED_ACTINIDE.get(), stack(ModItems.BILLET_TECHNETIUM.get(), 2), stack(ModItems.BILLET_U238.get(), 1));

        // Nonmetal
        addBedrockPhase(writer, "nonmetal", ModItems.BEDROCK_ORE_BASE_NONMETAL.get(), ModItems.BEDROCK_ORE_PRIMARY_NONMETAL.get(), stack(Items.COAL, 9), stack(ModItems.SULFUR.get(), 9), ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_NONMETAL.get(), ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_NONMETAL.get(), ModItems.BEDROCK_ORE_RAD_BYPRODUCT_NONMETAL.get(), ModItems.BEDROCK_ORE_CRUMBS_NONMETAL.get());
        itemRecipe(writer, "bedrock_sulfuric_washed_nonmetal", ModItems.BEDROCK_ORE_SULFURIC_WASHED_NONMETAL.get(), stack(ModItems.LIGNITE.get(), 9), stack(ModItems.SALTPETER.get(), 6), stack(ModItems.FLUORITE.get(), 6));
        itemRecipe(writer, "bedrock_solvent_washed_nonmetal", ModItems.BEDROCK_ORE_SOLVENT_WASHED_NONMETAL.get(), stack(ModItems.CRYSTAL_PHOSPHORUS.get(), 5), stack(ModItems.FLUORITE.get(), 6), stack(ModItems.SULFUR.get(), 6));
        itemRecipe(writer, "bedrock_rad_washed_nonmetal", ModItems.BEDROCK_ORE_RAD_WASHED_NONMETAL.get(), stack(ModItems.POWDER_CHLOROCALCITE.get(), 6), ingot(ModIngots.SILICON, 2), ingot(ModIngots.SILICON, 2));

        // Crystal
        addBedrockPhase(writer, "crystal", ModItems.BEDROCK_ORE_BASE_CRYSTAL.get(), ModItems.BEDROCK_ORE_PRIMARY_CRYSTAL.get(), stack(Items.REDSTONE, 9), stack(ModItems.CINNABAR.get(), 4), ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_CRYSTAL.get(), ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_CRYSTAL.get(), ModItems.BEDROCK_ORE_RAD_BYPRODUCT_CRYSTAL.get(), ModItems.BEDROCK_ORE_CRUMBS_CRYSTAL.get());
        itemRecipe(writer, "bedrock_sulfuric_washed_crystal", ModItems.BEDROCK_ORE_SULFURIC_WASHED_CRYSTAL.get(), stack(ModItems.GEM_SODALITE.get(), 9), ingot(ModIngots.ASBESTOS, 6), stack(Items.DIAMOND, 3));
        itemRecipe(writer, "bedrock_solvent_washed_crystal", ModItems.BEDROCK_ORE_SOLVENT_WASHED_CRYSTAL.get(), stack(ModItems.CINNABAR.get(), 3), ingot(ModIngots.ASBESTOS, 5), stack(Items.EMERALD, 3));
        itemRecipe(writer, "bedrock_rad_washed_crystal", ModItems.BEDROCK_ORE_RAD_WASHED_CRYSTAL.get(), stack(ModItems.BORAX.get(), 3), stack(ModItems.MOLYSITE.get(), 3), stack(ModItems.GEM_SODALITE.get(), 9));

        // Misc
        itemRecipe(writer, "blaze_rod", Items.BLAZE_ROD, stack(Items.BLAZE_POWDER, 1), stack(Items.BLAZE_POWDER, 1), stack(ModItems.FIRE_POWDER.get(), 1), stack(ModItems.FIRE_POWDER.get(), 1));
    }

    private static void addBedrockPhase(Consumer<FinishedRecipe> writer, String name, ItemLike base, ItemLike primary, ItemStack out1, ItemStack out2, ItemLike byproductS, ItemLike byproductSol, ItemLike byproductRad, ItemLike crumbs) {
        itemRecipe(writer, "bedrock_base_" + name, base, stack(primary, 1), gravel());
        itemRecipe(writer, "bedrock_primary_" + name, primary, out1, out2);
        itemRecipe(writer, "bedrock_primary_sulfuric_" + name, getSubItem(primary, "sulfuric"), stack(getSubItem(primary, "nosulfuric"), 2), stack(byproductS, 2));
        itemRecipe(writer, "bedrock_primary_solvent_" + name, getSubItem(primary, "solvent"), stack(getSubItem(primary, "nosolvent"), 2), stack(byproductS, 2), stack(byproductSol, 2));
        itemRecipe(writer, "bedrock_primary_rad_" + name, getSubItem(primary, "rad"), stack(getSubItem(primary, "norad"), 2), stack(byproductS, 2), stack(byproductSol, 2), stack(byproductRad, 2));
        
        ItemStack mOut1_18 = out1.copy(); mOut1_18.setCount(18);
        ItemStack mOut1_9 = out1.copy(); mOut1_9.setCount(9);
        ItemStack mOut2_18 = out2.copy(); mOut2_18.setCount(18);
        ItemStack mOut2_9 = out2.copy(); mOut2_9.setCount(9);
        
        itemRecipe(writer, "bedrock_primary_first_" + name, getSubItem(primary, "first"), mOut1_18, mOut2_9, stack(crumbs, 1));
        itemRecipe(writer, "bedrock_primary_second_" + name, getSubItem(primary, "second"), mOut1_9, mOut2_18, stack(crumbs, 1));
    }

    private static Item getSubItem(ItemLike parent, String sub) {
        String path = BuiltInRegistries.ITEM.getKey(parent.asItem()).getPath();
        ResourceLocation id = BaseRecipeBuilder.resLoc(path.replace("_primary_", "_primary_" + sub + "_"));
        return BuiltInRegistries.ITEM.get(id);
    }

    private static void tagRecipe(Consumer<FinishedRecipe> writer, String name, String tag, ItemStack... outputs) {
        CentrifugeRecipeBuilder.tagRecipe(tag, outputs).save(writer, recipeId(name));
    }
    
    private static void itemRecipe(Consumer<FinishedRecipe> writer, String name, ItemLike input, ItemStack... outputs) {
        CentrifugeRecipeBuilder.itemRecipe(input.asItem(), outputs).save(writer, recipeId(name));
    }

    private static String recipeId(String name) {
        return "centrifuge/" + name;
    }

    private static ItemStack gravel() {
        return new ItemStack(Items.GRAVEL);
    }

    private static ItemStack stack(ItemLike item, int count) {
        return new ItemStack(item, count);
    }

    private static ItemStack modPowder(ModPowders powder) {
        return modPowderCount(powder, 1);
    }

    private static ItemStack modPowderCount(ModPowders powder, int count) {
        var supplier = ModItems.getPowders(powder);
        return (supplier != null && supplier.isPresent()) ? new ItemStack(supplier.get(), count) : ItemStack.EMPTY;
    }

    private static ItemStack ingotPowder(ModIngots ingot) {
        return ingotPowderCount(ingot, 1);
    }

    private static ItemStack ingotPowderCount(ModIngots ingot, int count) {
        var supplier = ModItems.getPowder(ingot);
        return (supplier != null && supplier.isPresent()) ? new ItemStack(supplier.get(), count) : ItemStack.EMPTY;
    }
    
    private static ItemStack ingot(ModIngots ingot, int count) {
        var supplier = ModItems.getIngot(ingot);
        return (supplier != null && supplier.isPresent()) ? new ItemStack(supplier.get(), count) : ItemStack.EMPTY;
    }
}
//?}